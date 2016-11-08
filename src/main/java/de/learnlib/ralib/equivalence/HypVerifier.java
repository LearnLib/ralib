package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public interface HypVerifier {
	public boolean isCEForHyp(Word<PSymbolInstance> test, RegisterAutomaton hyp);
}
