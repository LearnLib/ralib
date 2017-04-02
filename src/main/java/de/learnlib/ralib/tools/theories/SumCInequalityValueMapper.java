package de.learnlib.ralib.tools.theories;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.lang.Number;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.mapper.ValueMapper;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.words.DataWords;

/**
 * Concrete value mapper for theories of inequality with sumc and fresh data values.
 */
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
	public DataValue<T> canonize(DataValue<T> decValue, Map<DataValue<T>, DataValue<T>> decToCanMap, Constants constants) {
		if (decToCanMap.containsKey(decValue)) {
			DataValue<T> mapping = decToCanMap.get(decValue);
			return  mapping; 
		}
		if (constants.containsValue(decValue))
			return decValue;
		
		for (DataValue<T> constant : this.sumConstants) {
			if (decToCanMap.containsKey(DataValue.sub(decValue, constant))) {
				DataValue<T> operand = decToCanMap.get(DataValue.sub(decValue, constant));
				SumCDataValue<T> sumc = new SumCDataValue<T>(operand, constant);
				return sumc;
			}
		}
		
		List<DataValue<T>> valList = DataWords.<T>joinValsToList(decToCanMap.values(), constants.values(decValue.getType()));
		DataValue<T> fv = this.theory.getFreshValue(valList);
		return new FreshValue<>(fv.getType(), fv.getId());
	}

	/**
	 * Decanonizes from SumC, Equal, Fresh Data and also Interval Values, to concrete values. .
	 */
	public DataValue<T> decanonize(DataValue<T> canValue, Map<DataValue<T>, DataValue<T>> canToDecMap, Constants constants) {
		if (canToDecMap.containsKey(canValue)) 
			return canToDecMap.get(canValue);
		if (constants.containsValue(canValue))
			return canValue;
		if (canValue instanceof IntervalDataValue) {
			IntervalDataValue<T> interval = (IntervalDataValue<T>) canValue;
			DataValue<T> left = null;
			DataValue<T> right = null;
			if (interval.getLeft() != null) 
				left = decanonize(interval.getLeft(), canToDecMap, constants);
			if (interval.getRight() != null) 
				right = decanonize(interval.getRight(), canToDecMap, constants);
			return this.theory.pickIntervalDataValue(left, right);
			
		}
		
		// value is a sumc, then we decanonize the operand
		if (canValue instanceof SumCDataValue) {
			SumCDataValue<T> sumCValue = (SumCDataValue<T>) canValue;
			DataValue<T> operand = decanonize(sumCValue.getOperand(), canToDecMap, constants);
			DataValue<T> sum = (DataValue<T>) DataValue.add(operand, sumCValue.getConstant());
			return sum;
		}
		
		List<DataValue<T>> valList = DataWords.<T>joinValsToList(canToDecMap.values(), constants.values(canValue.getType()));
		DataValue<T> fv = this.theory.getFreshValue(valList);
		return fv;
	}

}
