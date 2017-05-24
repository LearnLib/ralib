package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * A test purpose returns true if the generated test meets the purpose, false otherwise.
 */
public interface TestPurpose {
	public boolean isSatisfied(Word<PSymbolInstance> test);
}
