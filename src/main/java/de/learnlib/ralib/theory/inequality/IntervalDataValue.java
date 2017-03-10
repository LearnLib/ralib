package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;

public class IntervalDataValue<T extends Comparable<T>> extends DataValue<T>{
	/**
	 * Constructs interval DVs from left and right ends, by selecting a value
	 * in between. One of left or right can be null, meaning there is no boundary. 
	 * 
	 * In case where there is no boundary, smBgStep is deducted from/added to the end.
	 */
	public static <T extends Comparable<T>>  IntervalDataValue<T>  instantiateNew(DataValue<T> left, DataValue<T> right, DataValue<T> smBgStep) {
		DataType<T> type = left != null ? left.getType() : right.getType();
		Class<T> cls = type.getBase();
		
		T intvVal;
		
		// in case either is null, we just provide an increment/decrement
		if (left == null && right != null) {
			// we select a value at least 
			intvVal = cls.cast(DataValue.sub(right, smBgStep).getId());
		} else if (left != null && right == null) {
			intvVal = cls.cast(DataValue.add(left, smBgStep).getId());
		} else if (left != null && right != null) {
			intvVal = pickInBetweenValue(type.getBase(), left.getId(), right.getId());
			if (intvVal == null)
				throw new DecoratedRuntimeException("Invalid interval, left end bigger or equal to right end \n ")
				.addDecoration("left", left).addDecoration("right", right);
		} else {
			throw new RuntimeException("Both ends of the Interval cannot be null");
		}

		return new IntervalDataValue<T>(new DataValue<T>(type, intvVal), left, right);
	}
	
	private static <T extends Comparable<T>> T pickInBetweenValue(Class<T> clz, T leftVal, T rightVal) {
		T betweenVal;
		if (leftVal.compareTo(rightVal) >= 0 ) {
			return null;
		}
		
		if (clz.isAssignableFrom(Integer.class)) {
			Integer intVal; 
//			if ((((Integer) rightVal) - ((Integer) leftVal)) > INSIDE_STEP) {
//				intVal = ((Integer) leftVal) + INSIDE_STEP;
//			} else {
			// to avoid overflow
				intVal = Math.addExact((Integer) leftVal, Math.subtractExact((Integer) rightVal, (Integer) leftVal)/2) ;
				
//			}
			
			betweenVal =  clz.cast( intVal);
		} else {
			if(clz.isAssignableFrom(Double.class)) {
				Double doubleVal;
//				if ((((Double) rightVal) - ((Double) leftVal)) > INSIDE_STEP) {
//					doubleVal = ((Double) leftVal) + INSIDE_STEP;
//				} else {
					doubleVal = (((Double) rightVal) + ((Double) leftVal))/2 ;
//				}
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
