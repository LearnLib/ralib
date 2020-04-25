package de.learnlib.ralib.oracles;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public interface TraceCanonizer {
	/**
	 * Returns a unique canonical representation for a given trace.
	 * 
	 * @param trace
	 * @return
	 */
	public Word<PSymbolInstance> canonize(Word<PSymbolInstance> trace);
}
