package de.learnlib.ralib.learning;

import de.learnlib.ralib.data.PIV;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.word.Word;

public interface PrefixContainer {
	Word<PSymbolInstance> getPrefix();
	PIV getParsInVars();
}
