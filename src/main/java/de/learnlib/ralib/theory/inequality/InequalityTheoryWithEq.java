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
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.commons.util.Pair;
import net.automatalib.words.Word;

/**
 *
 * @author Sofia Cassel
 * @param<T>
 */
public abstract class InequalityTheoryWithEq<T extends Comparable<T>> implements TypedTheory<T> {

	private static final LearnLogger log = LearnLogger.getLogger(InequalityTheoryWithEq.class);
	private static final Map<Class<?>, ContinuousInequalityMerger> sdtMergers = new LinkedHashMap<>();
	{ 
		sdtMergers.put(Integer.class, new DiscreteInequalityMerger());
		sdtMergers.put(Double.class, new ContinuousInequalityMerger());
	}
	private boolean freshValues;
//	private InequalityMerger inequalityMerger;
	
	
	public InequalityTheoryWithEq() {
		this.freshValues = false;
//		this.getClass().getGenericInterfaces()[0].getClass()
//		this.inequalityMerger = new InequalityMerger(null);
	}
	
	private Map<SDTGuard, SDT> mergeGuards(final Map<SDTGuard, SDT> tempGuards, Map<SDTGuard, DataValue<T>> instantiations) {
		if (tempGuards.size() == 1) { // for true guard do nothing
			return tempGuards;
		}
		
		final List<SDTGuard> sortedGuards = tempGuards.keySet().stream().sorted(new Comparator<SDTGuard>() {
			public int compare(SDTGuard o1, SDTGuard o2) {
				DataValue<T> dv1 = instantiations.get(o1);
				DataValue<T> dv2 = instantiations.get(o2);
				int ret = ((java.lang.Comparable) dv1.getId()).compareTo((java.lang.Comparable) dv2.getId());
				// the generated guards can never have the same dv instantiation. In case they do, it signals collision and needs to be addressed.
				if (ret == 0) {
					throw new DecoratedRuntimeException("Different guards are instantiated with equal Dv")
					.addDecoration("guard1:", o1).addDecoration("dv1", dv1)
					.addDecoration("guard2:", o2).addDecoration("dv2", dv2);
				}
				return ret;
			}
		}).collect(Collectors.toList());
		
//		InequalityGuardMerger merger = new OldInequalityGuardsMerger();
//		//Map<SDTGuard, SDT> 
//		Map<SDTGuard, SDT> merged = merger.merge(sortedGuards, tempGuards);
//		System.out.println("merged1:\n" + merged);
		
		ContinuousInequalityMerger inequalityMerger = new ContinuousInequalityMerger();
		//Map<SDTGuard, SDT> 
		Map<SDTGuard, SDT> merged2 =inequalityMerger.merge(sortedGuards, tempGuards);
//		System.out.println("merged2:\n" + merged2);

		//assert merged.size() == merged2.size() && merged2.equals(merged);

		return merged2;
	}
	
    @Override
    public void setCheckForFreshOutputs(boolean doit) {
        this.freshValues = doit;
    }


