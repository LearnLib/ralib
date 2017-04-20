package de.learnlib.ralib.equivalence;

import java.util.Map;
import java.util.Random;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class IORWalkFromState extends BoundedIOEquivalenceOracle implements IOEquivalenceOracle{

	public IORWalkFromState(Random rand, IOOracle target, boolean uniform,
            double stopProbability, double regProb, double hisProb, double relatedProb, 
            long maxRuns, int maxDepth, Constants constants, boolean resetRuns, Map<DataType, Theory> teachers, 
             TraceCanonizer traceCanonizer, AccessSequenceProvider accessSequenceProvider, ParameterizedSymbol... inputs) {
		super(target, traceCanonizer, maxRuns, resetRuns);
		TraceGenerator traceGen = new IORWalkFromStateTraceGenerator(rand, uniform, stopProbability, regProb, hisProb, relatedProb, maxDepth, constants, teachers, accessSequenceProvider, inputs);
		super.setTraceGenerator(traceGen);
	}
}
