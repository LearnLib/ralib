package de.learnlib.ralib.exceptions;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * Exception throw if the output in the Cache does not correspond with that received from the SUL.
 */
public class NonDeterminismException extends DecoratedRuntimeException{
	private static final long serialVersionUID = 1L;
	
	public NonDeterminismException(Word<PSymbolInstance> after, PSymbolInstance expected, PSymbolInstance got) {
		this.addDecoration("after", after).addDecoration("expected", expected).addDecoration("got", got);
	}
}
