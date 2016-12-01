package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class DiscreteInequalityGuardLogic extends InequalityGuardLogic {
	

	public DiscreteInequalityGuardLogic() {
	}

	public SDTGuard disjunction(SDTGuard guard1, SDTGuard guard2) {
		EqualityGuard equGuard;
		IntervalGuard intGuard;
		if (guard1 instanceof EqualityGuard && guard2 instanceof EqualityGuard) {
			equGuard = (EqualityGuard) guard1;
			EqualityGuard equGuard2 = (EqualityGuard) guard2;
			return new IntervalGuard(guard1.getParameter(), equGuard.getExpression(), Boolean.FALSE, equGuard2.getExpression(), Boolean.FALSE);
		}
		if (guard1 instanceof IntervalGuard && guard2 instanceof EqualityGuard) {
			equGuard = (EqualityGuard) guard2;
			intGuard = (IntervalGuard) guard1;
			return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), equGuard.getExpression(), Boolean.FALSE); 
		} 
		
		if (guard1 instanceof EqualityGuard && guard2 instanceof IntervalGuard) {
			equGuard = (EqualityGuard) guard1;
			intGuard = (IntervalGuard) guard2;
			return new IntervalGuard(guard1.getParameter(), equGuard.getExpression(), Boolean.FALSE, intGuard.getRightExpr(), intGuard.getRightOpen());
		}
		
		SDTGuard ineqDisjunction = super.disjunction(guard1, guard2);
		
		return ineqDisjunction;
	}
	

}
