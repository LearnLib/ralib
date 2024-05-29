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
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.word.Word;

/**
 *
 * @author Sofia Cassel
 * @param<T>
 */
public abstract class InequalityTheoryWithEq<T> implements Theory<T> {

	boolean useSuffixOpt = false;

	public Map<DataValue<T>, SDTGuard> equivalenceClasses(Word<PSymbolInstance> prefix,
			SymbolicSuffix suffix,
			SuffixValue suffixValue,
			Map<DataValue<T>, SymbolicDataValue> potValuation,
			Constants consts) {
		Map<DataValue<T>, SDTGuard> valueGuards = generateEquivClasses(prefix, suffixValue, potValuation, consts);
		// apply suffix restrictions
		return filterEquivClasses(valueGuards, prefix, suffix, suffixValue, potValuation, consts);
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
			Constants consts) {
		WordValuation suffixVals = new WordValuation();
		Iterator<Map.Entry<DataValue<T>, SymbolicDataValue>> it = potValuation
				.entrySet()
				.stream()
				.filter(e -> e.getValue() instanceof SuffixValue)
				.iterator();
		while (it.hasNext()) {
			Map.Entry<DataValue<T>, SymbolicDataValue> e = it.next();
			suffixVals.put(e.getValue().getId(), e.getKey());
		}
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
		assert currSdt != null;

		Comparator<DataValue<T>> comparator = getComparator();
		Iterator<DataValue<T>> filtit = sort(filteredOut).iterator();
		DataValue<T> smallerDataValue = getSmallerDataValue(first);
		DataValue<T> filtered = filtit.hasNext() ? filtit.next() : smallerDataValue;
		boolean lastFilteredOut = comparator.compare(first, filtered) == 0;

		while(ecit.hasNext()) {
			DataValue<T> next = ecit.next();
			SDTGuard nextGuard = equivClasses.get(next);
			SDT nextSdt = sdts.get(nextGuard);
			boolean thisFilteredOut = comparator.compare(next, filtered) == 0;
			boolean inequivalentSdts = !currSdt.isEquivalent(nextSdt, new VarMapping<>());
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

		return splitDisjunctions(merged);
	}

	private SDTGuard mergeIntervals(SDTGuard leftGuard, SDTGuard rightGuard) {
		SuffixValue suffixValue = leftGuard.getParameter();
		if (leftGuard instanceof IntervalGuard) {
			IntervalGuard igLeft = (IntervalGuard)leftGuard;
			if (!igLeft.isBiggerGuard()) {
				if (rightGuard instanceof EqualityGuard &&
						((EqualityGuard) rightGuard).getRegister().equals(igLeft.getRightReg())) {
					return new SDTOrGuard(suffixValue, leftGuard, rightGuard);
				}
			}
		} else if (leftGuard instanceof EqualityGuard) {
			EqualityGuard egLeft = (EqualityGuard)leftGuard;
			if (rightGuard instanceof IntervalGuard) {
				IntervalGuard igRight = (IntervalGuard)rightGuard;
				if (!igRight.isSmallerGuard() &&
						igRight.getLeftReg().equals(egLeft.getRegister())) {
					return new SDTOrGuard(suffixValue, leftGuard, rightGuard);
				}
			} else if (rightGuard instanceof SDTOrGuard) {
				List<SDTGuard> subGuards = ((SDTOrGuard) rightGuard).getGuards();
				if (subGuards.size() == 1)
					return mergeIntervals(leftGuard, subGuards.get(0));
				if (subGuards.size() == 2 &&
						subGuards.get(0) instanceof IntervalGuard &&
						subGuards.get(1) instanceof EqualityGuard) {
					IntervalGuard igRight = (IntervalGuard) subGuards.get(0);
					EqualityGuard egRight = (EqualityGuard) subGuards.get(1);
					SymbolicDataValue lr = egLeft.getRegister();
					SymbolicDataValue rr = egRight.getRegister();
					if (igRight.isIntervalGuard() &&
							igRight.getLeftReg().equals(lr) &&
							igRight.getRightReg().equals(rr)) {
						return new SDTOrGuard(suffixValue, egLeft, igRight, egRight);
					}
				}
			}
		} else if (leftGuard instanceof SDTMultiGuard) {
			List<SDTGuard> subGuards = ((SDTOrGuard) leftGuard).getGuards();
			if (subGuards.size() == 1)
				return mergeIntervals(subGuards.get(0), rightGuard);
			if (subGuards.size() == 2) {
				if (subGuards.get(0) instanceof IntervalGuard &&
						subGuards.get(1) instanceof EqualityGuard &&
						rightGuard instanceof IntervalGuard) {
					IntervalGuard igLeft = (IntervalGuard) subGuards.get(0);
					EqualityGuard egMid = (EqualityGuard) subGuards.get(1);
					IntervalGuard igRight = (IntervalGuard) rightGuard;
					SymbolicDataValue r = egMid.getRegister();
					if (!igLeft.isBiggerGuard() && igLeft.getRightReg().equals(r) &&
							!igRight.isSmallerGuard() && igRight.getLeftReg().equals(r)) {
						if (igLeft.isSmallerGuard()) {
							if (igRight.isBiggerGuard()) {
								return new SDTTrueGuard(suffixValue);
							} else if (igRight.isIntervalGuard()) {
								return new IntervalGuard(suffixValue, null, igRight.getRightReg());
							}
						} else if (igLeft.isIntervalGuard()) {
							if (igRight.isBiggerGuard()) {
								return new IntervalGuard(suffixValue, igLeft.getLeftReg(), null);
							} else if (igRight.isIntervalGuard()) {
								return new IntervalGuard(suffixValue, igLeft.getLeftReg(), igRight.getRightReg());
							}
						}
					}
				} else if (subGuards.get(0) instanceof EqualityGuard &&
						subGuards.get(1) instanceof IntervalGuard &&
						rightGuard instanceof EqualityGuard) {
					EqualityGuard egLeft = (EqualityGuard) subGuards.get(0);
					IntervalGuard igMid = (IntervalGuard) subGuards.get(1);
					EqualityGuard egRight = (EqualityGuard) rightGuard;
					if (egLeft.getRegister().equals(igMid.getLeftReg()) &&
							egRight.getRegister().equals(igMid.getRightReg())) {
						return new SDTOrGuard(suffixValue, egLeft, igMid, egRight);
					}
				}
			} else if (subGuards.size() == 3) {
				if (subGuards.get(0) instanceof EqualityGuard &&
						subGuards.get(1) instanceof IntervalGuard &&
						subGuards.get(2) instanceof EqualityGuard &&
						rightGuard instanceof IntervalGuard) {
					EqualityGuard egFirst = (EqualityGuard) subGuards.get(0);
					IntervalGuard igSecond = (IntervalGuard) subGuards.get(1);
					EqualityGuard egThird = (EqualityGuard) subGuards.get(2);
					IntervalGuard igLast = (IntervalGuard) rightGuard;
					SymbolicDataValue lr = egFirst.getRegister();
					SymbolicDataValue rr = egThird.getRegister();
					if (igSecond.isIntervalGuard() &&
							igSecond.getLeftReg().equals(lr) &&
							igSecond.getRightReg().equals(rr)) {
						if (!igLast.isSmallerGuard() && igLast.getLeftReg().equals(rr)) {
							return new SDTOrGuard(suffixValue,
									egFirst,
									new IntervalGuard(suffixValue, lr, igLast.getRightReg()));
						}
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

	private Map<SDTGuard, SDT> splitDisjunctions(Map<SDTGuard, SDT> children) {
		Map<SDTGuard, SDT> ret = new LinkedHashMap<>();
		for (Map.Entry<SDTGuard, SDT> e : children.entrySet()) {
			SDTGuard guard = e.getKey();
			SDT sdt = e.getValue();
			if (guard instanceof SDTOrGuard) {
				for (SDTGuard g : ((SDTOrGuard) guard).getGuards()) {
					ret.put(g, sdt);
				}
			} else {
				ret.put(guard, sdt);
			}
		}
		return ret;
	}

    private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
        PIV ret = new PIV();
        for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
            SDTGuard mg = e.getKey();
            if (mg instanceof SDTIfGuard) {
                //LOGGER.trace(mg.toString());
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
        Map<DataValue<T>, SDTGuard> filteredEquivClasses = filterEquivClasses(equivClasses, prefix, suffix, currentParam, pot, consts);

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

    private DataValue getRegisterValue(SymbolicDataValue r, PIV piv,
            List<DataValue> prefixValues, Constants constants,
            ParValuation pval) {
        if (r.isRegister()) {
//            LOGGER.trace("piv: " + piv + " " + r.toString() + " " + prefixValues);
            Parameter p = piv.getOneKey((Register) r);
//            LOGGER.trace("p: " + p.toString());
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

//        LOGGER.trace("prefix values : " + prefixValues.toString());

        if (guard instanceof EqualityGuard) {
            EqualityGuard eqGuard = (EqualityGuard) guard;
            SymbolicDataValue ereg = eqGuard.getRegister();
            if (ereg.isRegister()) {
//                LOGGER.trace("piv: " + piv.toString() + " " + ereg.toString() + " " + param.toString());
                Parameter p = piv.getOneKey((Register) ereg);
//                LOGGER.trace("p: " + p.toString());
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
            //System.out.println("already used = " + alreadyUsedValues);
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
                //instantiate(guard, val, param, constants);
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
                returnThis = this.instantiate(prefix, ps, piv, pval, constants, aGuard, param, oldDvs);
//                val = getValuation(guard, piv, prefixValues, constants, pval);
//                returnThis = instantiate(aGuard, val, constants, alreadyUsedValues);
            } else {
                throw new IllegalStateException("only =, != or interval allowed. Got " + guard);
            }

//                        }
//                    } else if (iGuard instanceof SDTIfGuard) {
//                        SymbolicDataValue r = ((SDTIfGuard) iGuard).getRegister();
//                        DataValue<T> regVal = getRegisterValue(r, piv,
//                                prefixValues, constants, pval);
//                        val.setValue(r.toVariable(), regVal);
//                    }
//                }
//            }
            if (!(oldDvs.isEmpty())) {
//                System.out.println("old dvs: " + oldDvs);
                for (DataValue<T> oldDv : oldDvs) {
                    Valuation newVal = new Valuation();
                    newVal.putAll(val);
                    newVal.setValue(toVariable( new SuffixValue(param.getType(), param.getId()) ), oldDv.getId());
//            System.out.println("instantiating " + guard + " with " + newVal);
                    DataValue inst = instantiate(guard, newVal, constants, alreadyUsedValues);
                    if (inst != null) {
//                        System.out.println("returning (reused): " + inst);
                        return inst;
                    }
                }
            }
            returnThis = instantiate(guard, val, constants, alreadyUsedValues);
//            System.out.println("returning (no reuse): " + ret);

        }
        return returnThis;
    }

    public void useSuffixOptimization(boolean useSuffixOpt) {
    	this.useSuffixOpt = useSuffixOpt;
    }

}
