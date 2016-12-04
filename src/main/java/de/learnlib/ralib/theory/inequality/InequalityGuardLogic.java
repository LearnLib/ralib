package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuardLogic;


/**
 * Logic for inequalities. Note, depending on the valuation of variables, obtained guards might not be satisfiable.
 */
public class InequalityGuardLogic implements SDTGuardLogic{
	
	private EqualityGuardLogic equalityLogic;

	public InequalityGuardLogic() {
		this.equalityLogic = new EqualityGuardLogic();
	}

	public SDTGuard conjunction(SDTGuard guard1, SDTGuard guard2) {
		
		if (guard1 instanceof IntervalGuard && guard2 instanceof IntervalGuard) {
			IntervalGuard intv1 = (IntervalGuard) guard1;
			IntervalGuard intv2 = (IntervalGuard) guard2;
			
			// (a, b] (b, ) -> (a, )
			if (!intv1.isSmallerGuard() && !intv2.isBiggerGuard()) {
				if (intv1.getLeftExpr().equals(intv2.getRightExpr()) && (!intv1.getLeftOpen() && !intv2.getRightOpen())) {
					return new EqualityGuard(intv1.getParameter(), intv1.getLeftExpr());
				} 
			}
			
			// (, b) [b c) -> (, c)
			if (!intv1.isBiggerGuard() && !intv2.isSmallerGuard()) {
				if (intv1.getRightExpr().equals(intv2.getLeftExpr()) && (!intv1.getLeftOpen() && !intv2.getRightOpen())) {
					return new EqualityGuard(intv1.getParameter(), intv1.getRightExpr());
				} 
			}
			
			// (, a) (b, ) -> (b, a)
			if (intv1.isSmallerGuard() && intv2.isBiggerGuard()) {
				return new IntervalGuard(intv1.getParameter(), intv2.getLeftExpr(), intv2.getLeftOpen(), intv1.getRightExpr(), intv1.getRightOpen());
			}
			
			// (a, ) ( ,b) -> (a, b) 
			if (intv1.isBiggerGuard() && intv2.isSmallerGuard()) {
				return new IntervalGuard(intv1.getParameter(), intv1.getLeftExpr(), intv1.getLeftOpen(), intv2.getRightExpr(), intv2.getRightOpen());
			}
		}
		
		if (guard1 instanceof IntervalGuard && guard2 instanceof DisequalityGuard ||
				guard2 instanceof IntervalGuard && guard1 instanceof DisequalityGuard) {
			IntervalGuard intv = guard1 instanceof IntervalGuard ? (IntervalGuard) guard1 : (IntervalGuard) guard2;
			DisequalityGuard deq = guard1 instanceof DisequalityGuard ? (DisequalityGuard) guard1 : (DisequalityGuard) guard2;
			
			// [a, b) !=a -> (a, b)
			if (!intv.isSmallerGuard() && intv.getLeftExpr().equals(deq.getExpression()) && !intv.getLeftOpen()) 
				return new IntervalGuard(intv.getParameter(), intv.getLeftExpr(), Boolean.TRUE, intv.getRightExpr(), intv.getRightOpen());
			
			// (a, b] !=b -> (a, b)
			if (!intv.isBiggerGuard() && intv.getRightExpr().equals(deq.getExpression()) && !intv.getRightOpen()) 
				return new IntervalGuard(intv.getParameter(), intv.getLeftExpr(), intv.getLeftOpen(), intv.getRightExpr(), Boolean.TRUE);
		}
		
		
		SDTGuard equDisjunction = this.equalityLogic.conjunction(guard1, guard2);
		return equDisjunction;
	}

	public SDTGuard disjunction(SDTGuard guard1, SDTGuard guard2) {
		IntervalGuard intGuard;
		EqualityGuard equGuard;
		
		// (a, b) ==b -> (a, b] 
		if (guard1 instanceof IntervalGuard && guard2 instanceof EqualityGuard) {
			intGuard = (IntervalGuard) guard1;
			equGuard = (EqualityGuard) guard2;
			
			if ( !intGuard.isBiggerGuard() && equGuard.getExpression().equals(intGuard.getRightExpr()))
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), intGuard.getRightExpr(), Boolean.FALSE);
			if ( !intGuard.isSmallerGuard() && equGuard.getExpression().equals(intGuard.getLeftExpr()))
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), Boolean.FALSE, intGuard.getRightExpr(), intGuard.getRightOpen());
		} 
		
		if (guard1 instanceof EqualityGuard && guard2 instanceof IntervalGuard) {
			intGuard = (IntervalGuard) guard2;
			equGuard = (EqualityGuard) guard1;
			
			if ( !intGuard.isBiggerGuard() && equGuard.getExpression().equals(intGuard.getRightExpr()))
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), intGuard.getRightExpr(), Boolean.FALSE);
			if ( !intGuard.isSmallerGuard() && equGuard.getExpression().equals(intGuard.getLeftExpr()))
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), Boolean.FALSE, intGuard.getRightExpr(), intGuard.getRightOpen());
		}
		
		if (guard1 instanceof IntervalGuard && guard2 instanceof IntervalGuard) {
			intGuard = (IntervalGuard) guard1;
			IntervalGuard intGuard2 = (IntervalGuard) guard2;
			
			if (!intGuard2.isSmallerGuard() && !intGuard.isBiggerGuard() && intGuard2.getLeftExpr().equals(intGuard.getRightExpr()))
				if (intGuard2.isBiggerGuard() && intGuard.isSmallerGuard())
					if (!intGuard2.getLeftOpen() || !intGuard.getRightOpen())
						return new SDTTrueGuard(guard1.getParameter());
					else
						return new DisequalityGuard(guard1.getParameter(), intGuard.getRightExpr());
				else
					return new IntervalGuard(guard1.getParameter(), intGuard2.getLeftExpr(), intGuard2.getLeftOpen(), intGuard.getRightExpr(), intGuard.getRightOpen());
			
			if (!intGuard.isSmallerGuard() && !intGuard2.isBiggerGuard() && intGuard.getLeftExpr().equals(intGuard2.getRightExpr()))
				if (intGuard.isBiggerGuard() && intGuard2.isSmallerGuard())
					if (!intGuard.getLeftOpen() || !intGuard2.getRightOpen())
						return new SDTTrueGuard(guard1.getParameter());
					else
						return new DisequalityGuard(guard1.getParameter(), intGuard.getRightExpr());
				else
					return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), intGuard2.getRightExpr(), intGuard2.getRightOpen());
		} 
		
		SDTGuard equDisjunction = this.equalityLogic.disjunction(guard1, guard2);
		return equDisjunction;
	}
	

}
