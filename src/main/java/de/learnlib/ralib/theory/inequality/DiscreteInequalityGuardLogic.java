package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.equality.EqualityGuard;

/**
 * Logic class for discrete domains. Note, this logic is only valid in the context of merging,
 * where the any disjoined guards are adjacent. 
 */
public class DiscreteInequalityGuardLogic implements SDTGuardLogic {
	

	private InequalityGuardLogic ineqGuardLogic;


	public DiscreteInequalityGuardLogic(InequalityGuardLogic ineqGuardLogic) {
		this.ineqGuardLogic = ineqGuardLogic;
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
		
		SDTGuard ineqDisjunction = this.ineqGuardLogic.disjunction(guard1, guard2);
		
		return ineqDisjunction;
	}
	
	public SDTGuard conjunction(SDTGuard guard1, SDTGuard guard2) {
		return this.ineqGuardLogic.conjunction(guard1, guard2);
	}
//	
//	
//	private boolean succ(SymbolicDataExpression expr1, SymbolicDataExpression expr2) {
//		if (expr1 != null && expr2 != null && expr1.getSDV().equals(expr2.getSDV())) {
//			if (expr2 instanceof SumCDataExpression) {
//				DataValue<?> cst = ((SumCDataExpression) expr2).getConstant();
//				DataValue<?> cst2;
//				if (expr1.isSDV())
//					cst2 = DataValue.ZERO(cst.getType())
//				else
//					cst2 = DataV
//				if (cst.getId().equals(DataValue.cast(1, cst.getType())))
//					return true; 
//					
//			}
//		}
//		return false; 
//	}
}
