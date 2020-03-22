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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.ParamSignature;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.oracles.mto.SDTQuery;
import de.learnlib.ralib.oracles.mto.SemanticEquivalenceChecker;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.IfElseGuardMerger;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTEquivalenceChecker;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.BranchingLogic.BranchingContext;
import de.learnlib.ralib.theory.inequality.BranchingLogic.BranchingStrategy;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.words.Word;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 *
 * @author Sofia Cassel
 * @param<T>
 */
public abstract class InequalityTheoryWithEq<T extends Comparable<T>> implements TypedTheory<T> {

	protected static final LearnLogger log = LearnLogger.getLogger(MethodHandles.lookup().lookupClass());

	protected static  de.learnlib.ralib.solver.ConstraintSolver getSolver(String name) {
		de.learnlib.ralib.solver.ConstraintSolver raLibSolver = de.learnlib.ralib.solver.ConstraintSolverFactory.createSolver(name);
		return raLibSolver;
	}
	
	/**
	 * Builds a guard instantiator. 
	 */
	protected static <P extends Comparable<P>> InequalityGuardInstantiator<P> getInstantiator(DataType<P> type,
			String name) {
		gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory fact = new gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory();
		gov.nasa.jpf.constraints.api.ConstraintSolver solver = fact.createSolver(name);;
		return new InequalityGuardInstantiatorImpl<P>(type, solver);
	}

	private boolean freshValues;
	protected DataType<T> type;
	private InequalityGuardInstantiator<T> instantiator;
	private final Function<DataType<T>, InequalityGuardInstantiator<T>> instantiatorSupplier;

	private final InequalityGuardMerger fullMerger;
	private final IfElseGuardMerger ifElseMerger;
	private boolean suffixOptimization;
	private de.learnlib.ralib.solver.ConstraintSolver solver;
	private Set<ParamSignature> exhSuffixParams;

	public InequalityTheoryWithEq(InequalityGuardMerger fullMerger,
			Function<DataType<T>, InequalityGuardInstantiator<T>> instantiatorSupplier, de.learnlib.ralib.solver.ConstraintSolver solver) {
		this.freshValues = false;
		this.suffixOptimization = false;
		this.instantiatorSupplier = instantiatorSupplier;
		this.fullMerger = fullMerger;
		this.ifElseMerger = new IfElseGuardMerger(this.getGuardLogic());
		this.solver = solver;
	}

	public InequalityTheoryWithEq(InequalityGuardMerger fullMerger) {
		this(fullMerger, t -> InequalityTheoryWithEq.getInstantiator(t, "z3"), getSolver("z3") );

	}

	@Override
	public void setCheckForFreshOutputs(boolean doit) {
		freshValues = doit;
	}

	/**
	 * Sets the type as well as the inequality guard merger and instantiator.
	 */
	public void setType(DataType<T> dataType) {
		this.type = dataType;
		instantiator = instantiatorSupplier.apply(dataType);
	}

	public DataType<T> getType() {
		return type;
	}
	
	protected Map<SDTGuard, SDT> mergeAllGuards(final Map<SDTGuard, SDT> tempGuards,
			Map<SDTGuard, DataValue<T>> instantiations, SDTEquivalenceChecker sdtChecker, Mapping<SymbolicDataValue, DataValue<?>> valuation) {
		if (tempGuards.size() == 1) { // for true guard do nothing
			return tempGuards;
		}

		final List<SDTGuard> sortedGuards = tempGuards.keySet().stream().sorted(new Comparator<SDTGuard>() {
			public int compare(SDTGuard o1, SDTGuard o2) {
				DataValue<T> dv1 = instantiations.get(o1);
				DataValue<T> dv2 = instantiations.get(o2);
				int ret = ((java.lang.Comparable) dv1.getId()).compareTo((java.lang.Comparable) dv2.getId());
				// the generated guards can never have the same dv
				// instantiation. In case they do, it signals collision and
				// needs to be addressed.
				if (ret == 0) {
					throw new DecoratedRuntimeException("Different guards are instantiated with equal Dv")
							.addDecoration("guard1:", o1).addDecoration("dv1", dv1).addDecoration("guard2:", o2)
							.addDecoration("dv2", dv2);
				}
				return ret;
			}
		}).collect(Collectors.toList());
		
		//System.out.println("TEMP: " + tempGuards);
		Map<SDTGuard, SDT> merged = fullMerger.merge(sortedGuards, tempGuards, sdtChecker, valuation);
		//System.out.println("RES: " + merged);

		return merged;
	}

	protected Map<SDTGuard, SDT> mergeEquDiseqGuards(final Map<SDTGuard, SDT> equGuards, SDTGuard elseGuard,
			SDT elseSDT, SDTEquivalenceChecker sdtChecker) {
		Map<SDTGuard, SDT> merged = ifElseMerger.merge(equGuards, elseGuard, elseSDT, sdtChecker);

		return merged;
	}

