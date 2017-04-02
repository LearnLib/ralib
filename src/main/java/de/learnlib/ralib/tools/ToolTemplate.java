package de.learnlib.ralib.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.util.RAToDot;
import de.learnlib.ralib.automata.xml.RegisterAutomatonExporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.equivalence.IOCounterExampleLoopRemover;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterExampleRelationRemover;
import de.learnlib.ralib.equivalence.IOCounterExampleSingleTransitionRemover;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.equivalence.IOHypVerifier;
import de.learnlib.ralib.equivalence.TracesEquivalenceOracle;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.mapper.ValueCanonizer;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.TreeOracleStatsLoggerWrapper;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.CachingSUL;
import de.learnlib.ralib.oracles.io.ExceptionHandlerOracle;
import de.learnlib.ralib.oracles.io.ExceptionHandlerSUL;
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
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.statistics.SimpleProfiler;
import net.automatalib.words.Word;

// This template can be used to define analyzers by connecting it
// to a parser class which provides alphabet and SUL info.
public abstract class ToolTemplate extends AbstractToolWithRandomWalk{
	
	private final ConfigurationOption<?>[] OPTIONS;
	
	private DataWordSUL sulLearn;
	private Map<DataType, Theory> teachers;
	private DataWordSUL sulCeAnalysis;
	private DataWordSUL sulTest;
	private IOHypVerifier hypVerifier;
	private IOOracle sulTraceOracle;
	private RaStar rastar;
	private TracesEquivalenceOracle traceTester;
	private IOCounterExampleLoopRemover ceOptLoops;
	private IOCounterExamplePrefixReplacer ceOptAsrep;
	private IOCounterExamplePrefixFinder ceOptPref;
	private IOCounterExampleSingleTransitionRemover ceOptSTR;
	private IOCounterExampleRelationRemover ceOptRelation;
	private String targetName;
	private IOEquivalenceOracle equOracle;
	
	
	private long resets;
	private long inputs;

	private Map<String, DataType> types;

	private SULParser sulParser;

	private Constants constants;

	public ToolTemplate(SULParser parser) throws ConfigurationException {
		OPTIONS = getOptions(parser.getClass(), this.getClass(), EquivalenceOracleFactory.class);
		this.sulParser = parser;
	
	}
	
	public void setup(Configuration config) throws ConfigurationException{
		super.setup(config);
		this.sulParser.parseConfig(config);
		this.sulLearn = sulParser.newSUL();
		this.targetName = sulParser.targetName();
		this.types = sulParser.getTypes();
		
		 final Constants consts = new Constants();

         String cstString = OPTION_CONSTANTS.parse(config); 
         if (cstString != null) {
         	final SymbolicDataValueGenerator.ConstantGenerator cgen = new SymbolicDataValueGenerator.ConstantGenerator(); 
         	DataValue<?> [] cstArray = super.parseDataValues(cstString, types);
         	Arrays.stream(cstArray).forEach(c -> consts.put(cgen.next(c.getType()), c));
         }
         this.constants = consts;
		
		this.teachers = super.buildTypeTheoryMapAndConfigureTheories(teacherClasses, config, types, consts);
        this.sulLearn = setupDataWordOracle(sulLearn, teachers, consts, useFresh, timeoutMillis);
        

        String debugTraces = OPTION_DEBUG_TRACES.parse(config);
        String debugSuffixes = OPTION_DEBUG_SUFFIXES.parse(config);
        if (debugTraces != null && debugSuffixes == null) 
        	runDebugTracesAndExit(debugTraces, this.sulLearn, this.teachers, consts);
        
        this.sulCeAnalysis = sulParser.newSUL(); 
        this.sulCeAnalysis = setupDataWordOracle(sulCeAnalysis, teachers, consts, useFresh, timeoutMillis);
        
        this.sulTest = sulParser.newSUL();
        this.sulTest = setupDataWordOracle(sulTest, teachers, consts, useFresh, timeoutMillis);
        
        String cacheSystem = OPTION_CACHE_SYSTEM.parse(config);
        IOCacheManager cacheManager = IOCacheManager.getCacheManager(cacheSystem);
        IOCache ioCache = setupCache(config, cacheManager, consts);
//        IOCache newIoCache = ioCache.getCacheExcluding((in, out) -> {
//        	return Arrays.stream(out.getParameterValues()).anyMatch(dv -> !(dv instanceof SumCDataValue) && dv.getId().equals(2920002L));
//        	}
//        );
//        try {
//			cacheManager.dumpCacheToFile("dump.ser", newIoCache, consts);
//			System.exit(0);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        
        IOCacheOracle ioLearnCacheOracle = setupCacheOracle(sulLearn, teachers, consts, ioCache, useFresh);
        IOCacheOracle ioCeAnalysisCacheOracle = setupCacheOracle(sulCeAnalysis, teachers, consts, ioCache, useFresh);
        if (OPTION_CACHE_TESTS.parse(config)) 
        	this.sulTest = new CachingSUL(this.sulTest, ioCache);
        
        this.sulTraceOracle = ioLearnCacheOracle;
        
        IOFilter ioOracle = new IOFilter(new ExceptionHandlerOracle(ioLearnCacheOracle), sulParser.getInputs());
        TreeOracle mto = new MultiTheoryTreeOracle(ioOracle, ioLearnCacheOracle, teachers, consts, solver);
        if (debugSuffixes != null) {
        	if (debugTraces == null) {
        		System.err.println("No debug traces given");
        		System.exit(0);
        	} else {
        		runDebugSuffixesAndExit(debugTraces, debugSuffixes, mto, teachers, consts);
        	}
        }
        
        
        IOFilter ioCeOracle = new IOFilter(new ExceptionHandlerOracle(ioCeAnalysisCacheOracle),  sulParser.getInputs());
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

        this.rastar = new RaStar(mto, ceMto, hypFactory, mlo, consts, 
                true, teachers, this.hypVerifier, solver, sulParser.getAlphabet());
        
        if (findCounterexamples) {
            this.equOracle = EquivalenceOracleFactory.buildEquivalenceOracle(config,
            		sulTest, teach, consts, random, sulParser.getInputs());
            String ver = OPTION_TEST_TRACES.parse(config);
            if (ver != null) {
            	List<String> tests = Arrays.stream(ver.split(";")).collect(Collectors.toList());
            	this.traceTester = new TracesEquivalenceOracle(sulTest, teachers, consts, tests, Arrays.asList(sulParser.getAlphabet()));
            }
        }
        

        this.ceOptLoops = new IOCounterExampleLoopRemover(sulTraceOracle, this.hypVerifier);
        this.ceOptAsrep = new IOCounterExamplePrefixReplacer(sulTraceOracle, this.hypVerifier);
        this.ceOptPref = new IOCounterExamplePrefixFinder(sulTraceOracle, this.hypVerifier);
        this.ceOptSTR = new IOCounterExampleSingleTransitionRemover(sulTraceOracle, this.hypVerifier);
        this.ceOptRelation = new IOCounterExampleRelationRemover(this.teachers, consts, this.solver, this.sulTraceOracle, this.hypVerifier);
	}
	
	
	private void runDebugTracesAndExit(String debug, DataWordSUL sul, Map<DataType, Theory> teachers, Constants consts) {
		List<String> testStrings = Arrays.stream(debug.split(";")).collect(Collectors.toList());
    	List<Word<PSymbolInstance>> tests = new TraceParser(testStrings, Arrays.asList(this.sulParser.getAlphabet())).getTraces();
    	SymbolicTraceCanonizer canonizer = new SymbolicTraceCanonizer(teachers, consts);
    	for (Word<PSymbolInstance> test : tests) {
    		Word<PSymbolInstance> canonizedTest = canonizer.canonizeTrace(test);
    		Word<PSymbolInstance> res = Word.epsilon();
    		sul.pre();
    		List<PSymbolInstance> inputs = canonizedTest.stream().filter(s -> 
    		(s.getBaseSymbol() instanceof InputSymbol)).collect(Collectors.toList());
    		for (PSymbolInstance inp : inputs) {
    			PSymbolInstance out = sul.step(inp);
    			res = res.append(inp).append(out);
    		}
    		sul.post();
    		System.out.println(res);
    	}
    	System.exit(0);
	}
	