	// given a set of registers and a set of guards, keep only the registers
	// that are mentioned in any guard
	//
	private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
		PIV ret = new PIV();
		for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
			SDTGuard mg = e.getKey();
			if (mg instanceof SDTIfGuard) {
				log.log(Level.FINEST, mg.toString());
				SymbolicDataValue r = ((SDTIfGuard) mg).getRegister();
				Parameter p = new Parameter(r.getType(), r.getId());
				if (r instanceof Register) {
					ret.put(p, (Register) r);
				}
			} else if (mg instanceof IntervalGuard) {
				IntervalGuard iGuard = (IntervalGuard) mg;
				if (!iGuard.isBiggerGuard()) {
					SymbolicDataValue r = iGuard.getRightSDV();
					Parameter p = new Parameter(r.getType(), r.getId());
					if (r instanceof Register) {
						ret.put(p, (Register) r);
					}

				}
				if (!iGuard.isSmallerGuard()) {
					SymbolicDataValue r = iGuard.getLeftSDV();
					Parameter p = new Parameter(r.getType(), r.getId());
					if (r instanceof Register) {
						ret.put(p, (Register) r);
					}
				}
			} else if (mg instanceof SDTOrGuard) {
				Set<SymbolicDataValue> rSet = ((SDTOrGuard) mg).getAllSDVsFormingGuard();
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
	public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values, PIV piv,
			Constants constants, SuffixValuation suffixValues, SDTConstructor oracle, IOOracle traceOracle) {

		int pId = values.size() + 1;
		List<SymbolicDataValue> regPotential = new ArrayList<>();
		SuffixValue sv = suffix.getDataValue(pId);
		DataType type = sv.getType();

		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

		DataValue<T>[] typedPrefixValues = DataWords.valsOf(prefix, type);
		WordValuation typedPrefixValuation = new WordValuation();
		for (int i = 0; i < typedPrefixValues.length; i++) {
			typedPrefixValuation.put(i + 1, typedPrefixValues[i]);
		}

		SuffixValue currentParam = new SuffixValue(type, pId);

		Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

		Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
				DataWords.<T>valSet(prefix, type), suffixValues.<T>values(type));

		List<DataValue<T>> potList = new ArrayList<>(potSet);
		List<DataValue<T>> potential = getPotential(potList);
		// WE ASSUME THE POTENTIAL IS SORTED
		int potSize = potential.size();
		Map<SDTGuard, DataValue<T>> guardDvs = new LinkedHashMap<>();

        ParameterizedSymbol ps = computeSymbol(suffix, pId);
		// special case: fresh values in outputs
        if (this.freshValues) {

            if (ps instanceof OutputSymbol && ps.getArity() > 0) {

                int idx = computeLocalIndex(suffix, pId);
                Word<PSymbolInstance> query = buildQuery(
                        prefix, suffix, values);
                Word<PSymbolInstance> trace = traceOracle.trace(query);
                PSymbolInstance out = trace.lastSymbol();

                if (out.getBaseSymbol().equals(ps)) {

                    DataValue<T> d = out.getParameterValues()[idx];

                    if (d instanceof FreshValue) {
                        d = getFreshValue(potential);
                        values.put(pId, new FreshValue<T>(d.getType(), d.getId()));
                        WordValuation trueValues = new WordValuation();
                        trueValues.putAll(values);
                        SuffixValuation trueSuffixValues
                                = new SuffixValuation();
                        trueSuffixValues.putAll(suffixValues);
                        trueSuffixValues.put(sv, d);
                        SDT sdt = oracle.treeQuery(
                                prefix, suffix, trueValues,
                                piv, constants, trueSuffixValues);

                        log.log(Level.FINEST,
                                " single deq SDT : " + sdt.toString());

                        Map<SDTGuard, SDT> temp = new LinkedHashMap<SDTGuard, SDT>();
                        SDTTrueGuard trueGuard = new SDTTrueGuard(currentParam);
                        temp.put(trueGuard, sdt);
                        guardDvs.put(trueGuard, d);
                        Map<SDTGuard, SDT> merged = mergeGuards(temp, guardDvs);

                        log.log(Level.FINEST,
                                "temporary guards = " + tempKids.keySet());
                        log.log(Level.FINEST,
                                "merged guards = " + merged.keySet());
                        log.log(Level.FINEST,
                                "merged pivs = " + piv.toString());

                        return new SDT(merged);
                    }
                }
            }
        }

