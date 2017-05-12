package de.learnlib.ralib.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.util.RAToDot;
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
import de.learnlib.ralib.oracles.CountingDataWordOracle;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.CachingSUL;
import de.learnlib.ralib.oracles.io.ConcurrentIOCacheOracle;
import de.learnlib.ralib.oracles.io.ConcurrentIOOracle;
import de.learnlib.ralib.oracles.io.DataWordIOOracle;
import de.learnlib.ralib.oracles.io.ExceptionHandlers;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOCacheManager;
import de.learnlib.ralib.oracles.io.IOCacheOracle;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.ConcurrentMultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.CanonizingSULOracle;
import de.learnlib.ralib.sul.CountingDataWordSUL;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.sul.InputCounter;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.statistics.SimpleProfiler;
import net.automatalib.words.Word;

// This template can be used to define analyzers by connecting it
// to a parser class which provides alphabet and SUL info.
public abstract class ToolTemplate extends AbstractToolWithRandomWalk{
	
	private final ConfigurationOption<?>[] OPTIONS;
	
    protected static final ConfigurationOption.StringOption OPTION_DEBUG_TRACES
    = new ConfigurationOption.StringOption("debug.traces",
            "Debug traces are run on the system at start with printing of the output, followed by exit. No learning is done."
            + "Debug traces format: test1; test2; ...", null, true);
    
    protected static final ConfigurationOption.IntegerOption OPTION_DEBUG_REPEATS
    = new ConfigurationOption.IntegerOption("debug.repeats",
            "Number of times a trace is executed."
            + "Non-negative number.", 1, true);
    
    
    protected static final ConfigurationOption.BooleanOption OPTION_STATS_CACHE
    = new ConfigurationOption.BooleanOption("stats.cache",
            "Also counts cached data in stats .", false, true);
    
    protected static final ConfigurationOption.StringOption OPTION_DEBUG_SUFFIXES
    = new ConfigurationOption.StringOption("debug.suffixes",
            "For the debug traces given, run the given suffixes exhaustively and exit. No learning is done."
            + "Debug suffixes format: suff1; suff2; ...", null, true);

	protected static final ConfigurationOption.StringOption OPTION_SUL_FACTORY
    = new ConfigurationOption.StringOption("sul.factory",
             "Provides a custom factory for generating SULs instead of the common one. The constructor should take in a SULParser type object", null, true);
    
	
	private DataWordIOOracle learnOracle;
	private Map<DataType, Theory> teachers;
	private DataWordIOOracle ceAnalysisOracle;
	private IOOracle testOracle;
	private Counters counters;
	private IOHypVerifier hypVerifier;
	private IOOracle sulReductionTraceOracle;
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

	private IOCacheManager cacheManager;

	private IOCache ioCache;

	private SULFactory sulFactory;
	
	// TODO remove this field/uses of this field after case study
	// determines if cache is set at SUL level or at IOOracle level
	// the the former, stats also include cached data, makes sense only if a cache is pre-loaded
	private boolean preCache;

	public ToolTemplate(SULParser parser) throws ConfigurationException {
		OPTIONS = getOptions(parser.getClass(), this.getClass(), EquivalenceOracleFactory.class);
		this.sulParser = parser;
	}
	
