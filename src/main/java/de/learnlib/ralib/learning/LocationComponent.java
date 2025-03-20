package de.learnlib.ralib.learning;

import java.util.Collection;

import de.learnlib.ralib.data.*;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public interface LocationComponent {
	boolean isAccepting();
	Word<PSymbolInstance> getAccessSequence();

	/**
	 * Remapping under which r becomes identical to the
	 * primary prefix of this component.
	 */
	Bijection<DataValue> getRemapping(PrefixContainer r);

	Branching getBranching(ParameterizedSymbol action);
	PrefixContainer getPrimePrefix();
	Collection<PrefixContainer> getOtherPrefixes();
}
