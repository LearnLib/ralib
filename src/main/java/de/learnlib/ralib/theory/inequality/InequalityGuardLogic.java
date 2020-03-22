package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuardLogic;

/**
 * Logic for inequalities. Note, depending on the valuation of variables,
 * obtained guards might not be satisfiable.
 */
public class InequalityGuardLogic implements SDTGuardLogic {

	private EqualityGuardLogic equalityLogic;

	public InequalityGuardLogic() {
		equalityLogic = new EqualityGuardLogic();
	}

	public SDTGuard conjunction(SDTGuard guard1, SDTGuard guard2) {
		
		if (guard1 instanceof IntervalGuard && guard2 instanceof IntervalGuard) {
			IntervalGuard intv1 = (IntervalGuard) guard1;
			IntervalGuard intv2 = (IntervalGuard) guard2;
			
			SDTGuard intersection = tryIntersectEnds(intv1, intv2);
			if (intersection == null)
				intersection = tryIntersectEnds(intv2, intv1);
			if (intersection != null)
				return intersection;
		}
		
		if (guard1 instanceof IntervalGuard && guard2 instanceof DisequalityGuard ||
				guard2 instanceof IntervalGuard && guard1 instanceof DisequalityGuard) {
			IntervalGuard intv = guard1 instanceof IntervalGuard ? (IntervalGuard) guard1 : (IntervalGuard) guard2;
			DisequalityGuard deq = guard1 instanceof DisequalityGuard ? (DisequalityGuard) guard1 : (DisequalityGuard) guard2;
			
			// [a, b) !=a -> (a, b)
			if (!intv.isSmallerGuard() && intv.getLeftExpr().equals(deq.getExpression())) 
				return new IntervalGuard(intv.getParameter(), intv.getLeftExpr(), Boolean.TRUE, intv.getRightExpr(), intv.getRightOpen());
			
			// (a, b] !=b -> (a, b)
			if (!intv.isBiggerGuard() && intv.getRightExpr().equals(deq.getExpression())) 
				return new IntervalGuard(intv.getParameter(), intv.getLeftExpr(), intv.getLeftOpen(), intv.getRightExpr(), Boolean.TRUE);
		}
		
		
		SDTGuard equDisjunction = equalityLogic.conjunction(guard1, guard2);
		return equDisjunction;
	}
	
	private SDTGuard tryIntersectEnds(IntervalGuard leftGuard, IntervalGuard rightGuard) {
		// .. e1) (e2 ..
		if (!leftGuard.isBiggerGuard() && !rightGuard.isSmallerGuard()) { 
			// (a, b] [b, ) -> ==b
			if (leftGuard.getRightExpr().equals(rightGuard.getLeftExpr()))
				if ( !leftGuard.getRightOpen() && !rightGuard.getLeftOpen())
					return new EqualityGuard(leftGuard.getParameter(), leftGuard.getRightExpr());
				else
					throw new DecoratedRuntimeException("Conjunction is false ")
					.addDecoration("int 1", leftGuard).addDecoration("int 2", rightGuard);
			
			// (, e1) (e2, )
			if (leftGuard.isSmallerGuard() && rightGuard.isBiggerGuard())
				// (, a) (b, ) -> (b, a) 
				return new IntervalGuard(leftGuard.getParameter(), rightGuard.getLeftExpr(), rightGuard.getLeftOpen(), leftGuard.getRightExpr(), leftGuard.getRightOpen());
			
			//  (e1, e2) (e3,)
			if (rightGuard.isBiggerGuard() && !leftGuard.isBiggerGuard()) 
				// (a, b) [a, ) -> (a, b)
				 if (leftGuard.getLeftExpr().equals(rightGuard.getLeftExpr()))
						 return new IntervalGuard(leftGuard.getParameter(), leftGuard.getLeftExpr(), rightGuard.getLeftOpen() || leftGuard.getLeftOpen(),
					
								 leftGuard.getRightExpr(), leftGuard.getRightOpen());
			//  (, e2) (e3, e4)
			if (leftGuard.isSmallerGuard() && !rightGuard.isBiggerGuard() )
				// (, b) (a, b)  -> (a, b)
				 if (leftGuard.getRightExpr().equals(rightGuard.getRightExpr()))
						 return new IntervalGuard(leftGuard.getParameter(), rightGuard.getLeftExpr(), leftGuard.getLeftOpen(),
								 rightGuard.getRightExpr(), rightGuard.getRightOpen() || leftGuard.getRightOpen());
		}
		return null;
	}

	public SDTGuard disjunction(SDTGuard guard1, SDTGuard guard2) {
		IntervalGuard intGuard;
		EqualityGuard equGuard;

		// (a, b) ==b -> (a, b]
		if (guard1 instanceof IntervalGuard && guard2 instanceof EqualityGuard) {
			intGuard = (IntervalGuard) guard1;
			equGuard = (EqualityGuard) guard2;

			if (!intGuard.isBiggerGuard() && equGuard.getExpression().equals(intGuard.getRightExpr()))
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(),
						intGuard.getRightExpr(), Boolean.FALSE);
			if (!intGuard.isSmallerGuard() && equGuard.getExpression().equals(intGuard.getLeftExpr()))
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), Boolean.FALSE,
						intGuard.getRightExpr(), intGuard.getRightOpen());
		}

		if (guard1 instanceof EqualityGuard && guard2 instanceof IntervalGuard) {
			intGuard = (IntervalGuard) guard2;
			equGuard = (EqualityGuard) guard1;

			if (!intGuard.isBiggerGuard() && equGuard.getExpression().equals(intGuard.getRightExpr()))
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(),
						intGuard.getRightExpr(), Boolean.FALSE);
			if (!intGuard.isSmallerGuard() && equGuard.getExpression().equals(intGuard.getLeftExpr()))
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), Boolean.FALSE,
						intGuard.getRightExpr(), intGuard.getRightOpen());
		}

		if (guard1 instanceof IntervalGuard && guard2 instanceof IntervalGuard) {
			intGuard = (IntervalGuard) guard1;
			IntervalGuard intGuard2 = (IntervalGuard) guard2;

			SDTGuard connectedInt = tryConnectEnds(intGuard, intGuard2);
			if (connectedInt == null)
				connectedInt = tryConnectEnds(intGuard2, intGuard);
			if (connectedInt != null)
				return connectedInt;
		}

		SDTGuard equDisjunction = equalityLogic.disjunction(guard1, guard2);
		return equDisjunction;
	}

	private SDTGuard tryConnectEnds(IntervalGuard leftGuard, IntervalGuard rightGuard) {
		// (a, b) [b c) -> (a, c) (,b) (b,) -> != b (,b) [b,) -> true
		if (!leftGuard.isBiggerGuard() && !rightGuard.isSmallerGuard()
				&& (leftGuard.getRightExpr().equals(rightGuard.getLeftExpr())))
			if (leftGuard.isSmallerGuard() && rightGuard.isBiggerGuard())
				if (!leftGuard.getRightOpen() || !rightGuard.getLeftOpen())
					return new SDTTrueGuard(leftGuard.getParameter());
				else
					return new DisequalityGuard(leftGuard.getParameter(), leftGuard.getRightExpr());
			else if (!leftGuard.getRightOpen() || !rightGuard.getLeftOpen())
				return new IntervalGuard(leftGuard.getParameter(), leftGuard.getLeftExpr(), leftGuard.getLeftOpen(),
						rightGuard.getRightExpr(), rightGuard.getRightOpen());
		return null;
	}

}
