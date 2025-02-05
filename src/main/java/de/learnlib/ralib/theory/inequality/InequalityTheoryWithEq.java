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

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
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
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.solver.jconstraints.JContraintsUtil;
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
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

/**
 *
 * @author Sofia Cassel
 * @author Fredrik Tåquist
 * @param<T>
 */
public abstract class InequalityTheoryWithEq<T> implements Theory<T> {

	boolean useSuffixOpt = false;

	public Map<DataValue<T>, SDTGuard> equivalenceClasses(Word<PSymbolInstance> prefix,
			SymbolicSuffix suffix,
			SuffixValue suffixValue,
			Map<DataValue<T>, SymbolicDataValue> potValuation,
			SuffixValuation suffixValues,
			Constants consts) {
		Map<DataValue<T>, SDTGuard> valueGuards = generateEquivClasses(prefix, suffixValue, potValuation, consts);
		// apply suffix restrictions
		return filterEquivClasses(valueGuards, prefix, suffix, suffixValue, potValuation, suffixValues, consts);
	}

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

	private Map<DataValue<T>, SDTGuard> filterEquivClasses(Map<DataValue<T>, SDTGuard> valueGuards,
			Word<PSymbolInstance> prefix,
			SymbolicSuffix suffix,
			SuffixValue suffixValue,
			Map<DataValue<T>, SymbolicDataValue> potValuation,
			SuffixValuation suffixVals,
			Constants consts) {
		List<DataValue<T>> equivClasses = new ArrayList<>();
		equivClasses.addAll(valueGuards.keySet());
		EquivalenceClassFilter<T> eqcFilter = new EquivalenceClassFilter<>(equivClasses, useSuffixOpt);
		List<DataValue<T>> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(suffixValue), prefix, suffix.getActions(), suffixVals, consts);

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

