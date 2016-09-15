package de.learnlib.ralib.data;

import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;

public interface SymbolicDataExpression {
	public SymbolicDataValue getSDV();

	public default boolean isRegister() {
		return this.getClass().equals(Register.class);
	}

	public default boolean isParameter() {
		return this.getClass().equals(Parameter.class);
	}

	public default boolean isConstant() {
		return this.getClass().equals(Constant.class);
	}

	public default boolean isSuffixValue() {
		return this.getClass().equals(SuffixValue.class);
	}
	
	
	/**
	 * Constructs a new symbolic data expression where the register is swapped
	 * by a new one. 
	 */
	public default SymbolicDataExpression swapSDV(SymbolicDataValue newSDV) {
		return newSDV;
	}
	
	/**
	 * Given a data value, solves the encapsulated sdv.
	 */
	public default <T> DataValue<T> solveSDVForValue(DataValue<T> val) {
		return val;
	}
}
