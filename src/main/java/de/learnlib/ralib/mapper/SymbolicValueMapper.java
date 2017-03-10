package de.learnlib.ralib.mapper;

import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.tools.theories.NumberInequalityTheory;
import de.learnlib.ralib.words.DataWords;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * A value mapper which canonizes based on semi-symbolic information found in data values.
 * The symbolic information is the class type, which shows where this value comes from.
 */
public class SymbolicValueMapper<T extends Comparable<T>> implements ValueMapper<T> {
	
	private Theory<T> theory;
	private DataType<T> type;

	public SymbolicValueMapper(Theory<T> theory, DataType<T> type) {
		this.theory = theory;
		this.type = type;
	}

	public DataValue<T> canonize(DataValue<T> value, Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		if (thisToOtherMap.containsKey(value)) {
			DataValue<T> mapping = thisToOtherMap.get(value);
			return new DataValue<T>( mapping.getType(), mapping.getId());
		}
		if (constants.containsValue(value)) 
			return value;
		DataValue<T> mappedValue = resolveValue(value, thisToOtherMap);
		if (mappedValue == null) {
			mappedValue = getFreshValue(thisToOtherMap, constants);
		}
		return mappedValue;
	}
	
	// returns the resolved value or null, if the value cannot be resolved. The value cannot be resolved if:
	// 	for SumC the operand is missing
	//  for Interval data guards, at least one of the endpoints are missing or the interval is invalid
	private DataValue<T> resolveValue(DataValue<T> value, Map<DataValue<T>, DataValue<T>> thisToOtherMap) {
		if (value instanceof SumCDataValue) {
			SumCDataValue<T> sumc = (SumCDataValue<T>) value;
			DataValue<T> operand = resolveValue(sumc.getOperand(), thisToOtherMap);
			if (operand != null) {
				return  new SumCDataValue<T>(operand, sumc.getConstant());
			} else {
				return null;
			}
		} else if (value instanceof IntervalDataValue) {
			IntervalDataValue<T> intv = (IntervalDataValue<T>) value;
			DataValue<T> newLeft = null;
			if (intv.getLeft() != null) {
				newLeft = resolveValue(intv.getLeft(), thisToOtherMap);
			}
			
			DataValue<T> newRight = null;
			if (intv.getRight() != null) {
				newRight = resolveValue(intv.getRight(), thisToOtherMap);
			}

			// if the endpoints of the interval were resolved
			if (!Boolean.logicalXor(newRight == null, intv.getRight() == null) && !Boolean.logicalXor(newLeft == null, intv.getLeft() == null)
					&& (newRight != null || newLeft != null)) {
				if ((newRight == null || newLeft == null) || (newRight.getId().compareTo(newLeft.getId()) > 0))
					return ((NumberInequalityTheory<T>)this.theory).pickIntervalDataValue(newLeft, newRight); 
				else 
					// since we use this to canonize a trace from which segments where removed, it can happen that fresh values appear disorderly,
					// that is a bigger fresh value appears before a smaller one. In such cases, intervals are mapped to fresh values
					
					return null;
			} else {
				return null;
			}
			
		} else {
			return thisToOtherMap.get(value);
		}
	}
	
	private FreshValue<T> getFreshValue(Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		List<DataValue<T>> valList = DataWords.joinValsToList(thisToOtherMap.values(), constants.values(this.type));
		DataValue<T> fv = this.theory.getFreshValue(valList);
		return new FreshValue<T>(fv.getType(), fv.getId());
	}


	public DataValue<T> decanonize(DataValue<T> value,
			Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		throw new NotImplementedException();
	}
}