	protected Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> sdts,
			Map<DataValue<T>, SDTGuard> equivClasses,
			Collection<DataValue<T>> filteredOut) {
		Map<SDTGuard, SDT> merged = new LinkedHashMap<>();

		List<DataValue<T>> ecValuesSorted = sort(equivClasses.keySet());
		Iterator<DataValue<T>> ecit = ecValuesSorted.iterator();
		DataValue<T> first = ecit.next();
		SDTGuard mergedGuards = equivClasses.get(first);
		SDT currSdt = sdts.get(mergedGuards);

		Comparator<DataValue<T>> comparator = getComparator();
		Iterator<DataValue<T>> filtit = sort(filteredOut).iterator();
		DataValue<T> smallerDataValue = getSmallerDataValue(first);
		DataValue<T> filtered = filtit.hasNext() ? filtit.next() : smallerDataValue;
		boolean lastFilteredOut = comparator.compare(first, filtered) == 0;
		if (lastFilteredOut) {
			assert ecit.hasNext();
			filtered = filtit.hasNext() ? filtit.next() : smallerDataValue;
		}

		while(ecit.hasNext()) {
			DataValue<T> next = ecit.next();
			SDTGuard nextGuard = equivClasses.get(next);
			SDT nextSdt = sdts.get(nextGuard);
			boolean thisFilteredOut = comparator.compare(next, filtered) == 0;
			boolean inequivalentSdts = thisFilteredOut || (currSdt == null ? false : !currSdt.isEquivalent(nextSdt, new VarMapping<>()));
			if (thisFilteredOut || inequivalentSdts || lastFilteredOut) {
				if (!lastFilteredOut) {
					merged.put(mergedGuards, currSdt);
				}
				mergedGuards = nextGuard;
				currSdt = nextSdt;
			} else {
				mergedGuards = mergeIntervals(mergedGuards, nextGuard);
			}

			lastFilteredOut = thisFilteredOut;
			if (comparator.compare(next, filtered) >= 0 && filtit.hasNext()) {
				filtered = filtit.next();
			}
		}
		if (!lastFilteredOut) {
			merged.put(mergedGuards, currSdt);
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

	private DataValue<T> getSmallerDataValue(DataValue<T> dv) {
		SuffixValue s = new SuffixValue(dv.getType(), 1);
		Register r = new Register(dv.getType(), 1);
		IntervalGuard ig = new IntervalGuard(s, null, r);
		Valuation val = new Valuation();
		val.setValue(toVariable(r), dv.getId());
		return instantiate(ig, val, new Constants(), new ArrayList<>());
	}

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
        Map<DataValue<T>, SDTGuard> filteredEquivClasses = filterEquivClasses(equivClasses, prefix, suffix, currentParam, pot, suffixValues, consts);

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

    public abstract DataValue<T> instantiate(SDTGuard guard, Valuation val,
            Constants constants, Collection<DataValue<T>> alreadyUsedValues);

    private Expression<Boolean> instantiateGuard(SDTGuard guard) {
    	if (guard instanceof SDTMultiGuard) {
    		SDTMultiGuard mg = (SDTMultiGuard) guard;
    		List<Expression<Boolean>> eList = new ArrayList<>();
    		for (SDTGuard g : mg.getGuards()) {
    			GuardExpression x = g.toExpr();
    			eList.add(JContraintsUtil.toExpression(x));
    		}

    		if (guard instanceof SDTAndGuard) {
    			return ExpressionUtil.and(eList);
    		} else if (guard instanceof SDTOrGuard) {
    			return ExpressionUtil.or(eList);
    		} else {
    			throw new RuntimeException("Incompatible multiguard");
    		}
    	}
    	GuardExpression x = guard.toExpr();
    	return JContraintsUtil.toExpression(x);
    }

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
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        if (guard instanceof EqualityGuard) {
            EqualityGuard eqGuard = (EqualityGuard) guard;
            SymbolicDataValue ereg = eqGuard.getRegister();
            if (ereg.isRegister()) {
                Parameter p = piv.getOneKey((Register) ereg);
                int idx = p.getId();
                return prefixValues.get(idx - 1);
            } else if (ereg.isSuffixValue()) {
                Parameter p = new Parameter(type, ereg.getId());
                return (DataValue<T>) pval.get(p);
            } else if (ereg.isConstant()) {
                return (DataValue<T>) constants.get((SymbolicDataValue.Constant) ereg);
            }
        } else if (guard instanceof SDTTrueGuard || guard instanceof DisequalityGuard) {
            Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                    constants.<T>values(type),
                    DataWords.<T>valSet(prefix, type),
                    pval.<T>values(type));

            return getFreshValue(new ArrayList<>(potSet));
        }

        Valuation valuation = new Valuation();
        Mapping<SymbolicDataValue, DataValue<?>> valueMapping = new Mapping<>();
        ParameterGenerator pgen = new ParameterGenerator();
        for (DataValue<?> pv : prefixValues) {
        	Parameter p = pgen.next(pv.getType());
        	valuation.setValue(toVariable(p), pv.getId());
        	valueMapping.put(p, pv);
        }
        for (Map.Entry<Constant, DataValue<?>> c : constants.entrySet()) {
        	DataValue<?> d = (DataValue<?>) c.getValue().getId();
        	valuation.setValue(toVariable(c.getKey()), d.getId());
        	valueMapping.put(c.getKey(), d);
        }
        for (SymbolicDataValue s : guard.getComparands(guard.getParameter())) {
        	if (s.isSuffixValue()) {
        		Parameter p = new Parameter(s.getType(), s.getId());
        		DataValue<?> dv = pval.get(p);
        		valuation.setValue(toVariable(s), dv.getId());
        		valueMapping.put(s, dv);
        	} else if (s.isRegister()) {
        		Parameter p = piv.getOneKey((Register)s);
        		DataValue<?> dv = prefixValues.get(p.getId() - 1);
        		valuation.setValue(toVariable(s), dv.getId());
        		valueMapping.put(s, dv);
        	}
        }
        Collection<DataValue<T>> alreadyUsedValues
        = DataWords.<T>joinValsToSet(
                constants.<T>values(type),
                DataWords.<T>valSet(prefix, type),
                pval.<T>values(type));

        Expression<Boolean> x = instantiateGuard(guard);
        Set<SymbolicDataValue> expressed = new LinkedHashSet<>();
        List<Expression<Boolean>> eList = symbolicDataValueExpression(expressed, guard, valuation);
        eList.add(x);
        Collection<DataValue<T>> filteredUsedValues = removePotentialEqualities(alreadyUsedValues, guard, valueMapping);
        DataValue<T> dataValue = instantiate(guard.getParameter(), eList, valuation, filteredUsedValues);

        return dataValue;
    }

    protected abstract List<Expression<Boolean>> symbolicDataValueExpression(Set<SymbolicDataValue> expressed, SDTGuard g, Valuation val);

    protected abstract DataValue<T> instantiate(SuffixValue sp, List<Expression<Boolean>> exprList, Valuation val, Collection<DataValue<T>> alreadyUsedValues);

    private Collection<DataValue<T>> removePotentialEqualities(Collection<DataValue<T>> alreadyUsedValues, SDTGuard guard, Mapping<SymbolicDataValue, DataValue<?>> valueMapping) {
    	Collection<DataValue<T>> filtered = new ArrayList<>();
    	filtered.addAll(alreadyUsedValues);

    	if (guard instanceof EqualityGuard) {
    		SymbolicDataValue s = ((EqualityGuard) guard).getRegister();
    		DataValue<?> d = valueMapping.get(s);
    		filtered.remove(d);
    	} else if (guard instanceof IntervalGuard) {
    		IntervalGuard ig = (IntervalGuard) guard;
    		if (ig.isLeftClosed()) {
    			SymbolicDataValue l = ig.getLeftReg();
    			if (l != null) {
    				DataValue<?> d = valueMapping.get(l);
    				filtered.remove(d);
    			}
    		}
    		if (ig.isRightClosed()) {
    			SymbolicDataValue r = ig.getRightReg();
    			if (r != null) {
    				DataValue<?> d = valueMapping.get(r);
    				filtered.remove(d);
    			}
    		}
    	} else if (guard instanceof SDTMultiGuard) {
    		SDTMultiGuard mg = (SDTMultiGuard) guard;
    		for (SDTGuard g : mg.getGuards()) {
    			filtered = removePotentialEqualities(filtered, g, valueMapping);
    		}
    	}
    	return filtered;
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
}
