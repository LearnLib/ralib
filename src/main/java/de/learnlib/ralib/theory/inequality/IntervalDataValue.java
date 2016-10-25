package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;

public class IntervalDataValue<T extends Comparable<T>> extends DataValue<T>{
	
	/**
	 * Constructs interval DVs from left and right ends, by selecting a value
	 * in between. One of left or right can be null, meaning there is no boundary. 
	 */
	public static <T extends Comparable<T>>  IntervalDataValue<T>  instantiateNew(DataValue<T> left, DataValue<T> right) {
		DataType type = left != null ? left.getType() : right.getType();
		
		T betweenVal;
		
		// in case either is null, we just provide an increment/decrement
		if (left == null) {
			left = (DataValue<T>) DataValue.sub(right, new DataValue<>(type, cast(2, type)));
		} else {
			if (right == null)
				right = (DataValue<T>) DataValue.add(left, new DataValue<>(type, cast(2, type)));
		}
		T leftVal = left.getId();
		T rightVal = right.getId();
		
		
		if (leftVal.compareTo(rightVal) >= 0 ) {
			throw new RuntimeException("Invalid interval, left end bigger than right end \n "
					+ "left: " + left + " right: " + right + " ]");
		}
		
		if (type.getBase().isAssignableFrom(Integer.class)) {
			Integer intVal; 
			if ((((Integer) rightVal) - ((Integer) leftVal)) > 1) {
				intVal = ((Integer) leftVal) + 1;
			} else {
				throw new RuntimeException("Cannot instantiate value in int interval \n "
						+ "left: " + left + " right: " + right + " ]");
			}
			betweenVal = (T) intVal;
		} else {
			if(type.getBase().isAssignableFrom(Double.class)) {
				Double doubleVal;
				if ((((Double) rightVal) - ((Double) leftVal)) > 1.0) {
					doubleVal = ((Double) leftVal) + 1.0;
				} else {
					doubleVal = (((Double) rightVal) + ((Double) leftVal))/2 ;
				}
				betweenVal = (T) doubleVal;
			} else {
				throw new RuntimeException("Unsupported type " + leftVal.getClass());
			}
		}
		
		return new IntervalDataValue<T>(new DataValue<T>(type, betweenVal), left, right);
	}

	private DataValue<T> left;
	private DataValue<T> right;
	
	public IntervalDataValue(DataValue<T> dv, DataValue<T> left, DataValue<T> right) {
		super(dv.getType(), dv.getId());
		this.left = left;
		this.right = right;
		
	}
	
	public String toString() {
		return super.toString() + " ( " + (this.getLeft() != null ? this.getLeft().getId().toString() : "") + ":" +
					(this.getRight() != null ? this.getRight().getId().toString() : "") + ")"; 
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
