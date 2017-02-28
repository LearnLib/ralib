package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public interface AccessSequenceProvider {
	public Word<PSymbolInstance> getAccessSequence(RegisterAutomaton reg, RALocation raLoc);
	
	class HypAccessSequenceProvider implements AccessSequenceProvider {

		public Word<PSymbolInstance> getAccessSequence(RegisterAutomaton reg, RALocation raLoc) {
			if (! (reg instanceof Hypothesis)) 
				throw new DecoratedRuntimeException("Only works on reg automatas of Hypothesis type")
				.addDecoration("actual hyp type", reg.getClass());
			Hypothesis a = ((Hypothesis) reg);
			Word<PSymbolInstance> accessSequence = a.getAccessSequence(raLoc);
			return accessSequence;
		}
	}
}
