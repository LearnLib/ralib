package de.learnlib.ralib.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
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
import de.learnlib.ralib.equivalence.HypVerifier;
import de.learnlib.ralib.equivalence.IOCounterExampleLoopRemover;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterExampleRelationRemover;
import de.learnlib.ralib.equivalence.IOCounterExampleSingleTransitionRemover;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.equivalence.TracesEquivalenceOracle;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.CountingDataWordOracle;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.BasicIOCacheOracle;
import de.learnlib.ralib.oracles.io.CachingSUL;
import de.learnlib.ralib.oracles.io.DataWordIOOracle;
import de.learnlib.ralib.oracles.io.ExceptionHandlers;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOCacheManager;
import de.learnlib.ralib.oracles.io.CanonizingIOCacheOracle;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.CanonizingSULOracle;
import de.learnlib.ralib.sul.CountingDataWordSUL;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.DeterminizerDataWordSUL;
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
public abstract class ToolTemplate extends AbstractToolWithRandomWalk {

	private final ConfigurationOption<?>[] OPTIONS;

	protected static final ConfigurationOption.StringOption OPTION_DEBUG_TRACES = new ConfigurationOption.StringOption(
			"debug.traces",
			"Debug traces are run on the system at start with printing of the output, followed by exit. No learning is done."
					+ "Debug traces format: test1; test2; ...",
			null, true);

	protected static final ConfigurationOption.IntegerOption OPTION_DEBUG_REPEATS = new ConfigurationOption.IntegerOption(
			"debug.repeats", "Number of times a trace is executed." + "Non-negative number.", 1, true);

	protected static final ConfigurationOption.BooleanOption OPTION_STATS_CACHE = new ConfigurationOption.BooleanOption(
			"stats.cache", "Also counts cached data in stats .", false, true);

	protected static final ConfigurationOption.StringOption OPTION_DEBUG_SUFFIXES = new ConfigurationOption.StringOption(
			"debug.suffixes",
			"For the debug traces given, run the given suffixes exhaustively and exit. No learning is done."
					+ "Debug suffixes format: suff1; suff2; ...",
			null, true);

	protected static final ConfigurationOption.StringOption OPTION_SUL_FACTORY = new ConfigurationOption.StringOption(
			"sul.factory",
			"Provides a custom factory for generating SULs instead of the common one. The constructor should take in a SULParser type object",
			null, true);

	protected static final ConfigurationOption.LongOption OPTION_MAX_INPUTS = new ConfigurationOption.LongOption(
			"max.inputs", "Maximum number of inputs that can be run", null, true);

	private DataWordIOOracle learnOracle;
	private Map<DataType, Theory> teachers;
	private DataWordIOOracle ceAnalysisOracle;
	private IOOracle testOracle;
	private Counters counters;
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
	// the the former, stats also include cached data, makes sense only if a
	// cache is pre-loaded
	private boolean preCache;

	private Long maxInputs;

	public ToolTemplate(SULParser parser) throws ConfigurationException {
		OPTIONS = getOptions(parser.getClass(), this.getClass(), EquivalenceOracleFactory.class);
		this.sulParser = parser;
	}

