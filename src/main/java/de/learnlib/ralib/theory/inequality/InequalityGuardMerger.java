package de.learnlib.ralib.theory.inequality;

import java.util.List;
import java.util.Map;

import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.GuardMerger;
import de.learnlib.ralib.theory.SDTEquivalenceChecker;
import de.learnlib.ralib.theory.SDTGuard;

public interface InequalityGuardMerger extends GuardMerger {
	
	/**
	 * Merges an ordered list of inequality guards based on SDT Equivalence and returns a merged map.
	 * @param sdtChecker TODO
	 */
	public  Map<SDTGuard, SDT>  merge(List<SDTGuard> sortedInequalityGuards, Map<SDTGuard, SDT> sdtMap, SDTEquivalenceChecker sdtChecker);
	
	
}
