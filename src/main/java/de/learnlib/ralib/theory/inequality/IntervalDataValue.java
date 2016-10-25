package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;

public class IntervalDataValue<T extends Comparable<T>> extends DataValue<T>{
	
	/**
	 * Constructs interval DVs from left and right ends, by selecting a value
	 * in between. One of left or right can be null, meaning there is no boundary. 
	 */
	public static <T extends Comparable<T>>  IntervalDataValue<T>  instantiateNew(DataValue<T> left, DataValue<T> right) {
		DataType<T> type = left != null ? left.getType() : right.getType();
		Class<T> cls = type.getBase();
		
		T betweenVal;
		T leftVal;
		T rightVal;
		
		// in case either is null, we just provide an increment/decrement
		if (left == null) 
			leftVal = cls.cast(DataValue.sub(right, new DataValue<>(type, cast(2, type))).getId());
		else 
			leftVal = left.getId();
		if (right == null)
			rightVal = cls.cast(DataValue.add(left, new DataValue<>(type, cast(2, type))).getId());
		else
			rightVal = right.getId();

		betweenVal = pickInBetweenValue(type.getBase(), leftVal, rightVal);

		return new IntervalDataValue<T>(new DataValue<T>(type, betweenVal), left, right);
	}
	
	private static <T extends Comparable<T>> T pickInBetweenValue(Class<T> clz, T leftVal, T rightVal) {
		T betweenVal;
		if (leftVal.compareTo(rightVal) >= 0 ) {
			throw new RuntimeException("Invalid interval, left end bigger than right end \n "
					+ "left: " + leftVal + " right: " + rightVal + " ]");
		}
		
		if (clz.isAssignableFrom(Integer.class)) {
			Integer intVal; 
			if ((((Integer) rightVal) - ((Integer) leftVal)) > 1) {
				intVal = ((Integer) leftVal) + 1;
			} else {
				throw new RuntimeException("Cannot instantiate value in int interval \n "
						+ "left: " + leftVal + " right: " + rightVal + " ]");
			}
			betweenVal =  clz.cast( intVal);
		} else {
			if(clz.isAssignableFrom(Double.class)) {
				Double doubleVal;
				if ((((Double) rightVal) - ((Double) leftVal)) > 1.0) {
					doubleVal = ((Double) leftVal) + 1.0;
				} else {
					doubleVal = (((Double) rightVal) + ((Double) leftVal))/2 ;
				}
				betweenVal = clz.cast(doubleVal);
			} else {
				throw new RuntimeException("Unsupported type " + leftVal.getClass());
			}
		}
		
		return betweenVal;
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
