package de.learnlib.ralib.tools;

import de.learnlib.ralib.sul.DataWordSUL;

public interface SULFactory {
	/**
	 *  Constructs new SUL
	 */
	DataWordSUL newSUL();
	
	/**
	 * Returns true if SULs can be run in parallel.  
	 */
	boolean isParallelizable();
	
	/**
	 * Constructs a number of SUL instances which can be run in parallel. 
	 */
	DataWordSUL [] newIndependentSULs(int numInstances);
}
