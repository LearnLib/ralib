/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.theory.inequality;

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toExpression;
import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.EquivalenceClassFilter;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3Solver;
import net.automatalib.word.Word;

/**
 *
 * @author Sofia Cassel
 * @author Fredrik Tåquist
 * @param<T>
 */
public abstract class InequalityTheoryWithEq<T> implements Theory<T> {

    boolean useSuffixOpt = false;

    /**
     * Given a prefix and a potential, generate data values for each equivalence class.
     *
     * @param prefix
     * @param suffixValue
     * @param potValuation
     * @param consts
     * @return A mapping from data values to their corresponding SDT guards
     */
    private Map<DataValue<T>, SDTGuard> generateEquivClasses(Word<PSymbolInstance> prefix,
    		SuffixValue suffixValue,
    		Map<DataValue<T>, SymbolicDataValue> potValuation,
    		Constants consts) {

    	Map<DataValue<T>, SDTGuard> valueGuards = new LinkedHashMap<>();

    	if (potValuation.isEmpty()) {
    		DataValue<T> fresh = getFreshValue(new ArrayList<>());
    		valueGuards.put(fresh, new SDTTrueGuard(suffixValue));
    		return valueGuards;
    	}
        int usedVals = potValuation.size();
        List<DataValue<T>> potential = new ArrayList<>();
        potential.addAll(potValuation.keySet());
        List<DataValue<T>> sortedPot = sort(potential);

        Valuation vals = new Valuation();
        for (Map.Entry<DataValue<T>, SymbolicDataValue> pot : potValuation.entrySet()) {
            SymbolicDataValue r = pot.getValue();
            DataValue<T> dv = pot.getKey();
            // TODO: fix unchecked invocation
            vals.setValue(toVariable(r), dv.getId());
        }

        // smallest
        DataValue<T> dl = sortedPot.get(0);
        SymbolicDataValue rl = potValuation.get(dl);
        IntervalGuard sg = new IntervalGuard(suffixValue, null, rl);
        DataValue<T> smallest = instantiate(sg, vals, consts, sortedPot);
        valueGuards.put(smallest, sg);

        for (int i = 1; i < usedVals; i++) {
            // equality
            EqualityGuard eg = new EqualityGuard(suffixValue, rl);
            valueGuards.put(dl, eg);

            // interval
            DataValue<T> dr = sortedPot.get(i);
            SymbolicDataValue rr = potValuation.get(dr);
            IntervalGuard ig = new IntervalGuard(suffixValue, rl, rr);
            DataValue<T> di = instantiate(ig, vals, consts, sortedPot);
            valueGuards.put(di, ig);

            dl = dr;
            rl = rr;
        }
        EqualityGuard eg = new EqualityGuard(suffixValue, rl);
        valueGuards.put(dl, eg);

        // greatest
        IntervalGuard gg = new IntervalGuard(suffixValue, rl, null);
        DataValue<T> dg = instantiate(gg, vals, consts, sortedPot);
        valueGuards.put(dg, gg);

        return valueGuards;
    }

    /**
     * Filter out equivalence classes that are to be removed through suffix optimization.
     *
     * @param valueGuards - a mapping between data values and corresponding SDT guards
     * @param prefix - the prefix
     * @param suffix - the suffix
     * @param suffixValue - the suffix value for which to apply optimizations
     * @param potValuation - potential
     * @param suffixVals - suffix valuation
     * @param consts - constants
     * @param values - word valuation
     * @return valueGuards without data values that are filtered out due to optimizations
     */
	private Map<DataValue<T>, SDTGuard> filterEquivClasses(Map<DataValue<T>, SDTGuard> valueGuards,
			Word<PSymbolInstance> prefix,
			SymbolicSuffix suffix,
			SuffixValue suffixValue,
			Map<DataValue<T>, SymbolicDataValue> potValuation,
			SuffixValuation suffixVals,
			Constants consts,
			WordValuation values) {
		List<DataValue<T>> equivClasses = new ArrayList<>();
		equivClasses.addAll(valueGuards.keySet());
		EquivalenceClassFilter<T> eqcFilter = new EquivalenceClassFilter<>(equivClasses, useSuffixOpt);
		List<DataValue<T>> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(suffixValue), prefix, suffix.getActions(), values);

