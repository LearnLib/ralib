package de.learnlib.ralib.equivalence;

import java.util.Map;
import java.util.Random;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class IORandomWalkTraceGenerator implements TraceGenerator{
	
	private double stopProbability;
	private Constants constants;
	private int maxDepth;
	private Map<DataType, Theory> teachers;
	private ParameterizedSymbol[] inputs;
	protected Random rand;
	private ParameterizedSymbol error;
	private SimulatorSUL target;
	private InputSelector inpSelector;

	public IORandomWalkTraceGenerator(Random rand, double stopProbability, int maxDepth,
			InputSelector inpSelector, Constants constants,
			Map<DataType, Theory> teachers, ParameterizedSymbol... inputs) {

		this.rand = rand;
		this.inputs = inputs;
		this.stopProbability = stopProbability;
		this.constants = constants;
		this.maxDepth = maxDepth;
		this.teachers = teachers;
		this.inpSelector = inpSelector;
		this.error = null;
	}

	public Word<PSymbolInstance> generateTrace(RegisterAutomaton hyp) {
		Word<PSymbolInstance> run = this.randomTraceFromPrefix(Word.epsilon(), hyp);		
		return run;
	}
	

	public void setError(ParameterizedSymbol error) {
		this.error = error;
	}
	
	/**
	 * Generates a trace from the given location prefix. Does not include the prefix in the generated run. 
	 */
	protected Word<PSymbolInstance> randomTraceFromPrefix( Word<PSymbolInstance> inLocPrefix, RegisterAutomaton hyp) {
		int depth = 0;
		this.target = new SimulatorSUL(hyp, teachers, constants);
		target.pre();
		
		for (int i=0; i<inLocPrefix.length(); i=i+2) {
			PSymbolInstance out = target.step(inLocPrefix.getSymbol(i));
			assert out.equals(inLocPrefix.getSymbol(i+1));
		}
		Word<PSymbolInstance> trace = inLocPrefix;
		PSymbolInstance out;
		do {
			PSymbolInstance next = this.inpSelector.nextInput(trace, hyp);
			assert next!=null;
			depth++;
			out = null;
			trace = trace.append(next);
			out = target.step(next);
			trace = trace.append(out);

		} while (rand.nextDouble() > stopProbability && depth < maxDepth && !out.getBaseSymbol().equals(error));
		target.post();
		return trace.suffix(trace.size()-inLocPrefix.size());
	}
	
	

}
