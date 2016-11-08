package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class AcceptHypVerifier implements HypVerifier{
	
	@Override
	public boolean isCEForHyp(Word<PSymbolInstance> test, RegisterAutomaton hyp) {
		return !hyp.accepts(test);
	}

}
