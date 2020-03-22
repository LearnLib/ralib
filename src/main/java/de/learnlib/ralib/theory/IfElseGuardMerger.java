package de.learnlib.ralib.theory;

import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.oracles.mto.SDT;

/**
 * Provides a general implementation of merging a series of if guards with an
 * else guard.
 */
public class IfElseGuardMerger implements GuardMerger{
	
	private SDTGuardLogic logic;
	public IfElseGuardMerger(SDTGuardLogic logic) {
		this.logic = logic;
	}
	
	/**
	 * Tries to merge each if guard with an else guard based on SDT equivalence.
	 * It assumes that the elseGuard is a conjunction over the negation of each
	 * of the if guards.
	 * @param sdtChecker TODO
	 */
	public LinkedHashMap<SDTGuard, SDT> merge(Map<SDTGuard, SDT> ifGuards, SDTGuard elseGuard, SDT elseSDT, SDTEquivalenceChecker sdtChecker) {
		LinkedHashMap<SDTGuard, SDT> merged = new LinkedHashMap<>();
		SDTGuard newElseGuard = elseGuard;
		for (SDTGuard ifGuard : ifGuards.keySet()) {
			SDT ifSdt = ifGuards.get(ifGuard);
			boolean equiv = sdtChecker.checkSDTEquivalence(ifGuard, ifSdt, elseGuard, elseSDT);
			// are equivalent
			if (equiv) {
				newElseGuard = logic.disjunction(ifGuard, newElseGuard);
			} else {
				merged.put(ifGuard, ifSdt);
			}
		}
		SDTGuard finalElseGuard = newElseGuard;
		
		if (finalElseGuard instanceof SDTMultiGuard)
			 if (((SDTMultiGuard) finalElseGuard).getGuards().size() == 1)
				 finalElseGuard = ((SDTMultiGuard) finalElseGuard).getGuards().get(0);
			 else if (((SDTMultiGuard) finalElseGuard).getGuards().size() == 0)
				 finalElseGuard = new SDTTrueGuard(finalElseGuard.getParameter());

		merged.put(finalElseGuard, elseSDT);
		return merged;
	}
}
