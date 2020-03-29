package de.learnlib.ralib.equivalence;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.learnlib.api.EquivalenceOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class TracesEquivalenceOracle implements EquivalenceOracle<RegisterAutomaton, PSymbolInstance, Boolean> {

	private List<Word<PSymbolInstance>> testTraces;
	private IOHypVerifier hypVerifier;
	private IOOracle testOracle;

	public TracesEquivalenceOracle(IOOracle testOracle, Map<DataType, Theory> teachers, Constants constants,
			List<Word<PSymbolInstance>> tests) {
		this.hypVerifier = new IOHypVerifier(teachers, constants);
		this.testTraces = tests;
		this.testOracle = testOracle;
	}
	

	public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
			Collection<? extends PSymbolInstance> inputs) {
		System.out.println("Executing conformance tests:");
		for (Word<PSymbolInstance> testWord : testTraces) {
			Word<PSymbolInstance> sulTrace = testOracle.trace(testWord);

			if (hypVerifier.isCEForHyp(sulTrace, hypothesis))
				return new DefaultQuery<>(sulTrace, true);
		}
		
		return null;
	}

}
