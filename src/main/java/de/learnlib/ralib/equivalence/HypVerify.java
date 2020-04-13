package de.learnlib.ralib.equivalence;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;

public final class HypVerify {
	
	/**
	 * Returns true if the answered query is a counterexample.
	 */
	public static final boolean isCEForHyp(DefaultQuery<PSymbolInstance, Boolean> sulQuery, RegisterAutomaton hyp) {
		return (hyp.accepts(sulQuery.getInput()) && !sulQuery.getOutput()) 
				|| (!hyp.accepts(sulQuery.getInput()) && sulQuery.getOutput());
	}
}
