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
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.CachingSUL;
import de.learnlib.ralib.oracles.io.ConcurrentIOCacheOracle;
import de.learnlib.ralib.oracles.io.ConcurrentIOOracle;
import de.learnlib.ralib.oracles.io.DataWordIOOracle;
import de.learnlib.ralib.oracles.io.ExceptionHandlers;
import de.learnlib.ralib.oracles.io.ExceptionHandlerSUL;
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
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
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

	public ToolTemplate(SULParser parser) throws ConfigurationException {
		OPTIONS = getOptions(parser.getClass(), this.getClass(), EquivalenceOracleFactory.class);
		this.sulParser = parser;
	
	}
	
	public void setup(Configuration config) throws ConfigurationException{
		super.setup(config);
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
		
		this.teachers = super.buildTypeTheoryMapAndConfigureTheories(teacherClasses, config, types, consts);
		this.sulLearn = sulParser.newSUL();
		boolean canFork = sulLearn.canFork();
		this.sulLearn = setupDataWordOracle(this.sulLearn, teachers, consts, useFresh, timeoutMillis);

        String debugTraces = OPTION_DEBUG_TRACES.parse(config);
        String debugSuffixes = OPTION_DEBUG_SUFFIXES.parse(config);
        if (debugTraces != null && debugSuffixes == null) {
        	int repeats = OPTION_DEBUG_REPEATS.parse(config);
        	runDebugTracesAndExit(debugTraces, repeats, this.sulLearn, this.teachers, consts);
        }
        

        String cacheSystem = OPTION_CACHE_SYSTEM.parse(config);
        this.cacheManager = IOCacheManager.getCacheManager(cacheSystem);
        this.ioCache = setupCache(config, sulParser.getAlphabet(), teachers, cacheManager, consts);

		this.sulCeAnalysis = sulParser.newSUL();
		this.sulCeAnalysis = setupDataWordOracle(sulCeAnalysis, teachers, consts, useFresh, timeoutMillis);
		
        this.sulTest = sulParser.newSUL();
        this.sulTest = setupDataWordOracle(sulTest, teachers, consts, useFresh, timeoutMillis);
        
        Integer sulInstances = OPTION_SUL_INSTANCES.parse(config);
        boolean isConcurrent = sulInstances > 1 && canFork; 
        
        DataWordIOOracle ioLearnCacheOracle;
        DataWordIOOracle ioCeAnalysisCacheOracle;
        boolean handleExc = true;
        if (isConcurrent) {
	        ioLearnCacheOracle = setupDataWordIOOracle(sulLearn, teachers, consts, ioCache, useFresh, handleExc);
	        ioCeAnalysisCacheOracle = setupDataWordIOOracle(sulCeAnalysis, teachers, consts, ioCache, useFresh, handleExc);
        } else {
        	List<DataWordSUL> sulLearnForks = setupDataWordOracle(sulInstances, sulParser.newSUL(), teachers, consts, useFresh, timeoutMillis);
        	List<DataWordSUL> sulCeForks = setupDataWordOracle(sulInstances, sulParser.newSUL(), teachers, consts, useFresh, timeoutMillis);
        	ioLearnCacheOracle = setupDataWordIOOracle(sulLearnForks, teachers, consts, ioCache, useFresh, handleExc);
        	ioCeAnalysisCacheOracle = setupDataWordIOOracle(sulCeForks, teachers, consts, ioCache, useFresh, handleExc);
        }
        
        //new CachingSUL(this.sulCeAnalysis, ioCache)
        // we need to also add caching at the SUL level, since reduced CE's cannot be found
        // reduction implies that we use a determinize-type oracle
        this.sulReductionTraceOracle = setupDataWordIOOracle(this.sulCeAnalysis, teachers, consts, ioCache, true, handleExc);
        
        IOFilter ioOracle = new IOFilter(ioLearnCacheOracle, sulParser.getInputs());
        TreeOracle mto = !isConcurrent ? new MultiTheoryTreeOracle(ioOracle, ioLearnCacheOracle, teachers, consts, solver) :
        	new ConcurrentMultiTheoryTreeOracle(ioOracle, ioLearnCacheOracle, teachers, consts, solver);
        if (debugSuffixes != null) {
        	if (debugTraces == null) {
        		System.err.println("No debug traces given");
        		System.exit(0);
        	} else {
        		runDebugSuffixesAndExit(debugTraces, debugSuffixes, mto, teachers, consts);
        	}
        }
        
        
        IOFilter ioCeOracle = new IOFilter(ioCeAnalysisCacheOracle,  sulParser.getInputs());
        TreeOracle ceMto =  !isConcurrent ? new MultiTheoryTreeOracle(ioCeOracle, ioCeAnalysisCacheOracle, teachers, consts, solver) :
        	new ConcurrentMultiTheoryTreeOracle(ioCeOracle, ioCeAnalysisCacheOracle, teachers, consts, solver);
        //ceMto = new TreeOracleStatsLoggerWrapper(ceMto, this.sulCeAnalysis);
        
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
                
                return !isConcurrent? new MultiTheoryTreeOracle(hypOracle, hypTraceOracle,  teachers, consts, solver):
                	new ConcurrentMultiTheoryTreeOracle(hypOracle, hypTraceOracle,  teachers, consts, solver);
            }
        };
        
        this.hypVerifier = new IOHypVerifier(teach, consts);

        this.rastar = new RaStar(mto, ceMto, hypFactory, mlo, consts, 
                true, teachers, this.hypVerifier, solver, sulParser.getAlphabet());
        
        if (findCounterexamples) { //uglyyyyy
        	IOOracle testOracle;
        	if (!isConcurrent) {
        		testOracle = new CanonizingSULOracle(sulTest, SpecialSymbols.ERROR, new SymbolicTraceCanonizer(this.teachers, consts));
	        	if (OPTION_CACHE_TESTS.parse(config))
	        		testOracle = new IOCacheOracle(testOracle, ioCache, new SymbolicTraceCanonizer(teachers, consts));
	        	if (handleExc)
        			testOracle = ExceptionHandlers.wrapIOOracle(testOracle);
	        	this.equOracle = EquivalenceOracleFactory.buildEquivalenceOracle(config,
	            		testOracle, teach, consts, random, sulParser.getInputs());
        	} else {
        		List<DataWordSUL> sulTestForks = setupDataWordOracle(sulInstances, sulParser.newSUL(), teachers, consts, useFresh, timeoutMillis);
        		List<IOOracle> concurrentOracles = sulTestForks.stream()
        				.map(st -> new CanonizingSULOracle(st, SpecialSymbols.ERROR, new SymbolicTraceCanonizer(this.teachers, consts)))
        				.map(st -> ExceptionHandlers.wrapIOOracle(st))
        				.collect(Collectors.toList());
        		if (OPTION_CACHE_TESTS.parse(config)){
	        		testOracle = new ConcurrentIOCacheOracle(concurrentOracles, ioCache, new SymbolicTraceCanonizer(teachers, consts));
	        	} else {
	        		testOracle = new ConcurrentIOOracle(concurrentOracles);
	        	}
        		if (handleExc)
        			testOracle = ExceptionHandlers.wrapIOOracle(testOracle);
        		this.equOracle = EquivalenceOracleFactory.buildEquivalenceOracle(config,
        				testOracle,  sulTestForks.size(), teach, consts, random, sulParser.getInputs());
        	}
            
            String ver = OPTION_TEST_TRACES.parse(config);
            if (ver != null) {
            	List<String> tests = Arrays.stream(ver.split(";")).collect(Collectors.toList());
            	this.traceTester = new TracesEquivalenceOracle(sulTest, teachers, consts, tests, Arrays.asList(sulParser.getAlphabet()));
            }
        }
        

        this.ceOptLoops = new IOCounterExampleLoopRemover(sulReductionTraceOracle, this.hypVerifier);
        this.ceOptAsrep = new IOCounterExamplePrefixReplacer(sulReductionTraceOracle, this.hypVerifier);
        this.ceOptPref = new IOCounterExamplePrefixFinder(sulReductionTraceOracle, this.hypVerifier);
        this.ceOptSTR = new IOCounterExampleSingleTransitionRemover(sulReductionTraceOracle, this.hypVerifier);
        this.ceOptRelation = new IOCounterExampleRelationRemover(this.teachers, consts, this.solver, this.sulReductionTraceOracle, this.hypVerifier);
	}
	
	
	private void runDebugTracesAndExit(String debug, int numRepeats, DataWordSUL sul, Map<DataType, Theory> teachers, Constants consts) {
    	List<Word<PSymbolInstance>> canonizedTests = getCanonizedWordsFromString(debug, this.sulParser.getAlphabet(), teachers, consts);
    	sul = new ExceptionHandlerSUL(sul);
    	for (int i=0; i<numRepeats; i++) 
	    	for (Word<PSymbolInstance> canonizedTest : canonizedTests) {
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
		List<String> suffixStrings = Arrays.stream(debugSuffixes.split(";")).collect(Collectors.toList());
		List<Word<PSymbolInstance>> prefixes = getCanonizedWordsFromString(debugPrefixes, this.sulParser.getAlphabet(), teachers, constants);
    	List<GeneralizedSymbolicSuffix> suffixes = new SuffixParser(suffixStrings, Arrays.asList(this.sulParser.getAlphabet()), teachers).getSuffixes();
    	
    	for (GeneralizedSymbolicSuffix suffix : suffixes) {
//    		suffix.getPrefixRelations(1).clear();
//    		suffix.getSuffixRelations(1, 3).clear();
//    		suffix.getSuffixRelations(2, 3).clear();
//    		suffix.getPrefixRelations(3).clear();
    		for (Word<PSymbolInstance> prefix : prefixes) {
        		System.out.println(prefix + " " + suffix);
        		TreeQueryResult tree = mto.treeQuery(prefix, suffix);
        		System.out.println(tree.toString());
    		}
    	}
    	System.exit(0);
	}
	

	// could use a builder pattern here
    private DataWordSUL setupDataWordOracle(DataWordSUL basicSulOracle, Map<DataType, Theory> teachers, Constants consts, 
    		boolean determinize, long timeoutMillis) {
    	DataWordSUL sulLearn = basicSulOracle;
    	if (determinize) {
        	sulLearn = new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(teachers, consts), sulLearn);
        }
        if (timeoutMillis > 0L) {
        	
            sulLearn = new TimeOutSUL(sulLearn, timeoutMillis);
        }
        return sulLearn;
    }
    
    private List<DataWordSUL> setupDataWordOracle(Integer forkableInstances, DataWordSUL basicSulOracle, Map<DataType, Theory> teachers, Constants consts, 
    		boolean determinize, long timeoutMillis) {
    	List<DataWordSUL> forks = new ArrayList<>(forkableInstances);
    	for (int i=0; i<forkableInstances; i++) {
    		forks.add(this.setupDataWordOracle(basicSulOracle, teachers, consts, determinize, timeoutMillis));
    		basicSulOracle = (DataWordSUL) basicSulOracle.fork();
    	}
    	return forks;
    }

    private DataWordIOOracle setupDataWordIOOracle(DataWordSUL sulLearn, Map<DataType, Theory> teachers, Constants consts, 
    		IOCache ioCache, boolean determinize, boolean handleExceptions) {
    	IOOracle ioOracle;
    	IOCacheOracle ioCacheOracle;
    	
	    if (determinize)
	    	ioOracle = new CanonizingSULOracle(sulLearn, SpecialSymbols.ERROR, new SymbolicTraceCanonizer(this.teachers, consts));
	    else 
	    	ioOracle = new BasicSULOracle(sulLearn, SpecialSymbols.ERROR);
	    
	    if (determinize)
	    	ioCacheOracle = new IOCacheOracle(ioOracle, ioCache, new SymbolicTraceCanonizer(this.teachers,consts));
	    else 
	    	ioCacheOracle = new IOCacheOracle(ioOracle, ioCache, trace -> trace);
	    if (handleExceptions)
	    	return ExceptionHandlers.wrapDataWordIOOracle(ioCacheOracle);
	    else 
	    	return ioCacheOracle;
    }
    
    private DataWordIOOracle setupDataWordIOOracle(List<DataWordSUL> forks, Map<DataType, Theory> teachers, Constants consts, IOCache ioCache, boolean determinize, boolean handleExceptions) {
    	ConcurrentIOCacheOracle ioCacheOracle;
    	
    	List<IOOracle> oracles = forks.stream().map(sul -> {
    		IOOracle oracle;
    		if (determinize)
    	    	oracle =  new CanonizingSULOracle(sul, SpecialSymbols.ERROR, new SymbolicTraceCanonizer(this.teachers, consts));
    	    else 
    	    	oracle = new BasicSULOracle(sul, SpecialSymbols.ERROR);
    		if (handleExceptions)
    			oracle =  ExceptionHandlers.wrapIOOracle(oracle);
    		return oracle;
    		
    	}).collect(Collectors.toList());
	    
	    if (determinize)
	    	ioCacheOracle = new ConcurrentIOCacheOracle(oracles, ioCache, new SymbolicTraceCanonizer(this.teachers,consts));
	    else 
	    	ioCacheOracle = new ConcurrentIOCacheOracle(oracles, ioCache, tr -> tr);
	    if (handleExceptions)
	    	return ExceptionHandlers.wrapDataWordIOOracle(ioCacheOracle);
	    else 
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

            Word<PSymbolInstance> sysTrace = sulReductionTraceOracle.trace(ce.getInput());
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
