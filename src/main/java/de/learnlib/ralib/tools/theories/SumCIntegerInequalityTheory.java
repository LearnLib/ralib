package de.learnlib.ralib.tools.theories;

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.words.Word;

public class SumCIntegerInequalityTheory extends IntegerInequalityTheory{
	// default constants
	private static Integer [] defaultSumConst = new Integer [] {
			1,
			100
			//10000.0
			};
	private static Integer [] defaultRegularConst = new Integer [] {
		//	0.0
			};
	
	
	private List<DataValue<Integer>> sumConstants;
	private List<DataValue<Integer>> regularConstants;

	public SumCIntegerInequalityTheory() {
		super();
	}
	
	public void setSumcConstants(SumConstants constants) {
		this.sumConstants = new ArrayList<>(constants.values(this.type));
	}
	
	public void setConstants(Constants constants) {
		this.regularConstants = new ArrayList<>(constants.values(this.type));
	}
	
	public SDT treeQuery(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix, WordValuation values, PIV piv,
			Constants constants, SuffixValuation suffixValues, SDTConstructor oracle, IOOracle traceOracle) {

		int pId = values.size() + 1;
		SuffixValue sv = suffix.getDataValue(pId);
		DataType type = sv.getType();

		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

		DataValue<Integer>[] typedPrefixValues = DataWords.valsOf(prefix, type);
		WordValuation typedPrefixValuation = new WordValuation();
		for (int i = 0; i < typedPrefixValues.length; i++) {
			typedPrefixValuation.put(i + 1, typedPrefixValues[i]);
		}

		SuffixValue currentParam = new SuffixValue(type, pId);

		Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

		Collection<DataValue<Integer>> potSet = DataWords.<Integer>joinValsToSet(constants.<Integer>values(type),
				DataWords.<Integer>valSet(prefix, type), values.<Integer>values(type));

		List<DataValue<Integer>> potList = new ArrayList<>(potSet);
		List<DataValue<Integer>> potential = getPotential(potList);
		// WE ASSUME THE POTENTIAL IS SORTED
		int potSize = potential.size();
		Map<SDTGuard, DataValue<Integer>> guardDvs = new LinkedHashMap<>();

		ParameterizedSymbol ps = SymbolicSuffix.computeSymbol(suffix, pId);
		// special case: fresh values in outputs
		if (super.hasFreshValues()) {

			if (ps instanceof OutputSymbol && ps.getArity() > 0) {

				int idx = SymbolicSuffix.computeLocalIndex(suffix, pId);
				Word<PSymbolInstance> query = SymbolicSuffix.buildQuery(prefix, suffix, values);
				Word<PSymbolInstance> trace = traceOracle.trace(query);
				PSymbolInstance out = trace.lastSymbol();

				if (out.getBaseSymbol().equals(ps)) {

					DataValue<Integer> d = out.getParameterValues()[idx];

					if (d instanceof FreshValue) {
						d = getFreshValue(potential);
						values.put(pId, new FreshValue<Integer>(d.getType(), d.getId()));
						WordValuation trueValues = new WordValuation();
						trueValues.putAll(values);
						SuffixValuation trueSuffixValues = new SuffixValuation();
						trueSuffixValues.putAll(suffixValues);
						trueSuffixValues.put(sv, d);
						SDT sdt = oracle.treeQuery(prefix, suffix, trueValues, piv, constants, trueSuffixValues);

						log.log(Level.FINEST, " single deq SDT : " + sdt.toString());

						Map<SDTGuard, SDT> temp = new LinkedHashMap<SDTGuard, SDT>();
						SDTTrueGuard trueGuard = new SDTTrueGuard(currentParam);
						temp.put(trueGuard, sdt);
						guardDvs.put(trueGuard, d);
						Map<SDTGuard, SDT> merged = mergeGuards(temp, guardDvs);

						log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
						log.log(Level.FINEST, "merged guards = " + merged.keySet());
						log.log(Level.FINEST, "merged pivs = " + piv.toString());

						return new SDT(merged);
					}
				}
			}
		}

		// System.out.println("potential " + potential);
		if (potential.isEmpty()) {
			// System.out.println("empty potential");
			WordValuation elseValues = new WordValuation();
			DataValue<Integer> fresh = getFreshValue(potential);
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
			DataValue<Integer> dvRight = potential.get(0);
			IntervalGuard sguard = makeSmallerGuard(dvRight, prefixValues, currentParam, smValues, piv, constants);
			SymbolicDataValue rsm = (SymbolicDataValue) sguard.getRightExpr();
			// System.out.println("setting valuation, symDV: " +
			// rsm.toVariable() + " dvright: " + dvRight);
			smVal.setValue(toVariable(rsm), dvRight.getId());
			DataValue<Integer> smcv = IntervalDataValue.instantiateNew(null, dvRight);
			// instantiate(sguard, smVal, constants, potential);
			// smcv = new IntervalDataValue<Integer>(smcv, null, dvRight);
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

			DataValue<Integer> dvLeft = potential.get(potSize - 1);
			IntervalGuard bguard = makeBiggerGuard(dvLeft, prefixValues, currentParam, bgValues, piv, constants);
			updateValuation(bgVal, bguard.getLeftExpr(), dvLeft);
			DataValue<Integer> bgcv = IntervalDataValue.instantiateNew(dvLeft, null);
			// instantiate(bguard, bgVal, constants, potential);
			// bgcv = new IntervalDataValue<Integer>(bgcv, dvLeft, null);
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
					DataValue<Integer> dvMRight = potential.get(i);
					DataValue<Integer> dvMLeft = potential.get(i - 1);
					
					if (dvMRight.getId() > dvMLeft.getId() +1) {

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
	
						DataValue<Integer> cv = IntervalDataValue.instantiateNew(dvMLeft, dvMRight);
						// instantiate(intervalGuard, val, constants, potential);
						// cv = new IntervalDataValue<Integer>(cv, dvMLeft, dvMRight);
						currentValues.put(pId, cv);
						currentSuffixValues.put(sv, cv);
	
						SDT oracleSdt = oracle.treeQuery(prefix, suffix, currentValues, piv, constants,
								currentSuffixValues);
	
						tempKids.put(intervalGuard, oracleSdt);
						guardDvs.put(intervalGuard, cv);
					}
				}
			}
			
			
			// System.out.println("eq potential is: " + potential);
			for (DataValue<Integer> newDv : potential) {
				// log.log(Level.FINEST, newDv.toString());

				// this is the valuation of the suffixvalues in the suffix
				SuffixValuation ifSuffixValues = new SuffixValuation();
				ifSuffixValues.putAll(suffixValues); // copy the suffix
														// valuation

				// construct the equality guard. Depending on newDv, a certain
				// type of equality is instantiated (newDv can be a SumCDv, in
				// which case a SumC equality is instantiated)
				EqualityGuard eqGuard = makeEqualityGuard(newDv, prefixValues, currentParam, values, constants);
				// log.log(Level.FINEST, "eqGuard is: " + eqGuard.toString());

				// we normalize newDv so that we only store plain data values in
				// the if suffix
				// newDv = new DataValue<Integer>(newDv.getType(), newDv.getId());
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

		// System.out.println("TEMPKIDS for " + prefix + " + " + suffix + " = "
		// + tempKids);
		// Map<SDTGuard, SDT> merged = mgGuards(tempKids, currentParam,
		// regPotential);
		Map<SDTGuard, SDT> merged = mergeGuards(tempKids, guardDvs);
		// Map<SDTGuard, SDT> merged = tempKids;
		// only keep registers that are referenced by the merged guards
		// System.out.println("MERGED = " + merged);
		assert !merged.keySet().isEmpty();
		// if (ps instanceof OutputSymbol && merged.size() >= 3) {
		// System.out.println(prefix + " " + suffix + " " + suffixValues);
		// System.out.println(tempKids);
		// System.out.println(merged);
		// guardDvs.forEach((g, dv) -> System.out.println(g + " " + dv ));
		// throw new RuntimeException("For an output symbol, there cannot be
		// more than 2 branches");
		// }

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
		// System.out.println("MERGED = " + merged);
		SDT returnSDT = new SDT(merged);
		return returnSDT;

	}
	
	
	public SumCIntegerInequalityTheory(DataType<Integer> dataType) {
		// the constants have to be introduced manually
		super(dataType);
		setupDefaultConstants(dataType);
	}
	
