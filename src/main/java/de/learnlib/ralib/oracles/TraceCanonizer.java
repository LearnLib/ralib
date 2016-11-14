package de.learnlib.ralib.oracles;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public interface TraceCanonizer {
	public Word<PSymbolInstance> canonizeTrace(Word<PSymbolInstance> trace);
}
