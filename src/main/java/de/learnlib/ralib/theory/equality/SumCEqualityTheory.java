package de.learnlib.ralib.theory.equality;

import static de.learnlib.ralib.theory.DataRelation.ALL;
import static de.learnlib.ralib.theory.DataRelation.EQ;
import static de.learnlib.ralib.theory.DataRelation.EQ_SUMC1;
import static de.learnlib.ralib.theory.DataRelation.EQ_SUMC2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.mapper.Determinizer;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.IfElseGuardMerger;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTEquivalenceChecker;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.SyntacticEquivalenceChecker;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.theories.SumCTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

abstract public class SumCEqualityTheory<T extends Number & Comparable<T>> implements TypedTheory<T>, SumCTheory {

	static public final class Cpr<N extends Comparable<N>> implements Comparator<DataValue<N>> {
		

		@Override
		public int compare(DataValue<N> one, DataValue<N> other) {
			return one.getId().compareTo(other.getId());
		}
	}
	
	
	protected boolean useNonFreeOptimization;

	protected boolean freshValues = false;

	private IfElseGuardMerger ifElseMerger;

	private DataType type;

	private static final LearnLogger log = LearnLogger.getLogger(EqualityTheory.class);

	public SumCEqualityTheory() {
		this.ifElseMerger = new IfElseGuardMerger(getGuardLogic());
	}
	
	@Override
	public void setCheckForFreshOutputs(boolean doit) {
		freshValues = doit;
	}
	
	public void setUseSuffixOpt(boolean useit) {
		this.useNonFreeOptimization = useit;
	}


	public abstract EnumSet<DataRelation> recognizedRelations();