	public SumCIntegerInequalityTheory(DataType<Integer> dataType, List<DataValue<Integer>> sumConstants, List<DataValue<Integer>> regularConstants) {
		super(dataType);
		this.sumConstants = sumConstants;
		this.regularConstants = regularConstants;
	}
	
	public void setType(DataType<Integer> dataType) {
		super.setType(dataType);
		setupDefaultConstants(dataType);
	}
	
	private void setupDefaultConstants(DataType<Integer> dataType) {
		this.sumConstants = Arrays.asList(defaultSumConst).stream().map(c -> new DataValue<Integer>(dataType, c)).collect(Collectors.toList());
		this.regularConstants = Arrays.asList(defaultRegularConst).stream().map(c -> new DataValue<Integer>(dataType, c)).collect(Collectors.toList());
	}
	
	
    public List<DataValue<Integer>> getPotential(List<DataValue<Integer>> dvs) {
        //assume we can just sort the list and get the values
        List<DataValue<Integer>> sortedList = makeNewPotsWithSumC(dvs);
        //sortedList.addAll(dvs);
        Collections.sort(sortedList, new Cpr());

        //System.out.println("I'm sorted!  " + sortedList.toString());
        return sortedList;
    }
    
    /** Creates a list of values comprising the data values supplied, plus all values
     * obtained by adding each of the sum constants to each of the data values supplied.
     * 
     *  The sum values are wrapped around a {@link SumCDataValue} element. In case sums
     *  with different constants lead to the same value (for example 100+1.0 and 1+100.0 with
     *  1.0 and 100.0 as constants), we pick the sum with the constant of the smallest index
     *  in the sumConstants list.  
     * 
     */
    private List<DataValue<Integer>> makeNewPotsWithSumC(List<DataValue<Integer>> dvs) {
    	List<DataValue<Integer>> pot = new ArrayList<DataValue<Integer>> (dvs.size() * (sumConstants.size()+1));
    	pot.addAll(dvs);
    	List<DataValue<Integer>> dvWithoutConsts = dvs.stream().filter(dv -> !regularConstants.contains(dv)).collect(Collectors.toList());
    	// potential optimization, don't make sums out of sumC
    	//dvWithoutConsts = dvWithoutConsts.stream().filter(dv -> dv.getId() < 100).collect(Collectors.toList()); // ignore sumc constants
    	List<DataValue<Integer>> flattenedPot = new ArrayList<DataValue<Integer>> (dvs.size() * (sumConstants.size()+1));
    	flattenedPot.addAll(pot);
    	for (DataValue<Integer> sumConst : sumConstants) {
	    	for (DataValue<Integer> dv : dvWithoutConsts.stream().filter(pdv -> pdv.getType().equals(sumConst.getType())).collect(Collectors.toList()) ) {
	    		DataValue<Integer> regularSum = (DataValue<Integer>) DataValue.add(dv, sumConst);
	    		if ( !flattenedPot.contains(regularSum)) {
	    			SumCDataValue<Integer> sumDv = new SumCDataValue<Integer>(dv, sumConst);
	    			pot.add(sumDv);
	    		}
	    		flattenedPot.add(regularSum);
	    	}
    	}
    	
    	
    	return pot;
    }
    
    
    public Collection<DataValue<Integer>> getAllNextValues(
            List<DataValue<Integer>> vals) {
    	// adds window size interesting values
    	List<DataValue<Integer>> potential = getPotential(vals);
    	potential = potential.stream().map(dv -> 
    	dv instanceof SumCDataValue? ((SumCDataValue<Integer>) dv).toRegular(): dv)
    			.collect(Collectors.toList());
    	// the superclass should complete this list with in-between values.
    	return super.getAllNextValues(potential);
    }

}
