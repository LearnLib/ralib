/*
 * Copyright (C) 2015 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.tools;

import de.learnlib.logging.Category;
import de.learnlib.logging.filter.CategoryFilter;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoader;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import de.learnlib.statistics.SimpleProfiler;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author falk
 */
public class IOSimulator implements RaLibTool {

    private static final ConfigurationOption.LongOption OPTION_RANDOM_SEED = 
            new ConfigurationOption.LongOption("random.seed", "Seed for RNG", 0L, true);
        
    private static final ConfigurationOption<Level> OPTION_LOGGING_LEVEL =
            new ConfigurationOption<Level>("logging.level", "Log Level", Level.INFO, true) {

            @Override
            public Level parse(Configuration c) throws ConfigurationException {
                if (!c.containsKey(this.getKey())) {
                    if (!this.isOptional()) {
                        throw new ConfigurationException("Missing config value for " + this.getKey());
                    }
                    return this.getDefaultValue();
                }
                Level lvl = Level.parse(c.getProperty(this.getKey()));
                return lvl;
            }
        };
    
    private static final ConfigurationOption<EnumSet<Category>> OPTION_LOGGING_CATEGORY =
            new ConfigurationOption<EnumSet<Category>>("logging.category", "Log category", 
                    EnumSet.allOf(Category.class), true) {

            @Override
            public EnumSet<Category> parse(Configuration c) throws ConfigurationException {
                if (!c.containsKey(this.getKey())) {
                    if (!this.isOptional()) {
                        throw new ConfigurationException("Missing config value for " + this.getKey());
                    }
                    return this.getDefaultValue();
                }
                String[] names = c.getProperty(this.getKey()).split(",");
                List<Category> list = new ArrayList<>();
                for (String n : names) {
                    list.add(parseCategory(n));
                }
                EnumSet<Category> ret = EnumSet.copyOf(list);
                return ret;
            }

            private Category parseCategory(String n) throws ConfigurationException {
                n = n.toUpperCase();
                switch (n) {
                    case "CONFIG": return Category.CONFIG;
                    case "COUNTEREXAMPLE": return Category.COUNTEREXAMPLE;
                    case "DATASTRUCTURE": return Category.DATASTRUCTURE;
                    case "EVENT": return Category.EVENT;
                    case "MODEL": return Category.MODEL;
                    case "PHASE": return Category.PHASE;
                    case "PROFILING": return Category.PROFILING;
                    case "QUERY": return Category.QUERY;
                    case "STATISTIC": return Category.STATISTIC;
                    case "SYSTEM": return Category.SYSTEM;
                }
                throw new ConfigurationException("can not parse " + this.getKey() + ": " + n);
            }
        };
            
    private static final ConfigurationOption.StringOption OPTION_TARGET = 
            new ConfigurationOption.StringOption("target", 
                    "XML file with target sul", null, false);    
            
    private static final ConfigurationOption.BooleanOption OPTION_USE_EQTEST =
            new ConfigurationOption.BooleanOption("use.eqtest", 
                    "Use an eq test for finding counterexamples", Boolean.FALSE, true);
    
    private static final ConfigurationOption.BooleanOption OPTION_USE_RWALK =
            new ConfigurationOption.BooleanOption("use.rwalk", 
                    "Use random walk for finding counterexamples. "
                    + "This will override any ces produced by the eq test", Boolean.FALSE, true);
    
    private static final ConfigurationOption.BooleanOption OPTION_USE_CEOPT =
            new ConfigurationOption.BooleanOption("use.ceopt", 
                    "Use counterexample optimizers", Boolean.FALSE, true);
    
    private static final ConfigurationOption.IntegerOption OPTION_MAX_ROUNDS =
            new ConfigurationOption.IntegerOption("max.rounds", 
                    "Maximum number of rounds", -1, true);

    private static final ConfigurationOption.BooleanOption OPTION_RWALK_DRAW =
            new ConfigurationOption.BooleanOption("rwalk.draw.uniform", 
                    "Draw next input uniformly", null, false);
    
    private static final ConfigurationOption.BooleanOption OPTION_RWALK_RESET =
            new ConfigurationOption.BooleanOption("rwalk.reset.count", 
                    "Reset limit counters after each counterexample", null, false);

    private static final ConfigurationOption.DoubleOption OPTION_RWALK_RESET_PROB = 
            new ConfigurationOption.DoubleOption("rwalk.prob.reset", 
                    "Probability of performing reset instead of step", null, false);
    
    private static final ConfigurationOption.DoubleOption OPTION_RWALK_FRESH_PROB = 
            new ConfigurationOption.DoubleOption("rwalk.prob.fresh", 
                    "Probability of using a fresh data value", null, false);

    private static final ConfigurationOption.LongOption OPTION_RWALK_MAX_RUNS = 
            new ConfigurationOption.LongOption("rwalk.max.runs", 
                    "Maximum number of random walks", null, false);
    
