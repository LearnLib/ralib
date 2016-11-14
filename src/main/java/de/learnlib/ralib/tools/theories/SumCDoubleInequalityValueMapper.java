package de.learnlib.ralib.tools.theories;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.words.DataWords;

public class SumCDoubleInequalityValueMapper implements ValueMapper<Double>{

	private DoubleInequalityTheory theory;
	private List<DataValue<Double>> sumConstants;

	public SumCDoubleInequalityValueMapper(DoubleInequalityTheory theory) {
		this(theory, Collections.emptyList());
	}
			
	
	public SumCDoubleInequalityValueMapper(DoubleInequalityTheory theory, List<DataValue<Double>> sumConstants) {
		this.theory = theory;
		this.sumConstants = sumConstants;
		
	}

	/**
	 * Canonizes concrete values to SumC, Equal or Fresh Data Values. 
	 */
	public DataValue<Double> canonize(DataValue<Double> value, Map<DataValue<Double>, DataValue<Double>> thisToOtherMap, Constants constants) {
		if (thisToOtherMap.containsKey(value)) {
			DataValue<Double> mapping = thisToOtherMap.get(value);
			return new DataValue<>(mapping.getType(), mapping.getId()); 
		}
		if (constants.containsValue(value))
			return value;
		for (DataValue<Double> constant : this.sumConstants) {
			if (thisToOtherMap.containsKey(DataValue.sub(value, constant))) {
				DataValue<Double> operand = thisToOtherMap.get(DataValue.sub(value, constant));
				return new SumCDataValue<Double>(operand, constant);
			}
		}
		
		List<DataValue<Double>> valList = DataWords.joinValsToList(thisToOtherMap.values(), constants.values(value.getType()));
		DataValue<Double> fv = this.theory.getFreshValue(valList);
		return new FreshValue<>(fv.getType(), fv.getId());
	}

	/**
	 * Decanonizes from SumC, Equal, Fresh Data and also Interval Values, to concrete values. 
	 * Also decanonizes from concretele past values and sums, s.t. it is not needed that all dvs are symbolic.
	 */
	public DataValue<Double> decanonize(DataValue<Double> value, Map<DataValue<Double>, DataValue<Double>> thisToOtherMap, Constants constants) {
		if (thisToOtherMap.containsKey(value)) 
			return thisToOtherMap.get(value);
		if (constants.containsValue(value))
			return value;
		if (value instanceof IntervalDataValue) {
			IntervalDataValue<Double> interval = (IntervalDataValue<Double>) value;
			DataValue<Double> left = null;
			DataValue<Double> right = null;
			if (interval.getLeft() != null) 
				left = decanonize(interval.getLeft(), thisToOtherMap, constants);
			if (interval.getRight() != null) 
				right = decanonize(interval.getRight(), thisToOtherMap, constants);
			return IntervalDataValue.instantiateNew(left, right);
			
		}
		
		// value is a sumc, then we decanonize the operand
		if (value instanceof SumCDataValue) {
			SumCDataValue<Double> sumCValue = (SumCDataValue<Double>) value;
			DataValue<Double> operand = decanonize(sumCValue.getOperand(), thisToOtherMap, constants);
			return new DataValue<Double>( operand.getType(), operand.getId() + sumCValue.getConstant().getId());
		}
		
		// a second check, in case casting a SumC was used, no SumCDataValue was created (this fallback can only be applied on SumC, not on interval DVs)
		for (DataValue<Double> constant : this.sumConstants) {
			DataValue<Double> sub = (DataValue<Double>) DataValue.sub(value, constant);
			if (thisToOtherMap.containsKey(sub)) {
				DataValue<Double> otherOperand = thisToOtherMap.get(sub);
				return new DataValue<Double>( otherOperand.getType(), otherOperand.getId() + constant.getId());
			}
		}
		
		List<DataValue<Double>> valList = DataWords.joinValsToList(thisToOtherMap.values(), constants.values(value.getType()));
		DataValue<Double> fv = this.theory.getFreshValue(valList);
		return fv;
	}

}