	private void runDebugSuffixesAndExit(String debugPrefixes, String debugSuffixes, TreeOracle mto, Map<DataType, Theory> teachers, Constants constants) {
		List<String> prefixStrings = Arrays.stream(debugPrefixes.split(";")).collect(Collectors.toList());
		List<String> suffixStrings = Arrays.stream(debugSuffixes.split(";")).collect(Collectors.toList());
		List<Word<PSymbolInstance>> prefixes = new TraceParser(prefixStrings, Arrays.asList(this.sulParser.getAlphabet())).getTraces();
    	List<GeneralizedSymbolicSuffix> suffixes = new SuffixParser(suffixStrings, Arrays.asList(this.sulParser.getAlphabet()), teachers).getSuffixes();
    	SymbolicTraceCanonizer canonizer = new SymbolicTraceCanonizer(teachers, constants);
    	for (GeneralizedSymbolicSuffix suffix : suffixes) {
    		for (Word<PSymbolInstance> prefix : prefixes) {
    			prefix = canonizer.canonizeTrace(prefix);
        		System.out.println(prefix + " " + suffix);
        		TreeQueryResult tree = mto.treeQuery(prefix, suffix);
        		System.out.println(tree.toString());
    		}
    	}
    	System.exit(0);
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
        sulLearn = new ExceptionHandlerSUL(sulLearn);
        return sulLearn;
    }
    

    private IOCacheOracle setupCacheOracle(DataWordSUL sulLearn, Map<DataType, Theory> teachers, Constants consts, IOCache ioCache, boolean useFresh) {
    	IOOracle ioOracle;
    	IOCacheOracle ioCacheOracle;
    	sulLearn = new CachingSUL(sulLearn, ioCache);
	    if (useFresh)
	    	ioOracle = new CanonizingSULOracle(sulLearn, SpecialSymbols.ERROR, new SymbolicTraceCanonizer(this.teachers, consts));
	    else 
	    	ioOracle = new BasicSULOracle(sulLearn, SpecialSymbols.ERROR);
	    
	    if (useFresh)
	    	ioCacheOracle = new IOCacheOracle(ioOracle, ioCache, new SymbolicTraceCanonizer(this.teachers,consts));
	    else 
	    	ioCacheOracle = new IOCacheOracle(ioOracle, ioCache, trace -> trace);
	    
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
        System.out.println(this.targetName);
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
                ce = this.equOracle.findCounterExample(hyp, null);
                if (ce == null && traceTester != null) {
                	ce = this.traceTester.findCounterExample(hyp, null);
                }
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
                ce = ceOptRelation.optimizeCE(ce.getInput(), hyp);
                System.out.println("Relation reduced CE : " + ce);
            
            }

            ceLengthsShortened.add(ce.getInput().length());

            Word<PSymbolInstance> sysTrace = sulTraceOracle.trace(ce.getInput());
            System.out.println("### SYS TRACE: " + sysTrace);

            SimulatorSUL hypSul = new SimulatorSUL(hyp, teachers, this.constants);
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



	@Override
	public abstract String description();

}
