package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public interface HypVerifier {
	
	/**
	 * Returns true if the test word is a counterexample for the hypothesis.
	 */
	public boolean isCEForHyp(Word<PSymbolInstance> test, RegisterAutomaton hyp);
}
