package de.learnlib.ralib.data;

import java.util.LinkedHashMap;

/**
 * Maps symbolic data values to simple expressions over other SDVs. Used to replace
 * SDVs by these expressions, when checking for equivalence under equality. 
 */
public class Replacement extends LinkedHashMap<SymbolicDataValue, SymbolicDataExpression> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