	public void setup(Configuration config) throws ConfigurationException {
		super.setup(config);
		preCache = OPTION_STATS_CACHE.parse(config);
		sulParser.parseConfig(config);
		targetName = sulParser.targetName();
		types = sulParser.getTypes();
		maxInputs = OPTION_MAX_INPUTS.parse(config);

		final Constants consts = new Constants();

		String cstString = OPTION_CONSTANTS.parse(config);
		if (cstString != null) {
			final SymbolicDataValueGenerator.ConstantGenerator cgen = new SymbolicDataValueGenerator.ConstantGenerator();
			DataValue<?>[] cstArray = super.parseDataValues(cstString, types);
			Arrays.stream(cstArray).forEach(c -> consts.put(cgen.next(c.getType()), c));
		}
		constants = consts;

		teachers = super.buildTypeTheoryMapAndConfigureTheories(teacherClasses, config, types, sulParser.getInputs(),
				consts);
		String facString = OPTION_SUL_FACTORY.parse(config);
		if (facString == null) {
			sulFactory = sulParser.newSULFactory();
		} else {
			sulFactory = instantiateCustomFactory(facString, sulParser);
		}

		boolean determinize = useFresh;
		boolean handleExceptions = true;
		counters = new Counters();

		String debugTraces = OPTION_DEBUG_TRACES.parse(config);
		String debugSuffixes = OPTION_DEBUG_SUFFIXES.parse(config);
		if (debugTraces != null && debugSuffixes == null) {
			int repeats = OPTION_DEBUG_REPEATS.parse(config);
			IOOracle ioOracle = setupIOOracle(sulFactory, teachers, consts, counters.ceInput, determinize,
					timeoutMillis, handleExceptions);
			runDebugTracesAndExit(debugTraces, repeats, ioOracle, teachers, consts);
		}

		// boolean determinize, long timeoutMillis, boolean handleExceptions,
		// int sulInstances
		String cacheSystem = OPTION_CACHE_SYSTEM.parse(config);
		cacheManager = IOCacheManager.getCacheManager(cacheSystem);
		ioCache = setupCache(config, sulParser.getAlphabet(), teachers, cacheManager, consts);

		IOOracle learnIOOracle = setupIOOracle(sulFactory, teachers, consts, counters.learnerInput, determinize,
				timeoutMillis, handleExceptions);
		learnOracle = setupDataWordIOOracle(learnIOOracle, consts, ioCache, determinize, handleExceptions);
		learnOracle = new CountingDataWordOracle(learnOracle, counters.learnerQuery);

		IOOracle ceAnalysisIOOracle = setupIOOracle(sulFactory, teachers, consts, counters.ceInput, determinize,
				timeoutMillis, handleExceptions);
		ceAnalysisOracle = setupDataWordIOOracle(ceAnalysisIOOracle, consts, ioCache, determinize, handleExceptions);
		ceAnalysisOracle = new CountingDataWordOracle(ceAnalysisOracle, counters.ceQuery);
		sulReductionTraceOracle = setupDataWordIOOracle(ceAnalysisIOOracle, consts, ioCache, true, handleExceptions);

		IOFilter ioOracle = new IOFilter(learnOracle, sulParser.getInputs());
		TreeOracle mto = new MultiTheoryTreeOracle(ioOracle, learnOracle, teachers, consts, solver);
		if (debugSuffixes != null) {
			if (debugTraces == null) {
				System.err.println("No debug traces given");
				System.exit(0);
			} else {
				runDebugSuffixesAndExit(debugTraces, debugSuffixes, mto, teachers, consts);
			}
		}

		IOFilter ioCeOracle = new IOFilter(ceAnalysisOracle, sulParser.getInputs());
		TreeOracle ceMto = new MultiTheoryTreeOracle(ioCeOracle, ceAnalysisOracle, teachers, consts, solver);

		MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

		final long timeout = timeoutMillis;
		final Map<DataType, Theory> teach = teachers;
		TreeOracleFactory hypFactory = new TreeOracleFactory() {
			@Override
			public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
				DataWordOracle hypOracle = new SimulatorOracle(hyp);
				if (timeout > 0L) {
					hypOracle = new TimeOutOracle(hypOracle, timeout);
				}
				SimulatorSUL hypDataWordSimulation = new SimulatorSUL(hyp, teachers, consts);
				IOOracle hypTraceOracle = new BasicSULOracle(hypDataWordSimulation, SpecialSymbols.ERROR);

				return new MultiTheoryTreeOracle(hypOracle, hypTraceOracle, teachers, consts, solver);
			}
		};

		rastar = new RaStar(mto, ceMto, hypFactory, mlo, consts, true, teachers, solver, sulParser.getAlphabet());

		if (findCounterexamples) {
			testOracle = setupIOOracle(sulFactory, teachers, constants, counters.testInput, determinize, timeoutMillis,
					handleExceptions);
			boolean cacheTests = OPTION_CACHE_TESTS.parse(config);

			IOCache ioCache = preCache ? new IOCache() : this.ioCache;
			if (cacheTests) {
				if (determinize) {
					testOracle = new CanonizingIOCacheOracle(testOracle, ioCache);
				} else {
					testOracle = new BasicIOCacheOracle(testOracle, ioCache);
				}
			}
			
			if (handleExceptions)
				testOracle = ExceptionHandlers.wrapIOOracle(testOracle);
			equOracle = EquivalenceOracleFactory.buildEquivalenceOracle(config, testOracle, teach, consts, random,
					sulParser.getInputs());

			String ver = OPTION_TEST_TRACES.parse(config);
			if (ver != null) {
				List<Word<PSymbolInstance>> tests = getCanonizedWordsFromString(ver, sulParser.getAlphabet(),
						teachers, consts);
				traceTester = new TracesEquivalenceOracle(testOracle, teachers, consts, tests);
			}
		}
		
