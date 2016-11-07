package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public interface CEVerifier {
	public boolean isCE(Word<PSymbolInstance> test);
}
