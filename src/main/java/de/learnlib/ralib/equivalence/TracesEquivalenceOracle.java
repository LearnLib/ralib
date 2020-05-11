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
	private IOOracle testOracle;
	private HypVerifier hypVerifier;

	public TracesEquivalenceOracle(IOOracle testOracle, Map<DataType, Theory> teachers, Constants constants,
			List<Word<PSymbolInstance>> tests) {
		this.testTraces = tests;
		this.testOracle = testOracle;
		this.hypVerifier = HypVerifier.getVerifier(true, teachers, constants);
	}
	

	public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
			Collection<? extends PSymbolInstance> inputs) {
		for (Word<PSymbolInstance> testWord : testTraces) {
			Word<PSymbolInstance> sulTrace = testOracle.trace(testWord);
			DefaultQuery<PSymbolInstance, Boolean> ce = new DefaultQuery<>(sulTrace, true);

			if (hypVerifier.isCEForHyp(ce, hypothesis))
				return new DefaultQuery<>(sulTrace, true);
		}
		
		return null;
	}

}
