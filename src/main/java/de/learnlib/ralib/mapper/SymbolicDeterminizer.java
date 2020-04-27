package de.learnlib.ralib.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.tools.theories.NumberInequalityTheory;
import de.learnlib.ralib.words.DataWords;

public class SymbolicDeterminizer<T extends Comparable<T>> implements Determinizer<T> {
	
	private Theory<T> theory;
	private DataType type;

	public SymbolicDeterminizer(Theory<T> theory, DataType type) {
		this.theory = theory;
		this.type = type;
	}

	/**
	 * Replaces by fresh values constituent data values with operands/interval ends not included in the keys of the map.
	 * Doing so, it computes a new value which it returns.
	 * 
	 * @param thisValue - the value whose data values may be replaced
	 * */
	public DataValue<T> canonize(DataValue<T> thisValue, Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		if (thisToOtherMap.containsKey(thisValue)) {
			DataValue<T> mapping = thisToOtherMap.get(thisValue);
			return mapping;
		}
		if (constants.containsValue(thisValue)) 
			return thisValue;
		DataValue<T> mappedValue = resolveValue(thisValue, thisToOtherMap, constants);
		if (mappedValue == null) {
			mappedValue = getFreshValue(thisToOtherMap, constants);
		}
		return mappedValue;
	}
	
	// returns the resolved value or null, if the value cannot be resolved. The value cannot be resolved if:
	// 	for SumC the operand is missing
	//  for Interval data guards, at least one of the endpoints are missing or the interval is invalid 
	private DataValue<T> resolveValue(DataValue<T> thisValue, Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		if (constants.containsValue(thisValue))
			return thisValue;
		if (thisValue instanceof SumCDataValue) {
			SumCDataValue<T> sumc = (SumCDataValue<T>) thisValue;
			DataValue<T> operand = resolveValue(sumc.getOperand(), thisToOtherMap, constants);
			if (operand != null) {
				return  new SumCDataValue<T>(operand, sumc.getConstant());
			} else {
				return null;
			}
		} else if (thisValue instanceof IntervalDataValue) {
			IntervalDataValue<T> intv = (IntervalDataValue<T>) thisValue;
			DataValue<T> newLeft = null;
			if (intv.getLeft() != null) {
				newLeft = resolveValue(intv.getLeft(), thisToOtherMap, constants);
			}
			
			DataValue<T> newRight = null;
			if (intv.getRight() != null) {
				newRight = resolveValue(intv.getRight(), thisToOtherMap, constants);
			}

			// if the endpoints of the interval were resolved
			if (!Boolean.logicalXor(newRight == null, intv.getRight() == null) && !Boolean.logicalXor(newLeft == null, intv.getLeft() == null)
					&& (newRight != null || newLeft != null)) {
				if ((newRight == null || newLeft == null) || (newRight.getId().compareTo(newLeft.getId()) > 0)) {
					if (newRight != null && newLeft != null ) {
						// if intervals are not between values within more than fresh space of each other, we are not interested
						DataValue fv = this.theory.getFreshValue(Collections.singletonList(newLeft));
						boolean inSameFreshSpace = (newRight.getId().compareTo((T) fv.getId()) <=0);
						if (!inSameFreshSpace)
							return null;
					}
					IntervalDataValue<T> val = ((NumberInequalityTheory<T>)this.theory).pickIntervalDataValue(newLeft, newRight);
					return val;
				} else 
					// since we use this to canonize a trace from which segments where removed, it can happen that fresh values appear disorderly,
					// that is a bigger fresh value appears before a smaller one. In such cases, intervals are mapped to fresh values
					//
					if (Boolean.logicalXor(intv.getLeft() == null, intv.getRight() == null)) {
						throw new RuntimeException("Cannot canonize " + newLeft + " " + newRight);
					}
					return null;
			} else {
				return null;
			}
			
		} else {
			return thisToOtherMap.get(thisValue);
		}
	}
	
	private FreshValue<T> getFreshValue(Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		List<DataValue<T>> valList = DataWords.joinValsToList(thisToOtherMap.values(), constants.values(this.type));
		DataValue<T> fv = this.theory.getFreshValue(valList);
		return new FreshValue<T>(fv.getType(), fv.getId());
	}


	public DataValue<T> decanonize(DataValue<T> value,
			Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		throw new NotImplementedException("Symbolic canonizing can only be done one way");
	}
}