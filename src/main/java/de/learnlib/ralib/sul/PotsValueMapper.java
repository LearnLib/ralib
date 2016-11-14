package de.learnlib.ralib.sul;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.theory.Theory;


/**
 * A general value mapper which maps according to the index of the element in the
 * potential equal to the value to be mapped. 
 * 
 * Depending on the theory, this might or might not be a proper implementation
 * of R mapping for that particular theory.
 */
public abstract class PotsValueMapper<T> implements ValueMapper<T> {
	
	private Theory<T> theory;
	private DataType type;

	public PotsValueMapper(DataType type, Theory<T> theory) {
		this.theory = theory;
		this.type = type;
	}

//	public DataValue<T> canonize(T thisValue, Map<T,T> thisToOther) {
//		List<DataValue<T>> thisPots = getPots(this.type, thisToOther.keySet());
//
//		DataValue<T> rEquivValue = getREquivalentValueFromPots(thisValue, thisPots, thisToOther);
//		return rEquivValue;
//	}
//
//	protected DataValue<T> getREquivalentValueFromPots(T thisValue, List<DataValue<T>> thisPots, Map<T,T> thisToOther) {
//		DataValue<T> rEquivValue = getRelatedValueFromPots(thisValue, thisPots, thisToOther);
//		if (rEquivValue == null) {
//			T freshValue = getFreshValue(thisToOther.values());
//			rEquivValue = new FreshValue<T>(this.type, freshValue);
//		}
//		return rEquivValue;
//	}
//	
//	protected abstract DataValue<T> getRelatedValueFromPots(T thisValue, List<DataValue<T>> thisPots, Map<T,T> thisToOther);
//	
//	protected Origin<T> getOriginOfValue( T val, List<DataValue<T>> pots) {
//		List<DataValue<T>> relatedDVs = pots.stream().filter(potVal -> val.equals(potVal.getId()))
//				.collect(Collectors.toList());
//		assert relatedDVs.size() <= 1;
//		return relatedDVs.isEmpty() ? null : new Origin<T>(relatedDVs.get(0).getId(), pots.indexOf(relatedDVs.get(0)));
//	}
//
//	protected static class Origin<P> {
//		public final P value;
//		public final Integer indexInPots;
//
//		public Origin(P value, Integer indexInPots) {
//			this.value = value;
//			this.indexInPots = indexInPots;
//		}
//
//	}
//	
//	
//	private List<DataValue<T>> getPots(DataType t, Set<T> values) {
//		List<DataValue<T>> dataValues = toDvList(t, values);
//		List<DataValue<T>> pot = this.theory.getPotential(dataValues);
//		return pot;
//	}
//	
//	private List<DataValue<T>> toDvList(DataType t, Collection <T> vals) {
//		return vals.stream().map(val -> new DataValue<>(t, val)).
//		collect(Collectors.toList());
//	}
//
//	
//	private T getFreshValue(Collection<T> relativeToCollection) {
//		List<DataValue<T>> dvs = toDvList(this.type, new ArrayList<>(relativeToCollection));
//		DataValue<T> freshValue = this.theory.getFreshValue(dvs);
//		return freshValue.getId();
//	}
}
