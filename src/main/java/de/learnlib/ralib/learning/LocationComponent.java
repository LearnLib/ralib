package de.learnlib.ralib.learning;

import java.util.Collection;

import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public interface LocationComponent {
	boolean isAccepting();
	Word<PSymbolInstance> getAccessSequence();
	VarMapping getRemapping(PrefixContainer r);
	Branching getBranching(ParameterizedSymbol action);
	PrefixContainer getPrimePrefix();
	Collection<PrefixContainer> getOtherPrefixes();
}
