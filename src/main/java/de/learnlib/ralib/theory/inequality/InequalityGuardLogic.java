package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuardLogic;

public class InequalityGuardLogic implements SDTGuardLogic{
	
	private EqualityGuardLogic equalityLogic;

	public InequalityGuardLogic() {
		this.equalityLogic = new EqualityGuardLogic();
	}

	public SDTGuard conjunction(SDTGuard guard1, SDTGuard guard2) {
		
		if (guard1 instanceof IntervalGuard && guard2 instanceof IntervalGuard) {
			IntervalGuard intv1 = (IntervalGuard) guard1;
			IntervalGuard intv2 = (IntervalGuard) guard2;
			if (intv1.isBiggerGuard() && !intv2.isBiggerGuard()) {
				if (intv1.getLeftExpr().equals(intv2.getRightExpr())) {
					assert !intv1.getLeftOpen() && !intv2.getRightOpen();
					return new EqualityGuard(intv1.getParameter(), intv1.getLeftExpr());
				} else
					return new IntervalGuard(intv1.getParameter(), intv1.getLeftExpr(), intv1.getLeftOpen(), intv2.getRightExpr(), intv2.getRightOpen());
			} 
			if (intv1.isSmallerGuard() && !intv2.isSmallerGuard()) {
				if (intv1.getRightExpr().equals(intv2.getLeftExpr())) {
					assert !intv1.getRightOpen() && !intv2.getLeftOpen();
					return new EqualityGuard(intv1.getParameter(), intv1.getRightExpr());
				} else
					return new IntervalGuard(intv1.getParameter(), intv2.getLeftExpr(), intv2.getLeftOpen(), intv1.getRightExpr(), intv1.getRightOpen());
			}
			if (intv1.isSmallerGuard() && intv2.isBiggerGuard()) {
				return new IntervalGuard(intv1.getParameter(), intv2.getLeftExpr(), intv2.getLeftOpen(), intv1.getRightExpr(), intv1.getRightOpen());
			}
			if (intv1.isBiggerGuard() && intv2.isSmallerGuard()) {
				return new IntervalGuard(intv1.getParameter(), intv1.getLeftExpr(), intv1.getLeftOpen(), intv2.getRightExpr(), intv2.getRightOpen());
			}
		}
		
		if (guard1 instanceof IntervalGuard && guard2 instanceof DisequalityGuard ||
				guard2 instanceof IntervalGuard && guard1 instanceof DisequalityGuard) {
			IntervalGuard intv = guard1 instanceof IntervalGuard ? (IntervalGuard) guard1 : (IntervalGuard) guard2;
			DisequalityGuard deq = guard1 instanceof DisequalityGuard ? (DisequalityGuard) guard1 : (DisequalityGuard) guard2;
			
			if (!intv.isSmallerGuard() && intv.getLeftExpr().equals(deq.getExpression()) && !intv.getLeftOpen()) 
				return new IntervalGuard(intv.getParameter(), intv.getLeftExpr(), true, intv.getRightExpr(), intv.getRightOpen());
			if (!intv.isBiggerGuard() && intv.getRightExpr().equals(deq.getExpression()) && !intv.getRightOpen()) 
				return new IntervalGuard(intv.getParameter(), intv.getLeftExpr(), intv.getLeftOpen(), intv.getRightExpr(), true);
		}
		
		
		SDTGuard equDisjunction = this.equalityLogic.conjunction(guard1, guard2);
		return equDisjunction;
	}

	public SDTGuard disjunction(SDTGuard guard1, SDTGuard guard2) {
		IntervalGuard intGuard;
		
		if (guard1 instanceof IntervalGuard && guard2 instanceof EqualityGuard) {
			intGuard = (IntervalGuard) guard1;
			assert intGuard.getRightOpen();
			return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), intGuard.getRightExpr(), Boolean.FALSE); 
		} 
		
		if (guard1 instanceof EqualityGuard && guard2 instanceof IntervalGuard) {
			intGuard = (IntervalGuard) guard2;
			assert intGuard.getLeftOpen();
			return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), Boolean.FALSE, intGuard.getRightExpr(), intGuard.getRightOpen());
		}
		
		if (guard1 instanceof IntervalGuard && guard2 instanceof IntervalGuard) {
			intGuard = (IntervalGuard) guard1;
			IntervalGuard intGuard2 = (IntervalGuard) guard2;
			assert Boolean.logicalXor(intGuard.getRightOpen(), intGuard2.getLeftOpen());
			if (intGuard2.isBiggerGuard() && intGuard.isSmallerGuard())
				return new SDTTrueGuard(guard1.getParameter());
			else
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), intGuard2.getRightExpr(), intGuard2.getRightOpen());
		} 
		
		SDTGuard equDisjunction = this.equalityLogic.disjunction(guard1, guard2);
		return equDisjunction;
	}
	

}
