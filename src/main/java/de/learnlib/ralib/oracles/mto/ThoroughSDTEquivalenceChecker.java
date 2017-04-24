package de.learnlib.ralib.oracles.mto;

import java.util.List;

import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTGuard;

public class ThoroughSDTEquivalenceChecker implements de.learnlib.ralib.theory.SDTEquivalenceChecker{
	private MultiTheorySDTLogicOracle logic;
	Mapping<SymbolicDataValue, DataValue<?>> guardContext;
	private List<SDTGuard> suffGuards;

	public ThoroughSDTEquivalenceChecker(Constants constants, ConstraintSolver cSolver, List<SDTGuard> suffixGuards) {
		this(constants, cSolver, suffixGuards, new Mapping<>());
	}
	
	public ThoroughSDTEquivalenceChecker(Constants constants, ConstraintSolver cSolver, List<SDTGuard> suffixGuards, Mapping<SymbolicDataValue, DataValue<?>> guardContext) {
		this.logic = new MultiTheorySDTLogicOracle(constants, cSolver);
		this.guardContext = guardContext;
		this.suffGuards = suffixGuards;
	}
	
	public boolean checkSDTEquivalence(SDTGuard guard1,SDT sdt1,  SDTGuard guard2, SDT sdt2) {
		if (sdt1 instanceof SDTLeaf && sdt2 instanceof SDTLeaf) {
			return sdt1.equals(sdt2);
		}
		boolean equiv = this.logic.areEquivalent(sdt1, new PIV(), guard1.toExpr(), sdt2, new PIV(), guard2.toExpr(), this.guardContext);
		
		return equiv;
		
//		List<Conjunction> truePathsSdt1 = sdt1.getPathsAsExpressions(true);
//		List<Conjunction> truePathsSdt2 = sdt2.getPathsAsExpressions(true);
//		
//		List<Conjunction> falsePathsSdt1 = sdt1.getPathsAsExpressions(false);
//		List<Conjunction> falsePathsSdt2 = sdt2.getPathsAsExpressions(false);
//		
//		boolean truePathEquiv = areEquivalent(truePathsSdt1, guard1,  truePathsSdt2, guard2);
//		boolean falsePathEquiv = areEquivalent(falsePathsSdt1, guard1, falsePathsSdt2, guard2);
//		if (truePathEquiv != falsePathEquiv) {
//			System.out.println(guard1 +"\n" + sdt1 + "\n" + guard2 + "\n" + sdt2);
//		}
//		assert truePathEquiv == falsePathEquiv;
		
		//return truePathEquiv;
	}
	
	private boolean areEquivalent(List<Conjunction> path1, SDTGuard guard1, List<Conjunction> path2, SDTGuard guard2) {
		if (path1.isEmpty() || path2.isEmpty())
			return !Boolean.logicalXor(path1.isEmpty(), path2.isEmpty());
		Disjunction disj1 = new Disjunction (path1.toArray(new Conjunction[]{}));
		Conjunction disj1UnderGuard1 = new Conjunction(disj1, SDT.toPathExpression(this.suffGuards), guard1.toExpr());
		Conjunction disj1UnderGuard2 = new Conjunction(disj1, SDT.toPathExpression(this.suffGuards), guard2.toExpr());
		Disjunction disj2 = new Disjunction (path2.toArray(new Conjunction[]{}));
		Conjunction disj2UnderGuard2 = new Conjunction(disj2, guard2.toExpr(), SDT.toPathExpression(this.suffGuards));
		Conjunction disj2UnderGuard1 = new Conjunction(disj2, guard1.toExpr(), SDT.toPathExpression(this.suffGuards));
		boolean SAT1 = this.logic.canBothBeSatisfied(new Negation(disj1UnderGuard1), new PIV(), disj2UnderGuard1, new PIV(), guardContext);
		boolean SAT2 = this.logic.canBothBeSatisfied(new Negation(disj2UnderGuard2), new PIV(), disj1UnderGuard2, new PIV(), guardContext);
		return !SAT1 && !SAT2;
	}
	
/*	
 * Case where SAT1 != SAT2 (which shows that we need to compute for SAT1 and SAT2, and cannot just return !SAT1).
	(s3!=s2)
	SDT
	[]-+
	  []-TRUE: s4
	        [Leaf-]

	(s3==s2)
	[]-+
	  []-(s4=s2)
	   |    [Leaf+]
	   +-(s4!=s2)
	        [Leaf-] */
}