    private static final ConfigurationOption.IntegerOption OPTION_RWALK_MAX_DEPTH =
            new ConfigurationOption.IntegerOption("rwalk.max.depth", 
                    "Maximum length of each random walk", null, false);
            
    private static final ConfigurationOption[] OPTIONS = new ConfigurationOption[] {
        OPTION_LOGGING_LEVEL,
        OPTION_LOGGING_CATEGORY,
        OPTION_TARGET,
        OPTION_RANDOM_SEED,
        OPTION_USE_CEOPT,
        OPTION_USE_EQTEST,
        OPTION_USE_RWALK,
        OPTION_MAX_ROUNDS,
        OPTION_RWALK_FRESH_PROB,
        OPTION_RWALK_RESET_PROB,
        OPTION_RWALK_MAX_DEPTH,
        OPTION_RWALK_MAX_RUNS,
        OPTION_RWALK_RESET
        };
    
    private RegisterAutomaton model;

    private DataWordSUL sulLearn;
    
    private DataWordSUL sulTest;
        
    private IORandomWalk randomWalk = null;
    
    private IOEquivalenceTest eqTest;
    
    private RaStar rastar;

    private IOCounterexampleLoopRemover ceOptLoops;
    
    private IOCounterExamplePrefixReplacer ceOptAsrep;                      
    
    private IOCounterExamplePrefixFinder ceOptPref;
        
    private boolean useCeOptimizers;
    
    private boolean useEqTest;
    
    private boolean findCounterexamples;
    
    private int maxRounds = -1; 
    
    private long resets = 0;
    private long inputs = 0;
    
    @Override
    public String description() {
        return "uses an IORA model as SUL";
    }

    @Override
    public void setup(Configuration config) throws ConfigurationException {
        
        config.list(System.out);
        
        // logging
        Logger root = Logger.getLogger("");
        Level lvl = OPTION_LOGGING_LEVEL.parse(config);
        root.setLevel(lvl);
        EnumSet<Category> cat = OPTION_LOGGING_CATEGORY.parse(config);
        for (Handler h : root.getHandlers()) {
            h.setLevel(lvl);
            h.setFilter(new CategoryFilter(cat));
        }
        
        // target
        String filename = OPTION_TARGET.parse(config);
        FileInputStream fsi;        
        try {
            fsi = new FileInputStream(filename);
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
        RegisterAutomatonLoader loader = new RegisterAutomatonLoader(fsi);
        this.model = loader.getRegisterAutomaton();
        
        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();

        // random
        Long seed = (new Random()).nextLong();
        if (config.containsKey(OPTION_RANDOM_SEED.getKey())) {
            seed = OPTION_RANDOM_SEED.parse(config);
        }
        System.out.println("RANDOM SEED=" + seed);
        Random random = new Random(seed);
        config.setProperty("__seed", "" + seed);
        
        // create teachers
        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
        for (final DataType t : loader.getDataTypes()) {
            teachers.put(t, new EqualityTheory<Integer>() {
                @Override
                public DataValue getFreshValue(List<DataValue<Integer>> vals) {
                    //System.out.println("GENERATING FRESH: " + vals.size());
                    int dv = -1;
                    for (DataValue<Integer> d : vals) {
                        dv = Math.max(dv, d.getId());
                    }
                        
                    return new DataValue(t, dv + 1);
                }
            });
        }

        // oracles
        this.sulLearn = new SimulatorSUL(model, teachers, consts, inputs);
        this.sulTest  = new SimulatorSUL(model, teachers, consts, inputs);
        
        final ParameterizedSymbol ERROR
                = new OutputSymbol("_io_err", new DataType[]{});
        
       IOOracle back = new SULOracle(sulLearn, ERROR);
       IOCache ioCache = new IOCache(back);
       IOFilter ioOracle = new IOFilter(ioCache, inputs);
                
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioOracle, teachers, consts);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts);

