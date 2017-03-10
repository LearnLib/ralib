package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.equality.EqualityGuard;

/**
 * Provides a general implementation of merging a series of if guards with an
 * else guard.
 */
public class IfElseGuardMerger {
	
	private SDTGuardLogic logic;
	public IfElseGuardMerger(SDTGuardLogic logic) {
		this.logic = logic;
	}
	
	/**
	 * Tries to merge each if guard with an else guard based on SDT equivalence.
	 * It assumes that the elseGuard is a conjunction over the negation of each
	 * of the if guards.
	 */
	public LinkedHashMap<SDTGuard, SDT> merge(Map<SDTGuard, SDT> ifGuards, SDTGuard elseGuard, SDT elseSDT) {
		LinkedHashMap<SDTGuard, SDT> merged = new LinkedHashMap<>();
		SDTGuard newElseGuard = elseGuard;
		for (SDTGuard ifGuard : ifGuards.keySet()) {
			SDT ifSdt = ifGuards.get(ifGuard);
			boolean equiv = checkSDTEquivalence(ifGuard, ifSdt, elseGuard, elseSDT);
			// are equivalent
			if (equiv) {
				newElseGuard = this.logic.disjunction(ifGuard, newElseGuard);
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

	/**
	 * Returns true if the two subtrees are equivalent in the context of the
	 * guards and false otherwise.
	 */
	boolean checkSDTEquivalence(SDTGuard guard, SDT guardSdt, SDTGuard elseGuard, SDT elseGuardSdt) {
		boolean equiv = false;
		if (guard instanceof EqualityGuard) {
			List<EqualityGuard> eqGuards =  new ArrayList<EqualityGuard>();
			eqGuards.add((EqualityGuard)guard);
			equiv = guardSdt.isEquivalentUnderEquality(elseGuardSdt, eqGuards);
		} else
			equiv = guardSdt.isEquivalent(elseGuardSdt, new VarMapping());
		return equiv;
	}
}
