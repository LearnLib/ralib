package de.learnlib.ralib.utils;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class DataValueConstructor<T> {
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
	
	public IntervalDataValue<T> intv(T val, T bigger, T smaller) {
		DataValue<T> left = bigger != null? dv(bigger): null;
		DataValue<T> right = smaller != null? dv(smaller): null;
		return new IntervalDataValue<T>(dv(val), left, right);
	}
}
