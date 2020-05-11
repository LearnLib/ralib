package de.learnlib.ralib.equivalence;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;

public class AcceptorHypVerifier implements HypVerifier{
	AcceptorHypVerifier() {
	}

	@Override
	public boolean isCEForHyp(DefaultQuery<PSymbolInstance, Boolean> sulQuery, RegisterAutomaton hyp) {
		return (hyp.accepts(sulQuery.getInput()) && !sulQuery.getOutput()) 
				|| (!hyp.accepts(sulQuery.getInput()) && sulQuery.getOutput());
	}

}
