package de.learnlib.ralib.equivalence;

import java.util.Map;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class IOHypVerifier implements HypVerifier {
	private Map<DataType, Theory> teachers;
	private Constants constants;

	IOHypVerifier(Map<DataType, Theory> teachers, Constants constants) {
		this.teachers = teachers;
		this.constants = constants;
	}
	
	/**
	 * Verifies a test at a concrete level, by simulating the hyp and matching the outputs against those of the system.
	 * This is more thorough than simply checking whether a hyp accepts a word, since output guards in the hyp may under-approximate
	 * potential outcomes.
	 */
	public boolean isCEForHyp(DefaultQuery<PSymbolInstance, Boolean> sulQuery, RegisterAutomaton hyp) {
		if (!sulQuery.getOutput()) {
			return hyp.accepts(sulQuery.getInput());
		} else {
			SimulatorSUL hypSim = new SimulatorSUL(hyp, teachers, constants);
			int i = 0;
			Word<PSymbolInstance> sulTrace = sulQuery.getInput();
			hypSim.pre();
			try {
				for(i=1; i< sulTrace.length(); i = i+2) {
					PSymbolInstance input = sulTrace.getSymbol(i-1);
					PSymbolInstance output = hypSim.step(input);
					if (!output.equals(sulTrace.getSymbol(i))) {
						hypSim.post();
						return true;
					}
				}
			}catch(Exception exc) {
				DecoratedRuntimeException dexc = new DecoratedRuntimeException(exc.getMessage())
						.addDecoration("test trace", sulTrace).addDecoration("symbol index", i);
				dexc.addSuppressed(exc);
				throw dexc;
			}
			hypSim.post();
			return false;
		}
	}
}