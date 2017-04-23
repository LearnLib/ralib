package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.equality.EqualityGuard;


public class SyntacticEquivalenceChecker implements SDTEquivalenceChecker {

	/**
	 * Judges equivalence by comparing SDTs syntactically in the context of the guards. It assumes the canonical 
	 * labeling scheme was used in constructing the SDTs (whereby for example, guard s3==r1 is preferred over s3==s1 
	 * in case s1==s3), and relabels SDTs accordingly.   
	 */
	public boolean checkSDTEquivalence(SDTGuard guard, SDT guardSdt, SDTGuard otherGuard, SDT otherGuardSdt) {
		List<EqualityGuard> eqGuards =  new ArrayList<EqualityGuard>();
		if (guard instanceof EqualityGuard) 
			eqGuards.add((EqualityGuard)guard);
		if (otherGuard instanceof EqualityGuard)
			eqGuards.add((EqualityGuard)otherGuard);
				
		boolean equiv = guardSdt.isEquivalentUnderEquality(otherGuardSdt, eqGuards);
		return equiv;
	}

}
