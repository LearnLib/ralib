package de.learnlib.ralib;

import java.util.Map;
import java.util.Random;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 * An equivalence oracle builder allowing several useful options to be configured. 
 */
public class IOEquivalenceOracleBuilder {
	
	private IOEquivalenceOracleType type = IOEquivalenceOracleType.RWALK;
	private long maxRuns = 1000; // maximum number of runs
	private double resetProbability = 0.1; // reset probability
	private double freshProbability = 0.5; // prob. of choosing a fresh data value
	private int maxDepth = 100; // max depth
	
	/*
	 * TODO Add support for other EquivalenceOracles
	 */
	public IOEquivalenceOracleBuilder setType(IOEquivalenceOracleType type) {
		this.type = type;
		return this;
	}

	public IOEquivalenceOracleBuilder setMaxRuns(long maxRuns) {
		this.maxRuns = maxRuns;
		return this;
	}

	public IOEquivalenceOracleBuilder setResetProbability(double resetProbability) {
		this.resetProbability = resetProbability;
		return this;
	}

	public IOEquivalenceOracleBuilder setFreshProbability(double freshProbability) {
		this.freshProbability = freshProbability;
		return this;
	}

	public IOEquivalenceOracleBuilder setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
		return this;
	}

	IOEquivalenceOracle build(Random random, DataWordSUL sul, Map<DataType, Theory> teachers, Constants consts, ParameterizedSymbol [] inputSymbols) {
		IORandomWalk randWalk = new IORandomWalk(random, sul, false, // do not
		// draw
		// symbols
		// uniformly
		resetProbability, // reset probability
		freshProbability, // prob. of choosing a fresh data value
		maxRuns, // number of runs
		maxDepth, // max depth
		consts,
		false, // reset runs
		teachers, inputSymbols);
		return randWalk;
	}
	
}
