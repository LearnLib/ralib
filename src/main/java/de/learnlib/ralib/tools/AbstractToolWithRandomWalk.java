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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;

import de.learnlib.logging.Category;
import de.learnlib.logging.filter.CategoryFilter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SumConstantGenerator;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOCacheManager;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.ConstraintSolverFactory;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.tools.config.ConfigurationOption.BooleanOption;
import de.learnlib.ralib.tools.theories.SumCTheory;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.commons.util.Pair;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public abstract class AbstractToolWithRandomWalk implements RaLibTool {

	protected static final ConfigurationOption.LongOption OPTION_RANDOM_SEED = new ConfigurationOption.LongOption(
			"random.seed", "Seed for RNG", 0L, true);

	protected static final ConfigurationOption<Level> OPTION_LOGGING_LEVEL = new ConfigurationOption<Level>(
			"logging.level", "Log Level", Level.INFO, true) {

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

	protected static final ConfigurationOption<EnumSet<Category>> OPTION_LOGGING_CATEGORY = new ConfigurationOption<EnumSet<Category>>(
			"logging.category", "Log category", EnumSet.allOf(Category.class), true) {

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
			Category category = Category.valueOf(n);
			if (category == null)
				throw new ConfigurationException("can not parse " + this.getKey() + ": " + n);
			return category;
		}
	};

	protected static final ConfigurationOption.StringOption OPTION_LOGGING_FILE = new ConfigurationOption.StringOption(
			"logging.file", "Logg to a file", null, true);

	protected static final ConfigurationOption.BooleanOption OPTION_USE_RWALK = new ConfigurationOption.BooleanOption(
			"use.rwalk",
			"Use random walk for finding counterexamples. " + "This will override any ces produced by the eq test",
			Boolean.FALSE, true);

	protected static final ConfigurationOption.BooleanOption OPTION_USE_CEOPT = new ConfigurationOption.BooleanOption(
			"use.ceopt", "Use counterexample optimizers", Boolean.FALSE, true);

	protected static final ConfigurationOption.BooleanOption OPTION_USE_FRESH_VALUES = new ConfigurationOption.BooleanOption(
			"use.fresh", "Allow fresh values in output", Boolean.FALSE, true);

	protected static final ConfigurationOption.BooleanOption OPTION_USE_SUFFIXOPT = new ConfigurationOption.BooleanOption(
			"use.suffixopt", "Do only use fresh values for non-free suffix values", Boolean.FALSE, true);

	protected static final ConfigurationOption.IntegerOption OPTION_SUL_INSTANCES = new ConfigurationOption.IntegerOption(
			"sul.instances",
			"Number of sul instances to be run in parallel by tree queries. If set to more than 1, enables concurrent processing"
					+ "on all theories. The SUL must be fork-able. Forking is the way by which separate independent SUL instances are"
					+ "generated. ",
			1, true);

	protected static final ConfigurationOption.BooleanOption OPTION_EXPORT_MODEL = new ConfigurationOption.BooleanOption(
			"export.model", "Export final model to model.xml", Boolean.FALSE, true);

	protected static final ConfigurationOption.StringOption OPTION_CACHE_EXCLUDE = new ConfigurationOption.StringOption(
			"cache.exclude", "Excludes the specified traces after loading the cache. ", null, true);

	protected static final ConfigurationOption.StringOption OPTION_CACHE_DUMP = new ConfigurationOption.StringOption(
			"cache.dump", "Dump cache to file", null, true);

	protected static final ConfigurationOption.StringOption OPTION_CACHE_LOAD = new ConfigurationOption.StringOption(
			"cache.load", "Load cache from file if file exists", null, true);

	protected static final ConfigurationOption.StringOption OPTION_CACHE_SYSTEM = new ConfigurationOption.StringOption(
			"cache.system", "The type of caching employed: serialize|mock", "serialize", true);

	protected static final BooleanOption OPTION_CACHE_TESTS = new ConfigurationOption.BooleanOption("cache.tests",
			"Are tests cached as well?", true, true);

	protected static final ConfigurationOption.LongOption OPTION_TIMEOUT = new ConfigurationOption.LongOption(
			"max.time.millis", "Maximal run time for experiment in milliseconds", -1L, true);

	protected static final ConfigurationOption.IntegerOption OPTION_MAX_ROUNDS = new ConfigurationOption.IntegerOption(
			"max.rounds", "Maximum number of rounds", -1, true);

	protected static final ConfigurationOption.StringOption OPTION_TEST_TRACES = new ConfigurationOption.StringOption(
			"test.traces", "Test traces format: test1; test2; ...", null, true);

	protected static final ConfigurationOption.StringOption OPTION_TEACHERS = new ConfigurationOption.StringOption(
			"teachers", "Teachers. Format: type:class + type:class + ...", null, false);

	protected static final ConfigurationOption.StringOption OPTION_SOLVER = new ConfigurationOption.StringOption(
			"solver", "Constraints Solver. Options: " + ConstraintSolverFactory.ID_SIMPLE + ", "
					+ ConstraintSolverFactory.ID_Z3 + ".",
			ConstraintSolverFactory.ID_SIMPLE, true);

	protected static final ConfigurationOption.StringOption OPTION_CONSTANTS = new ConfigurationOption.StringOption(
			"constants", "Regular constants of form [{\"type\":typeA,\"value\":\"valueA\"}, ...] ", null, true);

	protected static final ConfigurationOption.StringOption OPTION_CONSTANTS_OUTPUT = new ConfigurationOption.StringOption(
			"constants.output", "Output constants of form [{\"type\":typeA,\"value\":\"valueA\"}, ...] ", null, true);

	protected static final ConfigurationOption.StringOption OPTION_CONSTANTS_SUMC = new ConfigurationOption.StringOption(
			"constants.sumc", "SumC constants of form [{\"type\":typeA,\"value\":\"valueA\"}, ...] ", null, true);

	protected Random random = null;

	protected boolean useCeOptimizers;

	protected boolean findCounterexamples;

	protected boolean useSuffixOpt;

	protected int maxRounds = -1;

	protected long timeoutMillis = -1L;

	protected boolean exportModel = false;

	protected boolean useFresh = false;

	protected final Map<String, TypedTheory> teacherClasses = new HashMap<>();

	protected ConstraintSolver solver;

	@Override
	public void setup(Configuration config) throws ConfigurationException {

		config.list(System.out);

		// logging
		Logger root = Logger.getLogger("");
		String file = OPTION_LOGGING_FILE.parse(config);
		if (file != null) {
			FileHandler fh = null;
			try {
				fh = new FileHandler(file);
			} catch (Exception e) {
				throw new ConfigurationException(e.getMessage());
			}
			fh.setFormatter(new SimpleFormatter());
			root.addHandler(fh);
		}
		Level lvl = OPTION_LOGGING_LEVEL.parse(config);
		root.setLevel(lvl);
		EnumSet<Category> cat = OPTION_LOGGING_CATEGORY.parse(config);
		for (Handler h : root.getHandlers()) {
			h.setLevel(lvl);
			h.setFilter(new CategoryFilter(cat));
		}

		// random
		Long seed = (new Random()).nextLong();
		if (config.containsKey(OPTION_RANDOM_SEED.getKey())) {
			seed = OPTION_RANDOM_SEED.parse(config);
		}
		System.out.println("RANDOM SEED=" + seed);
		this.random = new Random(seed);
		config.setProperty("__seed", "" + seed);

		this.useCeOptimizers = OPTION_USE_CEOPT.parse(config);
		this.findCounterexamples = OPTION_USE_RWALK.parse(config);
		this.maxRounds = OPTION_MAX_ROUNDS.parse(config);
		this.useSuffixOpt = OPTION_USE_SUFFIXOPT.parse(config);
		this.timeoutMillis = OPTION_TIMEOUT.parse(config);
		this.exportModel = OPTION_EXPORT_MODEL.parse(config);
		this.useFresh = OPTION_USE_FRESH_VALUES.parse(config);

		this.teacherClasses.putAll(buildTeachersFromConfig(config));

		this.solver = ConstraintSolverFactory.createSolver(OPTION_SOLVER.parse(config));
	}

	protected Map<String, TypedTheory> buildTeachersFromConfig(Configuration config) throws ConfigurationException {
		Map<String, TypedTheory> teacherClasses = new HashMap<String, TypedTheory>();
		String[] parsed = OPTION_TEACHERS.parse(config).split("\\+");
		for (String s : parsed) {
			Pair<String, TypedTheory> pair = parseTeacherConfig(s);
			teacherClasses.put(pair.getFirst(), pair.getSecond());
		}

		return teacherClasses;
	}

	/**
	 * Builds a mapping from types to the theories that handle them and sets on
	 * each teacher the type handled, as well as other configuration options.
	 * 
	 * @throws ConfigurationException
	 */
	protected final Map<DataType, Theory> buildTypeTheoryMapAndConfigureTheories(
			Map<String, TypedTheory> teacherClasses, Configuration configuration, Map<String, DataType> types,
			Constants constants) throws ConfigurationException {
		Map<DataType, Theory> teachers = new LinkedHashMap<>();
		for (String tName : teacherClasses.keySet()) {
			DataType t = types.get(tName);
			TypedTheory theory = teacherClasses.get(t.getName());
			teachers.put(t, theory);
		}
		configureTheories(teachers, configuration, types, constants);
		return teachers;
	}

	protected final void configureTheories(Map<DataType, Theory> teachers, Configuration config,
			Map<String, DataType> types, Constants constants) throws ConfigurationException {
		teachers.forEach((type, teach) -> {
			TypedTheory typedTheory = (TypedTheory) teach;
			typedTheory.setCheckForFreshOutputs(this.useFresh);
			typedTheory.setUseSuffixOpt(this.useSuffixOpt);
			typedTheory.setType(type);
		});
		applyCustomTeacherSettings(teachers, config, types, constants);
	}

	private void applyCustomTeacherSettings(Map<DataType, Theory> teachers, Configuration config,
			Map<String, DataType> types, Constants consts) throws ConfigurationException {
		Theory[] sumCTheories = teachers.values().stream().filter(th -> th instanceof SumCTheory)
				.toArray(Theory[]::new);
		if (sumCTheories.length > 0) {
			SumConstants sumConstants = new SumConstants();
			String sumcString = OPTION_CONSTANTS_SUMC.parse(config);
			if (sumcString != null) {
				DataValue<?>[] cstArray = parseDataValues(sumcString, types);
				final SumConstantGenerator cgen = new SymbolicDataValueGenerator.SumConstantGenerator();
				Arrays.stream(cstArray).forEach(cst -> sumConstants.put(cgen.next(cst.getType()), cst));
			}
			consts.setSumC(sumConstants);

			for (Theory teacher : sumCTheories) {
				((SumCTheory) teacher).setConstants(consts);
			}
		}

		//
		if (OPTION_SUL_INSTANCES.parse(config) > 1) {
			teachers.values().forEach(th -> th.enableConcurrentProcessing());
		}
	}

	private Pair<String, TypedTheory> parseTeacherConfig(String config) throws ConfigurationException {
		try {
			String[] parts = config.trim().split(":");
			Class<?> cl = Class.forName(parts[1].trim());

			TypedTheory th = (TypedTheory) cl.newInstance();
			String t = parts[0].trim();
			// Do this later !!!

			return new Pair<String, TypedTheory>(t, th);

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
			throw new ConfigurationException(ex.getMessage());
		}
	}

	protected DataValue[] parseDataValues(String gsonDataValueArray, Map<String, DataType> typeMap) {
		Gson gson = new Gson();
		GsonDataValue[] gDvs = gson.fromJson(gsonDataValueArray, GsonDataValue[].class);
		DataValue[] dataValues = new DataValue[gDvs.length];
		for (int i = 0; i < gDvs.length; i++) {
			DataType type = typeMap.get(gDvs[i].type);
			dataValues[i] = gDvs[i].toDataValue(type);
		}

		return dataValues;
	}

	static class GsonDataValue {
		public String type;
		public String value;

		public <T> DataValue<T> toDataValue(DataType<T> type) {
			if (type.getName().compareTo(this.type) != 0) {
				throw new RuntimeException("Type name mismatch");
			}
			DataValue<T> dv = DataValue.valueOf(value, type);

			return dv;
		}
	}

	protected IOCache setupCache(Configuration config, ParameterizedSymbol[] alphabet, Map<DataType, Theory> teachers,
			IOCacheManager cacheMgr, Constants consts) throws ConfigurationException {
		IOCache ioCache = null;
		String load = OPTION_CACHE_LOAD.parse(config);

		if (load != null && new File(load).exists()) {
			try {
				ioCache = cacheMgr.loadCacheFromFile(load, consts);
				String excludedTraces = OPTION_CACHE_EXCLUDE.parse(config);
				if (excludedTraces != null) {
					List<Word<PSymbolInstance>> traces = getCanonizedWordsFromString(excludedTraces, alphabet, teachers,
							consts);
					System.out.println("Cache Size: " + ioCache.getSize());
					for (Word<PSymbolInstance> trace : traces) {
						ioCache = ioCache.getCacheExcluding(trace);
						System.out.println("Cache Size After Excluding " + trace + " : " + ioCache.getSize());
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
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
						cacheMgr.dumpCacheToFile(dump, finalCache, consts);
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			}));
		}
		return ioCache;
	}

	/**
	 * Collects all configuration options from classes
	 */
	public static ConfigurationOption[] getOptions(Class<?>... classes) {
		Stream<ConfigurationOption> arrayStream = Stream.empty();
		for (Class<?> cls : classes) {
			arrayStream = Stream.concat(arrayStream, Arrays.stream(cls.getDeclaredFields())
					.filter(f -> ConfigurationOption.class.isAssignableFrom(f.getType())).map(f -> {
						ConfigurationOption ret = null;
						boolean acc = f.isAccessible();
						f.setAccessible(true);
						try {
							ret = ((ConfigurationOption) f.get(EquivalenceOracleFactory.class));
						} catch (Exception e) {
							e.printStackTrace();
						}
						f.setAccessible(acc);
						return ret;
					}));
		}
		return arrayStream.toArray(ConfigurationOption[]::new);
	}

	protected List<Word<PSymbolInstance>> getCanonizedWordsFromString(String debugString,
			ParameterizedSymbol[] alphabet, Map<DataType, Theory> teachers, Constants consts) {
		List<Word<PSymbolInstance>> tests = getWordsFromString(debugString, alphabet);
		SymbolicTraceCanonizer canonizer = new SymbolicTraceCanonizer(teachers, consts);
		List<Word<PSymbolInstance>> canonizedTests = tests.stream().map(w -> canonizer.canonizeTrace(w))
				.collect(Collectors.toList());
		return canonizedTests;
	}

	protected List<Word<PSymbolInstance>> getWordsFromString(String debugString, ParameterizedSymbol[] alphabet) {
		List<String> testStrings = Arrays.stream(debugString.split(";")).collect(Collectors.toList());
		List<Word<PSymbolInstance>> tests = new TraceParser(testStrings, Arrays.asList(alphabet)).getTraces();
		return tests;
	}

}
