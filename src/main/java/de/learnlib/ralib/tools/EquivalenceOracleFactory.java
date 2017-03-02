package de.learnlib.ralib.tools;

import java.util.Map;
import java.util.Random;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.AccessSequenceProvider;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.equivalence.IORWalkFromState;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class EquivalenceOracleFactory {
	private static final String rw = "rwalk";
    private static final String rws ="rwalkfromstate";
    
	protected static final ConfigurationOption.StringOption OPTION_EQ_ORACLE = 
    		new ConfigurationOption.StringOption("eqoracle",
    				"Select Equivalence Oracle [ " + rw + "," + rws + "]",rw, false);
    
    

    protected static final ConfigurationOption.BooleanOption OPTION_RWALK_DRAW
            = new ConfigurationOption.BooleanOption(rw +".draw.uniform",
                    "Draw next input uniformly", null, false);

    protected static final ConfigurationOption.BooleanOption OPTION_RWALK_RESET
            = new ConfigurationOption.BooleanOption(rw +".reset.count",
                    "Reset limit counters after each counterexample", null, false);

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
	            "Reset limit counters after each counterexample", null, false);
	
	protected static final ConfigurationOption.DoubleOption OPTION_RWALKFROMSTATE_RESET_PROB
	    = new ConfigurationOption.DoubleOption(rws + ".prob.reset",
	            "Probability of performing reset instead of step", null, false);
	
	protected static final ConfigurationOption.DoubleOption OPTION_RWALKFROMSTATE_HISTORY_PROB
	    = new ConfigurationOption.DoubleOption(rws + ".prob.history",
	            "Probability of generating a reg value", null, false);
	
	protected static final ConfigurationOption.DoubleOption OPTION_RWALKFROMSTATE_REGISTER_PROB
    = new ConfigurationOption.DoubleOption(rws + ".prob.register",
            "Probability of generating a history(excl reg) value", null, false);
	
	protected static final ConfigurationOption.DoubleOption OPTION_RWALKFROMSTATE_RELATED_PROB
    = new ConfigurationOption.DoubleOption(rws + ".prob.related",
            "Probability of generated a value from his/reg that is not equal", null, false);
	
	protected static final ConfigurationOption.LongOption OPTION_RWALKFROMSTATE_MAX_RUNS
	    = new ConfigurationOption.LongOption(rws + ".max.runs",
	            "Maximum number of random walks", null, false);
	
	protected static final ConfigurationOption.IntegerOption OPTION_RWALKFROMSTATE_MAX_DEPTH
	= new ConfigurationOption.IntegerOption(rws + ".max.depth",
	    "Maximum length of each random walk", null, false);


	public static IOEquivalenceOracle buildEquivalenceOracle(Configuration config,  DataWordSUL target, Map<DataType, Theory> teachers, 
			Constants constants, Random random, ParameterizedSymbol...  inputSymbols) throws ConfigurationException {
		String eqOracle = OPTION_EQ_ORACLE.parse(config);
		IOEquivalenceOracle equOracle = null; 
		if (eqOracle.equals(rw)) {
			boolean drawUniformly = OPTION_RWALK_DRAW.parse(config);
			double resetProbabilty = OPTION_RWALK_RESET_PROB.parse(config);
			double freshProbability = OPTION_RWALK_FRESH_PROB.parse(config);
			long maxTestRuns = OPTION_RWALK_MAX_RUNS.parse(config);
			int maxDepth = OPTION_RWALK_MAX_DEPTH.parse(config);
			boolean resetRuns = OPTION_RWALK_RESET.parse(config);
			IORandomWalk rwalk = new IORandomWalk(random, target, drawUniformly, // do
									// not
									// draw
									// symbols
									// uniformly
					resetProbabilty, // reset probability
					freshProbability, // prob. of choosing a fresh data
					// value
					maxTestRuns, // 1000 runs
					maxDepth, // max depth
					constants, resetRuns, // reset runs
					teachers, inputSymbols);
			rwalk.setError(SpecialSymbols.ERROR);
			equOracle = rwalk;
		} else {
			boolean drawUniformly = OPTION_RWALKFROMSTATE_DRAW.parse(config);
			double resetProbabilty = OPTION_RWALKFROMSTATE_RESET_PROB.parse(config);
			double drawRegister = OPTION_RWALKFROMSTATE_REGISTER_PROB.parse(config);
			double drawHistory = OPTION_RWALKFROMSTATE_HISTORY_PROB.parse(config);
			double drawRelated = OPTION_RWALKFROMSTATE_RELATED_PROB.parse(config);
			long maxTestRuns = OPTION_RWALKFROMSTATE_MAX_RUNS.parse(config);
			if (drawRegister + drawHistory + drawRelated > 1.0) 
				throw new ConfigurationException("The sum of the draw probabilities should be less than 1, "
						+ "with the difference being the fresh probability");
			
			int maxDepth = OPTION_RWALKFROMSTATE_MAX_DEPTH.parse(config);
			boolean resetRuns = OPTION_RWALKFROMSTATE_RESET.parse(config);
			IORWalkFromState rwalk  = new IORWalkFromState(random, target, drawUniformly, // do
									// not
									// draw
									// symbols
									// uniformly
					resetProbabilty, // reset probability
					drawRegister,
					drawHistory,
					drawRelated, // prob. of choosing a fresh data
					// value
					maxTestRuns, // 1000 runs
					maxDepth, // max depth
					constants, resetRuns, // reset runs
					teachers, new AccessSequenceProvider.HypAccessSequenceProvider(), inputSymbols);
			rwalk.setError(SpecialSymbols.ERROR);
			equOracle =rwalk;
		}
		return equOracle;
	}
		
	
	
	

}
