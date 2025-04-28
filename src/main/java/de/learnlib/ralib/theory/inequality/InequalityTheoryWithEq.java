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

import java.math.BigDecimal;
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

import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.theory.EquivalenceClassFilter;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3Solver;
import net.automatalib.word.Word;

/**
 *
 * @author Sofia Cassel
 * @author Fredrik Tåquist
 */
public abstract class InequalityTheoryWithEq implements Theory {

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
    private Map<DataValue, SDTGuard> generateEquivClasses(Word<PSymbolInstance> prefix,
    		SuffixValue suffixValue,
    		Map<DataValue, SDTGuardElement> potValuation,
    		Constants consts) {

	Map<DataValue, SDTGuardElement> filteredPotValuation = new LinkedHashMap<>(potValuation);
	for (DataValue d : potValuation.keySet()) {
		if (!d.getDataType().equals(suffixValue.getDataType())) {
			filteredPotValuation.remove(d);
		}
	}
	potValuation = filteredPotValuation;

    	Map<DataValue, SDTGuard> valueGuards = new LinkedHashMap<>();

    	if (potValuation.isEmpty()) {
    		DataValue fresh = getFreshValue(new ArrayList<>());
    		valueGuards.put(fresh, new SDTGuard.SDTTrueGuard(suffixValue));
    		return valueGuards;
    	}
        int usedVals = potValuation.size();
        List<DataValue> potential = new ArrayList<>();
        potential.addAll(potValuation.keySet());
        List<DataValue> sortedPot = sort(potential);

        Valuation vals = new Valuation();
        for (Map.Entry<DataValue, SDTGuardElement> pot : potValuation.entrySet()) {
            SDTGuardElement r = pot.getValue();
            DataValue dv = pot.getKey();
            // TODO: fix unchecked invocation
		if (!(r instanceof DataValue)) {
			vals.setValue((Variable<BigDecimal>) r, dv.getValue());
		}
        }

        // smallest
        DataValue dl = sortedPot.get(0);
        SDTGuardElement rl = potValuation.get(dl);
        SDTGuard.IntervalGuard sg = new SDTGuard.IntervalGuard(suffixValue, null, rl);
        DataValue smallest = instantiate(sg, vals, consts, sortedPot);
        assert smallest != null;
        valueGuards.put(smallest, sg);

        for (int i = 1; i < usedVals; i++) {
            // equality
            SDTGuard.EqualityGuard eg = new SDTGuard.EqualityGuard(suffixValue, rl);
            valueGuards.put(dl, eg);

            // interval
            DataValue dr = sortedPot.get(i);
            SDTGuardElement rr = potValuation.get(dr);
            SDTGuard.IntervalGuard ig = new SDTGuard.IntervalGuard(suffixValue, rl, rr);
            DataValue di = instantiate(ig, vals, consts, sortedPot);
            assert di != null;
            valueGuards.put(di, ig);

            dl = dr;
            rl = rr;
        }
        SDTGuard.EqualityGuard eg = new SDTGuard.EqualityGuard(suffixValue, rl);
        valueGuards.put(dl, eg);

        // greatest
        SDTGuard.IntervalGuard gg = new SDTGuard.IntervalGuard(suffixValue, rl, null);
        DataValue dg = instantiate(gg, vals, consts, sortedPot);
        assert dg != null;
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
    private Map<DataValue, SDTGuard> filterEquivClasses(Map<DataValue, SDTGuard> valueGuards,
			Word<PSymbolInstance> prefix,
			SymbolicSuffix suffix,
			SuffixValue suffixValue,
			Map<DataValue, SDTGuardElement> potValuation,
			SuffixValuation suffixVals,
			Constants consts,
			WordValuation values) {
		List<DataValue> equivClasses = new ArrayList<>();
		equivClasses.addAll(valueGuards.keySet());
		EquivalenceClassFilter eqcFilter = new EquivalenceClassFilter(equivClasses, useSuffixOpt);
		List<DataValue> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(suffixValue), prefix, suffix.getActions(), values);

		Map<DataValue, SDTGuard> ret = new LinkedHashMap<>();
		for (Map.Entry<DataValue, SDTGuard> e : valueGuards.entrySet()) {
			DataValue ec = e.getKey();
			if (filteredEquivClasses.contains(ec)) {
				ret.put(ec, e.getValue());
			}
		}
		return ret;
    }

