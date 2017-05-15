package de.learnlib.ralib.tools;

import java.util.Map;
import java.util.Random;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.AccessSequenceProvider;
import de.learnlib.ralib.equivalence.BoundedIOEquivalenceOracle;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.equivalence.IORWalkFromState;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.equivalence.InputSelector;
import de.learnlib.ralib.equivalence.RandomSymbolSelector;
import de.learnlib.ralib.equivalence.RandomTransitionSelector;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class EquivalenceOracleFactory {
	private static final String rw = "rwalk";
    private static final String rws ="rwalkfromstate";
    
	protected static final ConfigurationOption.StringOption OPTION_EQ_ORACLE = 
    		new ConfigurationOption.StringOption("eqoracle",
    				"Select Equivalence Oracle [ " + rw + "," + rws + "]",rw, true);
    
    

    protected static final ConfigurationOption.BooleanOption OPTION_RWALK_DRAW
            = new ConfigurationOption.BooleanOption(rw +".draw.uniform",
                    "Draw next input uniformly", null, false);

    protected static final ConfigurationOption.BooleanOption OPTION_RWALK_RESET
            = new ConfigurationOption.BooleanOption(rw +".reset.count",
                    "Reset limit counters after each counterexample", null, true);

    protected static final ConfigurationOption.DoubleOption OPTION_RWALK_RESET_PROB
            = new ConfigurationOption.DoubleOption(rw +".prob.reset",
                    "Probability of performing reset instead of step", null, false);

    protected static final ConfigurationOption.DoubleOption OPTION_RWALK_FRESH_PROB
            = new ConfigurationOption.DoubleOption(rw +".prob.fresh",
                    "Probability of using a fresh data value", null, false);
    
    protected static final ConfigurationOption.LongOption OPTION_RWALK_MAX_RUNS
            = new ConfigurationOption.LongOption(rw +".max.runs",
                    "Maximum number of random walks", null, false);
    
    protected static final ConfigurationOption.IntegerOption OPTION_RWALK_MAX_DEPTH
    = new ConfigurationOption.IntegerOption(rw + ".max.depth",
            "Maximum length of each random walk", null, false);
    
    
    protected static final ConfigurationOption.BooleanOption OPTION_RWALKFROMSTATE_DRAW
    = new ConfigurationOption.BooleanOption(rws + ".draw.uniform",
            "Draw next input uniformly", null, false);

	protected static final ConfigurationOption.BooleanOption OPTION_RWALKFROMSTATE_RESET
	    = new ConfigurationOption.BooleanOption(rws + ".reset.count",
	            "Reset counters after each counterexample", null, true);
	
	protected static final ConfigurationOption.DoubleOption OPTION_RWALKFROMSTATE_RESET_PROB
	    = new ConfigurationOption.DoubleOption(rws + ".prob.reset",
	            "Probability of performing reset instead of step", null, false);
	
	protected static final ConfigurationOption.DoubleOption OPTION_RWALKFROMSTATE_HISTORY_PROB
	    = new ConfigurationOption.DoubleOption(rws+".prob.history",
	            "Probability of generating a reg value", null, false);
	
	protected static final ConfigurationOption.DoubleOption OPTION_RWALKFROMSTATE_REGISTER_PROB
    = new ConfigurationOption.DoubleOption(rws+".prob.register",
            "Probability of generating a history(excl reg) value", null, false);
	
	protected static final ConfigurationOption.DoubleOption OPTION_RWALKFROMSTATE_RELATED_PROB
    = new ConfigurationOption.DoubleOption(rws+".prob.related",
            "Probability of generated a value from his/reg that is not equal", null, false);
	
	protected static final ConfigurationOption.LongOption OPTION_RWALKFROMSTATE_MAX_RUNS
	    = new ConfigurationOption.LongOption(rws + ".max.runs",
	            "Maximum number of random walks", null, false);
	
	protected static final ConfigurationOption.BooleanOption OPTION_RWALKFROMSTATE_TRANSITION
    = new ConfigurationOption.BooleanOption(rws+".transition",
            "Activate transition walk instead of symbol walk", true, true);
		
	protected static final ConfigurationOption.IntegerOption OPTION_RWALKFROMSTATE_MAX_DEPTH
	= new ConfigurationOption.IntegerOption(rws + ".max.depth",
	    "Maximum length of each random walk", null, false);
	
	public static IOEquivalenceOracle buildEquivalenceOracle(Configuration config,  IOOracle concurrentTarget, int sulInstances, Map<DataType, Theory> teachers, 
			Constants constants, Random random, ParameterizedSymbol...  inputSymbols) throws ConfigurationException {
		IOEquivalenceOracle eqOracle = buildEquivalenceOracle(config, concurrentTarget, teachers, constants, random, inputSymbols);
		int batchSize = 5*sulInstances;
		((BoundedIOEquivalenceOracle) eqOracle).setBatchExecution(batchSize);
		return eqOracle;
	}

	public static IOEquivalenceOracle buildEquivalenceOracle(Configuration config,  IOOracle target, Map<DataType, Theory> teachers, 
			Constants constants, Random random, ParameterizedSymbol...  inputSymbols) throws ConfigurationException {
		String eqOracle = OPTION_EQ_ORACLE.parse(config);
		TraceCanonizer traceCanonizer = new SymbolicTraceCanonizer(teachers, constants);
		IOEquivalenceOracle equOracle = null; 
		if (eqOracle.equals(rw)) {
			boolean drawUniformly = OPTION_RWALK_DRAW.parse(config);
			double stopProbabilty = OPTION_RWALK_RESET_PROB.parse(config);
			double freshProbability = OPTION_RWALK_FRESH_PROB.parse(config);
			double drawRegister = (1-freshProbability)/3; 
			double drawHistory = drawRegister;
			double drawRelated = drawRegister;
			RandomSymbolSelector inpSelector = new RandomSymbolSelector(random, teachers, constants, drawUniformly, drawRegister, drawHistory, drawRelated, inputSymbols);
			long maxTestRuns = OPTION_RWALK_MAX_RUNS.parse(config);
			int maxDepth = OPTION_RWALK_MAX_DEPTH.parse(config);
			boolean resetRuns = OPTION_RWALK_RESET.parse(config);

			IORandomWalk rwalk = new IORandomWalk(random, target,  
					stopProbabilty, // reset probability
					maxDepth, // max depth
					inpSelector,
					// value
					maxTestRuns, // 1000 runs
					constants, resetRuns, // reset runs
					teachers, traceCanonizer, inputSymbols);
			equOracle = rwalk;
		} else {
			boolean drawUniformly = OPTION_RWALKFROMSTATE_DRAW.parse(config);
			double resetProbabilty = OPTION_RWALKFROMSTATE_RESET_PROB.parse(config);
			long maxTestRuns = OPTION_RWALKFROMSTATE_MAX_RUNS.parse(config);
			double drawRegister = OPTION_RWALKFROMSTATE_REGISTER_PROB.parse(config);
			double drawHistory = OPTION_RWALKFROMSTATE_HISTORY_PROB.parse(config);
			double drawRelated = OPTION_RWALKFROMSTATE_RELATED_PROB.parse(config);
			Boolean transWalk = OPTION_RWALKFROMSTATE_TRANSITION.parse(config);
			if (drawRegister + drawHistory + drawRelated > 1.0) 
				throw new ConfigurationException("The sum of the draw probabilities should be less than 1, "
						+ "with the difference being the fresh probability");
			InputSelector inpSelector = !transWalk ? 
					new RandomSymbolSelector(random, teachers, constants, drawUniformly, drawRegister, drawHistory, drawRelated, inputSymbols) :
					new RandomTransitionSelector(random, teachers, constants, drawRegister, drawHistory, drawRelated, inputSymbols);
			int maxDepth = OPTION_RWALKFROMSTATE_MAX_DEPTH.parse(config);
			boolean resetRuns = OPTION_RWALKFROMSTATE_RESET.parse(config);
			IORWalkFromState rwalk  = new IORWalkFromState(random, target, 
					resetProbabilty, // reset probability
					maxTestRuns, // 1000 runs
					maxDepth, // max depth
					inpSelector,
					// value
					constants,
					resetRuns, // reset runs
					teachers, 
					traceCanonizer, new AccessSequenceProvider.HypAccessSequenceProvider(), inputSymbols);
			equOracle =rwalk;
		}
		return equOracle;
	}
		
	
	
	

}
