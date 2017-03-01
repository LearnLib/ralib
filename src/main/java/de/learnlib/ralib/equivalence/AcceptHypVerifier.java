package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class AcceptHypVerifier implements HypVerifier{
	
	@Override
	public PositiveResult isCEForHyp(Word<PSymbolInstance> test, RegisterAutomaton hyp) {
		
		if (!hyp.accepts(test)) {
			int i = test.length();
			while (!hyp.accepts(test.prefix(i))) 
				i --;
			return new PositiveResult(test.prefix(i+1));
		}
		return null;
	}

}