		HypVerifier hypVerifier = HypVerifier.getVerifier(true, teachers, consts);

		ceOptLoops = new IOCounterExampleLoopRemover(sulReductionTraceOracle, hypVerifier);
		ceOptAsrep = new IOCounterExamplePrefixReplacer(sulReductionTraceOracle, hypVerifier);
		ceOptPref = new IOCounterExamplePrefixFinder(sulReductionTraceOracle, hypVerifier);
		ceOptSTR = new IOCounterExampleSingleTransitionRemover(sulReductionTraceOracle, hypVerifier);
		ceOptRelation = new IOCounterExampleRelationRemover(teachers, consts, solver, sulReductionTraceOracle, hypVerifier);
	}

	private SULFactory instantiateCustomFactory(String facString, SULParser parser) throws ConfigurationException {
		SULFactory factory = null;
		try {
			Class<?> facCls = Class.forName(facString);
			try {
				factory = SULFactory.class.cast(facCls.getConstructor(SULParser.class).newInstance(parser));
			} catch (NoSuchMethodException meth) {
				factory = SULFactory.class.cast(facCls.newInstance());
			}
		} catch (Exception e) {
			throw new ConfigurationException(e.getMessage());
		}
		return factory;
	}

	private void runDebugTracesAndExit(String debug, int numRepeats, IOOracle ioOracle, Map<DataType, Theory> teachers,
			Constants consts) {
		List<Word<PSymbolInstance>> canonizedTests = getCanonizedWordsFromString(debug, sulParser.getAlphabet(),
				teachers, consts);
		List<Word<PSymbolInstance>> allTests = new ArrayList<>(canonizedTests.size() * numRepeats);
		for (int i = 0; i < numRepeats; i++) {
			allTests.addAll(canonizedTests);
		}
		List<Word<PSymbolInstance>> results = ioOracle.traces(allTests);
		System.out.println(StringUtils.join(results, "\n"));
		System.exit(0);
	}

	private void runDebugSuffixesAndExit(String debugPrefixes, String debugSuffixes, TreeOracle mto,
			Map<DataType, Theory> teachers, Constants constants) {
		List<String> suffixStrings = Arrays.stream(debugSuffixes.split(";")).collect(Collectors.toList());
		List<Word<PSymbolInstance>> prefixes = getCanonizedWordsFromString(debugPrefixes, sulParser.getAlphabet(),
				teachers, constants);
		List<GeneralizedSymbolicSuffix> suffixes = new SuffixParser(suffixStrings,
				Arrays.asList(sulParser.getAlphabet()), teachers).getSuffixes();

		for (GeneralizedSymbolicSuffix suffix : suffixes) {
			for (Word<PSymbolInstance> prefix : prefixes) {
				System.out.println(prefix + " " + suffix);
				TreeQueryResult tree = mto.treeQuery(prefix, suffix);
				System.out.println(tree.toString());
			}
		}
		System.exit(0);
	}

	private DataWordIOOracle setupDataWordIOOracle(IOOracle ioOracle, Constants consts, IOCache ioCache,
			boolean determinize, boolean handleExceptions) {
		DataWordIOOracle ioCacheOracle;
		ioCache = preCache ? new IOCache() : ioCache;
		if (determinize) {
			ioCacheOracle = new CanonizingIOCacheOracle(ioOracle, ioCache);
		} else {
			ioCacheOracle = new BasicIOCacheOracle(ioOracle, ioCache);
		}

		if (handleExceptions) {
			return ExceptionHandlers.wrapDataWordIOOracle(ioCacheOracle);
		} else {
			return ioCacheOracle;
		}
	}

	private IOOracle setupIOOracle(SULFactory sulFactory, Map<DataType, Theory> teachers, Constants consts,
			InputCounter inputCounter, boolean determinize, long timeoutMillis, boolean handleExceptions) {
		IOOracle ioOracle;

		DataWordSUL singleSUL = sulFactory.newSUL();
		DataWordSUL wrappedSUL = setupDataWordOracle(singleSUL, teachers, consts, inputCounter, determinize,
				timeoutMillis);
		if (determinize) {
			ioOracle = new CanonizingSULOracle(wrappedSUL, SpecialSymbols.ERROR,
					new SymbolicTraceCanonizer(teachers, consts));
		} else {
			ioOracle = new BasicSULOracle(wrappedSUL, SpecialSymbols.ERROR);
		}
		if (handleExceptions) {
			ioOracle = ExceptionHandlers.wrapIOOracle(ioOracle);
		}

		return ioOracle;
	}

	// could use a builder pattern here
	private DataWordSUL setupDataWordOracle(DataWordSUL sulInstance, Map<DataType, Theory> teachers, Constants consts,
			InputCounter inputCounter, boolean determinize, long timeoutMillis) {
		DataWordSUL sul = sulInstance;
		if (determinize) {
			sul = new DeterminizerDataWordSUL(teachers, consts, sul);
		}
		if (preCache) {
			sul = new CachingSUL(sul, ioCache);
		}
		if (maxInputs != null) {
			sul = new InputLimitOracle(sul, this.counters, maxInputs);
		}
		sul = new CountingDataWordSUL(sul, inputCounter);
		if (timeoutMillis > 0L) {
			sul = new TimeOutSUL(sul, timeoutMillis);
		}

		return sul;
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
			// try {
			// this.cacheManager.dumpCacheToFile(this.targetName.toLowerCase()+"-hyp"+rounds,
			// this.ioCache, constants);
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

			SimpleProfiler.stop(__LEARN__);
			SimpleProfiler.start(__EQ__);
			DefaultQuery<PSymbolInstance, Boolean> ce = null;

			SimpleProfiler.stop(__EQ__);
			SimpleProfiler.start(__SEARCH__);
			if (findCounterexamples) {
				ce = equOracle.findCounterExample(hyp, null);
				if (ce == null && traceTester != null) {
					ce = traceTester.findCounterExample(hyp, null);
				}
			}
			SimpleProfiler.stop(__SEARCH__);
			System.out.println("CE: " + ce);
			if (ce == null) {
				break;
			}

			resets = counters.testInput.getResets();
			inputs = counters.testInput.getInputs();

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

		System.out.println("ce lengths (oirginal): " + Arrays.toString(ceLengths.toArray()));

		if (useCeOptimizers) {
			System.out.println("ce lengths (shortend): " + Arrays.toString(ceLengthsShortened.toArray()));
		}
		if (traceTester != null) {
			DefaultQuery<PSymbolInstance, Boolean> ce = traceTester.findCounterExample(hyp, null);
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
				// try {
				// FileOutputStream fso = new FileOutputStream("model.xml");
				// RegisterAutomatonExporter.write(hyp, new Constants(), fso);
				// } catch (FileNotFoundException ex) {
				// System.out.println("... export failed");
				// }
				System.out.println("exporting model to model.dot");
				RAToDot dotExport = new RAToDot(hyp, true);
				String dot = dotExport.toString();

				try (BufferedWriter wr = new BufferedWriter(new FileWriter(new File("model.dot")))) {
					wr.write(dot, 0, dot.length());
				} catch (IOException ex) {
					System.out.println("... export failed");
				}
			}
		}

		counters.print(System.out);

		// + sums
		System.out.println("Resets: " + (resets + counters.learnerInput.getResets() + counters.ceInput.getResets()));
		System.out.println("Inputs: " + (inputs + counters.learnerInput.getInputs() + counters.ceInput.getInputs()));

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

		public Counters() {
			this.learnerInput = new InputCounter();
			this.ceInput = new InputCounter();
			this.testInput = new InputCounter();
			learnerQuery = new QueryCounter();
			ceQuery = new QueryCounter();
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					Counters.this.print(System.out);
				}
			}));
		}

		public long getTotalNumInputs() {
			return this.learnerInput.getInputs() + this.ceInput.getInputs() + this.testInput.getInputs();
		}

		public void print(PrintStream out) {
			Counters counters = this;
			// tests during learning
			// resets + inputs
			out.println("Resets Learning: " + counters.learnerInput.getResets());
			out.println("Inputs Learning: " + counters.learnerInput.getInputs());

			// tests during ce analysis
			// resets + inputs
			out.println("Resets Ce Analysis: " + counters.ceInput.getResets());
			out.println("Inputs Ce Analysis: " + counters.ceInput.getInputs());

			// tests during search
			// resets + inputs
			out.println("Resets Testing: " + counters.testInput.getResets());
			out.println("Inputs Testing: " + counters.testInput.getInputs());
		}
	}

}
