package de.learnlib.ralib.utils;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class DataValueConstructor<T extends Comparable<T>> {
	private DataType type;

	public DataValueConstructor(DataType type) {
		this.type = type;
	}
	
	public DataValue<T> dv(T val) {
		return new DataValue<T>(this.type, val);
			
	}
	
	public FreshValue<T> fv(T val) {
		return new FreshValue<T>(this.type, val);
	}
	
	public SumCDataValue<T> sumcv(T val, T cons) {
		return new SumCDataValue<T>(dv(val), dv(cons));
	}
	
	public IntervalDataValue<T> intv(T val, DataValue<T> biggerThan, DataValue<T> smallerThan) {
		
		return new IntervalDataValue<T>(dv(val), biggerThan, smallerThan);
	}
	
	public IntervalDataValue<T> intv(T val, T biggerThan, T smallerThan) {
		DataValue<T> left = biggerThan != null? dv(biggerThan): null;
		DataValue<T> right = smallerThan != null? dv(smallerThan): null;
		return new IntervalDataValue<T>(dv(val), left, right);
	}
}
