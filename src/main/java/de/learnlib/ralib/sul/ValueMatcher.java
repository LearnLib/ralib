package de.learnlib.ralib.sul;

import java.util.Collection;
import java.util.Map;

public interface ValueMatcher<T> {
	/**
	 * Gives the analogous value to {@code toKey} in {@code inMap},
	 * along with a marker which states if the value was found fresh. 
	 */
	public T getAnalogousValue(T toKey, Map<T,T> inMap);
	
	/**
	 * Gives the analogous value to {@code toValue} in {@code fromList} relative to {@code relativeToList}.
	 */
	public T getFreshValue(Collection<T> relativeToCollection);
	
	public boolean isValueFresh(T value, Collection<T> relativeToCollection);
}