		// System.out.println("potential " + potential);
		if (potential.isEmpty()) {
			// System.out.println("empty potential");
			WordValuation elseValues = new WordValuation();
			DataValue<T> fresh = getFreshValue(potential);
			elseValues.putAll(values);
			elseValues.put(pId, fresh);

			// this is the valuation of the suffixvalues in the suffix
			SuffixValuation elseSuffixValues = new SuffixValuation();
			elseSuffixValues.putAll(suffixValues);
			elseSuffixValues.put(sv, fresh);

			SDT elseOracleSdt = oracle.treeQuery(prefix, suffix, elseValues, piv, constants, elseSuffixValues);
			tempKids.put(new SDTTrueGuard(currentParam), elseOracleSdt);
		} // process each '<' case
		else {
			// Parameter p = new Parameter(
			// currentParam.getType(), currentParam.getId());

			// smallest case
			WordValuation smValues = new WordValuation();
			smValues.putAll(values);
			SuffixValuation smSuffixValues = new SuffixValuation();
			smSuffixValues.putAll(suffixValues);

			Valuation smVal = new Valuation();
			DataValue<T> dvRight = potential.get(0);
			IntervalGuard sguard = makeSmallerGuard(dvRight, prefixValues, currentParam, smValues, piv, constants);
			SymbolicDataValue rsm = (SymbolicDataValue) sguard.getRightExpr();
			// System.out.println("setting valuation, symDV: " +
			// rsm.toVariable() + " dvright: " + dvRight);
			smVal.setValue(toVariable(rsm), dvRight.getId());
			DataValue<T> smcv = IntervalDataValue.instantiateNew(null, dvRight); 
					//instantiate(sguard, smVal, constants, potential);
		//	smcv = new IntervalDataValue<T>(smcv, null, dvRight);
			smValues.put(pId, smcv);
			smSuffixValues.put(sv, smcv);

			SDT smoracleSdt = oracle.treeQuery(prefix, suffix, smValues, piv, constants, smSuffixValues);

			tempKids.put(sguard, smoracleSdt);
			guardDvs.put(sguard, smcv);

			// biggest case
			WordValuation bgValues = new WordValuation();
			bgValues.putAll(values);
			SuffixValuation bgSuffixValues = new SuffixValuation();
			bgSuffixValues.putAll(suffixValues);

			Valuation bgVal = new Valuation();

			DataValue<T> dvLeft = potential.get(potSize - 1);
			IntervalGuard bguard = makeBiggerGuard(dvLeft, prefixValues, currentParam, bgValues, piv, constants);
			updateValuation(bgVal, bguard.getLeftExpr(), dvLeft);
			DataValue<T> bgcv = IntervalDataValue.instantiateNew(dvLeft, null); 
					//instantiate(bguard, bgVal, constants, potential);
			//bgcv = new IntervalDataValue<T>(bgcv, dvLeft, null);
			bgValues.put(pId, bgcv);
			bgSuffixValues.put(sv, bgcv);

			SDT bgoracleSdt = oracle.treeQuery(prefix, suffix, bgValues, piv, constants, bgSuffixValues);

			tempKids.put(bguard, bgoracleSdt);
			guardDvs.put(bguard, bgcv);

			if (potSize > 1) { // middle cases
				for (int i = 1; i < potSize; i++) {

					WordValuation currentValues = new WordValuation();
					currentValues.putAll(values);
					SuffixValuation currentSuffixValues = new SuffixValuation();
					currentSuffixValues.putAll(suffixValues);
					// SDTGuard guard;
					Valuation val = new Valuation();
					DataValue<T> dvMRight = potential.get(i);
					DataValue<T> dvMLeft = potential.get(i - 1);

					// IntervalGuard smallerGuard = makeSmallerGuard(
					// dvMRight, prefixValues,
					// currentParam, currentValues, piv);
					// IntervalGuard biggerGuard = makeBiggerGuard(
					// dvMLeft, prefixValues, currentParam,
					// currentValues, piv);
					IntervalGuard intervalGuard = makeIntervalGuard(dvMLeft, dvMRight, prefixValues, currentParam,
							currentValues, piv, constants);

					// IntervalGuard guard = new IntervalGuard(
					// currentParam, biggerGuard.getLeftReg(),
					// smallerGuard.getRightReg());
					SymbolicDataValue rs = intervalGuard.getRightSDV();
					SymbolicDataValue rb = intervalGuard.getLeftSDV();
					updateValuation(val, intervalGuard.getRightExpr(), dvMRight);
					updateValuation(val, intervalGuard.getLeftExpr(), dvMLeft);

					DataValue<T> cv = IntervalDataValue.instantiateNew(dvMLeft, dvMRight);//instantiate(intervalGuard, val, constants, potential);
					//cv = new IntervalDataValue<T>(cv,  dvMLeft, dvMRight);
					currentValues.put(pId, cv);
					currentSuffixValues.put(sv, cv);

					SDT oracleSdt = oracle.treeQuery(prefix, suffix, currentValues, piv, constants,
							currentSuffixValues);

					tempKids.put(intervalGuard, oracleSdt);
					guardDvs.put(intervalGuard, cv);
					regPotential.add(i - 1, rb);
					regPotential.add(i, rs);
				}
			}
			// System.out.println("eq potential is: " + potential);
			for (DataValue<T> newDv : potential) {
				// log.log(Level.FINEST, newDv.toString());

				// this is the valuation of the suffixvalues in the suffix
				SuffixValuation ifSuffixValues = new SuffixValuation();
				ifSuffixValues.putAll(suffixValues); // copy the suffix
														// valuation

				// construct the equality guard. Depending on newDv, a certain
				// type of equality is instantiated (newDv can be a SumCDv, in
				// which case a SumC equality is instantiated)
				EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues, currentParam, values, constants);
				// log.log(Level.FINEST, "eqGuard is: " + eqGuard.toString());

				// we normalize newDv so that we only store plain data values in
				// the if suffix
//				newDv = new DataValue<T>(newDv.getType(), newDv.getId());
				// construct the equality guard
				// find the data value in the prefix
				// this is the valuation of the positions in the suffix
				WordValuation ifValues = new WordValuation();
				ifValues.putAll(values);
				ifValues.put(pId, newDv);
				ifSuffixValues.put(sv, newDv);
				SDT eqOracleSdt = oracle.treeQuery(prefix, suffix, ifValues, piv, constants, ifSuffixValues);

				tempKids.put(eqGuard, eqOracleSdt);
				guardDvs.put(eqGuard, newDv);
			}

		}

		//System.out.println("TEMPKIDS for " + prefix + " + " + suffix + " = " + tempKids);
		// Map<SDTGuard, SDT> merged = mgGuards(tempKids, currentParam, regPotential);
		Map<SDTGuard, SDT> merged = mergeGuards(tempKids, guardDvs);
		// Map<SDTGuard, SDT> merged = tempKids;
		// only keep registers that are referenced by the merged guards
		//System.out.println("MERGED = " + merged);
		assert !merged.keySet().isEmpty();
