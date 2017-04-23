package de.learnlib.ralib.theory;

import de.learnlib.ralib.oracles.mto.SDT;

public interface SDTEquivalenceChecker {
	public boolean checkSDTEquivalence(SDTGuard guard, SDT guardSdt, SDTGuard elseGuard, SDT elseGuardSdt);
}