		Map<DataValue<T>, SDTGuard> ret = new LinkedHashMap<>();
		for (Map.Entry<DataValue<T>, SDTGuard> e : valueGuards.entrySet()) {
			DataValue<T> ec = e.getKey();
			if (filteredEquivClasses.contains(ec)) {
				ret.put(ec, e.getValue());
			}
		}
		return ret;
	}

	private List<DataValue<T>> sort(Collection<DataValue<T>> list) {
		List<DataValue<T>> sorted = new ArrayList<>();
		sorted.addAll(list);
		sorted.sort(getComparator());
		return sorted;
	}

	protected abstract Comparator<DataValue<T>> getComparator();

	/**
	 * Merge SDT guards corresponding to data values representing equivalence classes from left (corresponding
	 * to lower data values) to right. Guards will be be merged if their respective sub-SDTs are equivalent,
	 * taking into account cases where two SDTs can be made equivalent by imposing equality. Any guards
	 * corresponding to equivalence classes that have been removed through suffix optimization will not be
	 * merged, but will instead be removed.
	 * If the merging results in only a single guard, it will be replaced by a True guard.
	 *
	 * @param sdts - a mapping from SDT guards to their corresponding sub-SDTs
	 * @param equivClasses - a mapping from data values to corresponding SDT guards
	 * @param filteredOut - data values removed through suffix optimization
	 * @return a mapping from merged SDT guards to their respective sub-trees
	 */
	protected Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> sdts,
			Map<DataValue<T>, SDTGuard> equivClasses,
			Collection<DataValue<T>> filteredOut) {
		Map<SDTGuard, SDT> merged = new LinkedHashMap<>();

		List<DataValue<T>> ecValuesSorted = sort(equivClasses.keySet());

		// merge guards from left (lesser values) to right (greater values)
		// stop merging when reaching
		// i) a value that is filtered out, or
		// ii) an inequivalent sub-tree
		SDT currSdt = null;
		SDTGuard currGuard = null;
		SDTGuard currMerged = null;
		SDTGuard prevGuard = null;
		SDT prevSdt = null;
		for (DataValue<T> nextDv : ecValuesSorted) {
			if (filteredOut.contains(nextDv)) {
				// stop merging if next guard was filtered out
				if (currGuard != null) {
					assert currMerged != null;
					assert currSdt != null;
					merged.put(currMerged, currSdt);
				}
				currSdt = null;
				currGuard = null;
				currMerged = null;
				prevGuard = null;
				prevSdt = null;
				continue;
			} else {
				boolean keepMerging = true;
				SDTGuard nextGuard = equivClasses.get(nextDv);
				SDT nextSdt = sdts.get(nextGuard);
				if (currSdt == null) {
					// this is the first guard of the run
					currGuard = nextGuard;
					currMerged = nextGuard;
					currSdt = nextSdt;
					continue;
				} else {
					if (equivalentWithRenaming(currSdt, currGuard, nextSdt, nextGuard)) {
						// if left guard is equality, check for equality with previous guard
						if (currGuard instanceof EqualityGuard && prevGuard != null &&
								!equivalentWithRenaming(prevSdt, prevGuard, nextSdt, nextGuard)) {
							keepMerging = false;
						}
					} else {
						keepMerging = false;
					}
				}

				if (keepMerging) {
					currMerged = mergeIntervals(currMerged, nextGuard);
					prevGuard = currGuard;
					prevSdt = currSdt;
				} else {
					assert currMerged != null;
					assert currSdt != null;
					merged.put(currMerged, currSdt);
					currMerged = nextGuard;
					currSdt = nextSdt;
					prevGuard = null;
					prevSdt = null;
				}
				currGuard = nextGuard;
			}
		}
		if (currMerged != null) {
			merged.put(currMerged, currSdt);
		}

		// check for disequality guard (i.e., both s < r and s > r guards are present for some r)
		merged = checkForDisequality(merged);

		// if only one guard, replace with true guard
		if (merged.size() == 1) {
			Map.Entry<SDTGuard, SDT> entry = merged.entrySet().iterator().next();
			SDTGuard g = entry.getKey();
			if (g instanceof DisequalityGuard || (g instanceof IntervalGuard && ((IntervalGuard) g).isBiggerGuard())) {
				merged = new LinkedHashMap<>();
				merged.put(new SDTTrueGuard(g.getParameter()), entry.getValue());
			}
		}

		assert !merged.isEmpty();

		return merged;
	}

	/**
	 * Check whether two SDTs are equivalent. If one of the SDT guards is an equality guard, check whether
	 * the SDT corresponding to the other guard is equivalent to the SDT corresponding to the equality guard
	 * under the restriction of that equality.
	 *
	 * @param sdt1
	 * @param guard1
	 * @param sdt2
	 * @param guard2
	 * @return true if sdt1 is equivalent to sdt2, or can be under equality guard2, or vice versa
	 */
	private boolean equivalentWithRenaming(SDT sdt1, SDTGuard guard1, SDT sdt2, SDTGuard guard2) {
		if (guard1 != null && guard1 instanceof EqualityGuard) {
			Expression<Boolean> renaming = toExpression(guard1.toExpr());
			return sdt1.isEquivalentUnderCondition(sdt2, renaming, getSolver());
		} else if (guard2 != null && guard2 instanceof EqualityGuard) {
			Expression<Boolean> renaming = toExpression(guard2.toExpr());
			return sdt2.isEquivalentUnderCondition(sdt1, renaming, getSolver());
		}
		return sdt1.isEquivalent(sdt2, new VarMapping<>());
	}

	/**
	 * Merge two SDT guards from left to right. Exactly one of the guards must include an equality such that
	 * there is either an equality on the right register of left guard or an equality on the left register
	 * of the right guard. In addition, the right register of the left guard must match the left register
	 * of the right guard. If these conditions are met, the guards will be merged. For example, a guard
	 * (r1 < s1 <= r2) can be merged with (r2 < s1 < r3), producing the merged guard (r1 < s1 < r3).
	 * Similarly, (s1 < r1) can be merged with (r1 == s1), producing the merged guard (s1 <= r1).
	 *
	 * @param leftGuard
	 * @param rightGuard
	 * @return leftGuard merged with rightGuard
	 */
	private SDTGuard mergeIntervals(SDTGuard leftGuard, SDTGuard rightGuard) {
		SuffixValue suffixValue = leftGuard.getParameter();
		if (leftGuard instanceof EqualityGuard) {
			EqualityGuard egLeft = (EqualityGuard) leftGuard;
			SymbolicDataValue rl = egLeft.getRegister();
			if (rightGuard instanceof IntervalGuard) {
				IntervalGuard igRight = (IntervalGuard) rightGuard;
				if (!igRight.isSmallerGuard() && igRight.getLeftReg().equals(rl)) {
					if (igRight.isBiggerGuard()) {
						return IntervalGuard.greaterOrEqualGuard(suffixValue, rl);
					} else {
						return new IntervalGuard(suffixValue, rl, igRight.getRightReg(), true, false);
					}
				}
			}
		} else if (leftGuard instanceof IntervalGuard && !((IntervalGuard) leftGuard).isBiggerGuard()) {
			IntervalGuard igLeft = (IntervalGuard) leftGuard;
			SymbolicDataValue rr = igLeft.getRightReg();
			if (igLeft.isSmallerGuard()) {
				if (rightGuard instanceof EqualityGuard && ((EqualityGuard) rightGuard).getRegister().equals(rr)) {
					return IntervalGuard.lessOrEqualGuard(suffixValue, rr);
				} else if (rightGuard instanceof IntervalGuard &&
						!((IntervalGuard) rightGuard).isSmallerGuard() &&
						((IntervalGuard) rightGuard).getLeftReg().equals(rr)) {
					IntervalGuard igRight = (IntervalGuard) rightGuard;
					if (igRight.isIntervalGuard()) {
						return IntervalGuard.lessGuard(suffixValue, igRight.getRightReg());
					} else {
						return new SDTTrueGuard(suffixValue);
					}
				}
			} else if (igLeft.isIntervalGuard()) {
				if (rightGuard instanceof EqualityGuard && ((EqualityGuard) rightGuard).getRegister().equals(rr)) {
					return new IntervalGuard(suffixValue, igLeft.getLeftReg(), rr, igLeft.isLeftClosed(), true);
				} else if (rightGuard instanceof IntervalGuard &&
						!((IntervalGuard) rightGuard).isSmallerGuard() &&
						((IntervalGuard) rightGuard).getLeftReg().equals(rr)) {
					IntervalGuard igRight = (IntervalGuard) rightGuard;
					if (igRight.isBiggerGuard()) {
						return new IntervalGuard(suffixValue, igLeft.getLeftReg(), null, igLeft.isLeftClosed(), false);
					} else {
						return new IntervalGuard(suffixValue, igLeft.getLeftReg(), igRight.getRightReg(), igLeft.isLeftClosed(), igRight.isRightClosed());
					}
				}
			}
		}
		throw new java.lang.IllegalArgumentException("Guards are not compatible for merging");
	}

	/**
	 * Check whether two interval guards can be transformed into a disequality guard. This is possible if
	 * the guards are of the form (r < s), (s == r), (s < r) such that the sub-SDTs of the guards (r < s)
	 * and (s < r) are equivalent. If so, the guards (r < s) and (s < r) are replaced by a guard (s != r).
	 *
	 * @param guards - a mapping from SDT guards to their corresponding sub-SDTs
	 * @return a new mapping from SDT guards to corresponding sub-SDTs, with guards transformed as described above
	 */
	private Map<SDTGuard, SDT> checkForDisequality(Map<SDTGuard, SDT> guards) {
		int size = guards.size();
		if (size < 1 || size > 3)
			return guards;

		Optional<SDTGuard> less = guards.keySet().stream().filter(g -> g instanceof IntervalGuard && ((IntervalGuard) g).isSmallerGuard()).findAny();
		Optional<SDTGuard> greater = guards.keySet().stream().filter(g -> g instanceof IntervalGuard && ((IntervalGuard) g).isBiggerGuard()).findAny();
		if (less.isPresent() && greater.isPresent()) {
			IntervalGuard lg = (IntervalGuard) less.get();
			IntervalGuard gg = (IntervalGuard) greater.get();
			SDT ls = guards.get(lg);
			SDT gs = guards.get(gg);
			SymbolicDataValue rr = lg.getRightReg();
			SymbolicDataValue rl = gg.getLeftReg();
			if (rr.equals(rl) && ls.isEquivalent(gs, new VarMapping<>())) {
				Map<SDTGuard, SDT> diseq = new LinkedHashMap<>();
				diseq.put(new DisequalityGuard(lg.getParameter(), rr), guards.get(lg));
				Optional<SDTGuard> equal = guards.keySet().stream().filter(g -> g instanceof EqualityGuard).findAny();
				if (equal.isPresent()) {
					EqualityGuard eg = (EqualityGuard) equal.get();
					assert eg.getRegister().equals(rr);
					diseq.put(eg, guards.get(eg));
				}
				return diseq;
			}
		}
		return guards;
	}

	private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
        PIV ret = new PIV();
        for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
            SDTGuard mg = e.getKey();
            if (mg instanceof SDTIfGuard) {
                SymbolicDataValue r = ((SDTIfGuard) mg).getRegister();
                Parameter p = new Parameter(r.getType(), r.getId());
                if (r instanceof Register) {
                    ret.put(p, (Register) r);
                }
            } else if (mg instanceof IntervalGuard) {
                IntervalGuard iGuard = (IntervalGuard) mg;
                if (!iGuard.isBiggerGuard()) {
                    SymbolicDataValue r = iGuard.getRightReg();
                    Parameter p = new Parameter(r.getType(), r.getId());
                    if (r instanceof Register) {
                        ret.put(p, (Register) r);
                    }

                }
                if (!iGuard.isSmallerGuard()) {
                    SymbolicDataValue r = iGuard.getLeftReg();
                    Parameter p = new Parameter(r.getType(), r.getId());
                    if (r instanceof Register) {
                        ret.put(p, (Register) r);
                    }
                }
            } else if (mg instanceof SDTOrGuard) {
                Set<SymbolicDataValue> rSet = ((SDTOrGuard) mg).getAllRegs();
                for (SymbolicDataValue r : rSet) {
                    Parameter p = new Parameter(r.getType(), r.getId());
                    if (r instanceof Register) {
                        ret.put(p, (Register) r);
                    }
                }
            } else if (mg instanceof SDTAndGuard) {
            	Set<SymbolicDataValue> rSet = ((SDTAndGuard) mg).getAllRegs();
            	for (SymbolicDataValue r : rSet) {
            		Parameter p = new Parameter(r.getType(), r.getId());
            		if (r instanceof Register) {
            			ret.put(p, (Register) r);
            		}
            	}
            } else if (!(mg instanceof SDTTrueGuard)) {
                throw new IllegalStateException("wrong kind of guard");
            }
        }
        return ret;
    }

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix,
    		SymbolicSuffix suffix,
    		WordValuation values,
    		PIV piv,
    		Constants consts,
    		SuffixValuation suffixValues,
    		SDTConstructor oracle) {

    	int pId = values.size() + 1;
    	SuffixValue currentParam = suffix.getSuffixValue(pId);
    	Map<DataValue<T>, SymbolicDataValue> pot = getPotential(prefix, suffixValues, consts);

        Map<DataValue<T>, SDTGuard> equivClasses = generateEquivClasses(prefix, currentParam, pot, consts);
        Map<DataValue<T>, SDTGuard> filteredEquivClasses = filterEquivClasses(equivClasses, prefix, suffix, currentParam, pot, suffixValues, consts, values);

        Map<SDTGuard, SDT> children = new LinkedHashMap<>();
        for (Map.Entry<DataValue<T>, SDTGuard> ec : filteredEquivClasses.entrySet()) {
        	SuffixValuation nextSuffixVals = new SuffixValuation();
        	WordValuation nextVals = new WordValuation();
        	nextVals.putAll(values);
        	nextVals.put(pId, ec.getKey());
        	nextSuffixVals.putAll(suffixValues);
        	nextSuffixVals.put(currentParam, ec.getKey());
        	SDT sdt = oracle.treeQuery(prefix, suffix, nextVals, piv, consts, nextSuffixVals);
        	children.put(ec.getValue(), sdt);
        }

        Collection<DataValue<T>> filteredOut = new ArrayList<>();
        filteredOut.addAll(equivClasses.keySet());
        filteredOut.removeAll(filteredEquivClasses.keySet());
        Map<SDTGuard, SDT> merged = mergeGuards(children, equivClasses, filteredOut);
        piv.putAll(keepMem(merged));

        Map<SDTGuard, SDT> reversed = new LinkedHashMap<>();
        List<SDTGuard> keys = new ArrayList<>(merged.keySet());
        Collections.reverse(keys);
        for (SDTGuard g : keys) {
        	reversed.put(g, merged.get(g));
        }

        return new SDT(reversed);
    }

    private Map<DataValue<T>, SymbolicDataValue> getPotential(Word<PSymbolInstance> prefix,
    		SuffixValuation suffixValues,
    		Constants consts) {
    	Map<DataValue<T>, SymbolicDataValue> pot = new LinkedHashMap<>();
    	RegisterGenerator rgen = new RegisterGenerator();

    	List<DataValue<T>> seen = new ArrayList<>();
    	for (PSymbolInstance psi : prefix) {
    		DataValue<?> dvs[] = psi.getParameterValues();
    		DataType dts[] = psi.getBaseSymbol().getPtypes();
    		for (int i = 0; i < dvs.length; i++) {
    			Register r = rgen.next(dts[i]);
    			DataValue<T> dv = safeCast(dvs[i]);
    			if (dv != null && !seen.contains(dv)) {
    				pot.put(dv, r);
    				seen.add(dv);
    			}
    		}
    	}

    	for (Map.Entry<SuffixValue, DataValue<?>> e : suffixValues.entrySet()) {
    		SuffixValue sv = e.getKey();
    		DataValue<T> dv = safeCast(e.getValue());
    		if (dv != null) {
    			pot.put(dv, sv);
    		}
    	}

    	for (Map.Entry<Constant, DataValue<?>> e : consts.entrySet()) {
    		Constant c = e.getKey();
    		DataValue<T> dv = safeCast(e.getValue());
    		if (dv != null) {
    			pot.put(dv, c);
    		}
    	}

    	return pot;
    }

    protected abstract DataValue<T> safeCast(DataValue<?> val);

    public abstract List<DataValue<T>> getPotential(List<DataValue<T>> vals);

    public abstract NativeZ3Solver getSolver();

    private DataValue getRegisterValue(SymbolicDataValue r, PIV piv,
            List<DataValue> prefixValues, Constants constants,
            ParValuation pval) {
        if (r.isRegister()) {
            Parameter p = piv.getOneKey((Register) r);
            int idx = p.getId();
            return prefixValues.get(idx - 1);
        } else if (r.isSuffixValue()) {
            Parameter p = new Parameter(r.getType(), r.getId());
            return pval.get(p);
        } else if (r.isConstant()) {
            return constants.get((SymbolicDataValue.Constant) r);
        } else {
            throw new IllegalStateException("this can't possibly happen");
        }
    }

    public abstract DataValue<T> instantiate(SDTGuard guard, Valuation val,
            Constants constants, Collection<DataValue<T>> alreadyUsedValues);

    @Override
    public DataValue instantiate(
            Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv,
            ParValuation pval,
            Constants constants,
            SDTGuard guard,
            Parameter param,
            Set<DataValue<T>> oldDvs) {

        DataType type = param.getType();
        DataValue<T> returnThis = null;
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        if (guard instanceof EqualityGuard) {
            EqualityGuard eqGuard = (EqualityGuard) guard;
            SymbolicDataValue ereg = eqGuard.getRegister();
            if (ereg.isRegister()) {
                Parameter p = piv.getOneKey((Register) ereg);
                int idx = p.getId();
                returnThis = prefixValues.get(idx - 1);
            } else if (ereg.isSuffixValue()) {
                Parameter p = new Parameter(type, ereg.getId());
                returnThis = (DataValue<T>) pval.get(p);
            } else if (ereg.isConstant()) {
                returnThis = (DataValue<T>) constants.get((SymbolicDataValue.Constant) ereg);
            }
        } else if (guard instanceof SDTTrueGuard || guard instanceof DisequalityGuard) {

            Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                    constants.<T>values(type),
                    DataWords.<T>valSet(prefix, type),
                    pval.<T>values(type));

            returnThis = this.getFreshValue(new ArrayList<>(potSet));
        } else {
            Collection<DataValue<T>> alreadyUsedValues
                    = DataWords.<T>joinValsToSet(
                            constants.<T>values(type),
                            DataWords.<T>valSet(prefix, type),
                            pval.<T>values(type));
            Valuation val = new Valuation();
            if (guard instanceof IntervalGuard) {
                IntervalGuard iGuard = (IntervalGuard) guard;
                if (!iGuard.isBiggerGuard()) {
                    SymbolicDataValue r = iGuard.getRightReg();
                    DataValue<T> regVal = getRegisterValue(r, piv,
                            prefixValues, constants, pval);

                    val.setValue(toVariable(r), regVal.getId());
                }
                if (!iGuard.isSmallerGuard()) {
                    SymbolicDataValue l = iGuard.getLeftReg();
                    DataValue regVal = getRegisterValue(l, piv,
                            prefixValues, constants, pval);

                    val.setValue(toVariable(l), regVal.getId());
                }
            } else if (guard instanceof SDTIfGuard) {
                SymbolicDataValue r = ((SDTIfGuard) guard).getRegister();
                DataValue<T> regVal = getRegisterValue(r, piv,
                        prefixValues, constants, pval);
                val.setValue(toVariable(r), regVal.getId());
            } else if (guard instanceof SDTOrGuard) {
                SDTGuard iGuard = ((SDTOrGuard) guard).getGuards().get(0);

                returnThis = instantiate(iGuard, val, constants, alreadyUsedValues);
            } else if (guard instanceof SDTAndGuard) {
                assert ((SDTAndGuard) guard).getGuards().stream().allMatch(g -> g instanceof DisequalityGuard);
                SDTGuard aGuard = ((SDTAndGuard) guard).getGuards().get(0);



                returnThis = instantiate(aGuard, val, constants, alreadyUsedValues);
            } else {
                throw new IllegalStateException("only =, != or interval allowed. Got " + guard);
            }
            if (!(oldDvs.isEmpty())) {
                for (DataValue<T> oldDv : oldDvs) {
                    Valuation newVal = new Valuation();
                    newVal.putAll(val);
                    newVal.setValue(toVariable( new SuffixValue(param.getType(), param.getId()) ), oldDv.getId());
                    DataValue inst = instantiate(guard, newVal, constants, alreadyUsedValues);
                    if (inst != null) {
                        return inst;
                    }
                }
            }
            returnThis = instantiate(guard, val, constants, alreadyUsedValues);

        }
        return returnThis;
    }

    public void useSuffixOptimization(boolean useSuffixOpt) {
    	this.useSuffixOpt = useSuffixOpt;
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts) {
    	int firstActionArity = suffix.size() > 0 ? suffix.getSymbol(0).getBaseSymbol().getArity() : 0;
    	if (suffixValue.getId() <= firstActionArity) {
    		return new UnrestrictedSuffixValue(suffixValue);
    	}

    	DataValue<?> prefixVals[] = DataWords.valsOf(prefix);
    	DataValue<?> suffixVals[] = DataWords.valsOf(suffix);
    	DataValue<?> constVals[] = new DataValue<?>[consts.size()];
    	constVals = consts.values().toArray(constVals);
    	DataValue<?> priorVals[] = new DataValue<?>[prefixVals.length + constVals.length + suffixValue.getId() - 1];
    	DataType svType = suffixValue.getType();
    	DataValue<T> svDataValue = safeCast(suffixVals[suffixValue.getId()-1]);
    	assert svDataValue != null;

    	System.arraycopy(prefixVals, 0, priorVals, 0, prefixVals.length);
    	System.arraycopy(constVals, 0, priorVals, prefixVals.length, constVals.length);
    	System.arraycopy(suffixVals, 0, priorVals, prefixVals.length + constVals.length, suffixValue.getId() - 1);

    	// is suffix value greater than all prior or smaller than all prior?
    	Comparator<DataValue<T>> comparator = getComparator();
    	boolean greater = false;
    	boolean lesser = false;
    	boolean foundFirst = false;
    	for (int i = 0; i < priorVals.length; i++) {
    		if (priorVals[i].getType().equals(svType)) {
    			DataValue<T> dv = safeCast(priorVals[i]);
    			assert dv != null;
    			int comparison = comparator.compare(svDataValue, dv);

    			if (foundFirst) {
    				if ((greater && comparison < 0) ||
    						(lesser && comparison > 0) ||
    						comparison == 0) {
    					return new UnrestrictedSuffixValue(suffixValue);
    				}
    			} else {
    				if (comparison > 0) {
    					greater = true;
    				} else if (comparison < 0) {
    					lesser = true;
    				} else {
    					return new UnrestrictedSuffixValue(suffixValue);
    				}
    				foundFirst = true;
    			}
    		}
    	}

    	if (!foundFirst) {
    		return new GreaterSuffixValue(suffixValue);
    	}

    	assert (greater && !lesser) || (!greater && lesser);
    	return greater ? new GreaterSuffixValue(suffixValue) : new LesserSuffixValue(suffixValue);
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
    	SuffixValue sv = guard.getParameter();

    	if (guard instanceof IntervalGuard) {
    		IntervalGuard ig = (IntervalGuard) guard;
    		if (ig.isBiggerGuard()) {
    			return new GreaterSuffixValue(sv);
    		} else if (ig.isSmallerGuard()) {
    			return new LesserSuffixValue(sv);
    		}
    	}
    	SuffixValueRestriction restr = SuffixValueRestriction.genericRestriction(guard, prior);
    	if (restr instanceof FreshSuffixValue) {
    		restr = new GreaterSuffixValue(sv);
    	}
    	return restr;
    }

    @Override
    public boolean guardRevealsRegister(SDTGuard guard, SymbolicDataValue register) {
    	if (guard instanceof EqualityGuard && ((EqualityGuard) guard).getRegister().equals(register)) {
    		return true;
    	} else if (guard instanceof DisequalityGuard && ((DisequalityGuard)guard).getRegister().equals(register)) {
    		return true;
    	} else if (guard instanceof IntervalGuard) {
    		IntervalGuard ig = (IntervalGuard) guard;
    		if (ig.getLeftReg().equals(register) || ig.getRightReg().equals(register)) {
    			return true;
    		}
    	} else if (guard instanceof SDTMultiGuard) {
    		boolean revealsGuard = false;
    		for (SDTGuard g : ((SDTMultiGuard)guard).getGuards()) {
    			revealsGuard = revealsGuard || this.guardRevealsRegister(g, register);
    		}
    		return revealsGuard;
    	}
    	return false;
    }
}
