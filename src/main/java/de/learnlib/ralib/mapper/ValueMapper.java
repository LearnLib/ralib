package de.learnlib.ralib.mapper;

import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;

/**
 * A value mapper associates a value to another such that by 
 * */
public interface ValueMapper<T> {
	
	/**
	 * Assuming keySet and valueSet are R indistinguishable, returns a value <b>ret</b> such that 
	 * [keySet:value] is also R indistinguishable from [valueSet:ret]. R is given by the theory used.
	 * 
	 * The returned value is wrapped around the specific DataValue type, which conveys the nature
	 * of the value. For example, if the value was found Fresh, it will be wrapped around a FreshValue
	 * data type.
	 * 
	 */
	public DataValue<T> canonize(DataValue<T> thisValue, Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants);
	
	/**
	 * Deconanization does exactly the same thing. In 
	 * @param thisValue
	 * @param thisToOtherMap
	 * @return
	 */
	public DataValue<T> decanonize(DataValue<T> thisValue, Map<DataValue<T>,DataValue<T>> thisToOtherMap, Constants constants);
	
	
}
