package de.learnlib.ralib.tools.theories;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.lang.Number;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.words.DataWords;

public class SumCInequalityValueMapper<T extends Number & Comparable<T>> implements ValueMapper<T>{

	private InequalityTheoryWithEq<T> theory;
	private List<DataValue<T>> sumConstants;

	public SumCInequalityValueMapper(InequalityTheoryWithEq<T> theory) {
		this(theory, Collections.emptyList());
	}
			
	
	public SumCInequalityValueMapper(InequalityTheoryWithEq<T> theory, List<DataValue<T>> sumConstants) {
		this.theory = theory;
		this.sumConstants = sumConstants;
		
	}

	/**
	 * Canonizes concrete values to SumC, Equal or Fresh Data Values. 
	 */
	public DataValue<T> canonize(DataValue<T> value, Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		if (thisToOtherMap.containsKey(value)) {
			DataValue<T> mapping = thisToOtherMap.get(value);
			return new DataValue<>(mapping.getType(), mapping.getId()); 
		}
		if (constants.containsValue(value))
			return value;
		for (DataValue<T> constant : this.sumConstants) {
			if (thisToOtherMap.containsKey(DataValue.sub(value, constant))) {
				DataValue<T> operand = thisToOtherMap.get(DataValue.sub(value, constant));
				return new SumCDataValue<T>(operand, constant);
			}
		}
		
		List<DataValue<T>> valList = DataWords.<T>joinValsToList(thisToOtherMap.values(), constants.values(value.getType()));
		DataValue<T> fv = this.theory.getFreshValue(valList);
		return new FreshValue<>(fv.getType(), fv.getId());
	}

	/**
	 * Decanonizes from SumC, Equal, Fresh Data and also Interval Values, to concrete values. .
	 */
	public DataValue<T> decanonize(DataValue<T> value, Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		if (thisToOtherMap.containsKey(value)) 
			return thisToOtherMap.get(value);
		if (constants.containsValue(value))
			return value;
		if (value instanceof IntervalDataValue) {
			IntervalDataValue<T> interval = (IntervalDataValue<T>) value;
			DataValue<T> left = null;
			DataValue<T> right = null;
			if (interval.getLeft() != null) 
				left = decanonize(interval.getLeft(), thisToOtherMap, constants);
			if (interval.getRight() != null) 
				right = decanonize(interval.getRight(), thisToOtherMap, constants);
			return IntervalDataValue.instantiateNew(left, right);
			
		}
		
		// value is a sumc, then we decanonize the operand
		if (value instanceof SumCDataValue) {
			SumCDataValue<T> sumCValue = (SumCDataValue<T>) value;
			DataValue<T> operand = decanonize(sumCValue.getOperand(), thisToOtherMap, constants);
			DataValue<T> sum = (DataValue<T>) DataValue.add(operand, sumCValue.getConstant());
			return sum;
		}
		
		List<DataValue<T>> valList = DataWords.<T>joinValsToList(thisToOtherMap.values(), constants.values(value.getType()));
		DataValue<T> fv = this.theory.getFreshValue(valList);
		return fv;
	}

}
