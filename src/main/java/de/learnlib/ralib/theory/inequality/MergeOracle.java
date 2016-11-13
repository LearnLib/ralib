package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.theory.SDTGuard;

public interface MergeOracle {
	/**
	 * Applied on two non-merged guards, returns true if the guards can be merged, otherwise false.
	 */
	public boolean canMerge(SDTGuard aGuard, SDTGuard withGuard);
}
