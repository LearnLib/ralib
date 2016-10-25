package de.learnlib.ralib.sul;

import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;

/**
 * A value mapper maps a value to another such that by appending each to their associated R indistinguishable ordered sets, 
 * the resulting sets remain R equivalent. 
 * 
 * */
public interface ValueMapper<T> {
	
	/**
	 * Assuming refSet and forSet are R indistinguishable, returns a value <b>ret</b> such that 
	 * [refSet|value] is also R indistinguishable to [forSet:ret]. R is given by the theory used.
	 * 
	 * The returned value is wrapped around the specific DataValue type, which conveys the nature
	 * of the value. For example, if the value was found Fresh, it will be wrapped around a FreshValue
	 * data type.
	 * 
	 */
	public DataValue<T> canonize(DataValue<T> thisValue, Map<DataValue<T>, DataValue<T>> thisToOtherMap);
	
	public DataValue<T> decanonize(DataValue<T> thisValue, Map<DataValue<T>,DataValue<T>> thisToOtherMap);
	
	
}
