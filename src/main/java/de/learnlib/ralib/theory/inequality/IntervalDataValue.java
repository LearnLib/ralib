package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataValue;

public class IntervalDataValue<T> extends DataValue<T>{

	private DataValue<T> left;
	private DataValue<T> right;
	
	public IntervalDataValue(DataValue<T> dv, DataValue<T> left, DataValue<T> right) {
		super(dv.getType(), dv.getId());
		this.left = left;
		this.right = right;
		
	}

	
	public DataValue<T> getLeft() {
		return this.left;
	}
	
	
	public DataValue<T> toRegular() {
		return new DataValue<T> (this.type, this.id);
	}


	public DataValue<T> getRight() {
		return this.right;
	}
}
