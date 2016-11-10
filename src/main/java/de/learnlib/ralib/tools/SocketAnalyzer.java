package de.learnlib.ralib.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonExporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IOHypVerifier;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
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
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.ClasssAnalyzerDataWordSUL;
import de.learnlib.ralib.tools.classanalyzer.MethodConfig;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.tools.sockanalyzer.IOConfig;
import de.learnlib.ralib.tools.sockanalyzer.SocketAnalyzerSUL;
import de.learnlib.ralib.tools.theories.TraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import de.learnlib.statistics.SimpleProfiler;
import net.automatalib.words.Word;

public class SocketAnalyzer extends AbstractToolWithRandomWalk{
    private static final ConfigurationOption.StringOption OPTION_TARGET_IP
    = new ConfigurationOption.StringOption("target.ip",
            "SUL IP address", "localhost", false);

    protected static final ConfigurationOption.IntegerOption OPTION_TARGET_PORT
    = new ConfigurationOption.IntegerOption("target.port",
            "SUL port number", 8000, false);

    protected static final ConfigurationOption.IntegerOption OPTION_DEPTH
    = new ConfigurationOption.IntegerOption("max.depth",
            "Maximum depth to explore", -1, true);

    private static final ConfigurationOption.StringOption OPTION_INPUTS
            = new ConfigurationOption.StringOption("inputs",
                    "traget input signatures. format: m1(class:type,class:type) + m2() + ...", null, false);
    
    private static final ConfigurationOption.StringOption OPTION_OUTPUTS
    = new ConfigurationOption.StringOption("outputs",
            "traget output signatures. format: m1(class:type,class:type) + m2() + ...", null, false);


    protected static final ConfigurationOption.IntegerOption OPTION_MAX_DEPTH
            = new ConfigurationOption.IntegerOption("max.depth",
                    "Maximum depth to explore", -1, true);
    
    protected static final ConfigurationOption.BooleanOption OPTION_OUTPUT_ERROR
    = new ConfigurationOption.BooleanOption("output.error",
            "Include error output", true, true);

    protected static final ConfigurationOption.StringOption OPTION_CACHE_DUMP
    = new ConfigurationOption.StringOption("cache.dump",
            "Dump cache to file", null, true);
    
    protected static final ConfigurationOption.StringOption OPTION_CACHE_LOAD
    = new ConfigurationOption.StringOption("cache.load",
            "Load cache from file if file exists", null, true);
    
    protected static final ConfigurationOption.BooleanOption OPTION_CACHE_TESTS
    = new ConfigurationOption.BooleanOption("cache.tests",
            "Also cache tests", false, true);
    
    private static final ConfigurationOption[] OPTIONS = new ConfigurationOption[]{
        OPTION_LOGGING_LEVEL,
        OPTION_LOGGING_CATEGORY,
        OPTION_INPUTS,
        OPTION_TEACHERS,
        OPTION_RANDOM_SEED,
        OPTION_USE_CEOPT,
        OPTION_USE_SUFFIXOPT,
        OPTION_EXPORT_MODEL,
        OPTION_TARGET_IP,
        OPTION_USE_RWALK,
        OPTION_MAX_ROUNDS,
        OPTION_MAX_DEPTH,
        OPTION_TIMEOUT,
        OPTION_RWALK_FRESH_PROB,
        OPTION_RWALK_RESET_PROB,
        OPTION_RWALK_MAX_DEPTH,
        OPTION_RWALK_MAX_RUNS,
        OPTION_RWALK_RESET,
        OPTION_OUTPUT_ERROR
    };

    private DataWordSUL sulLearn;

    private DataWordSUL sulTest;

    private IORandomWalk randomWalk = null;

    private RaStar rastar;

    private IOCounterexampleLoopRemover ceOptLoops;

    private IOCounterExamplePrefixReplacer ceOptAsrep;

    private IOCounterExamplePrefixFinder ceOptPref;

    private IOOracle back;

    private Map<DataType, Theory> teachers;
    
    private final Map<String, DataType> types = new LinkedHashMap<>();

    private final Map<ParameterizedSymbol, MethodConfig> methods = new LinkedHashMap<>();

    private long resets = 0;
    private long inputs = 0;

	private IOHypVerifier hypVerifier;

	private String systemIP;

	private Integer systemPort;

    @Override
    public String description() {
        return "analyzes Java classes";
    }