	public void setup(Configuration config) throws ConfigurationException{
		super.setup(config);
		this.preCache = OPTION_STATS_CACHE.parse(config);
		this.sulParser.parseConfig(config);
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
		
		this.teachers = super.buildTypeTheoryMapAndConfigureTheories(teacherClasses, config, types, sulParser.getInputs(), consts);
		String facString = OPTION_SUL_FACTORY.parse(config);
		if (facString == null)
			this.sulFactory = sulParser.newSULFactory();
		else
			this.sulFactory = instantiateCustomFactory(facString, sulParser);
		boolean canFork = this.sulFactory.isParallelizable();
		
        boolean determinize = this.useFresh;
        boolean handleExceptions = true;
        Integer sulInstances = OPTION_SUL_INSTANCES.parse(config);
        boolean isConcurrent = sulInstances > 1 && canFork; 
        this.counters = new Counters(isConcurrent);

        String debugTraces = OPTION_DEBUG_TRACES.parse(config);
        String debugSuffixes = OPTION_DEBUG_SUFFIXES.parse(config);
        if (debugTraces != null && debugSuffixes == null) {
        	int repeats = OPTION_DEBUG_REPEATS.parse(config);
        	IOOracle ioOracle = setupIOOracle(sulFactory, teachers, consts,  counters.ceInput, determinize, timeoutMillis, handleExceptions, sulInstances);
        	runDebugTracesAndExit(debugTraces, repeats, ioOracle, this.teachers, consts);
        }
        
      //boolean determinize, long timeoutMillis, boolean handleExceptions, int sulInstances 
        String cacheSystem = OPTION_CACHE_SYSTEM.parse(config);
        this.cacheManager = IOCacheManager.getCacheManager(cacheSystem);
        this.ioCache = setupCache(config, sulParser.getAlphabet(), teachers, cacheManager, consts);
        
        IOOracle learnIOOracle = setupIOOracle(sulFactory, teachers, consts,  counters.learnerInput, determinize, timeoutMillis, handleExceptions, sulInstances);
        this.learnOracle = setupDataWordIOOracle(learnIOOracle, consts, ioCache, determinize, isConcurrent, handleExceptions);
        learnOracle = new CountingDataWordOracle(learnOracle, counters.learnerQuery);
        
        
        
        IOOracle ceAnalysisIOOracle = setupIOOracle(sulFactory, teachers, consts,  counters.ceInput, determinize, timeoutMillis, handleExceptions, sulInstances);
        this.ceAnalysisOracle = setupDataWordIOOracle(ceAnalysisIOOracle, consts, ioCache, determinize, isConcurrent, handleExceptions);
        ceAnalysisOracle = new CountingDataWordOracle(ceAnalysisOracle, counters.ceQuery);
        sulReductionTraceOracle = setupDataWordIOOracle(ceAnalysisIOOracle, consts, ioCache, true, isConcurrent, handleExceptions);
        
        IOFilter ioOracle = new IOFilter(learnOracle, sulParser.getInputs());
        TreeOracle mto = !isConcurrent ? new MultiTheoryTreeOracle(ioOracle, learnOracle, teachers, consts, solver) :
        	new ConcurrentMultiTheoryTreeOracle(ioOracle, learnOracle, teachers, consts, solver);
        if (debugSuffixes != null) {
        	if (debugTraces == null) {
        		System.err.println("No debug traces given");
        		System.exit(0);
        	} else {
        		runDebugSuffixesAndExit(debugTraces, debugSuffixes, mto, teachers, consts);
        	}
        }
        
        
        IOFilter ioCeOracle = new IOFilter(ceAnalysisOracle,  sulParser.getInputs());
        TreeOracle ceMto =  !isConcurrent ? new MultiTheoryTreeOracle(ioCeOracle, ceAnalysisOracle, teachers, consts, solver) :
        	new ConcurrentMultiTheoryTreeOracle(ioCeOracle, ceAnalysisOracle, teachers, consts, solver);
        //ceMto = new TreeOracleStatsLoggerWrapper(ceMto, sulCeAnalysis);
        
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
                
                return !isConcurrent ? new MultiTheoryTreeOracle(hypOracle, hypTraceOracle,  teachers, consts, solver):
                	new ConcurrentMultiTheoryTreeOracle(hypOracle, hypTraceOracle,  teachers, consts, solver);
            }
        };
        
        this.hypVerifier = new IOHypVerifier(teach, consts);

        this.rastar = new RaStar(mto, ceMto, hypFactory, mlo, consts, 
                true, teachers, hypVerifier, solver, sulParser.getAlphabet());
        
        if (findCounterexamples) { 
        	this.testOracle = 
        			setupIOOracle(sulFactory, teachers, constants, counters.testInput, determinize, timeoutMillis, handleExceptions, sulInstances);
        	boolean cacheTests = OPTION_CACHE_TESTS.parse(config);
        	
    		IOCache ioCache = preCache? new IOCache() : this.ioCache ;
        	if (!isConcurrent) {
	        	if (cacheTests)
	        		testOracle = new IOCacheOracle(testOracle, ioCache, new SymbolicTraceCanonizer(teachers, consts));
	        	if (handleExceptions)
        			testOracle = ExceptionHandlers.wrapIOOracle(testOracle);
	        	this.equOracle = EquivalenceOracleFactory.buildEquivalenceOracle(config,
	            		testOracle, teach, consts, random, sulParser.getInputs());
        	} else {
        		if (cacheTests){
	        		testOracle = new ConcurrentIOCacheOracle(this.testOracle, ioCache, new SymbolicTraceCanonizer(teachers, consts));
	        	} 
        		if (handleExceptions)
        			testOracle = ExceptionHandlers.wrapIOOracle(testOracle);
        		this.equOracle = EquivalenceOracleFactory.buildEquivalenceOracle(config,
        				testOracle,  sulInstances, teach, consts, random, sulParser.getInputs());
        	}
            
            String ver = OPTION_TEST_TRACES.parse(config);
            if (ver != null) {
            	List<Word<PSymbolInstance>> tests =  getCanonizedWordsFromString(ver, this.sulParser.getAlphabet(), teachers, consts);
            	this.traceTester = new TracesEquivalenceOracle(this.testOracle, teachers, consts, tests);
            }
        }
        

        this.ceOptLoops = new IOCounterExampleLoopRemover(sulReductionTraceOracle, this.hypVerifier);
        this.ceOptAsrep = new IOCounterExamplePrefixReplacer(sulReductionTraceOracle, this.hypVerifier);
        this.ceOptPref = new IOCounterExamplePrefixFinder(sulReductionTraceOracle, this.hypVerifier);
        this.ceOptSTR = new IOCounterExampleSingleTransitionRemover(sulReductionTraceOracle, this.hypVerifier);
        this.ceOptRelation = new IOCounterExampleRelationRemover(teachers, consts, this.solver, this.sulReductionTraceOracle, this.hypVerifier);
	}
	
	
	private SULFactory instantiateCustomFactory(String facString, SULParser parser) throws ConfigurationException{
		SULFactory factory = null;
		try {
			Class<?> facCls = Class.forName(facString);
			try {
			factory =  SULFactory.class.cast(facCls.getConstructor(SULParser.class).newInstance(parser));
			} catch(NoSuchMethodException meth) {
				factory = SULFactory.class.cast(facCls.newInstance());
			}
		} catch (Exception e) {
			throw new ConfigurationException(e.getMessage());
		}
		return factory;
	}

	private void runDebugTracesAndExit(String debug, int numRepeats, IOOracle ioOracle, Map<DataType, Theory> teachers, Constants consts) {
    	List<Word<PSymbolInstance>> canonizedTests = getCanonizedWordsFromString(debug, this.sulParser.getAlphabet(), teachers, consts);
    	List<Word<PSymbolInstance>> allTests = new ArrayList<>(canonizedTests.size()*numRepeats);
    	for (int i=0; i<numRepeats; i++)
    		allTests.addAll(canonizedTests);
    	List<Word<PSymbolInstance>> results = ioOracle.traces(allTests);
    	System.out.println(StringUtils.join(results, "\n"));
    	System.exit(0);
	}
	
	private void runDebugSuffixesAndExit(String debugPrefixes, String debugSuffixes, TreeOracle mto, Map<DataType, Theory> teachers, Constants constants) {
		List<String> suffixStrings = Arrays.stream(debugSuffixes.split(";")).collect(Collectors.toList());
		List<Word<PSymbolInstance>> prefixes = getCanonizedWordsFromString(debugPrefixes, this.sulParser.getAlphabet(), teachers, constants);
    	List<GeneralizedSymbolicSuffix> suffixes = new SuffixParser(suffixStrings, Arrays.asList(this.sulParser.getAlphabet()), teachers).getSuffixes();
    	
    	for (GeneralizedSymbolicSuffix suffix : suffixes) {
    		for (Word<PSymbolInstance> prefix : prefixes) {
        		System.out.println(prefix + " " + suffix);
        		TreeQueryResult tree = mto.treeQuery(prefix, suffix);
        		System.out.println(tree.toString());
    		}
    	}
    	System.exit(0);
	}
	
    
    private DataWordIOOracle setupDataWordIOOracle(IOOracle ioOracle, Constants consts, 
    		IOCache ioCache, boolean determinize, boolean concurrent, boolean handleExceptions) {
    	DataWordIOOracle ioCacheOracle;
    	ioCache = preCache? new IOCache() : ioCache;
    	if (determinize)
	    	ioCacheOracle = new IOCacheOracle(ioOracle, ioCache, new SymbolicTraceCanonizer(this.teachers,consts));
	    else 
	    	ioCacheOracle = new IOCacheOracle(ioOracle, ioCache, trace -> trace);
	    if (handleExceptions)
	    	return ExceptionHandlers.wrapDataWordIOOracle(ioCacheOracle);
	    else 
	    	return ioCacheOracle;
    }
    
    private IOOracle setupIOOracle(SULFactory  sulFactory, Map<DataType, Theory> teachers, Constants consts,  
    		InputCounter inputCounter,
    		boolean determinize, long timeoutMillis, boolean handleExceptions, int sulInstances ) {
    	IOOracle ioOracle;

    	if (sulFactory.isParallelizable() && sulInstances > 1) {
    		DataWordSUL[] suls = sulFactory.newIndependentSULs(sulInstances);
    		List<DataWordSUL> wrappedSuls = setupDataWordOracle(suls, teachers, consts, inputCounter, determinize, timeoutMillis);
    		List<IOOracle> oracles = wrappedSuls.stream().map(sul -> {
	        	IOOracle oracle;
	        	if (determinize)
	        	   	oracle =  new CanonizingSULOracle(sul, SpecialSymbols.ERROR, new SymbolicTraceCanonizer(teachers, consts));
	        	else 
	        	  	oracle = new BasicSULOracle(sul, SpecialSymbols.ERROR);
	        	if (handleExceptions)
	        		oracle =  ExceptionHandlers.wrapIOOracle(oracle);
	        	return oracle;
        	}).collect(Collectors.toList());
    		ioOracle = new ConcurrentIOOracle(oracles);
    	} else {
    		DataWordSUL singleSUL = sulFactory.newSUL();
    		DataWordSUL wrappedSUL = setupDataWordOracle(singleSUL, teachers, consts, inputCounter, determinize, timeoutMillis);
    		if (determinize)
        	   	ioOracle =  new CanonizingSULOracle(wrappedSUL, SpecialSymbols.ERROR, new SymbolicTraceCanonizer(teachers, consts));
        	else 
        	  	ioOracle = new BasicSULOracle(wrappedSUL, SpecialSymbols.ERROR);
        	if (handleExceptions)
        		ioOracle =  ExceptionHandlers.wrapIOOracle(ioOracle);
    	}
    	
    	return ioOracle;
    }
    

	// could use a builder pattern here
    private DataWordSUL setupDataWordOracle(DataWordSUL sulInstance, Map<DataType, Theory> teachers, Constants consts, 
    		InputCounter inputCounter, boolean determinize, long timeoutMillis) {
    	DataWordSUL sul = sulInstance;
    	if (determinize) {
        	sul = new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(teachers, consts), sul);
        }
    	if (preCache) {
        	sul = new CachingSUL(sul, ioCache);
        }
    	sul = new CountingDataWordSUL(sul, inputCounter);
        if (timeoutMillis > 0L) {
            sul = new TimeOutSUL(sul, timeoutMillis);
        }
        
        return sul;
    }
    
    private List<DataWordSUL> setupDataWordOracle(DataWordSUL [] sulInstances, Map<DataType, Theory> teachers, Constants consts,
    		InputCounter inputCounter,
    		boolean determinize, long timeoutMillis) {
    	List<DataWordSUL> wrappedSulInstances = Arrays.stream(sulInstances)
    			.map(sulInstance -> this.setupDataWordOracle(sulInstance, teachers, consts, inputCounter, determinize, timeoutMillis))
    			.collect(Collectors.toList());
    	return wrappedSulInstances;
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
//            try {
//				this.cacheManager.dumpCacheToFile(this.targetName.toLowerCase()+"-hyp"+rounds, this.ioCache, constants);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

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

            resets = this.counters.testInput.getResets();
            inputs = this.counters.testInput.getInputs();

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

            Word<PSymbolInstance> sysTrace = testOracle.trace(ce.getInput());
            System.out.println("### SYS TRACE: " + sysTrace);
            assert sysTrace.equals(ce.getInput());

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
//                try {
//                    FileOutputStream fso = new FileOutputStream("model.xml");
//                    RegisterAutomatonExporter.write(hyp, new Constants(), fso);
//                } catch (FileNotFoundException ex) {
//                    System.out.println("... export failed");
//                }
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
        System.out.println("Resets Learning: " + counters.learnerInput.getResets());
        System.out.println("Inputs Learning: " + counters.learnerInput.getInputs());
        
        // tests during ce analysis
        // resets + inputs
        System.out.println("Resets Ce Analysis: " + counters.ceInput.getResets());
        System.out.println("Inputs Ce Analysis: " + counters.ceInput.getInputs());

        // tests during search
        // resets + inputs
        System.out.println("Resets Testing: " + counters.testInput.getResets());
        System.out.println("Inputs Testing: " + counters.testInput.getInputs());

        // + sums
        System.out.println("Resets: " + (resets +  counters.learnerInput.getResets() + counters.ceInput.getResets()));
        System.out.println("Inputs: " + (inputs +  counters.learnerInput.getInputs() + counters.ceInput.getInputs()));

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
	
	class Counters {
		final InputCounter learnerInput;
		final QueryCounter learnerQuery;
		final InputCounter ceInput;
		final QueryCounter ceQuery;
		final InputCounter testInput;
		public Counters(boolean concurrent) {
			InputCounter learnerInput = new InputCounter();
			InputCounter ceInput = new InputCounter();
			InputCounter testInput = new InputCounter();
			if (concurrent) {
				this.learnerInput = learnerInput.asThreadSafe();
				this.ceInput = ceInput.asThreadSafe();
				this.testInput = testInput.asThreadSafe();
			} else {
				this.learnerInput = learnerInput;
				this.ceInput = ceInput;
				this.testInput = testInput;
			}
			learnerQuery = new QueryCounter();
			ceQuery = new QueryCounter();
		}
	}

}
