package de.learnlib.ralib.equivalence;

import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class ConcreteCEVerifier implements CEVerifier {
	private SimulatorSUL hypSim;
	private SULOracle sul;

	public ConcreteCEVerifier(SULOracle sul, Hypothesis hyp, Map<DataType, Theory> teachers, Constants constants) {
		this.hypSim = new SimulatorSUL(hyp, teachers, constants);
		this.sul = sul;
	}
	
	/**
	 * Verifies a test at a concrete level, by simulating the hyp and matching the outputs against those of the system.
	 * This is more thorough than simply checking whether a hyp accepts a word, since output guards in the hyp may under-approximate
	 * potential outcomes.
	 */
	public boolean isCE(Word<PSymbolInstance> test) {
		boolean ret = true;
		hypSim.pre();
		Word<PSymbolInstance> sulTrace = sul.trace(test);
		for(int i=0; i< sulTrace.length(); i = i+2) {
			PSymbolInstance input = sulTrace.getSymbol(i);
			PSymbolInstance output = hypSim.step(input);
			if (!output.equals(sulTrace.getSymbol(i+1))) {
				ret = false;
				break;
			}
		}
		
		return ret;
	}
}
