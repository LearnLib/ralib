package de.learnlib.ralib.mapper;

import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;

/**
 * A determinizer maps between canonical/canonized (i.e. learner) and non-canonical/decanonized (i.e. SUT) values for a given Theory.
 * */
public interface Determinizer<T> {
	
	public DataValue<T> canonize(DataValue<T> decValue, Map<DataValue<T>, DataValue<T>> decToCanMap, Constants constants);
	
	public DataValue<T> decanonize(DataValue<T> canValue, Map<DataValue<T>,DataValue<T>> canToDecMap, Constants constants);
	
}
