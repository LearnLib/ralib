package de.learnlib.ralib.theory.equality;

import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.mapper.Determinizer;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.DataWords;

public class SumCEqualityDeterminizer <T extends Number> implements Determinizer<T>{

	private TypedTheory<T> theory;

	public SumCEqualityDeterminizer(TypedTheory<T> theory) {
		this.theory = theory;
	}
	
	@Override
	public DataValue<T> canonize(DataValue<T> decValue, Map<DataValue<T>, DataValue<T>> decToCanMap,
			Constants constants) {
		if (decToCanMap.containsKey(decValue)) {
			DataValue<T> mapping = decToCanMap.get(decValue);
			return  mapping; 
		}
		if (constants.containsValue(decValue)) {
			return decValue;
		}
		
		for (DataValue constant : constants.getSumCs(theory.getType())) {
			if (decToCanMap.containsKey(DataValue.sub(decValue, constant))) {
				DataValue<T> operand = decToCanMap.get(DataValue.sub(decValue, constant));
				SumCDataValue<T> sumc = new SumCDataValue<T>(operand, constant);
				return sumc;
			}
		}
		
		List<DataValue<T>> valList = DataWords.<T>joinValsToList(decToCanMap.values(), constants.values(decValue.getType()));
		DataValue<T> fv = theory.getFreshValue(valList);
		return new FreshValue<>(fv.getType(), fv.getId());
	}

	@Override
	public DataValue<T> decanonize(DataValue<T> canValue, Map<DataValue<T>, DataValue<T>> canToDecMap,
			Constants constants) {
		if (canToDecMap.containsKey(canValue)) { 
			return canToDecMap.get(canValue);
		}
		if (constants.containsValue(canValue)) {
			return canValue;
		}
		
		// value is a sumc, then we decanonize the operand
		if (canValue instanceof SumCDataValue) {
			SumCDataValue<T> sumCValue = (SumCDataValue<T>) canValue;
			DataValue<T> operand = decanonize(sumCValue.getOperand(), canToDecMap, constants);
			DataValue<T> sum = (DataValue<T>) DataValue.add(operand, sumCValue.getConstant());
			return sum;
		}
		
		List<DataValue<T>> valList = DataWords.<T>joinValsToList(canToDecMap.values(), constants.values(canValue.getType()));
		DataValue<T> fv = theory.getFreshValue(valList);
		return fv;
	}

}
