package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public interface HypVerifier {
	
	/**
	 * Returns either null (if it isn't a CE) or a PositiveResult (encapsulating CE info)
	 */
	public PositiveResult isCEForHyp(Word<PSymbolInstance> test, RegisterAutomaton hyp);
	
	class PositiveResult {
		public final Word<PSymbolInstance> testPrefix;
		public PositiveResult(Word<PSymbolInstance> testPrefix) {
			super();
			this.testPrefix = testPrefix;
		}
	}
}
