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

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonExporter;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
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
import de.learnlib.ralib.theory.equality.EqualityTheoryMS;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import de.learnlib.statistics.SimpleProfiler;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author falk
 */
public class IOSimulator extends AbstractToolWithRandomWalk {
            
    private static final ConfigurationOption.StringOption OPTION_TARGET = 
            new ConfigurationOption.StringOption("target", 
                    "XML file with target sul", null, false);    
            
    private static final ConfigurationOption.BooleanOption OPTION_USE_EQTEST =
            new ConfigurationOption.BooleanOption("use.eqtest", 
                    "Use an eq test for finding counterexamples", Boolean.FALSE, true);
                
    private static final ConfigurationOption[] OPTIONS = new ConfigurationOption[] {
        OPTION_LOGGING_LEVEL,
        OPTION_LOGGING_CATEGORY,
        OPTION_TARGET,
        OPTION_RANDOM_SEED,
        OPTION_TEACHERS,
        OPTION_USE_CEOPT,
        OPTION_USE_SUFFIXOPT,
        OPTION_USE_FRESH_VALUES,
        OPTION_EXPORT_MODEL,
        OPTION_USE_EQTEST,
        OPTION_USE_RWALK,
        OPTION_MAX_ROUNDS,
        OPTION_TIMEOUT,
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
    
    private boolean useEqTest;
 
    private long resets = 0;
    private long inputs = 0;
    
    private Constants consts;
    
    private final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
    
    @Override
    public String description() {
        return "uses an IORA model as SUL";
    }

    @Override
    public void setup(Configuration config) throws ConfigurationException {
        super.setup(config);
        
        config.list(System.out);
        
        // target
        String filename = OPTION_TARGET.parse(config);
        FileInputStream fsi;        
        try {
            fsi = new FileInputStream(filename);
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
        RegisterAutomatonImporter loader = new RegisterAutomatonImporter(fsi);
        this.model = loader.getRegisterAutomaton();
        
        ParameterizedSymbol[] inputSymbols = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        consts = loader.getConstants();
        
        // create teachers
        for (final DataType t : loader.getDataTypes()) {            
            TypedTheory theory = teacherClasses.get(t.getName());            
            theory.setType(t);
            if (this.useSuffixOpt) {
                theory.setUseSuffixOpt(this.useSuffixOpt);
            }            
            teachers.put(t, theory);
        }

        // oracles
        this.sulLearn = new SimulatorSUL(model, teachers, consts);
        if (this.timeoutMillis > 0L) {
           this.sulLearn = new TimeOutSUL(this.sulLearn, this.timeoutMillis);
        }
        this.sulTest  = new SimulatorSUL(model, teachers, consts);
        if (this.timeoutMillis > 0L) {
           this.sulTest = new TimeOutSUL(this.sulTest, this.timeoutMillis);
        }
        
        final ParameterizedSymbol ERROR
                = new OutputSymbol("_io_err", new DataType[]{});
        
       IOOracle back = new SULOracle(sulLearn, ERROR);
       IOCache ioCache = new IOCache(back);
       IOFilter ioOracle = new IOFilter(ioCache, inputSymbols);
                
       if (useFresh) {
           for (Theory t : teachers.values()) {
               ((TypedTheory) t).setCheckForFreshOutputs(true, ioCache);
           }
       }
       
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

        this.useEqTest = OPTION_USE_EQTEST.parse(config);
      
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
                    inputSymbols);
            
        }
          
        this.ceOptLoops = new IOCounterexampleLoopRemover(back);
        this.ceOptAsrep = new IOCounterExamplePrefixReplacer(back);                        
        this.ceOptPref = new IOCounterExamplePrefixFinder(back);
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
        
        for (Entry<DataType, Theory> e : teachers.entrySet()) {
            System.out.println("Theory: " + e.getKey() + " -> " + e.getValue().getClass().getName());
        }
        
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
            System.out.println("Transitions: " + hyp.getTransitions().size());
        
            // input locations + transitions            
            System.out.println("Input Locations: " + hyp.getInputStates().size());
            System.out.println("Input Transitions: " + hyp.getInputTransitions().size());
            
            if (this.exportModel) {
                System.out.println("exporting model to model.xml");
                try {
                    FileOutputStream fso = new FileOutputStream("model.xml");
                    RegisterAutomatonExporter.wtite(hyp, consts, fso);
                    
                } catch (FileNotFoundException ex) {
                    System.out.println("... export failed");
                }
            }
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
