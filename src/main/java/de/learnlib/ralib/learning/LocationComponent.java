package de.learnlib.ralib.learning;

import java.util.Collection;

import net.automatalib.data.VarMapping;
import de.learnlib.ralib.oracles.Branching;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.symbol.ParameterizedSymbol;
import net.automatalib.word.Word;

public interface LocationComponent {
	boolean isAccepting();
	Word<PSymbolInstance> getAccessSequence();
	VarMapping getRemapping(PrefixContainer r);
	Branching getBranching(ParameterizedSymbol action);
	PrefixContainer getPrimePrefix();
	Collection<PrefixContainer> getOtherPrefixes();
}