	// given a set of registers and a set of guards, keep only the registers
	// that are mentioned in any guard
	//
	protected PIV keepMem(Map<SDTGuard, SDT> guardMap) {
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
			} else if (mg instanceof SDTMultiGuard) {
				Set<SymbolicDataValue> rSet = ((SDTMultiGuard) mg).getAllSDVsFormingGuard();
				for (SymbolicDataValue r : rSet) {
					Parameter p = new Parameter(r.getType(), r.getId());
					if (r instanceof Register) {
						ret.put(p, (Register) r);
					}
				}
			} else if (!(mg instanceof SDTTrueGuard)) {
				throw new IllegalStateException("wrong kind of guard " + mg + " type: " + mg.getClass());
			}
		}
		return ret;
	}

	public SDT treeQuery(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix, WordValuation values, PIV piv,
			Constants constants, SuffixValuation suffixValues, SDTConstructor oracle, IOOracle traceOracle) {
		
		int pId = values.size() + 1;
		SuffixValue sv = suffix.getDataValue(pId);
		DataType<T> type = (DataType<T>) sv.getType();

		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
		DataValue<T>[] typedPrefixValues = DataWords.valsOf(prefix, type);
		
		WordValuation typedPrefixValuation = new WordValuation();
		for (int i = 0; i < typedPrefixValues.length; i++) {
			typedPrefixValuation.put(i + 1, typedPrefixValues[i]);
		}

		SuffixValue currentParam = new SuffixValue(type, pId);

		Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

		if (values.<T>values(type).contains(constants.<T>values(type))) {
			throw new DecoratedRuntimeException("Shouldn't happen");
		}
		Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(//constants.<T>values(type),
				DataWords.<T>valSet(prefix, type), values.<T>values(type));
		potSet.removeAll(constants.values());

		List<DataValue<T>> potList = new ArrayList<>(potSet);
		List<DataValue<T>> potential = getPotential(potList);
		// WE ASSUME THE POTENTIAL IS SORTED
		int potSize = potential.size();
		Map<SDTGuard, DataValue<T>> guardDvs = new LinkedHashMap<>();

		ParameterizedSymbol ps = SymbolicSuffix.computeSymbol(suffix, pId);
		// if suffix optimization is enabled, we compute an optimized branching context, 
		// otherwise we exhaustively check all branches
		BranchingContext<T> context; 
		if (suffixOptimization) {
			BranchingLogic<T> logic = new BranchingLogic<T>(this);
			context = logic.computeBranchingContext(pId, potential, prefix, constants, suffixValues,
					suffix);
			if (context.getStrategy() != BranchingStrategy.TRUE_FRESH) {
				 boolean exhBr =  this.shouldBeTreatedExhaustively(suffix, pId);
				 if (exhBr) 
					 context = new BranchingContext<>(BranchingStrategy.FULL, potential);
			}
			
		} else 
			context = new BranchingContext<>(BranchingStrategy.FULL, potential);

		BranchingStrategy branching = context.getStrategy();
	
		// special case: fresh values in outputs
		if (freshValues) {

			if (ps instanceof OutputSymbol && ps.getArity() > 0) {

				int idx = SymbolicSuffix.computeLocalIndex(suffix, pId);
				Word<PSymbolInstance> query = SymbolicSuffix.buildQuery(prefix, suffix, values);
				Word<PSymbolInstance> trace = traceOracle.trace(query);
				PSymbolInstance out = trace.lastSymbol();

				// we compare not only the output generated to the suffix, but also the query thus far to the trace obtained.
				// In case outputs in the trace are different, tracing an output value to a sdv 
				// might not be possible, since it could originate from an output value the system generated
				// that was not captured by the suffix. In such cases, we terminate recursion by returning a rejecting subtree.
				if (out.getBaseSymbol().equals(ps) && query.equals(trace.prefix(-1))) {

					DataValue<T> d = out.getParameterValues()[idx];

					if (d instanceof FreshValue && !potential.contains(d)) {
						d = getFreshValue(potential);
						WordValuation trueValues = new WordValuation();
						trueValues.putAll(values);
						trueValues.put(pId, new FreshValue<T>(d.getType(), d.getId()));
						SuffixValuation trueSuffixValues = new SuffixValuation(suffixValues);
						trueSuffixValues.put(sv, d);
						SDTTrueGuard trueGuard = new SDTTrueGuard(currentParam);
						trueSuffixValues.addSuffGuard(trueGuard);
						SDT sdt = oracle.treeQuery(prefix, suffix, trueValues, piv, constants, trueSuffixValues);

						log.log(Level.FINEST, " single deq SDT : " + sdt.toString());

						Map<SDTGuard, SDT> merged = new LinkedHashMap<SDTGuard, SDT>();
						merged.put(trueGuard, sdt);

						return new SDT(merged);
					} else {
						// as outputs, we can shortcut, as we only support
						// sumc/equality, also, merging is not necessary,
						// since we know that an output value other than the
						// system's is not accepted
						SymbolicDataExpression outExpr = getSDExprForDV(d, prefixValues,  values,
								constants);
						//System.out.println(currentParam + " " + d + " " + outExpr);
						if (outExpr == null) {
							throw new DecoratedRuntimeException("Couldn't find " + d + " in prefixValues: " + prefixValues + " values:" + values + " "
									+ "constants: " + constants + "\n query:" + query + "\n trace:" + trace);
						}
						
						WordValuation eqValues = new WordValuation(values);
						SuffixValuation eqSuffixValues = new SuffixValuation(suffixValues);
						eqValues.put(pId, d);
						eqSuffixValues.put(sv, d);
						EqualityGuard eqGuard = new EqualityGuard(currentParam, outExpr);
						eqSuffixValues.addSuffGuard(eqGuard);
						SDT eqSdt = oracle.treeQuery(prefix, suffix, eqValues, piv, constants, eqSuffixValues);
						tempKids.put(eqGuard, eqSdt);
						
						WordValuation deqValues = new WordValuation(values);
						SuffixValuation deqSuffixValues = new SuffixValuation(suffixValues);
						DataValue<T> deqValue = this.getFreshValue(potential);
						deqValues.put(pId, deqValue);
						deqSuffixValues.put(sv, deqValue);
						DisequalityGuard deqGuard = new DisequalityGuard(currentParam, outExpr);
						deqSuffixValues.addSuffGuard(deqGuard);
						SDT deqSdt = oracle.treeQuery(prefix, suffix, deqValues, piv, constants, deqSuffixValues);
						
						Mapping<SymbolicDataValue, DataValue<?>> guardContext = this.buildContext(prefixValues, values, constants);
						SDTEquivalenceChecker eqChecker =  
								new SemanticEquivalenceChecker(constants, solver, suffixValues.getSuffGuards(), guardContext);
						
						Map<SDTGuard, SDT> merged = this.mergeEquDiseqGuards(tempKids, deqGuard, deqSdt, eqChecker);
						
						piv.putAll(keepMem(merged));
						return new SDT(merged);
					}
				} else {
					int maxSufIndex = DataWords.paramLength(suffix.getActions()) + 1;
					SDT rejSdt = makeRejectingBranch(currentParam.getId() + 1, maxSufIndex);
					SDTTrueGuard trueGuard = new SDTTrueGuard(currentParam);
					tempKids.put(trueGuard, rejSdt);

					piv.putAll(keepMem(tempKids));
					return new SDT(tempKids);
				}
			}
		}
		
		// System.out.println("potential " + potential);
		if (potential.isEmpty() || branching == BranchingStrategy.TRUE_FRESH) {
			// System.out.println("empty potential");
			WordValuation elseValues = new WordValuation(values);
			DataValue<T> fresh = getFreshValue(potential);
			elseValues.put(pId, fresh);

			// this is the valuation of the suffixvalues in the suffix
			SuffixValuation elseSuffixValues = new SuffixValuation(suffixValues);
			elseSuffixValues.put(sv, fresh);
			SDTGuard trueGuard = new SDTTrueGuard(currentParam);
			elseSuffixValues.addSuffGuard(trueGuard);

			SDT elseOracleSdt = oracle.treeQuery(prefix, suffix, elseValues, piv, constants, elseSuffixValues);
			tempKids.put(trueGuard, elseOracleSdt);
			piv.putAll(keepMem(tempKids));
			return new SDT(tempKids);
		} else if (branching == BranchingStrategy.TRUE_PREV) {
			DataValue<T> prev = context.getBranchingValue();

			// System.out.println("empty potential");
			WordValuation equValues = new WordValuation(values);
			equValues.put(pId, prev);

			// this is the valuation of the suffixvalues in the suffix
			SuffixValuation equSuffixValues = new SuffixValuation(suffixValues);
			equSuffixValues.put(sv, prev);
			EqualityGuard eqGuard = makeEqualityGuard(prev, prefixValues, currentParam, values, constants);
			equSuffixValues.addSuffGuard(eqGuard);

			SDT equOracleSdt = oracle.treeQuery(prefix, suffix, equValues, piv, constants, equSuffixValues);
			//tempKids.put(new SDTTrueGuard(currentParam), equOracleSdt);
			//tempKids.put(new SDTTrueGuard(currentParam), equOracleSdt);
			tempKids.put(eqGuard, equOracleSdt);
			piv.putAll(keepMem(tempKids));
			return new SDT(tempKids);
		}
		// process each '<' case
		else {

			if (branching == BranchingStrategy.FULL || branching == BranchingStrategy.TRUE_SMALLER) {

				if (branching == BranchingStrategy.FULL || branching == BranchingStrategy.TRUE_SMALLER) {
					// smallest case
					Valuation smVal = new Valuation();
					DataValue<T> dvRight = potential.get(0);
					IntervalGuard sguard = makeSmallerGuard(dvRight, prefixValues, currentParam, values, piv, constants);
					SymbolicDataValue rsm = (SymbolicDataValue) sguard.getRightExpr();
					// System.out.println("setting valuation, symDV: " +
					// rsm.toVariable() + " dvright: " + dvRight);
					smVal.setValue(toVariable(rsm), dvRight.getId());
					DataValue<T> smcv = this.pickIntervalDataValue(null, dvRight);
					guardDvs.put(sguard, smcv);
				}

				if (branching == BranchingStrategy.FULL) {
					// biggest case
					Valuation bgVal = new Valuation();
	
					DataValue<T> dvLeft = potential.get(potSize - 1);
					IntervalGuard bguard = makeBiggerGuard(dvLeft, prefixValues, currentParam, values, piv, constants);
					updateValuation(bgVal, bguard.getLeftExpr(), dvLeft);
					
					DataValue<T> bgcv = this.pickIntervalDataValue(dvLeft, null);
					guardDvs.put(bguard, bgcv);
				}
			}

			if (branching == BranchingStrategy.FULL || branching == BranchingStrategy.IF_INTERVALS_ELSE) {

				// middle cases
				List<Range<T>> ranges = generateRangesFromPotential(potential);

				for (Range<T> range : ranges) {
					Valuation val = new Valuation();
					DataValue<T> dvMRight = range.right;
					DataValue<T> dvMLeft = range.left;

					IntervalGuard intervalGuard = makeIntervalGuard(dvMLeft, dvMRight, prefixValues, currentParam,
							values, piv, constants);

					updateValuation(val, intervalGuard.getRightExpr(), dvMRight);
					updateValuation(val, intervalGuard.getLeftExpr(), dvMLeft);

					DataValue<T> cv = this.pickIntervalDataValue(dvMLeft, dvMRight);
					guardDvs.put(intervalGuard, cv);
				}

				if (branching == BranchingStrategy.IF_INTERVALS_ELSE) {
					throw new RuntimeException("Processing for " + branching.name() + " not yet implemented");
				}
			}

			if (branching == BranchingStrategy.FULL || branching == BranchingStrategy.IF_EQU_ELSE) {
				List<DataValue<T>> branchingValues = context.getBranchingValues();

				// System.out.println("eq potential is: " + potential);
				for (DataValue<T> newDv : branchingValues) {
					// log.log(Level.FINEST, newDv.toString());
					EqualityGuard eqGuard = makeEqualityGuard(newDv, prefixValues, currentParam, values, constants);

					guardDvs.put(eqGuard, newDv);
				}
			}
			
			if (branching == BranchingStrategy.IF_EQU_ELSE) {
				SDTGuard[] elseConjuncts = guardDvs.keySet().stream().map(g -> ((EqualityGuard) g).toDeqGuard())
						.toArray(SDTGuard[]::new);

				SDTGuard elseGuard = elseConjuncts.length == 1? elseConjuncts[0] : new SDTAndGuard(currentParam, elseConjuncts);
				DataValue<T> elseValue = getFreshValue(potential);
				guardDvs.put(elseGuard, elseValue);
			}
		}
		
		
		tempKids = treeQueriesForInstantiations(guardDvs, suffix, oracle, prefix, values, piv, constants, suffixValues);

		// System.out.println("TEMPKIDS for " + prefix + " + " + suffix + " = "
		// + tempKids);
		
		Map<SDTGuard, SDT> merged;
		Mapping<SymbolicDataValue, DataValue<?>> guardContext = this.buildContext(prefixValues, values, constants);
		SDTEquivalenceChecker eqChecker =  new SemanticEquivalenceChecker(constants, solver, suffixValues.getSuffGuards(), guardContext);  
		
		if (branching == BranchingStrategy.FULL) {
			merged = mergeAllGuards(tempKids, guardDvs, eqChecker, guardContext);
		} else {
			if (tempKids.size() == 1) 
				merged = tempKids;
			else { 
				assert branching == BranchingStrategy.IF_EQU_ELSE;
				SDTGuard elseGuard = new ArrayList<>(tempKids.keySet()).get(tempKids.size()-1);
				SDT elseSDT = tempKids.remove(elseGuard);
				merged = mergeEquDiseqGuards(tempKids, elseGuard, elseSDT, eqChecker);
			}
		}

		// System.out.println("MERGED = " + merged);
		assert !merged.keySet().isEmpty();

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

		SDT returnSDT = new SDT(merged);
		return returnSDT;

	}
	
	public Mapping<SymbolicDataValue, DataValue<?>> buildContext(List<DataValue> prefixValues,
			WordValuation ifValues, Constants constants) {
		Mapping<SymbolicDataValue, DataValue<?>> context = new Mapping<SymbolicDataValue, DataValue<?>>();
		HashSet<DataValue> prSet = new HashSet<DataValue> (prefixValues);
		for (DataValue dv : prSet) {
			Register reg = this.getRegisterWithValue(dv, prefixValues);
			context.put(reg, dv);
		}
		
		for (DataValue dv : ifValues.values()) {
			SuffixValue suff = this.getSuffixWithValue(dv, ifValues);
			context.put(suff, dv);
		}
		
		context.putAll(constants.ofType(this.getType()));
		return context;
	}
	
	// Probably can be extracted to a separate class.
	private Map<SDTGuard, SDT> treeQueriesForInstantiations(Map<SDTGuard, DataValue<T>> guardDvs, GeneralizedSymbolicSuffix suffix, SDTConstructor oracle, Word<PSymbolInstance> prefix, WordValuation wordVals, PIV piv, Constants constants, SuffixValuation suffixVals) {
		int pId = wordVals.size() +1;
		SuffixValue sv = suffix.getDataValue(pId);
		Map<SDTGuard, SDTQuery> answers = new LinkedHashMap<>();
		final Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();
		List<SDTQuery> queries = new ArrayList<>(guardDvs.size());
		
		for (SDTGuard sdtGuard : guardDvs.keySet()) {
			DataValue<T> dv = guardDvs.get(sdtGuard);
			WordValuation newWordVals = new WordValuation(wordVals);
			newWordVals.put(pId, dv);
			SuffixValuation newSuffixVals = new SuffixValuation(suffixVals);
			newSuffixVals.put(sv, dv);
			newSuffixVals.addSuffGuard(sdtGuard);
			SDTQuery sdtQuery = new SDTQuery(newWordVals, newSuffixVals);
			queries.add(sdtQuery);
			answers.put(sdtGuard, sdtQuery);
		}
		
		oracle.processTreeQueryBatch(queries, prefix, suffix, piv, constants);
		
		answers.forEach((g, ans) -> tempKids.put(g, ans.getAnswer()));
		return tempKids;
	}
	
	
	
	protected List<Range<T>> generateRangesFromPotential(List<DataValue<T>> potential) {
		int potSize = potential.size();
		List<Range<T>> ranges = new ArrayList<Range<T>>(potential.size());
		for (int i = 1; i < potSize; i++) 
			ranges.add(new Range<T>(potential.get(i-1), potential.get(i)));
		
		return ranges;
	} 
	
	protected static class Range<T>{
		public final DataValue<T> left;
		public final DataValue<T> right;
		public Range(DataValue<T> left, DataValue<T> right) {
			super();
			this.left = left;
			this.right = right;
		}
		
		public String toString() {
			return left + "..." + right;
		}
	} 

	/**
	 * Creates a "unary tree" of depth maxIndex - nextSufIndex which leads to a
	 * rejecting Leaf. The edges are True guards over suffix values with index
	 * from nextSufIndex to (excl.) maxIndex.
	 * 
	 * Used to shortcut output processing.
	 */
	private SDT makeRejectingBranch(int nextSufIndex, int maxIndex) {
		if (nextSufIndex == maxIndex) {
			// map.put(guard, SDTLeaf.REJECTING);
			return SDTLeaf.REJECTING;
		} else {
			Map<SDTGuard, SDT> map = new LinkedHashMap<>();
			SDTTrueGuard trueGuard = new SDTTrueGuard(new SuffixValue(this.getType(), nextSufIndex));
			map.put(trueGuard, makeRejectingBranch(nextSufIndex + 1, maxIndex));
			SDT sdt = new SDT(map);
			return sdt;
		}
	}

	protected DataValue<T> updateValuation(Valuation valuation, SymbolicDataExpression expr, DataValue<T> concValue) {
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

	protected IntervalGuard makeIntervalGuard(DataValue<T> biggerDv, DataValue<T> smallerDv,
			List<DataValue> prefixValues, SuffixValue currentParam, WordValuation ifValues, PIV pir,
			Constants constants) {
		IntervalGuard smallerGuard = makeSmallerGuard(smallerDv, prefixValues, currentParam, ifValues, pir, constants);
		IntervalGuard biggerGuard = makeBiggerGuard(biggerDv, prefixValues, currentParam, ifValues, pir, constants);
		return new IntervalGuard(currentParam, biggerGuard.getLeftExpr(), smallerGuard.getRightExpr());
	}

	protected IntervalGuard makeBiggerGuard(DataValue<T> biggerDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
		SymbolicDataExpression regOrSuffixExpr = getSDExprForDV(biggerDv, prefixValues, ifValues,
				constants);
		IntervalGuard bg = new IntervalGuard(currentParam, regOrSuffixExpr, null);
		return bg;
	}

	protected IntervalGuard makeSmallerGuard(DataValue<T> smallerDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
		SymbolicDataExpression regOrSuffixExpr = getSDExprForDV(smallerDv, prefixValues, ifValues,
				constants);
		IntervalGuard sg = new IntervalGuard(currentParam, null, regOrSuffixExpr);
		return sg;
	}

	protected EqualityGuard makeEqualityGuard(DataValue<T> equDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, Constants constants) {
		DataType type = equDv.getType();
		SymbolicDataExpression sdvExpr = getSDExprForDV(equDv, prefixValues,  ifValues, constants);
		return new EqualityGuard(currentParam, sdvExpr);
	}

	private SymbolicDataExpression getSDExprForDV(DataValue<T> dv, List<DataValue> prefixValues,
			WordValuation ifValues, Constants constants) {
		SymbolicDataValue SDV;
		if (constants.containsValue(dv)) {
			return constants.getConstantWithValue(dv);
		} else if (dv instanceof SumCDataValue) {
			SumCDataValue<T> sumDv = (SumCDataValue<T>) dv;
			SDV = getSDVForDV(sumDv.toRegular(), prefixValues, ifValues, constants);
			// if there is no previous value equal to the summed value, we pick
			// the data value referred by the sum
			// by this structure, we always pick equality before sumc equality
			// when the option is available
			if (SDV == null) {
				DataValue<T> constant = sumDv.getConstant();
				DataValue<T> prevDV = sumDv.getOperand();
				SymbolicDataValue prevSDV = getSDVForDV(prevDV, prefixValues, ifValues, constants);
				return new SumCDataExpression(prevSDV, constant);
			} else {
				return SDV;
			}
		} else {
			SDV = getSDVForDV(dv, prefixValues, ifValues, constants);
			return SDV;
		}
	}

	private SymbolicDataValue getSDVForDV(DataValue<T> dv, @Nullable List<DataValue> prefixValues,
			WordValuation ifValues, Constants constants) {
		int newDv_i;
		DataType type = dv.getType();

		if (constants.containsValue(dv)) {
			return constants.getConstantWithValue(dv);
		}

		SymbolicDataValue sdv = getRegisterWithValue(dv, prefixValues);
		
		if (sdv == null) // no register found 
			sdv = getSuffixWithValue(dv, ifValues);
		
		return sdv;
	}
	
	private SuffixValue getSuffixWithValue (DataValue<T> dv, WordValuation ifValues) {
		if (ifValues.containsValue(dv)) {
			int first = Collections.min(ifValues.getAllKeys(dv));
			return new SuffixValue(type, first);
		}
		return null;
	}
	
	private Register getRegisterWithValue(DataValue<T> dv, List<DataValue> prefixValues) {
		if (prefixValues.contains(dv)) {
			int newDv_i = prefixValues.indexOf(dv) + 1;
			Register newDv_r = new Register(type, newDv_i);
			return newDv_r;
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

	private DataValue<T> instantiateSDExpr(SymbolicDataExpression sdExpr, DataType type, List<DataValue> prefixValues,
			PIV piv, ParValuation pval, Constants constants) {
		DataValue<T> returnThis = null;
		if (sdExpr instanceof SumCDataExpression) {
			DataValue<T> opDv = instantiateSDExpr(sdExpr.getSDV(), type, prefixValues, piv, pval, constants);
			returnThis = new SumCDataValue(opDv, ((SumCDataExpression) sdExpr).getConstant());
		} else {
			assert sdExpr.isConstant() || sdExpr.isParameter() || sdExpr.isRegister() || sdExpr.isSuffixValue();
			returnThis = getRegisterValue((SymbolicDataValue) sdExpr, piv, prefixValues, constants, pval);
		}
		return returnThis;
	}

	/**
	 * Instantiates the guard over the symbol {@ps param} for the given values.
	 * If guard is satisfiable, returns a DataValue containing the concrete
	 * value, but also symbolic information on how it was originated. Otherwise,
	 * returns null.
	 * 
	 * This method had to be adapted in order to also decorate the data values (otherwise you cannot determinize).
	 * 
	 * <b>NOTE:</b> The instantiate function uniformly makes use of the
	 * IntervalDataValue.instantiateNew in order to instantiate intervals. We do
	 * this instead of using the cs-solver for better control over what is
	 * instantiated and also easier instantiation. It is important that
	 * DataValues that come out are annotated s.t. they reflect the guards that
	 * satisfy. This is needed for canonicalization purposes. </br>
	 * The implementation IS NOT full proof, since our current DataValue
	 * sub-types cannot express conjunctions, just lone guards. To fully handle
	 * conjunctions, we would need to use a cs-solver and also value independent
	 * annotations of the DataValues. (see SuccessorDataValue as an example)
	 * 
	 */
	@Override
	public DataValue<T> instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
			Constants constants, SDTGuard guard, Parameter param, Set<DataValue<T>> oldDvs, boolean useSolver) {

		// useSolver = useSolver || !this.freshValues;
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

			returnValue = getFreshValue(new ArrayList<>(potSet));
		} else {
			Collection<DataValue<T>> alreadyUsedValues = DataWords.<T>joinValsToSet(constants.<T>values(type),
					DataWords.<T>valSet(prefix, type), pval.<T>values(type));
			Valuation val = new Valuation();
			if (guard instanceof DisequalityGuard) {
				DisequalityGuard diseqGuard = (DisequalityGuard) guard;
				SymbolicDataValue r = (SymbolicDataValue) diseqGuard.getRegister();
				DataValue<T> rRegVal = getRegisterValue(r, piv, prefixValues, constants, pval);
				val.setValue(toVariable(r), rRegVal.getId());

				Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
						DataWords.<T>valSet(prefix, type), pval.<T>values(type));
				returnValue = getFreshValue(new ArrayList<>(potSet));
			} else

			if (guard instanceof IntervalGuard) {
				IntervalGuard iGuard = (IntervalGuard) guard;
				DataValue<T> rExprVal = null, lExprVal = null, rRegVal = null, lRegVal = null;
				if (!iGuard.isBiggerGuard()) {
					SymbolicDataValue r = (SymbolicDataValue) iGuard.getRightSDV();
					rExprVal = instantiateSDExpr(iGuard.getRightExpr(), r.getType(), prefixValues, piv, pval,
							constants);
					rRegVal = getRegisterValue(r, piv, prefixValues, constants, pval);
		
					val.setValue(toVariable(r), rRegVal.getId());
				}
		
				if (!iGuard.isSmallerGuard()) {
					SymbolicDataValue l = (SymbolicDataValue) iGuard.getLeftSDV();
					lExprVal = instantiateSDExpr(iGuard.getLeftExpr(), l.getType(), prefixValues, piv, pval, constants);
					lRegVal = getRegisterValue(l, piv, prefixValues, constants, pval);
		
					val.setValue(toVariable(l), lRegVal.getId());
				}
				if (useSolver) {	
					// we decorate it with interval information
					returnValue = instantiator.instantiateGuard(guard, val, constants, alreadyUsedValues);
					if (returnValue != null)
						returnValue = new IntervalDataValue<>(returnValue, lExprVal, rExprVal);
				} else {
					returnValue = this.instantiateIntervalGuard(iGuard, piv, prefixValues, constants, pval);
				}
			} else if (guard instanceof SDTIfGuard) {
				SymbolicDataValue r = ((SDTIfGuard) guard).getRegister();
				DataValue<T> regVal = getRegisterValue(r, piv, prefixValues, constants, pval);
				val.setValue(toVariable(r), regVal.getId());
				// this section is highly technical, should be revised
			} else if (guard instanceof SDTMultiGuard) {

				List<SDTGuard> allGuards = ((SDTMultiGuard) guard).getGuards();
				boolean onlyDiseq = !allGuards.stream()
						.filter(gu -> !(gu instanceof DisequalityGuard))
						.findAny()
						.isPresent();
				if (onlyDiseq) {
					Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
							DataWords.<T>valSet(prefix, type), pval.<T>values(type));
					returnValue = getFreshValue(new ArrayList<>(potSet));
				} else {
					Set<SymbolicDataValue> regs = ((SDTAndGuard) guard).getAllSDVsFormingGuard();
					regs.forEach(reg -> {
						DataValue<T> regVal = getRegisterValue(reg, piv, prefixValues, constants, pval);
						val.setValue(toVariable(reg), regVal.getId());
					});
					if (!useSolver){
						List<EqualityGuard> eqGuards = allGuards.stream().filter(g -> g instanceof EqualityGuard).map(g -> (EqualityGuard)g)
								.distinct()
								.collect(Collectors.toList());
						if (guard instanceof SDTOrGuard) {
							assert ((SDTOrGuard) guard).getGuards().stream().allMatch(g -> !(g instanceof IntervalGuard)) : 
								"not expecting ORGuads over intervals";
							assert eqGuards.size() == 1 : "expecting at most one eq";
							returnValue = instantiateSDExpr(eqGuards.get(0).getExpression(),type,  prefixValues, piv, pval, constants);
						} else {
							DataValue<T> andInst = this.instantiator.instantiateGuard(guard, val, constants, Collections.emptyList()); 
							// if it cannot be instantiated by a CS, give up, otherwise we can assume that it is instantiatable
							if (andInst == null)
								return null;
							else {
								// if it is equal, just return what it is equal to, and exit
								if (!eqGuards.isEmpty()) {
									return andInst; 
								} else {
									// otherwise, we should have intervals and inequalities( all other cases should have been covered)
									Set<DataValue<T>> prohibitedValues = allGuards.stream()
											.filter(g -> g instanceof DisequalityGuard)
											.map(g -> instantiateSDExpr(((SDTIfGuard) g).getExpression(), type,
													prefixValues, piv, pval, constants))
											.collect(Collectors.toSet());
									oldDvs = Sets.difference(oldDvs, prohibitedValues).immutableCopy();
									List<IntervalGuard> intervalGuards = allGuards.stream().filter(g -> g instanceof IntervalGuard).map(g -> (IntervalGuard) g)
											.collect(Collectors.toList());
									assert intervalGuards.size() == 1: "expected one interval guard. " + (intervalGuards.size()>1? " Cannot reliably instantiate more than one interval guard.":"");
									IntervalGuard intGuard = intervalGuards.get(0);
									DataValue<T> intDv = this.instantiateIntervalGuard(intGuard, piv, prefixValues, constants, pval, 
											prohibitedValues.toArray(new DataValue[]{}));
									DataValue<T> eqDv = null;
									SymbolicDataExpression eqExpr = null;
									if (Boolean.FALSE.equals(intGuard.getLeftOpen()))
										eqExpr = intGuard.getLeftExpr();
									if (Boolean.FALSE.equals(intGuard.getRightOpen()))
										eqExpr = intGuard.getRightExpr();
									if (eqExpr != null) {
										eqDv = instantiateSDExpr(eqExpr, type,
												prefixValues, piv, pval, constants);
									}
									
									// we normally prefer to select equality value
									if (eqDv != null && (!oldDvs.contains(intDv) || oldDvs.contains(eqDv)) && !prohibitedValues.contains(eqDv))
										return eqDv;
									else {
										assert !prohibitedValues.contains(intDv);
										return intDv;
									}
								}
							}
						}
					}
				}
			} else {
				throw new IllegalStateException("only =, !=, intervals, AND and OR are allowed");
			}

			if (!(oldDvs.isEmpty()) && !oldDvs.contains(returnValue)) {
				// System.out.println("old dvs: " + oldDvs);
				for (DataValue<T> oldDv : oldDvs) {
					Valuation newVal = new Valuation();
					newVal.putAll(val);
					newVal.setValue(toVariable(new SuffixValue(param.getType(), param.getId())), oldDv.getId());
					// System.out.println("instantiating " + guard + " with " +
					// newVal);
					DataValue<T> inst = instantiator.instantiateGuard(guard, newVal, constants, alreadyUsedValues);
					if (inst != null) {
						return oldDv;
					}
				}
			}

			if (returnValue == null)
				returnValue = instantiator.instantiateGuard(guard, val, constants, alreadyUsedValues);

		}
		return returnValue;
	}
	
	private IntervalDataValue<T> instantiateIntervalGuard(IntervalGuard iGuard, PIV piv, List<DataValue> prefixValues, Constants constants,
			ParValuation pval, DataValue ...prohibited) {
		DataValue<T> rExprVal = null, lExprVal = null, rRegVal = null, lRegVal = null;
		IntervalDataValue<T> intVal = null;
		if (!iGuard.isBiggerGuard()) {
			SymbolicDataValue r = (SymbolicDataValue) iGuard.getRightSDV();
			rExprVal = instantiateSDExpr(iGuard.getRightExpr(), r.getType(), prefixValues, piv, pval,
					constants);
			rRegVal = getRegisterValue(r, piv, prefixValues, constants, pval);
		}

		if (!iGuard.isSmallerGuard()) {
			SymbolicDataValue l = (SymbolicDataValue) iGuard.getLeftSDV();
			lExprVal = instantiateSDExpr(iGuard.getLeftExpr(), l.getType(), prefixValues, piv, pval, constants);
			lRegVal = getRegisterValue(l, piv, prefixValues, constants, pval);
		}

		HashSet<DataValue> prohibitedSet = Sets.newHashSet(prohibited);
		intVal =  this.pickIntervalDataValue(lExprVal, rExprVal, prohibitedSet);
		if (intVal == null)
			throw new RuntimeException("could not pick value between " + lExprVal + " and " + rExprVal + " not in " + prohibitedSet);
		return intVal;
	}
	
	public IntervalDataValue<T> pickIntervalDataValue(DataValue<T> left, DataValue<T> right, Set<DataValue> prohibited) {
		if (left != null && right != null && left.getId().compareTo(right.getId()) >= 0) 
			return null;
		IntervalDataValue<T> dv = pickIntervalDataValue(left, right);
		if (prohibited.contains(dv)) {
			if (right != null) {
				dv = pickIntervalDataValue(left, dv, prohibited);
				if (dv != null)
					return dv;
			}
			if (left != null) { 
				dv = pickIntervalDataValue(dv, right, prohibited);
				if (dv != null)
					return dv;
			}
		}
		return dv;
	}
	
	// TODO This should be defined at the concrete theory level
	public IntervalDataValue<T> pickIntervalDataValue(DataValue<T> left, DataValue<T> right) {
		return IntervalDataValue.instantiateNew(left, right, DataValue.CONST(1000, type));
	}
	
	public List<EnumSet<DataRelation>> getRelations(List<DataValue<T>> left, DataValue<T> right) {

		List<EnumSet<DataRelation>> ret = new ArrayList<>();
		left.stream().forEach((dv) -> {
			final int c = dv.getId().compareTo(right.getId());
			if (c == 0)
				ret.add(EnumSet.of(DataRelation.EQ));
			else if (c > 0)
				ret.add(EnumSet.of(DataRelation.DEFAULT));
			else 
				ret.add(EnumSet.of(DataRelation.LT));
		});

		return ret;
	}
	
    public void setUseSuffixOpt(boolean useit, ParamSignature ... exhSuffixParams) {
    	suffixOptimization = useit;
    	this.exhSuffixParams = Arrays.stream(exhSuffixParams).collect(Collectors.toSet());
    }
    
    private boolean shouldBeTreatedExhaustively(GeneralizedSymbolicSuffix suffix, int pid) {
    	ParamSignature param = suffix.getParamSignature(pid);
    	boolean should = exhSuffixParams.contains(param);
    	return should;
    	
    }
}
