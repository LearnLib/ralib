package de.learnlib.ralib.equivalence;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.learnlib.api.EquivalenceOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.TraceParser;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class TracesEquivalenceOracle implements EquivalenceOracle<RegisterAutomaton, PSymbolInstance, Boolean> {

	private List<List<PSymbolInstance>> testTraces;
	private IOHypVerifier hypVerifier;
	private DataWordSUL target;

	public TracesEquivalenceOracle(DataWordSUL target, Map<DataType, Theory> teachers, Constants constants,
			List<List<PSymbolInstance>> tests) {
		this.hypVerifier = new IOHypVerifier(teachers, constants);
		this.testTraces = tests;
		this.target = target;
	}
	
	public TracesEquivalenceOracle(DataWordSUL target, Map<DataType, Theory> teachers, Constants constants,
			List<String> tests, List<ParameterizedSymbol> actionSignatures) {
		this.hypVerifier = new IOHypVerifier(teachers, constants);
		this.testTraces = new TraceParser(tests, actionSignatures).getInputSequencesForTraces();
		this.target = target;
	}
	

	public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
			Collection<? extends PSymbolInstance> inputs) {
		System.out.println("Executing conformance tests:");
		for (List<PSymbolInstance> test : testTraces) {
			target.pre();
			Word<PSymbolInstance> run = Word.epsilon();
			for (PSymbolInstance input : test) {
				run = run.append(input);
				PSymbolInstance out = target.step(input);
				run = run.append(out);
				if (this.hypVerifier.isCEForHyp(run, hypothesis) != null) {
					//return new DefaultQuery<>(run, true);
					return null;
				}
			}
			target.post();
		}
		
		return null;
	}

}
