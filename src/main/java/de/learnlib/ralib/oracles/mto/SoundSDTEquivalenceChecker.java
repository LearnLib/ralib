package de.learnlib.ralib.oracles.mto;

import java.util.List;

import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTGuard;

public class SoundSDTEquivalenceChecker implements de.learnlib.ralib.theory.SDTEquivalenceChecker{
	private ConstraintSolver cSolver;

	public SoundSDTEquivalenceChecker(ConstraintSolver cSolver) {
		this.cSolver = cSolver;
	}
	
	public boolean checkSDTEquivalence(SDTGuard guard1,SDT sdt1,  SDTGuard guard2, SDT sdt2, Constants consts) {
		List<Conjunction> truePathsSdt1 = sdt1.getPathsAsExpressions(consts, true);
		List<Conjunction> truePathsSdt2 = sdt2.getPathsAsExpressions(consts, true);
		List<Conjunction> falsePathsSdt1 = sdt1.getPathsAsExpressions(consts, false);
		List<Conjunction> falsePathsSdt2 = sdt2.getPathsAsExpressions(consts, false);
		if (sdt1.getNumberOfLeaves() == 6) {
			System.out.println("Interesting");
			
		}
		return areEquivalent(truePathsSdt1, guard1,  truePathsSdt2, guard2) && 
				
				areEquivalent(falsePathsSdt1, guard1, falsePathsSdt2, guard2);
	}
	
	private boolean areEquivalent(List<Conjunction> path1, SDTGuard guard1, List<Conjunction> path2, SDTGuard guard2) {
		Disjunction disj1 = new Disjunction (path1.toArray(new Conjunction[]{}));
		Disjunction disj2 = new Disjunction (path2.toArray(new Conjunction[]{}));
		boolean SAT = this.cSolver.isSatisfiable(new Conjunction(
				new Conjunction(guard2.toTG().getCondition(), disj1), 
				new Negation(new Conjunction(guard1.toTG().getCondition(), disj2) ) ) );
		return !SAT;
	}
}
