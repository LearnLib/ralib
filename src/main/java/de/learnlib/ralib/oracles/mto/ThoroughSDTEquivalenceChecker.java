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
import de.learnlib.ralib.theory.SyntacticEquivalenceChecker;

public class ThoroughSDTEquivalenceChecker implements de.learnlib.ralib.theory.SDTEquivalenceChecker{
	private MultiTheorySDTLogicOracle logic;
	Mapping<SymbolicDataValue, DataValue<?>> guardContext;
	private List<SDTGuard> suffGuards;
	private SyntacticEquivalenceChecker syntacticChecker;

	public ThoroughSDTEquivalenceChecker(Constants constants, ConstraintSolver cSolver, List<SDTGuard> suffixGuards) {
		this(constants, cSolver, suffixGuards, new Mapping<>());
	}
	
	public ThoroughSDTEquivalenceChecker(Constants constants, ConstraintSolver cSolver, List<SDTGuard> suffixGuards, Mapping<SymbolicDataValue, DataValue<?>> guardContext) {
		this.logic = new MultiTheorySDTLogicOracle(constants, cSolver);
		this.guardContext = guardContext;
		this.suffGuards = suffixGuards;
		this.syntacticChecker = new SyntacticEquivalenceChecker();
	}
	
	public boolean checkSDTEquivalence(SDTGuard guard1,SDT sdt1,  SDTGuard guard2, SDT sdt2) {
		if (sdt1 instanceof SDTLeaf && sdt2 instanceof SDTLeaf) {
			return sdt1.equals(sdt2);
		}
		
		boolean syntacticEquiv = this.syntacticChecker.checkSDTEquivalence(guard1, sdt1, guard2, sdt2);
		boolean semanticEquiv = syntacticEquiv;
		if (!syntacticEquiv)
			semanticEquiv = this.logic.areEquivalent(sdt1, new PIV(), guard1.toExpr(), sdt2, new PIV(), guard2.toExpr(), this.guardContext);
		
		return semanticEquiv;
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