    @Override
    public void setup(Configuration config) throws ConfigurationException {

        List<ParameterizedSymbol> inList = new ArrayList<>();
        List<ParameterizedSymbol> outList = new ArrayList<>();
        List<ParameterizedSymbol> actList = new ArrayList<>();
        try {
            super.setup(config);

            this.types.put("boolean", SpecialSymbols.BOOLEAN_TYPE);

            this.systemIP = OPTION_TARGET_IP.parse(config);
            this.systemPort = OPTION_TARGET_PORT.parse(config);
            

            String[] inputStrings = OPTION_INPUTS.parse(config).split("\\+");
            for (String input : inputStrings) {
                IOConfig mc = new IOConfig(input, true, this.types);
                inList.add(mc.getSymbol());
                actList.add(mc.getSymbol());
            }
            
            String[] outputStrings = OPTION_OUTPUTS.parse(config).split("\\+");
            for (String output : outputStrings) {
                IOConfig mc = new IOConfig(output, false, this.types);
                outList.add(mc.getSymbol());
                actList.add(mc.getSymbol());
            }

            Integer md = OPTION_MAX_DEPTH.parse(config);
            
            // create teachers
            teachers = new LinkedHashMap<DataType, Theory>();

            for (String tName : teacherClasses.keySet()) {
                DataType t = types.get(tName);
                TypedTheory theory = teacherClasses.get(t.getName());
                theory.setType(t);
                if (this.useSuffixOpt) {
                    theory.setUseSuffixOpt(this.useSuffixOpt);
                }
                teachers.put(t, theory);
            }


            sulLearn = new SocketAnalyzerSUL(systemIP, systemPort, md, inList, outList);
            if (this.useFresh) {
            	this.sulLearn = new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(this.teachers), sulLearn);
            }
            if (this.timeoutMillis > 0L) {
            	
                this.sulLearn = new TimeOutSUL(this.sulLearn, this.timeoutMillis);
            }
            sulTest = new SocketAnalyzerSUL(systemIP, systemPort, md, inList, outList);
            if (this.useFresh) {
            	this.sulTest = new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(this.teachers), sulTest);
            }
            if (this.timeoutMillis > 0L) {
                this.sulTest = new TimeOutSUL(this.sulTest, this.timeoutMillis);
            }

            ParameterizedSymbol[] inputSymbols = inList.toArray(new ParameterizedSymbol[]{});
            boolean hasError = OPTION_OUTPUT_ERROR.parse(config);

            if (hasError)
            	actList.add(SpecialSymbols.ERROR);
            if (!md.equals(-1))
            	actList.add(SpecialSymbols.DEPTH);
            ParameterizedSymbol[] actions = actList.toArray(new ParameterizedSymbol[]{});

            final Constants consts = new Constants();

            if (useFresh)
            	back = new CanonizingSULOracle(sulLearn, SpecialSymbols.ERROR, new TraceCanonizer(this.teachers));
            else 
            	back = new BasicSULOracle(sulLearn, SpecialSymbols.ERROR);
            
            IOCacheOracle ioCacheOracle = null;
            IOCache ioCache = setupCache(config, IOCacheManager.JAVA_SERIALIZE);
            
            if (useFresh)
            	ioCacheOracle = new IOCacheOracle(back, ioCache, new TraceCanonizer(this.teachers));
            else 
            	ioCacheOracle = new IOCacheOracle(back, ioCache);
            
            IOFilter ioOracle = new IOFilter(ioCacheOracle, inputSymbols);
            
            if (useFresh) {
                for (Theory t : teachers.values()) {
                    ((TypedTheory) t).setCheckForFreshOutputs(true, ioCacheOracle);
                }
            }
            
            MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioOracle, teachers, consts, solver);
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
                    return new MultiTheoryTreeOracle(hypOracle, teachers, consts, solver);
                }
            };
            
            this.hypVerifier = new IOHypVerifier(teach, consts);

            this.rastar = new RaStar(mto, hypFactory, mlo, consts, true, this.hypVerifier, actions);

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
            }
            

            this.ceOptLoops = new IOCounterexampleLoopRemover(back, this.hypVerifier);
            this.ceOptAsrep = new IOCounterExamplePrefixReplacer(back, this.hypVerifier);
            this.ceOptPref = new IOCounterExamplePrefixFinder(back, this.hypVerifier);

        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            ex.printStackTrace();
            throw new ConfigurationException(ex.getMessage());
        }

    }
    
    private IOCache setupCache(Configuration config, IOCacheManager cacheMgr) throws ConfigurationException {
    	IOCache ioCache = null;
        String load = OPTION_CACHE_LOAD.parse(config);
        if (load != null && new File(load).exists()) {
        	try {
				ioCache = cacheMgr.loadCacheFromFile(load);
			} catch (Exception e) {
				throw new ConfigurationException(e.getMessage());
			}
        } else {
        	ioCache = new IOCache();
        }
        final String dump = OPTION_CACHE_DUMP.parse(config);
        final IOCache finalCache = ioCache;
        if (dump != null) {
        	Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					try {
						cacheMgr.dumpCacheToFile(dump, finalCache);
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}}));
        }
        return ioCache;
    }

    @Override
    public void run() throws RaLibToolException {
        System.out.println("=============================== START ===============================");

        final String __RUN__ = "overall execution time";
        final String __LEARN__ = "learning";
        final String __SEARCH__ = "ce searching";
        final String __EQ__ = "eq tests";

        System.out.println("SYS:------------------------------------------------");
        System.out.println("server (" + this.systemIP + ";" + this.systemPort + ")");
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
                System.out.println("Shorter CE: " + ce);
                ce = ceOptAsrep.optimizeCE(ce.getInput(), hyp);
                System.out.println("New Prefix CE: " + ce);
                ce = ceOptPref.optimizeCE(ce.getInput(), hyp);
                System.out.println("Prefix of CE is CE: " + ce);
            }

            ceLengthsShortened.add(ce.getInput().length());

            Word<PSymbolInstance> sysTrace = back.trace(ce.getInput());
            System.out.println("### SYS TRACE: " + sysTrace);

            SimulatorSUL hypSul = new SimulatorSUL(hyp, teachers, new Constants());
            IOOracle iosul = new SULOracle(hypSul, SpecialSymbols.ERROR);//, () -> new ValueCanonizer(this.teachers));        
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