	// given a map from guards to SDTs, merge guards based on whether they can
	// use another SDT. Base case: always add the 'else' guard first.
	private Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> ifGuards, SDTGuard deqGuard, SDT deqSdt) {
		SDTEquivalenceChecker sdtChecker = new SyntacticEquivalenceChecker();
		Map<SDTGuard, SDT> retMap = ifElseMerger.merge(ifGuards, deqGuard, deqSdt, sdtChecker);
		assert !retMap.isEmpty();
		return retMap;
	}

	// given a set of registers and a set of guards, keep only the registers
	// that are mentioned in any guard
	private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
		PIV ret = new PIV();
		for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
			SDTGuard mg = e.getKey();
			if (mg instanceof EqualityGuard) {
				log.log(Level.FINEST, mg.toString());
				SymbolicDataValue r = ((EqualityGuard) mg).getRegister();
				Parameter p = new Parameter(r.getType(), r.getId());
				if (r instanceof Register) {
					ret.put(p, (Register) r);
				}
			}
		}
		return ret;
	}

	// process a tree query
	@Override
	public SDT treeQuery(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix, WordValuation values, PIV pir,
			Constants constants, SuffixValuation suffixValues, SDTConstructor oracle, IOOracle traceOracle) {

		int pId = values.size() + 1;

		SuffixValue sv = suffix.getDataValue(pId);
		DataType type = sv.getType();

		SuffixValue currentParam = new SuffixValue(type, pId);

		Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

		Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
				DataWords.<T>valSet(prefix, type), suffixValues.<T>values(type));

		List<DataValue<T>> potList = new ArrayList<>(potSet);
		List<DataValue<T>> potential = getPotential(potList);

		// special case: fresh values in outputs
		if (freshValues) {

			ParameterizedSymbol ps = SymbolicSuffix.computeSymbol(suffix, pId);

			if (ps instanceof OutputSymbol && ps.getArity() > 0) {

				int idx = SymbolicSuffix.computeLocalIndex(suffix, pId);
				Word<PSymbolInstance> query = buildQuery(prefix, suffix, values);
				Word<PSymbolInstance> trace = traceOracle.trace(query);
				PSymbolInstance out = trace.lastSymbol();

				if (out.getBaseSymbol().equals(ps)) {

					DataValue d = out.getParameterValues()[idx];

					if (d instanceof FreshValue && !potential.contains(d)) {
						d = getFreshValue(potential);
						values.put(pId, d);
						WordValuation trueValues = new WordValuation(values);
						SuffixValuation trueSuffixValues = new SuffixValuation(suffixValues);
						trueSuffixValues.put(sv, d);
						SDT sdt = oracle.treeQuery(prefix, suffix, trueValues, pir, constants, trueSuffixValues);

						log.log(Level.FINEST, " single deq SDT : " + sdt.toString());

						Map<SDTGuard, SDT> merged = mergeGuards(tempKids, new SDTAndGuard(currentParam), sdt);

						log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
						log.log(Level.FINEST, "merged guards = " + merged.keySet());
						log.log(Level.FINEST, "merged pivs = " + pir.toString());

						return new SDT(merged);
					}
				}
			}
		}
		
		if (useNonFreeOptimization) {
			SuffixValue eqSuf = null;
			DataRelation eqRel = null;
			List<DataRelation> eqRels = Arrays.asList( EQ, EQ_SUMC1, EQ_SUMC2 );
			 
			for (int i=0; i< constants.getSumCs(type).size() + 1; i++) {
				eqRel = eqRels.get(i);
				eqSuf = suffix.findLeftMostRelatedSuffixExact(pId, eqRel);
				if (eqSuf != null) {
					break;
				}
			}
			

			boolean isNotFree = Sets.intersection(suffix.getPrefixRelations(pId), Sets.immutableEnumSet(eqRels)).isEmpty()   
					&& !suffix.getPrefixRelations(pId).contains(ALL)
					&& ( eqSuf != null || suffix.getSuffixRelations(pId).isEmpty());

			if (isNotFree) {
				DataValue d = null;

				if (eqSuf == null) {
					d = getFreshValue(potential);
				} else {
					d = suffixValues.get(eqSuf);
					if (eqRel == EQ_SUMC1) {
						d = new SumCDataValue(d, constants.getSumCs(type).get(0));
					} else if (eqRel == EQ_SUMC2) {
						d = new SumCDataValue(d, constants.getSumCs(type).get(1));
					}
				}
				assert d != null;
				
				/*
				 * TODO is it ok that we use True guards also for equal-to-past-suffix-param case?
				 */

				values.put(pId, d);
				WordValuation trueValues = new WordValuation();
				trueValues.putAll(values);
				SuffixValuation trueSuffixValues = new SuffixValuation();
				trueSuffixValues.putAll(suffixValues);
				trueSuffixValues.put(sv, d);
				SDT sdt = oracle.treeQuery(prefix, suffix, trueValues, pir, constants, trueSuffixValues);

				log.log(Level.FINEST, " single deq SDT : " + sdt.toString());

				Map<SDTGuard, SDT> merged = mergeGuards(tempKids, new SDTAndGuard(currentParam), sdt);

				log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
				// log.log(Level.FINEST,"temporary pivs = " + tempPiv.keySet());
				log.log(Level.FINEST, "merged guards = " + merged.keySet());
				log.log(Level.FINEST, "merged pivs = " + pir.toString());

				return new SDT(merged);
			}
		}

		log.log(Level.FINEST, "potential " + potential.toString());

		// process each 'if' case
		// prepare by picking up the prefix values
		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

		log.log(Level.FINEST, "prefix list    " + prefixValues.toString());

		DataValue fresh = getFreshValue(potential);

		List<DisequalityGuard> diseqList = new ArrayList<DisequalityGuard>();
		for (DataValue<T> newDv : potential) {
			log.log(Level.FINEST, newDv.toString());

			// this is the valuation of the suffixvalues in the suffix
			SuffixValuation ifSuffixValues = new SuffixValuation(suffixValues);
			ifSuffixValues.put(sv, newDv);

			EqualityGuard eqGuard = makeEqualityGuard(newDv, prefixValues, currentParam, values, constants);
			log.log(Level.FINEST, "eqGuard is: " + eqGuard.toString());
			diseqList.add(new DisequalityGuard(currentParam, eqGuard.getExpression()));
			// construct the equality guard
			// find the data value in the prefix
			// this is the valuation of the positions in the suffix
			WordValuation ifValues = new WordValuation(values);
			ifValues.put(pId, newDv);
			SDT eqOracleSdt = oracle.treeQuery(prefix, suffix, ifValues, pir, constants, ifSuffixValues);

			tempKids.put(eqGuard, eqOracleSdt);
		}

		// process the 'else' case
		// this is the valuation of the positions in the suffix
		WordValuation elseValues = new WordValuation(values);
		elseValues.put(pId, fresh);

		// this is the valuation of the suffixvalues in the suffix
		SuffixValuation elseSuffixValues = new SuffixValuation(suffixValues);
		elseSuffixValues.put(sv, fresh);

		SDT elseOracleSdt = oracle.treeQuery(prefix, suffix, elseValues, pir, constants, elseSuffixValues);

		SDTAndGuard deqGuard = new SDTAndGuard(currentParam, (diseqList.toArray(new DisequalityGuard[] {})));
		log.log(Level.FINEST, "diseq guard = " + deqGuard.toString());

		// merge the guards
		Map<SDTGuard, SDT> merged = mergeGuards(tempKids, deqGuard, elseOracleSdt);

		// only keep registers that are referenced by the merged guards
		pir.putAll(keepMem(merged));

		log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
		log.log(Level.FINEST, "merged guards = " + merged.keySet());
		log.log(Level.FINEST, "merged pivs = " + pir.toString());

		// clear the temporary map of children
		tempKids.clear();

		for (SDTGuard g : merged.keySet()) {
			assert !(g == null);
		}

		SDT returnSDT = new SDT(merged);
		return returnSDT;

	}
	
	public void setType(DataType type) {
		this.type = type;
	}
    
    public DataType getType() {
    	return type;
    }

	@Override
	public DataValue instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
			Constants constants, SDTGuard guard, Parameter param, Set<DataValue<T>> oldDvs, boolean useSolver) {

		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
		log.log(Level.FINEST, "prefix values : " + prefixValues.toString());
		DataType type = param.getType();

		if (guard instanceof EqualityGuard) {
			log.log(Level.FINEST, "equality guard " + guard.toString());
			EqualityGuard eqGuard = (EqualityGuard) guard;
			SymbolicDataValue ereg = eqGuard.getRegister();
			DataValue value = null;
			if (ereg.isRegister()) {
				log.log(Level.FINEST, "piv: " + piv.toString() + " " + ereg.toString() + " " + param.toString());
				Parameter p = piv.getOneKey((Register) ereg);
				log.log(Level.FINEST, "p: " + p.toString());
				int idx = p.getId();
				value = prefixValues.get(idx - 1);
			} else if (ereg.isSuffixValue()) {
				Parameter p = new Parameter(type, ereg.getId());
				value = pval.get(p);
			} else if (ereg.isConstant()) {
				value = constants.get((Constant) ereg);
			} else {
				throw new IllegalStateException("Could not resolve symbolic data value of EqualityGuard " + ereg);
			}
			
			Mapping<SymbolicDataValue, DataValue<?>> valuation = new Mapping<SymbolicDataValue, DataValue<?>>();
			valuation.putAll(constants);
			valuation.put(ereg, value);
			DataValue<?> val = eqGuard.getExpression().instantiateExprForValuation(valuation);
			return val;
		}

		Collection values = DataWords.<T>joinValsToSet(constants.<T>values(type), DataWords.<T>valSet(prefix, type),
				pval.<T>values(type));
		
		List<DataValue<T>> potSet = getPotential(new ArrayList<>(values));

		if (!values.isEmpty()) {
			log.log(Level.FINEST, "potSet = " + potSet.toString());
		} else {
			log.log(Level.FINEST, "potSet is empty");
		}
		DataValue fresh = this.getFreshValue(new ArrayList<DataValue<T>>(potSet));
		log.log(Level.FINEST, "fresh = " + fresh.toString());
		return fresh;

	}

	private Word<PSymbolInstance> buildQuery(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix,
			WordValuation values) {

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

	private EqualityGuard makeEqualityGuard(DataValue<T> equDv, List<DataValue> prefixValues, SuffixValue currentParam,
			WordValuation ifValues, Constants constants) {
		SymbolicDataExpression sdvExpr = getSDExprForDV(equDv, prefixValues, ifValues, constants);
		return new EqualityGuard(currentParam, sdvExpr);
	}

	private SymbolicDataExpression getSDExprForDV(DataValue<T> dv, List<DataValue> prefixValues, WordValuation ifValues,
			Constants constants) {
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

		if (constants.containsValue(dv)) {
			return constants.getConstantWithValue(dv);
		}

		SymbolicDataValue sdv = getRegisterWithValue(dv, prefixValues);

		if (sdv == null) // no register found
			sdv = getSuffixWithValue(dv, ifValues);

		return sdv;
	}

	private SuffixValue getSuffixWithValue(DataValue<T> dv, WordValuation ifValues) {
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

	public SDTGuardLogic getGuardLogic() {
		return new EqualityGuardLogic();
	}
	
	@Override
	public Determinizer<T> getDeterminizer() {
		return new SumCEqualityDeterminizer<T>(this);
	}
}
