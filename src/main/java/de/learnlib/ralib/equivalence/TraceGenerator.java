package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

// TODO : A two phase process might be done: first generate transitions, then instantiate values for those transitions
public interface TraceGenerator {
	/**
     * Generates a run on the hypothesis. Used in testing.
     */
	public Word<PSymbolInstance> generateTrace(RegisterAutomaton hyp);
}
