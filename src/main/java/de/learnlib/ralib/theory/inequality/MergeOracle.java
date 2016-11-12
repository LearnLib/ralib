package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.theory.SDTGuard;

public interface MergeOracle {
	public boolean canMerge(SDTGuard aGuard, SDTGuard withGuard);
}