	private List<DataValue> sort(Collection<DataValue> list) {
		List<DataValue> sorted = new ArrayList<>();
		sorted.addAll(list);
		sorted.sort(getComparator());
		return sorted;
	}

	protected abstract Comparator<DataValue> getComparator();

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
			Map<DataValue, SDTGuard> equivClasses,
			Collection<DataValue> filteredOut) {
		Map<SDTGuard, SDT> merged = new LinkedHashMap<>();

		List<DataValue> ecValuesSorted = sort(equivClasses.keySet());

		// merge guards from left (lesser values) to right (greater values)
		// stop merging when reaching
		// i) a value that is filtered out, or
		// ii) an inequivalent sub-tree
		SDT currSdt = null;
		SDTGuard currGuard = null;
		SDTGuard currMerged = null;
		SDTGuard prevGuard = null;
		SDT prevSdt = null;
		for (DataValue nextDv : ecValuesSorted) {
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
						if (currGuard instanceof SDTGuard.EqualityGuard && prevGuard != null &&
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

		merged = checkForDisequality(merged);

		// if only one guard, replace with true guard
		if (merged.size() == 1) {
			Map.Entry<SDTGuard, SDT> entry = merged.entrySet().iterator().next();
			SDTGuard g = entry.getKey();
			if (g instanceof SDTGuard.DisequalityGuard || (g instanceof SDTGuard.IntervalGuard && ((SDTGuard.IntervalGuard) g).isBiggerGuard())) {
				merged = new LinkedHashMap<>();
				merged.put(new SDTGuard.SDTTrueGuard(g.getParameter()), entry.getValue());
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
		if (guard1 != null && guard1 instanceof SDTGuard.EqualityGuard) {
			Expression<Boolean> renaming = SDTGuard.toExpr(guard1);
			return sdt1.isEquivalentUnderCondition(sdt2, renaming);
		} else if (guard2 != null && guard2 instanceof SDTGuard.EqualityGuard) {
			Expression<Boolean> renaming = SDTGuard.toExpr(guard2);
			return sdt2.isEquivalentUnderCondition(sdt1, renaming);
		}
		return sdt1.isEquivalent(sdt2, new Bijection<>());
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
		if (leftGuard instanceof SDTGuard.EqualityGuard) {
			SDTGuard.EqualityGuard egLeft = (SDTGuard.EqualityGuard) leftGuard;
			SDTGuardElement rl = egLeft.register();
			if (rightGuard instanceof SDTGuard.IntervalGuard) {
				SDTGuard.IntervalGuard igRight = (SDTGuard.IntervalGuard) rightGuard;
				if (!igRight.isSmallerGuard() && igRight.smallerElement().equals(rl)) {
					if (igRight.isBiggerGuard()) {
						return SDTGuard.IntervalGuard.greaterOrEqualGuard(suffixValue, rl);
					} else {
						return new SDTGuard.IntervalGuard(suffixValue, rl, igRight.greaterElement(), true, false);
					}
				}
			}
		} else if (leftGuard instanceof SDTGuard.IntervalGuard && !((SDTGuard.IntervalGuard) leftGuard).isBiggerGuard()) {
			SDTGuard.IntervalGuard igLeft = (SDTGuard.IntervalGuard) leftGuard;
			SDTGuardElement rr = igLeft.greaterElement();
			if (igLeft.isSmallerGuard()) {
				if (rightGuard instanceof SDTGuard.EqualityGuard && ((SDTGuard.EqualityGuard) rightGuard).register().equals(rr)) {
					return SDTGuard.IntervalGuard.lessOrEqualGuard(suffixValue, rr);
				} else if (rightGuard instanceof SDTGuard.IntervalGuard &&
						!((SDTGuard.IntervalGuard) rightGuard).isSmallerGuard() &&
						((SDTGuard.IntervalGuard) rightGuard).smallerElement().equals(rr)) {
					SDTGuard.IntervalGuard igRight = (SDTGuard.IntervalGuard) rightGuard;
					if (igRight.isIntervalGuard()) {
						return SDTGuard.IntervalGuard.lessGuard(suffixValue, igRight.greaterElement());
					} else {
						return new SDTGuard.SDTTrueGuard(suffixValue);
					}
				}
			} else if (igLeft.isIntervalGuard()) {
				if (rightGuard instanceof SDTGuard.EqualityGuard && ((SDTGuard.EqualityGuard) rightGuard).register().equals(rr)) {
					return new SDTGuard.IntervalGuard(suffixValue, igLeft.smallerElement(), rr, igLeft.isLeftClosed(), true);
				} else if (rightGuard instanceof SDTGuard.IntervalGuard &&
						!((SDTGuard.IntervalGuard) rightGuard).isSmallerGuard() &&
						((SDTGuard.IntervalGuard) rightGuard).smallerElement().equals(rr)) {
					SDTGuard.IntervalGuard igRight = (SDTGuard.IntervalGuard) rightGuard;
					if (igRight.isBiggerGuard()) {
						return new SDTGuard.IntervalGuard(suffixValue, igLeft.smallerElement(), null, igLeft.isLeftClosed(), false);
					} else {
						return new SDTGuard.IntervalGuard(suffixValue, igLeft.smallerElement(), igRight.greaterElement(), igLeft.isLeftClosed(), igRight.isRightClosed());
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

		Optional<SDTGuard> less = guards.keySet().stream().filter(g -> g instanceof SDTGuard.IntervalGuard && ((SDTGuard.IntervalGuard) g).isSmallerGuard()).findAny();
		Optional<SDTGuard> greater = guards.keySet().stream().filter(g -> g instanceof SDTGuard.IntervalGuard && ((SDTGuard.IntervalGuard) g).isBiggerGuard()).findAny();
		if (less.isPresent() && greater.isPresent()) {
			SDTGuard.IntervalGuard lg = (SDTGuard.IntervalGuard) less.get();
			SDTGuard.IntervalGuard gg = (SDTGuard.IntervalGuard) greater.get();
			SDT ls = guards.get(lg);
			SDT gs = guards.get(gg);
			SDTGuardElement rr = lg.greaterElement();
			SDTGuardElement rl = gg.smallerElement();
			if (rr.equals(rl) && ls.isEquivalent(gs, new Bijection<>())) {
				Map<SDTGuard, SDT> diseq = new LinkedHashMap<>();
				diseq.put(new SDTGuard.DisequalityGuard(lg.getParameter(), rr), guards.get(lg));
				Optional<SDTGuard> equal = guards.keySet().stream().filter(g -> g instanceof SDTGuard.EqualityGuard).findAny();
				if (equal.isPresent()) {
					SDTGuard.EqualityGuard eg = (SDTGuard.EqualityGuard) equal.get();
					assert eg.register().equals(rr);
					diseq.put(eg, guards.get(eg));
				}
				return diseq;
			}
		}
		return guards;
	}

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix,
    		SymbolicSuffix suffix,
    		WordValuation values,
    		Constants consts,
    		SuffixValuation suffixValues,
		MultiTheoryTreeOracle oracle) {

    	int pId = values.size() + 1;
    	SuffixValue currentParam = suffix.getSuffixValue(pId);
    	Map<DataValue, SDTGuardElement> pot = getPotential(prefix, suffixValues, consts);

        Map<DataValue, SDTGuard> equivClasses = generateEquivClasses(prefix, currentParam, pot, consts);
        Map<DataValue, SDTGuard> filteredEquivClasses = filterEquivClasses(equivClasses, prefix, suffix, currentParam, pot, suffixValues, consts, values);

        Map<SDTGuard, SDT> children = new LinkedHashMap<>();
        for (Map.Entry<DataValue, SDTGuard> ec : filteredEquivClasses.entrySet()) {
        	SuffixValuation nextSuffixVals = new SuffixValuation();
        	WordValuation nextVals = new WordValuation();
        	nextVals.putAll(values);
        	nextVals.put(pId, ec.getKey());
        	nextSuffixVals.putAll(suffixValues);
        	nextSuffixVals.put(currentParam, ec.getKey());
        	SDT sdt = oracle.treeQuery(prefix, suffix, nextVals, consts, nextSuffixVals);
        	children.put(ec.getValue(), sdt);
        }

        Collection<DataValue> filteredOut = new ArrayList<>();
        filteredOut.addAll(equivClasses.keySet());
        filteredOut.removeAll(filteredEquivClasses.keySet());
        Map<SDTGuard, SDT> merged = mergeGuards(children, equivClasses, filteredOut);

        Map<SDTGuard, SDT> reversed = new LinkedHashMap<>();
        List<SDTGuard> keys = new ArrayList<>(merged.keySet());
        Collections.reverse(keys);
        for (SDTGuard g : keys) {
        	reversed.put(g, merged.get(g));

        }

        return new SDT(reversed);
    }

    private Map<DataValue, SDTGuardElement> getPotential(Word<PSymbolInstance> prefix,
    		SuffixValuation suffixValues,
    		Constants consts) {
    	Map<DataValue, SDTGuardElement> pot = new LinkedHashMap<>();
    	//RegisterGenerator rgen = new RegisterGenerator();

    	List<DataValue> seen = new ArrayList<>();
    	for (PSymbolInstance psi : prefix) {
    		DataValue dvs[] = psi.getParameterValues();
    		DataType dts[] = psi.getBaseSymbol().getPtypes();
    		for (int i = 0; i < dvs.length; i++) {
    			//Register r = rgen.next(dts[i]);
    			DataValue dv = safeCast(dvs[i]);
    			if (dv != null && !seen.contains(dv)) {
    				pot.put(dv, dv);
    				seen.add(dv);
    			}
    		}
    	}

    	for (Map.Entry<SuffixValue, DataValue> e : suffixValues.entrySet()) {
    		SuffixValue sv = e.getKey();
    		DataValue dv = safeCast(e.getValue());
    		if (dv != null) {
    			pot.put(dv, sv);
    		}
    	}

    	for (Map.Entry<Constant, DataValue> e : consts.entrySet()) {
    		Constant c = e.getKey();
    		DataValue dv = safeCast(e.getValue());
    		if (dv != null) {
    			pot.put(dv, c);
    		}
    	}

    	return pot;
    }

    protected abstract DataValue safeCast(DataValue val);

    public abstract List<DataValue> getPotential(List<DataValue> vals);

    public abstract NativeZ3Solver getSolver();

    private DataValue getRegisterValue(SDTGuardElement r,
            List<DataValue> prefixValues, Constants constants,
            SuffixValuation pval) {
        if (SDTGuardElement.isDataValue(r)) {
//            LOGGER.trace("piv: " + piv + " " + r.toString() + " " + prefixValues);
//            LOGGER.trace("p: " + p.toString());
            return (DataValue) r;
        } else if (SDTGuardElement.isSuffixValue(r)) {
            return pval.get( (SuffixValue) r);
        } else if (SDTGuardElement.isConstant(r)) {
            return constants.get((SymbolicDataValue.Constant) r);
        } else {
            throw new IllegalStateException("this can't possibly happen");
        }
    }

    public abstract DataValue instantiate(SDTGuard guard, Valuation val,
            Constants constants, Collection<DataValue> alreadyUsedValues);

    @Override
    public DataValue instantiate(
            Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps,
            SuffixValuation pval,
            Constants constants,
            SDTGuard guard,
            SuffixValue param,
            Set<DataValue> oldDvs) {

        DataType type = param.getDataType();
        DataValue returnThis = null;
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        if (guard instanceof SDTGuard.EqualityGuard) {
            SDTGuard.EqualityGuard eqGuard = (SDTGuard.EqualityGuard) guard;

            SDTGuardElement ereg = eqGuard.register();
            if (SDTGuardElement.isDataValue(ereg)) {
                returnThis = (DataValue) ereg;
            } else if (SDTGuardElement.isSuffixValue(ereg)) {
                returnThis = pval.get( (SuffixValue) ereg);
            } else if (SDTGuardElement.isConstant(ereg)) {
                returnThis = constants.get((SymbolicDataValue.Constant) ereg);
            }
            assert returnThis != null;
        } else if (guard instanceof SDTGuard.SDTTrueGuard || guard instanceof SDTGuard.DisequalityGuard) {

            Collection<DataValue> potSet = DataWords.joinValsToSet(
                    constants.values(type),
                    DataWords.valSet(prefix, type),
                    pval.values(type));

            returnThis = this.getFreshValue(new ArrayList<>(potSet));
        } else {
            Collection<DataValue> alreadyUsedValues
                    = DataWords.joinValsToSet(
                            constants.values(type),
                            DataWords.valSet(prefix, type),
                            pval.values(type));
            Valuation val = new Valuation();
            if (guard instanceof SDTGuard.IntervalGuard) {
                SDTGuard.IntervalGuard iGuard = (SDTGuard.IntervalGuard) guard;
                if (!iGuard.isBiggerGuard()) {
                    SDTGuardElement r = iGuard.greaterElement();
                    if (SDTGuardElement.isSuffixValue(r) || SDTGuardElement.isConstant(r)) {
                        DataValue regVal = getRegisterValue(r, prefixValues, constants, pval);
                        val.setValue( (Variable) r, regVal.getValue());
                    }
                }
                if (!iGuard.isSmallerGuard()) {
                    SDTGuardElement l =  iGuard.smallerElement();
                    if (SDTGuardElement.isSuffixValue(l) || SDTGuardElement.isConstant(l)) {
                        DataValue regVal = getRegisterValue(l, prefixValues, constants, pval);
                        val.setValue( (Variable) l, regVal.getValue());
                    }
                }
            /*} else if (guard instanceof SDTGuard.SDTIfGuard) {
                SymbolicDataValue r = ((SDTIfGuard) guard).getRegister();
                DataValue regVal = getRegisterValue(r,
                        prefixValues, constants, pval);
                val.setValue(r, regVal.getValue());

            } else if (guard instanceof SDTGuard.SDTOrGuard) {
                SDTGuard iGuard = ((SDTGuard.SDTOrGuard) guard).disjuncts().get(0);

                returnThis = instantiate(iGuard, val, constants, alreadyUsedValues);
            } else if (guard instanceof SDTGuard.SDTAndGuard) {
                assert ((SDTGuard.SDTAndGuard) guard).conjuncts().stream().allMatch(g -> g instanceof SDTGuard.DisequalityGuard);
                SDTGuard aGuard = ((SDTGuard.SDTAndGuard) guard).conjuncts().get(0);

                returnThis = instantiate(aGuard, val, constants, alreadyUsedValues);
            */
            } else {

                throw new IllegalStateException("only =, != or interval allowed. Got " + guard);
            }

            if (!(oldDvs.isEmpty())) {
                for (DataValue oldDv : oldDvs) {
                    Valuation newVal = new Valuation();
                    newVal.putAll(val);
                    newVal.setValue( new SuffixValue(param.getDataType(), param.getId()) , oldDv.getValue());
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

    	DataValue prefixVals[] = DataWords.valsOf(prefix);
    	DataValue suffixVals[] = DataWords.valsOf(suffix);
    	DataValue constVals[] = new DataValue[consts.size()];
    	constVals = consts.values().toArray(constVals);
    	DataValue priorVals[] = new DataValue[prefixVals.length + constVals.length + suffixValue.getId() - 1];
    	DataType svType = suffixValue.getDataType();
    	DataValue svDataValue = safeCast(suffixVals[suffixValue.getId()-1]);
    	assert svDataValue != null;

    	System.arraycopy(prefixVals, 0, priorVals, 0, prefixVals.length);
    	System.arraycopy(constVals, 0, priorVals, prefixVals.length, constVals.length);
    	System.arraycopy(suffixVals, 0, priorVals, prefixVals.length + constVals.length, suffixValue.getId() - 1);

    	// is suffix value greater than all prior or smaller than all prior?
    	Comparator<DataValue> comparator = getComparator();
    	boolean greater = false;
    	boolean lesser = false;
    	boolean foundFirst = false;
    	for (int i = 0; i < priorVals.length; i++) {
    		if (priorVals[i].getDataType().equals(svType)) {
    			DataValue dv = safeCast(priorVals[i]);
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

    	if (guard instanceof SDTGuard.IntervalGuard) {
    		SDTGuard.IntervalGuard ig = (SDTGuard.IntervalGuard) guard;
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
    	if (guard instanceof SDTGuard.EqualityGuard && ((SDTGuard.EqualityGuard) guard).register().equals(register)) {
    		return true;
    	} else if (guard instanceof SDTGuard.DisequalityGuard && ((SDTGuard.DisequalityGuard)guard).register().equals(register)) {
    		return true;
    	} else if (guard instanceof SDTGuard.IntervalGuard) {
    		SDTGuard.IntervalGuard ig = (SDTGuard.IntervalGuard) guard;
    		if (ig.smallerElement().equals(register) || ig.greaterElement().equals(register)) {
    			return true;
    		}
    	} else if (guard instanceof SDTGuard.SDTOrGuard) {
    		boolean revealsGuard = false;
    		for (SDTGuard g : ((SDTGuard.SDTOrGuard)guard).disjuncts()) {
    			revealsGuard = revealsGuard || this.guardRevealsRegister(g, register);
    		}
    		return revealsGuard;
    	} else if (guard instanceof SDTGuard.SDTAndGuard) {
		boolean revealsGuard = false;
		for (SDTGuard g : ((SDTGuard.SDTAndGuard)guard).conjuncts()) {
			revealsGuard = revealsGuard || this.guardRevealsRegister(g, register);
		}
		return revealsGuard;
	}
	return false;
    }

}
