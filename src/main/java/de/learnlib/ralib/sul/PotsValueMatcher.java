package de.learnlib.ralib.sul;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.Theory;

public class PotsValueMatcher<T> implements ValueMatcher<T> {

	private Theory<T> theory;
	private DataType type;

	public PotsValueMatcher(DataType type, Theory<T> theory) {
		this.theory = theory;
		this.type = type;
	}

	@Override
	public T getFreshValue(Collection<T> relativeToCollection) {
		List<DataValue<T>> dvs = toDvList(this.type, new ArrayList<>(relativeToCollection));
		DataValue<T> freshValue = this.theory.getFreshValue(dvs);
		return freshValue.getId();
	}
	

	@Override
	public boolean isValueFresh(T value, Collection<T> relativeToCollection) {
		List<T> list = relativeToCollection.stream().collect(Collectors.toList());
		List<DataValue<T>> pots = getPotsOfList(this.type, list);
		return pots.stream().anyMatch(pot -> pot.getId().equals(value));
	}
	
	@Override
	public T getAnalogousValue(T toKey, Map<T, T> inMap) {
		List<T> keys = new ArrayList<T>(inMap.size());
		List<T> values = new ArrayList<T>(inMap.size());
		for (Map.Entry<T, T> entry : inMap.entrySet()) {
			keys.add(entry.getKey());
			values.add(entry.getValue());	
		}
		
		return this.getAnalogousValue(toKey, keys, values);
	}
	
	

	private T getAnalogousValue(T toValue, List<T> inList, List<T> relativeToList) {
		assert inList.size() == relativeToList.size();
		Origin<T> origin = getOriginOfValueFromList(this.type, toValue, inList);
		if (origin != null) {
			List<DataValue<T>> potsOfOtherList = getPotsOfList(this.type, relativeToList);
			return potsOfOtherList.get(origin.indexInPots).getId();
		}
		return null;
	}

	private Origin<T> getOriginOfValueFromList(DataType t, T val, List<T> values) {
		List<DataValue<T>> pots = getPotsOfList(t, values);
		List<DataValue<T>> relatedDVs = pots.stream().filter(potVal -> val.equals(potVal.getId()))
				.collect(Collectors.toList());
		return relatedDVs.isEmpty() ? null : new Origin<T>(relatedDVs.get(0).getId(), pots.indexOf(relatedDVs.get(0)));
	}

	static class Origin<P> {
		public final P value;
		public final Integer indexInPots;

		public Origin(P value, Integer indexInPots) {
			this.value = value;
			this.indexInPots = indexInPots;
		}

	}

	private List<DataValue<T>> getPotsOfList(DataType t, List<T> values) {
		List<DataValue<T>> dataValues = toDvList(t, values);
		List<DataValue<T>> pot = this.theory.getPotential(dataValues);
		return pot;
	}
	
	private List<DataValue<T>> toDvList(DataType t, List<T> vals) {
		return vals.stream().map(val -> new DataValue<>(t, val)).
		collect(Collectors.toList());
	}

}
