package de.learnlib.ralib.theory.equality;

import de.learnlib.ralib.theory.DefaultGuardLogic;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;

public class EqualityGuardLogic extends DefaultGuardLogic{
	
	public SDTGuard conjunction(SDTGuard guard1, SDTGuard guard2) {
		assert guard1.getParameter().equals(guard2.getParameter());
		return super.conjunction(guard1, guard2);
	}

	@Override
	public SDTGuard disjunction(SDTGuard guard1, SDTGuard guard2) {
		assert guard1.getParameter().equals(guard2.getParameter());
		if (guard1 instanceof DisequalityGuard && guard2 instanceof EqualityGuard
				|| guard2 instanceof DisequalityGuard && guard1 instanceof EqualityGuard) {
			EqualityGuard eq = guard1 instanceof EqualityGuard ? (EqualityGuard) guard1 : (EqualityGuard) guard2;
			DisequalityGuard deq = guard1 instanceof DisequalityGuard ? (DisequalityGuard) guard1: (DisequalityGuard) guard2;
			if (eq.getExpression().equals(eq.getExpression()))
				return new SDTTrueGuard(eq.getParameter());
		}
		
		return super.disjunction(guard1, guard2);
	}
}