        TreeOracleFactory hypFactory = new TreeOracleFactory() {
            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                return new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts);
            }
        };

        this.rastar = new RaStar(mto, hypFactory, mlo, consts, true, actions);
        this.eqTest = new IOEquivalenceTest(model, teachers, consts, true, actions);

        this.useCeOptimizers = OPTION_USE_CEOPT.parse(config);
        this.useEqTest = OPTION_USE_EQTEST.parse(config);
        this.findCounterexamples = OPTION_USE_RWALK.parse(config);
      
        if (findCounterexamples) {

            boolean drawUniformly = OPTION_RWALK_DRAW.parse(config);
            double resetProbabilty = OPTION_RWALK_RESET_PROB.parse(config);
            double freshProbability = OPTION_RWALK_FRESH_PROB.parse(config);        
            long maxTestRuns = OPTION_RWALK_MAX_RUNS.parse(config);
            int maxDepth = OPTION_RWALK_MAX_DEPTH.parse(config);        
            boolean resetRuns = OPTION_RWALK_RESET.parse(config);

            this.randomWalk = new IORandomWalk(random,
                    sulTest,
                    drawUniformly, // do not draw symbols uniformly 
                    resetProbabilty, // reset probability 
                    freshProbability, // prob. of choosing a fresh data value
                    maxTestRuns, // 1000 runs 
                    maxDepth, // max depth
                    consts,
                    resetRuns, // reset runs 
                    teachers,
                    inputs);
            
        }
          
        this.ceOptLoops = new IOCounterexampleLoopRemover(back);
        this.ceOptAsrep = new IOCounterExamplePrefixReplacer(back);                        
        this.ceOptPref = new IOCounterExamplePrefixFinder(back);
        
        this.maxRounds = OPTION_MAX_ROUNDS.parse(config);
    }
    
    @Override
    public void run() {

        System.out.println("=============================== START ===============================");
        
        final String __RUN__ = "overall execution time";
        final String __LEARN__ = "learning";
        final String __SEARCH__ = "ce searching";
        final String __EQ__ = "eq tests";
                
        System.out.println("SYS:------------------------------------------------");
        System.out.println(model);
        System.out.println("----------------------------------------------------");
        
        SimpleProfiler.start(__RUN__);
        SimpleProfiler.start(__LEARN__);
        
        boolean eqTestfoundCE = false;
        ArrayList<Integer> ceLengths = new ArrayList<>();
        ArrayList<Integer> ceLengthsShortened = new ArrayList<>();
        Hypothesis hyp = null;
        
        int rounds = 0;
        while (true && (maxRounds < 0 || rounds < maxRounds)) {
                        
            rounds++;
            rastar.learn();            
            hyp = rastar.getHypothesis();
            System.out.println("HYP:------------------------------------------------");
            System.out.println(hyp);
            System.out.println("----------------------------------------------------");

            SimpleProfiler.stop(__LEARN__);
            SimpleProfiler.start(__EQ__);
            DefaultQuery<PSymbolInstance, Boolean> ce  = null; 
            
            if (useEqTest) {
                ce = this.eqTest.findCounterExample(hyp, null);

                if (ce != null) {
                    eqTestfoundCE = true;
                    System.out.println("EQ-TEST found counterexample: " + ce);
                } else {
                    eqTestfoundCE = false;
                    System.out.println("EQ-TEST did not find counterexample!");                
                }
            }
            
            SimpleProfiler.stop(__EQ__);
            SimpleProfiler.start(__SEARCH__);
            if (findCounterexamples) {
                ce = this.randomWalk.findCounterExample(hyp, null);
            }

            SimpleProfiler.stop(__SEARCH__);
            System.out.println("CE: " + ce);
            if (ce == null) {
                break;
            }

            resets = sulTest.getResets();
            inputs = sulTest.getInputs();
            
            SimpleProfiler.start(__LEARN__);
            ceLengths.add(ce.getInput().length());
            
            if (useCeOptimizers) {
                ce = ceOptLoops.optimizeCE(ce.getInput(), hyp);
                System.out.println("Shorter CE: " + ce);
                ce = ceOptAsrep.optimizeCE(ce.getInput(), hyp);
                System.out.println("New Prefix CE: " + ce);
                ce = ceOptPref.optimizeCE(ce.getInput(), hyp);
                System.out.println("Prefix of CE is CE: " + ce);
            }
   
            ceLengthsShortened.add(ce.getInput().length());
            
            assert model.accepts(ce.getInput());
            assert !hyp.accepts(ce.getInput());
            
            rastar.addCounterexample(ce);
        }
        
        System.out.println("=============================== STOP ===============================");
        System.out.println(SimpleProfiler.getResults());
        
        if (useEqTest) {
            System.out.println("Last EQ Test found a counterexample: " + eqTestfoundCE);
        }
        
        System.out.println("ce lengths (oirginal): " + 
                Arrays.toString(ceLengths.toArray()));
        
        if (useCeOptimizers) {
            System.out.println("ce lengths (shortend): " + 
                    Arrays.toString(ceLengthsShortened.toArray()));
        }
                
        // model
        if (hyp != null) {            
            System.out.println("Locations: " + hyp.getStates().size());
        }
        
        // tests during learning
        // resets + inputs
        System.out.println("Resets Learning: " + sulLearn.getResets());
        System.out.println("Inputs Learning: " + sulLearn.getInputs());
        
        // tests during search
        // resets + inputs
        System.out.println("Resets Testing: " + resets);
        System.out.println("Inputs Testing: " + inputs);
        
        // + sums
        System.out.println("Resets: " + (resets + sulLearn.getResets()));
        System.out.println("Inputs: " + (inputs + sulLearn.getInputs()));
        
    }

    
    
    @Override
    public String help() {
        StringBuilder sb = new StringBuilder();
        for (ConfigurationOption o : OPTIONS) {
            sb.append(o.toString()).append("\n");
        }
        return sb.toString();
    }
    
    
}
