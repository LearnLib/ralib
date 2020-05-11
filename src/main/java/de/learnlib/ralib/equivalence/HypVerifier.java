package de.learnlib.ralib.equivalence;

import java.util.Map;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;

public interface HypVerifier {
	
	public static HypVerifier getVerifier(boolean ioMode, Map<DataType, Theory> teachers, Constants constants) {
		if (ioMode) {
			return new IOHypVerifier(teachers, constants);
		} else {
			return new AcceptorHypVerifier();
		}
	}
	
	public boolean isCEForHyp(DefaultQuery<PSymbolInstance, Boolean> sulQuery, RegisterAutomaton hyp);
}