//		if (ps instanceof OutputSymbol && merged.size() >= 3) {
//			System.out.println(prefix + " " + suffix + " " + suffixValues);
//			System.out.println(tempKids);
//			System.out.println(merged);
//			guardDvs.forEach((g, dv) -> System.out.println(g + " " + dv ));
//			throw new RuntimeException("For an output symbol, there cannot be more than 2 branches");
//		}

		// System.out.println("MERGED = " + merged);
		piv.putAll(keepMem(merged));

		log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
		log.log(Level.FINEST, "merged guards = " + merged.keySet());
		log.log(Level.FINEST, "merged pivs = " + piv.toString());

		// clear the temporary map of children
		tempKids = new LinkedHashMap<SDTGuard, SDT>();

		for (SDTGuard g : merged.keySet()) {
			assert !(g == null);
			if (g instanceof SDTTrueGuard) {
				if (merged.keySet().size() != 1) {
					throw new IllegalStateException("only one true guard allowed: \n" + prefix + " + " + suffix);
				}
				// assert merged.keySet().size() == 1;
			}
		}
//		System.out.println("MERGED = " + merged);
		SDT returnSDT = new SDT(merged);
		return returnSDT;

	}

	private DataValue<T> updateValuation(Valuation valuation, SymbolicDataExpression expr, DataValue<T> concValue) {
		SymbolicDataValue sdvForExpr = expr.getSDV();
		DataValue<T> sdvValuation;
		if (expr instanceof SymbolicDataValue) {
			sdvValuation = concValue;
		} else {
			if (expr instanceof SumCDataExpression) {
				sdvValuation = ((SumCDataValue<T>) concValue).getOperand();
			} else {
				throw new RuntimeException(
						"Cannot update valuation for expression " + expr + " assigned data value " + concValue);
			}
		}
		valuation.setValue(toVariable(sdvForExpr), sdvValuation.getId());
		return sdvValuation;
	}

	private IntervalGuard makeIntervalGuard(DataValue<T> biggerDv, DataValue<T> smallerDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
		IntervalGuard smallerGuard = makeSmallerGuard(smallerDv, prefixValues, currentParam, ifValues, pir, constants);
		IntervalGuard biggerGuard = makeBiggerGuard(biggerDv, prefixValues, currentParam, ifValues, pir, constants);
		return new IntervalGuard(currentParam, biggerGuard.getLeftExpr(), smallerGuard.getRightExpr());
	}

	private IntervalGuard makeBiggerGuard(DataValue<T> biggerDv, List<DataValue> prefixValues, SuffixValue currentParam,
			WordValuation ifValues, PIV pir, Constants constants) {
		SymbolicDataExpression regOrSuffixExpr = getSDExprForDV(biggerDv, prefixValues, currentParam, ifValues,
				constants);
		IntervalGuard bg = new IntervalGuard(currentParam, regOrSuffixExpr, null);
		return bg;
	}

	private IntervalGuard makeSmallerGuard(DataValue<T> smallerDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
		SymbolicDataExpression regOrSuffixExpr = getSDExprForDV(smallerDv, prefixValues, currentParam, ifValues,
				constants);
		IntervalGuard sg = new IntervalGuard(currentParam, null, regOrSuffixExpr);
		return sg;
	}

	private EqualityGuard pickupDataValue(DataValue<T> newDv, List<DataValue> prefixValues, SuffixValue currentParam,
			WordValuation ifValues, Constants constants) {
		DataType type = currentParam.getType();
		SymbolicDataExpression sdvExpr = getSDExprForDV(newDv, prefixValues, currentParam, ifValues, constants);
		return new EqualityGuard(currentParam, sdvExpr);
	}

	private SymbolicDataExpression getSDExprForDV(DataValue<T> dv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, Constants constants) {
		SymbolicDataValue SDV;
		if (dv instanceof SumCDataValue) {
			SumCDataValue<T> sumDv = (SumCDataValue<T>) dv;
			SDV = getSDVForDV(sumDv.toRegular(), prefixValues, currentParam, ifValues, constants);
			// if there is no previous value equal to the summed value, we pick
			// the data value referred by the sum
			// by this structure, we always pick equality before sumc equality
			// when the option is available
			if (SDV == null) {
				DataValue<T> constant = sumDv.getConstant();
				DataValue<T> prevDV = sumDv.getOperand();
				SymbolicDataValue prevSDV = getSDVForDV(prevDV, prefixValues, currentParam, ifValues, constants);
				return new SumCDataExpression(prevSDV, sumDv.getConstant());
			} else {
				return SDV;
			}
		} else {
			SDV = getSDVForDV(dv, prefixValues, currentParam, ifValues, constants);
			return SDV;
		}
	}

	private SymbolicDataValue getSDVForDV(DataValue<T> dv, @Nullable List<DataValue> prefixValues, SuffixValue currentParam,
			WordValuation ifValues, Constants constants) {
		int newDv_i;
		DataType type = currentParam.getType();
		if (prefixValues.contains(dv)) {
			newDv_i = prefixValues.indexOf(dv) + 1;
			Register newDv_r = new Register(type, newDv_i);
			return newDv_r;
		}

		if (ifValues.containsValue(dv)) {
			int smallest = Collections.max(ifValues.getAllKeys(dv));
			return new SuffixValue(type, smallest);
		}

		if (constants.containsValue(dv)) {
			for (SymbolicDataValue.Constant c : constants.keySet()) {
				if (constants.get(c).equals(dv)) {
					return c;
				}
			}
		}

		return null;
	}

	public abstract List<DataValue<T>> getPotential(List<DataValue<T>> vals);

	private DataValue getRegisterValue(SymbolicDataValue r, PIV piv, List<DataValue> prefixValues, Constants constants,
			ParValuation pval) {
		if (r.isRegister()) {
			log.log(Level.FINEST, "piv: " + piv + " " + r.toString() + " " + prefixValues);
			Parameter p = piv.getOneKey((Register) r);
			log.log(Level.FINEST, "p: " + p.toString());
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

	public abstract DataValue<T> instantiate(SDTGuard guard, Valuation val, Constants constants,
			Collection<DataValue<T>> alreadyUsedValues);

	private DataValue<T> instantiateSDExpr(SymbolicDataExpression sdExpr, DataType type, List<DataValue> prefixValues,
			PIV piv, ParValuation pval, Constants constants) {
		DataValue<T> returnThis = null;
		if (sdExpr instanceof SumCDataExpression) {
			DataValue<T> opDv = instantiateSDExpr(sdExpr.getSDV(), type, prefixValues, piv, pval, constants);
			returnThis = new SumCDataValue(opDv, ((SumCDataExpression) sdExpr).getConstant());
		} else {
			assert sdExpr.isConstant() || sdExpr.isParameter() || sdExpr.isRegister() || sdExpr.isSuffixValue();
			returnThis = getRegisterValue((SymbolicDataValue)sdExpr, piv, prefixValues, constants, pval);
		}
		return returnThis;
	}

	
	/**
	 * Instantiates the guard over the symbol {@ps param} for the given values. If guard is satisfiable,  returns a DataValue 
	 * containing the concrete value, but also symbolic information on how it was originated. Otherwise, returns null.
	 *   
	 * <b>NOTE:</b> The instantiate function uniformly makes use of the IntervalDataValue.instantiateNew in order to instantiate intervals.
	 * We do this instead of using the cs-solver for better control over what is instantiated and also easier instantiation.
	 * It is important that DataValues that come out are annotated s.t. they reflect the guards that satisfy. This is needed
	 * for canonicalization purposes. 
	 * </br>
	 * The implementation IS NOT full proof, since our current DataValue sub-types cannot express conjunctions, just lone guards. To fully handle
	 * conjunctions, we would need to use a cs-solver and also value independent annotations of the DataValues. (see SuccessorDataValue as an
	 * example)
	 *   
	 */
	@Override
	public DataValue<T> instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
			Constants constants, SDTGuard guard, Parameter param, Set<DataValue<T>> oldDvs, boolean useSolver) {

		useSolver = useSolver || !this.freshValues;
		DataType type = param.getType();
		DataValue<T> returnValue = null;
		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
		log.log(Level.FINEST, "prefix values : " + prefixValues.toString());

		if (guard instanceof EqualityGuard) {
			EqualityGuard eqGuard = (EqualityGuard) guard;
			returnValue = instantiateSDExpr(eqGuard.getExpression(), type, prefixValues, piv, pval, constants);
			assert returnValue != null;
		} else if (guard instanceof SDTTrueGuard) {
			// might be a problem, what if we select an increment as a fresh
			// value ?
			Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
					DataWords.<T>valSet(prefix, type), pval.<T>values(type));

			returnValue = this.getFreshValue(new ArrayList<>(potSet));
		} else {
			Collection<DataValue<T>> alreadyUsedValues = DataWords.<T>joinValsToSet(constants.<T>values(type),
					DataWords.<T>valSet(prefix, type), pval.<T>values(type));
			Valuation val = new Valuation();
			if( guard instanceof DisequalityGuard) {
				DisequalityGuard diseqGuard = (DisequalityGuard) guard;
				SymbolicDataValue r = (SymbolicDataValue) diseqGuard.getRegister();
				DataValue<T> diseqExprVal = instantiateSDExpr(diseqGuard.getExpression(), r.getType(), prefixValues, piv, pval, constants);
				DataValue<T> rRegVal = getRegisterValue(r, piv, prefixValues, constants, pval);
				val.setValue(toVariable(r), rRegVal.getId());
				
				Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
						DataWords.<T>valSet(prefix, type), pval.<T>values(type));
				returnValue = this.getFreshValue(new ArrayList<>(potSet));
			} else
			
			if (guard instanceof IntervalGuard) {
				IntervalGuard iGuard = (IntervalGuard) guard;
				DataValue<T> rExprVal = null, lExprVal = null, rRegVal = null, lRegVal = null;
				if (!iGuard.isBiggerGuard()) {
					SymbolicDataValue r = (SymbolicDataValue) iGuard.getRightSDV();
					rExprVal = instantiateSDExpr(iGuard.getRightExpr(), r.getType(), prefixValues,  piv, pval, constants);
					rRegVal = getRegisterValue(r, piv, prefixValues, constants, pval);

					val.setValue(toVariable(r), rRegVal.getId());
				} 
				
				if (!iGuard.isSmallerGuard()) {
					SymbolicDataValue l = (SymbolicDataValue) iGuard.getLeftSDV();
					lExprVal = instantiateSDExpr(iGuard.getLeftExpr(), l.getType(), prefixValues,  piv, pval, constants);
					lRegVal = getRegisterValue(l, piv, prefixValues, constants, pval);

					val.setValue(toVariable(l), lRegVal.getId());
				} 
				
				for (DataValue<T> oldDv : oldDvs) {
					if ((lExprVal == null ||  lExprVal.getId().compareTo(oldDv.getId()) < 0) && (rExprVal == null ||  rExprVal.getId().compareTo(oldDv.getId()) > 0)) 
						return new IntervalDataValue<>(oldDv, lExprVal, rExprVal);
				}
				
				if (!useSolver) {
					return IntervalDataValue.instantiateNew(lExprVal, rExprVal);
				} else {
				 //we decorate it with interval information
					returnValue = instantiate(guard, val, constants, alreadyUsedValues); 
					returnValue = new IntervalDataValue<>(returnValue, lExprVal, rExprVal);
				}
			} else if (guard instanceof SDTIfGuard) {
				SymbolicDataValue r = ((SDTIfGuard) guard).getRegister();
				DataValue<T> regVal = getRegisterValue(r, piv, prefixValues, constants, pval);
				val.setValue(toVariable(r), regVal.getId());
			} else if (guard instanceof SDTMultiGuard) {
				List<SDTGuard> allGuards = ((SDTMultiGuard) guard).getGuards();
				List<SDTGuard> eqGuards = allGuards.stream().filter(g -> g instanceof EqualityGuard).distinct()
						.collect(Collectors.toList());
				if (!eqGuards.isEmpty()) {
					// if the end guard contains two equ guards it should not be
					// instantiateable
					if (eqGuards.size() > 1 && guard instanceof SDTAndGuard) {
						assert !instantiateSDExpr(((SDTIfGuard) eqGuards.get(0)).getExpression(), type, prefixValues,
								piv, pval, constants)
										.equals(instantiateSDExpr(((SDTIfGuard) eqGuards.get(1)).getExpression(), type,
												prefixValues, piv, pval, constants));
						return null;
					}

					SDTGuard firstEqGuard = eqGuards.get(0);
					DataValue<T> equValue = instantiateSDExpr(((SDTIfGuard) firstEqGuard).getExpression(), type,
							prefixValues, piv, pval, constants);
					assert equValue != null;
					// for an OR guard, we pick equality whenever there's an
					// equality guard contained
					if (guard instanceof SDTOrGuard)
						return equValue;
					// for an AND guard, we set the suffix so that it satisfied
					// the equality guard and then we check if this relation can
					// be satisfied
					else {
						
						val.setValue(toVariable(guard.getParameter()), equValue.getId());
						alreadyUsedValues.remove(equValue);
					}

				} else {
					if (guard instanceof SDTAndGuard) {
							if (!useSolver) {
							List<SDTGuard> intervalGuards = allGuards.stream().filter(g -> g instanceof IntervalGuard).collect(Collectors.toList());
							if ( intervalGuards.size() >= 2) {
								throw new DecoratedRuntimeException("Cannot reliably instantiate a 2 guard interval with conjunctions")
								.addDecoration("intervals", intervalGuards);
							}
							Optional<SDTGuard> intGuard = intervalGuards.stream().findAny();
							if ( intGuard.isPresent()) {
								// this is required if we are not using the constraint solver for instantiation
								final List<DataValue<T>> prohibitedValues = allGuards.stream().filter(g -> g instanceof DisequalityGuard).
										map(g -> instantiateSDExpr(((SDTIfGuard) g).getExpression(), type,
												prefixValues, piv, pval, constants)).collect(Collectors.toList());
								oldDvs = oldDvs.stream().filter(a -> !prohibitedValues.contains(a)).collect(Collectors.toSet());
								SDTGuard intv = intGuard.get();
								DataValue<T> intDv = instantiate(prefix,ps, piv, pval, constants, intv, param, oldDvs, useSolver);
								return intDv;
							} 
						}
					}
				}

				Set<SymbolicDataValue> regs = ((SDTAndGuard) guard).getAllSDVsFormingGuard();
				regs.forEach(reg -> {
					DataValue<T> regVal = getRegisterValue(reg, piv, prefixValues, constants, pval);
					val.setValue(toVariable(reg), regVal.getId());
				});

			} else {
				throw new IllegalStateException("only =, !=, intervals, AND and OR are allowed");
			}

			if (!(oldDvs.isEmpty())) {
				// System.out.println("old dvs: " + oldDvs);
				for (DataValue<T> oldDv : oldDvs) {
					Valuation newVal = new Valuation();
					newVal.putAll(val);
					newVal.setValue(toVariable(new SuffixValue(param.getType(), param.getId())), oldDv.getId());
					// System.out.println("instantiating " + guard + " with " +
					// newVal);
					DataValue inst = instantiate(guard, newVal, constants, alreadyUsedValues);
					if (inst != null) {
						return inst;
					}
				}
			}

			if (returnValue == null)
				returnValue = instantiate(guard, val, constants, alreadyUsedValues);

		}
		return returnValue;
	}
	
	
	


    private ParameterizedSymbol computeSymbol(SymbolicSuffix suffix, int pId) {
        int idx = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            idx += a.getArity();
            if (idx >= pId) {
                return a;
            }
        }
        return suffix.getActions().size() > 0
                ? suffix.getActions().firstSymbol() : null;
    }

    private int computeLocalIndex(SymbolicSuffix suffix, int pId) {
        int idx = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            idx += a.getArity();
            if (idx >= pId) {
                return pId - (idx - a.getArity()) - 1;
            }
        }
        return pId - 1;
    }

    private Word<PSymbolInstance> buildQuery(Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix, WordValuation values) {

        Word<PSymbolInstance> query = prefix;
        int base = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            if (base + a.getArity() > values.size()) {
                break;
            }
            DataValue[] vals = new DataValue[a.getArity()];
            for (int i = 0; i < a.getArity(); i++) {
                vals[i] = values.get(base + i + 1);
            }
            query = query.append(new PSymbolInstance(a, vals));
            base += a.getArity();
        }
        return query;
    }
}
