/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.util.RAToDot;
import de.learnlib.ralib.automata.xml.RegisterAutomatonExporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterExampleSingleTransitionRemover;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IOHypVerifier;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.equivalence.TracesEquivalenceOracle;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.TreeOracleStatsLoggerWrapper;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOCacheManager;
import de.learnlib.ralib.oracles.io.IOCacheOracle;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.CanonizingSULOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.ClasssAnalyzerDataWordSUL;
import de.learnlib.ralib.tools.classanalyzer.FieldConfig;
import de.learnlib.ralib.tools.classanalyzer.MethodConfig;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import de.learnlib.statistics.SimpleProfiler;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class ClassAnalyzer extends AbstractToolWithRandomWalk {

    private static final ConfigurationOption.StringOption OPTION_TARGET
            = new ConfigurationOption.StringOption("target",
                    "traget class name", null, false);
    
    private static final ConfigurationOption.StringOption OPTION_CONFIG
    = new ConfigurationOption.StringOption("config",
            "sets class fields to given values after instantiation, format: field:value[;field:value]* ", null, true);

    private static final ConfigurationOption.StringOption OPTION_METHODS
            = new ConfigurationOption.StringOption("methods",
                    "traget method signatures. format: m1(class:type,class:type)class:type + m2() + ...", null, false);

    protected static final ConfigurationOption.IntegerOption OPTION_MAX_DEPTH
            = new ConfigurationOption.IntegerOption("max.depth",
                    "Maximum depth to explore", -1, true);
    
    protected static final ConfigurationOption.BooleanOption OPTION_OUTPUT_ERROR
    = new ConfigurationOption.BooleanOption("output.error",
            "Include error output", true, true);

    protected static final ConfigurationOption.BooleanOption OPTION_OUTPUT_NULL
    = new ConfigurationOption.BooleanOption("output.null",
            "Include null output", true, true);
    
    protected static final ConfigurationOption.BooleanOption OPTION_CACHE_TESTS
    = new ConfigurationOption.BooleanOption("cache.tests",
            "Also cache tests", false, true);
    
    private static final ConfigurationOption<?>[] OPTIONS = new ConfigurationOption[]{
        OPTION_LOGGING_LEVEL,
        OPTION_LOGGING_CATEGORY,
        OPTION_TARGET,
        OPTION_METHODS,
        OPTION_TEACHERS,
        OPTION_RANDOM_SEED,
        OPTION_USE_CEOPT,
        OPTION_USE_SUFFIXOPT,
        OPTION_EXPORT_MODEL,
        OPTION_USE_RWALK,
        OPTION_MAX_ROUNDS,
        OPTION_MAX_DEPTH,
        OPTION_TIMEOUT,
        OPTION_RWALK_FRESH_PROB,
        OPTION_RWALK_RESET_PROB,
        OPTION_RWALK_MAX_DEPTH,
        OPTION_RWALK_MAX_RUNS,
        OPTION_RWALK_RESET,
        OPTION_OUTPUT_NULL,
        OPTION_OUTPUT_ERROR
    };

    private DataWordSUL sulLearn;

    private DataWordSUL sulTest;

    private IORandomWalk randomWalk = null;

    private RaStar rastar;

    private IOCounterexampleLoopRemover ceOptLoops;

    private IOCounterExamplePrefixReplacer ceOptAsrep;

    private IOCounterExamplePrefixFinder ceOptPref;

    private IOOracle sulTraceOracle;

    private Map<DataType, Theory> teachers;

    private Class<?> target = null;

    private final Map<String, DataType> types = new LinkedHashMap<>();

    private final Map<ParameterizedSymbol, MethodConfig> methods = new LinkedHashMap<>();

    private long resets = 0;
    private long inputs = 0;

	private IOHypVerifier hypVerifier;

	private DataWordSUL sulCeAnalysis;

	private IOCounterExampleSingleTransitionRemover ceOptSTR;

	private TracesEquivalenceOracle traceTester;

    @Override
    public String description() {
        return "analyzes Java classes";
    }

    @Override
    public void setup(Configuration config) throws ConfigurationException {

        List<ParameterizedSymbol> inList = new ArrayList<>();
        List<ParameterizedSymbol> actList = new ArrayList<>();
        try {
            super.setup(config);

            this.types.put("boolean", SpecialSymbols.BOOLEAN_TYPE);

            String className = OPTION_TARGET.parse(config);
            this.target = Class.forName(className);
            boolean hasVoid = false;
            boolean hasBoolean = false;

            String[] mcStrings = OPTION_METHODS.parse(config).split("\\+");
            for (String mcs : mcStrings) {
                MethodConfig mc = new MethodConfig(mcs, this.target, this.types);
                this.methods.put(mc.getInput(), mc);
                inList.add(mc.getInput());
                actList.add(mc.getInput());
                if (!mc.isVoid() && !mc.getRetType().equals(SpecialSymbols.BOOLEAN_TYPE)) {
                    actList.add(mc.getOutput());
                }
                hasVoid = hasVoid || mc.isVoid();
                hasBoolean = hasBoolean || mc.getRetType().equals(SpecialSymbols.BOOLEAN_TYPE);
            }

            Integer md = OPTION_MAX_DEPTH.parse(config);
            

            final Constants consts = new Constants();

            String cstString = OPTION_CONSTANTS.parse(config); 
            if (cstString != null) {
            	final SymbolicDataValueGenerator.ConstantGenerator cgen = new SymbolicDataValueGenerator.ConstantGenerator(); 
            	DataValue<?> [] cstArray = super.parseDataValues(cstString, types);
            	Arrays.stream(cstArray).forEach(c -> consts.put(cgen.next(c.getType()), c));
            }
            
            ParameterizedSymbol[] inputSymbols = inList.toArray(new ParameterizedSymbol[]{});
            boolean hasError = OPTION_OUTPUT_ERROR.parse(config);
            boolean hasNull = OPTION_OUTPUT_NULL.parse(config);

            if (hasError)
            	actList.add(SpecialSymbols.ERROR);
            if (hasNull)
            	actList.add(SpecialSymbols.NULL);
            if (hasVoid)
            	actList.add(SpecialSymbols.VOID);
            if (hasBoolean) {
	            actList.add(SpecialSymbols.TRUE);
	            actList.add(SpecialSymbols.FALSE);
            }
            if (!md.equals(-1))
            	actList.add(SpecialSymbols.DEPTH);
            ParameterizedSymbol[] actions = actList.toArray(new ParameterizedSymbol[]{});
            
            // create teachers
            this.teachers = super.buildTypeTheoryMapAndConfigureTheories(teacherClasses, config, types, consts);

            FieldConfig fieldConfig = null;
            String fieldConfigString = OPTION_CONFIG.parse(config);
            if (fieldConfigString != null) {
            	String[] fieldConfigSplit = fieldConfigString.replaceAll("\\s", "").split("\\;");
            	fieldConfig = new FieldConfig(target, fieldConfigSplit);
            }
            
            this.sulLearn = new ClasssAnalyzerDataWordSUL(target, methods, md, fieldConfig);
            this.sulLearn = setupDataWordOracle(sulLearn, teachers, consts, useFresh, timeoutMillis);
            
            this.sulCeAnalysis = new ClasssAnalyzerDataWordSUL(target, methods, md, fieldConfig); 
            this.sulCeAnalysis = setupDataWordOracle(sulCeAnalysis, teachers, consts, useFresh, timeoutMillis);
            
            this.sulTest = new ClasssAnalyzerDataWordSUL(target, methods, md, fieldConfig);
            this.sulTest = setupDataWordOracle(sulTest, teachers, consts, useFresh, timeoutMillis);
            
            IOCache ioCache = setupCache(config, IOCacheManager.JAVA_SERIALIZE);
            
            IOCacheOracle ioLearnCacheOracle = setupCacheOracle(sulLearn, teachers, consts, ioCache, useFresh);
            IOCacheOracle ioCeAnalysisCacheOracle = setupCacheOracle(sulCeAnalysis, teachers, consts, ioCache, useFresh);
            
            this.sulTraceOracle = ioLearnCacheOracle;
            
            IOFilter ioOracle = new IOFilter(ioLearnCacheOracle, inputSymbols);
            TreeOracle mto = new MultiTheoryTreeOracle(ioOracle, ioLearnCacheOracle, teachers, consts, solver);
            
            IOFilter ioCeOracle = new IOFilter(ioCeAnalysisCacheOracle, inputSymbols);
            TreeOracle ceMto = new MultiTheoryTreeOracle(ioCeOracle, ioCeAnalysisCacheOracle, teachers, consts, solver);
            ceMto = new TreeOracleStatsLoggerWrapper(ceMto, this.sulCeAnalysis);

            MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

            final long timeout = this.timeoutMillis;
            final Map<DataType, Theory> teach = this.teachers;
            TreeOracleFactory hypFactory = new TreeOracleFactory() {
                @Override
                public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                    DataWordOracle hypOracle = new SimulatorOracle(hyp);
                    if (timeout > 0L) {
                        hypOracle = new TimeOutOracle(hypOracle, timeout);
                    }
                    SimulatorSUL hypDataWordSimulation = new SimulatorSUL(hyp, teachers, consts);
                    IOOracle hypTraceOracle = new CanonizingSULOracle(hypDataWordSimulation, SpecialSymbols.ERROR, new SymbolicTraceCanonizer(teachers, consts));  
                    
                    return new MultiTheoryTreeOracle(hypOracle, hypTraceOracle,  teachers, consts, solver);
                }
            };
            
            this.hypVerifier = new IOHypVerifier(teach, consts);

            this.rastar = new RaStar(mto, ceMto, hypFactory, mlo, consts, true, teachers, this.hypVerifier, actions);

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

                this.randomWalk.setError(SpecialSymbols.ERROR);
                String ver = OPTION_TEST_TRACES.parse(config);
                if (ver != null) {
                	List<String> tests = Arrays.stream(ver.split(";")).collect(Collectors.toList());
                	traceTester = new TracesEquivalenceOracle(sulTest, teachers, consts, tests, actList);
                }
            }
            

            this.ceOptLoops = new IOCounterexampleLoopRemover(sulTraceOracle, this.hypVerifier);
            this.ceOptAsrep = new IOCounterExamplePrefixReplacer(sulTraceOracle, this.hypVerifier);
            this.ceOptPref = new IOCounterExamplePrefixFinder(sulTraceOracle, this.hypVerifier);
            this.ceOptSTR = new IOCounterExampleSingleTransitionRemover(sulTraceOracle, this.hypVerifier);

        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            ex.printStackTrace();
            throw new ConfigurationException(ex.getMessage());
        }

    }
    
    // could use a builder pattern here
    private DataWordSUL setupDataWordOracle(DataWordSUL basicSulOracle, Map<DataType, Theory> teachers, Constants consts, boolean useFresh, long timeoutMillis) {
    	DataWordSUL sulLearn = basicSulOracle;
    	if (useFresh) {
        	sulLearn = new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(teachers, consts), sulLearn);
        }
        if (timeoutMillis > 0L) {
        	
            sulLearn = new TimeOutSUL(sulLearn, timeoutMillis);
        }
        return sulLearn;
    }
    

    private IOCacheOracle setupCacheOracle(DataWordSUL sulLearn, Map<DataType, Theory> teachers, Constants consts, IOCache ioCache, boolean useFresh) {
    	IOOracle ioOracle;
    	IOCacheOracle ioCacheOracle;
	    if (useFresh)
	    	ioOracle = new CanonizingSULOracle(sulLearn, SpecialSymbols.ERROR, new SymbolicTraceCanonizer(this.teachers, consts));
	    else 
	    	ioOracle = new BasicSULOracle(sulLearn, SpecialSymbols.ERROR);
	    
	    if (useFresh)
	    	ioCacheOracle = new IOCacheOracle(ioOracle, ioCache, new SymbolicTraceCanonizer(this.teachers,consts));
	    else 
	    	ioCacheOracle = new IOCacheOracle(ioOracle, ioCache, null);
	    
    	return ioCacheOracle;
    }

	@Override
    public void run() throws RaLibToolException {
        System.out.println("=============================== START ===============================");

        final String __RUN__ = "overall execution time";
        final String __LEARN__ = "learning";
        final String __SEARCH__ = "ce searching";
        final String __EQ__ = "eq tests";

        System.out.println("SYS:------------------------------------------------");
        System.out.println(this.target.getName());
        System.out.println("----------------------------------------------------");

        SimpleProfiler.start(__RUN__);
        SimpleProfiler.start(__LEARN__);

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
            DefaultQuery<PSymbolInstance, Boolean> ce = null;

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
                System.out.println("Shorter CE Loops: " + ce);
                ce = ceOptSTR.optimizeCE(ce.getInput(), hyp);
                System.out.println("Shorter CE Single Transition Removal: " + ce);
                ce = ceOptAsrep.optimizeCE(ce.getInput(), hyp);
                System.out.println("New Prefix CE: " + ce);
                ce = ceOptPref.optimizeCE(ce.getInput(), hyp);
                System.out.println("Prefix of CE is CE: " + ce);
            }

            ceLengthsShortened.add(ce.getInput().length());

            Word<PSymbolInstance> sysTrace = sulTraceOracle.trace(ce.getInput());
            System.out.println("### SYS TRACE: " + sysTrace);

            SimulatorSUL hypSul = new SimulatorSUL(hyp, teachers, new Constants());
            IOOracle iosul = new BasicSULOracle(hypSul, SpecialSymbols.ERROR);        

            Word<PSymbolInstance> hypTrace = iosul.trace(ce.getInput());
            System.out.println("### HYP TRACE: " + hypTrace);

            assert !hypTrace.equals(sysTrace);
            rastar.addCounterexample(ce);
        }

        System.out.println("=============================== STOP ===============================");
        System.out.println(SimpleProfiler.getResults());

        System.out.println("ce lengths (oirginal): "
                + Arrays.toString(ceLengths.toArray()));

        if (useCeOptimizers) {
            System.out.println("ce lengths (shortend): "
                    + Arrays.toString(ceLengthsShortened.toArray()));
        }
        if (this.traceTester != null) {
        	DefaultQuery<PSymbolInstance, Boolean> ce = this.traceTester.findCounterExample(hyp, null);
        	if (ce != null) {
        		System.out.println("Learned model is incorrect " + ce);
        	}
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
                    RegisterAutomatonExporter.write(hyp, new Constants(), fso);
                } catch (FileNotFoundException ex) {
                    System.out.println("... export failed");
                }
                System.out.println("exporting model to model.dot");
                RAToDot dotExport = new RAToDot(hyp, true);
                String dot = dotExport.toString();

                try (BufferedWriter wr = new BufferedWriter(
                        new FileWriter(new File("model.dot")))) {
                    wr.write(dot, 0, dot.length());
                } catch (IOException ex) {
                    System.out.println("... export failed");
                }
            }
        }

        // tests during learning
        // resets + inputs
        System.out.println("Resets Learning: " + sulLearn.getResets());
        System.out.println("Inputs Learning: " + sulLearn.getInputs());
        
        // tests during ce analysis
        // resets + inputs
        System.out.println("Resets Ce Analysis: " + sulCeAnalysis.getResets());
        System.out.println("Inputs Ce Analysis: " + sulCeAnalysis.getInputs());

        // tests during search
        // resets + inputs
        System.out.println("Resets Testing: " + resets);
        System.out.println("Inputs Testing: " + inputs);

        // + sums
        System.out.println("Resets: " + (resets + sulLearn.getResets() + sulCeAnalysis.getResets()));
        System.out.println("Inputs: " + (inputs + sulLearn.getInputs() + sulCeAnalysis.getInputs()));

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
