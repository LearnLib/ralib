package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.equality.SumCEqualityTheory;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class IntegerSumCEqualityTheory extends SumCEqualityTheory<Integer> {
	
	
	// maxSumC : the biggest sum constant
	
	// Fresh values are generated at a fresh step distance from each other, in increasing order, starting from 0.
	// The fresh step is either 1 in case there are no sum constants, or maxSumC x FRESH_FACTOR
	
	private static int FRESH_FACTOR = 100;
	

	private Constants constants;

	private int freshStep;

	@Override
	public Class<Integer> getDomainType() {
		return Integer.class;
	}

	@Override
	public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
		assert freshStep != 0;
		if (vals.isEmpty()) {
			return new DataValue<Integer>(getType(), 0);
		} else {
			DataValue<Integer> maxVal = Collections.max(vals, new Cpr<Integer>());
			DataValue<Integer> nextFresh = new DataValue<Integer>(getType(), 
					Math.floorDiv(maxVal.getId(), freshStep) * freshStep + freshStep);
			return nextFresh;
		}
	}

	@Override
	public Collection<DataValue<Integer>> getAllNextValues(List<DataValue<Integer>> vals) {
		List<DataValue<Integer>> nextValues = getPotential(vals);
		nextValues.add(getFreshValue(vals));
		return nextValues;
	}

	@Override
	public List<EnumSet<DataRelation>> getRelations(List<DataValue<Integer>> left, DataValue<Integer> right) {
		List<EnumSet<DataRelation>> ret = new ArrayList<>();
		for (DataValue<Integer> dv : left) {
			DataRelation rel = getRelation(dv, right);
			ret.add(EnumSet.of(rel));
		}
		return ret;
	}
	
	private DataRelation getRelation(DataValue<Integer> left, DataValue<Integer> right) {
		if (right.getId().equals(left.getId())) {
			return DataRelation.EQ;
		} else {
			List<DataValue<Integer>> sumConsts = constants.getSumCs(getType());
			for (DataValue<Integer> sc : sumConsts) {
				if (right.getId().equals(sc.getId() + left.getId())) {
					switch (sumConsts.indexOf(sc)) {
					case 0: 
						return DataRelation.EQ_SUMC1; 
					case 1: 
						return DataRelation.EQ_SUMC2;
					default: assert false;
					}
					break;
				}
			}
			return DataRelation.DEFAULT;
		} 
	}
			
	@Override
	public EnumSet<DataRelation> recognizedRelations() {
		return EnumSet.of(DataRelation.EQ, DataRelation.EQ_SUMC1, DataRelation.EQ_SUMC2, DataRelation.DEFAULT);
	}

	@Override
	public List<DataValue<Integer>> getPotential(List<DataValue<Integer>> vals) {
		List<DataValue<Integer>> sortedPot = new ArrayList<DataValue<Integer>>( vals);
		List<DataValue<Integer>> sumConsts = constants.getSumCs(getType());
		
		for (DataValue<Integer> dv : sumConsts) {
			for (DataValue<Integer> potVal : vals ) {
				SumCDataValue<Integer> sumc = new SumCDataValue<Integer>(potVal, dv);
				// we exclude constants from sums, we also don't include duplicates
				if (!constants.containsValue(potVal) && !sortedPot.contains(sumc)) {
					sortedPot.add(new SumCDataValue<Integer>(potVal, dv));
				}
			}
		}
		
		Collections.sort(sortedPot, new Cpr<Integer>());
		
		return sortedPot;
	}

	@Override
	public void setConstants(Constants constants) {
		this.constants = constants;
		List<DataValue<Integer>> sumConsts = constants.getSumCs(getType());
		if (sumConsts.isEmpty()) {
			freshStep = 1;
		} else {
			Integer maxSumConst = Collections.max(sumConsts, new Cpr<Integer>()).getId();
			freshStep = maxSumConst * FRESH_FACTOR;
		}
	}
}
