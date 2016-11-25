package de.learnlib.ralib.theory.equality;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.theory.IfElseGuardMerger;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;

public class IfElseEquGuardMerger extends IfElseGuardMerger{
	/**
	 * In case of equalities, the else guard is a conjunction over disequalities, which 
	 * comprise the negations of each if equality guard.
	 */

	protected SDTGuard merge(SDTGuard guard, SDTGuard elseGuard) {
		SDTAndGuard diseqConj = (SDTAndGuard) elseGuard;
		EqualityGuard eqGuard = (EqualityGuard) guard;
		SDTGuard newElseGuard = null; 
		DisequalityGuard diseqGuard = eqGuard.toDeqGuard();
		List<SDTGuard> newDiseqGuards = new ArrayList<SDTGuard>(diseqConj.getGuards());
		assert newDiseqGuards.remove(diseqGuard);
		
	
			SDTGuard[] conjArray = newDiseqGuards.toArray(new SDTGuard[]{});
			newElseGuard = new SDTAndGuard(elseGuard.getParameter(), conjArray);
		return newElseGuard;
	}
}
