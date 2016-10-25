package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDoubleInequalityValueMapper implements ValueMapper<Double>{

	private DataType type;
	private DoubleInequalityTheory theory;
	private List<DataValue<Double>> constants;

	public SumCDoubleInequalityValueMapper(DataType type, DoubleInequalityTheory theory) {
		this(type, theory, Collections.emptyList());
	}
			
	
	public SumCDoubleInequalityValueMapper(DataType type, DoubleInequalityTheory theory, List<DataValue<Double>> sumConstants) {
		this.type = type;
		this.theory = theory;
		this.constants = sumConstants;
		
	}

	/**
	 * We can only canonize to SumC, Equal or Fresh Data Values. 
	 */
	
	public DataValue<Double> canonize(DataValue<Double> value, Map<DataValue<Double>, DataValue<Double>> thisToOtherMap) {
		if (thisToOtherMap.containsKey(value)) {
			DataValue<Double> dv = thisToOtherMap.get(value);
			return new DataValue<>(dv.getType(), dv.getId()); 
		}
		for (DataValue<Double> constant : this.constants) {
			if (thisToOtherMap.containsKey(DataValue.sub(value, constant))) {
				DataValue<Double> operand = thisToOtherMap.get(DataValue.sub(value, constant));
				return new SumCDataValue<Double>(operand, constant);
			}
		}
		
		DataValue<Double> fv = this.theory.getFreshValue(new ArrayList<>(thisToOtherMap.values()));
		return new FreshValue<>(fv.getType(), fv.getId());
	}

	public DataValue<Double> decanonize(DataValue<Double> value, Map<DataValue<Double>, DataValue<Double>> thisToOtherMap) {
		if (thisToOtherMap.containsKey(value)) 
			return thisToOtherMap.get(value);
		if (value instanceof IntervalDataValue) {
			IntervalDataValue<Double> interval = (IntervalDataValue<Double>) value;
			DataValue<Double> left = null;
			DataValue<Double> right = null;
			if (interval.getLeft() != null) 
				left = decanonize(interval.getLeft(), thisToOtherMap);
			if (interval.getRight() != null) 
				right = decanonize(interval.getRight(), thisToOtherMap);
			return IntervalDataValue.instantiateNew(left, right);
			
		}
		
		// value is a sumc, then we decanonize the operand
		if (value instanceof SumCDataValue) {
			SumCDataValue<Double> sumCValue = (SumCDataValue<Double>) value;
			DataValue<Double> operand = decanonize(sumCValue.getOperand(), thisToOtherMap);
			return new DataValue<Double>( operand.getType(), operand.getId() + sumCValue.getConstant().getId());
		}
		
		// a second check, in case casting a SumC was used, no SumCDataValue was created (this fallback can only be applied on SumC, not on interval DVs)
		for (DataValue<Double> constant : this.constants) {
			DataValue<Double> sub = (DataValue<Double>) DataValue.sub(value, constant);
			if (thisToOtherMap.containsKey(sub)) {
				DataValue<Double> otherOperand = thisToOtherMap.get(sub);
				return new DataValue<Double>( otherOperand.getType(), otherOperand.getId() + constant.getId());
			}
		}

//		if (!thisToOtherMap.isEmpty() && value.getId() 
//				< Collections.max(thisToOtherMap.keySet().stream().map(k -> k.getId()).collect(Collectors.toList()))) {
//			return value;
//		}
		
		DataValue<Double> fv = this.theory.getFreshValue(thisToOtherMap.values().stream().collect(Collectors.toList())); 
//				this.theory.getFreshValue(thisToOtherMap.values().stream().
//				map(v -> new DataValue<>(this.type, v)).collect(Collectors.toList()));
		return fv;
	}

}
