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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.learnlib.logging.Category;
import de.learnlib.logging.filter.CategoryFilter;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.ConstraintSolverFactory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import net.automatalib.commons.util.Pair;

/**
 *
 * @author falk
 */
public abstract class AbstractToolWithRandomWalk implements RaLibTool {
    public static final String LEARNER_SLLAMBDA = "sllambda";
    public static final String LEARNER_SLSTAR = "slstar";
    public static final String LEARNER_RADT = "sldt";
    public static final String RATTT_CEANALYSIS_SUFFIX = "suffix";
    public static final String RATTT_CEANALYSIS_PREFIX = "prefix";


    protected static final ConfigurationOption.StringOption OPTION_LEARNER
            = new ConfigurationOption.StringOption("learner",
            "Learning Algorithm: slstar (default) or rattt", LEARNER_SLSTAR, true);

    protected static final ConfigurationOption.LongOption OPTION_RANDOM_SEED
            = new ConfigurationOption.LongOption("random.seed", "Seed for RNG", 0L, true);

    protected static final ConfigurationOption<Level> OPTION_LOGGING_LEVEL
            = new ConfigurationOption<Level>("logging.level", "Log Level", Level.INFO, true) {

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

    protected static final ConfigurationOption<EnumSet<Category>> OPTION_LOGGING_CATEGORY
            = new ConfigurationOption<EnumSet<Category>>("logging.category", "Log category",
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
                        case "CONFIG":
                            return Category.CONFIG;
                        case "COUNTEREXAMPLE":
                            return Category.COUNTEREXAMPLE;
                        case "DATASTRUCTURE":
                            return Category.DATASTRUCTURE;
                        case "EVENT":
                            return Category.EVENT;
                        case "MODEL":
                            return Category.MODEL;
                        case "PHASE":
                            return Category.PHASE;
                        case "PROFILING":
                            return Category.PROFILING;
                        case "QUERY":
                            return Category.QUERY;
                        case "STATISTIC":
                            return Category.STATISTIC;
                        case "SYSTEM":
                            return Category.SYSTEM;
                    }
                    throw new ConfigurationException("can not parse " + this.getKey() + ": " + n);
                }
            };

    protected static final ConfigurationOption.BooleanOption OPTION_USE_RWALK
            = new ConfigurationOption.BooleanOption("use.rwalk",
                    "Use random walk for finding counterexamples. "
                    + "This will override any ces produced by the eq test", Boolean.FALSE, true);

    protected static final ConfigurationOption.BooleanOption OPTION_USE_CEOPT
            = new ConfigurationOption.BooleanOption("use.ceopt",
                    "Use counterexample optimizers", Boolean.FALSE, true);

    protected static final ConfigurationOption.BooleanOption OPTION_USE_FRESH_VALUES
            = new ConfigurationOption.BooleanOption("use.fresh",
                    "Allow fresh values in output", Boolean.FALSE, true);

    protected static final ConfigurationOption.BooleanOption OPTION_EXPORT_MODEL
            = new ConfigurationOption.BooleanOption("export.model",
                    "Export final model to model.xml", Boolean.FALSE, true);

    protected static final ConfigurationOption.BooleanOption OPTION_USE_SUFFIXOPT
            = new ConfigurationOption.BooleanOption("use.suffixopt",
                    "Do only use fresh values for non-free suffix values", Boolean.FALSE, true);

    protected static final ConfigurationOption.LongOption OPTION_TIMEOUT
            = new ConfigurationOption.LongOption("max.time.millis",
                    "Maximal run time for experiment in milliseconds", -1L, true);

    protected static final ConfigurationOption.IntegerOption OPTION_MAX_ROUNDS
            = new ConfigurationOption.IntegerOption("max.rounds",
                    "Maximum number of rounds", -1, true);

    protected static final ConfigurationOption.BooleanOption OPTION_CACHE_TESTS
    = new ConfigurationOption.BooleanOption("cache.tests",
            "Cache test queries executed during CE analysis", false, true);

    protected static final ConfigurationOption.BooleanOption OPTION_RWALK_DRAW
            = new ConfigurationOption.BooleanOption("rwalk.draw.uniform",
                    "Draw next input uniformly", null, false);

    protected static final ConfigurationOption.BooleanOption OPTION_RWALK_RESET
            = new ConfigurationOption.BooleanOption("rwalk.reset.count",
                    "Reset limit counters after each counterexample", null, false);

    protected static final ConfigurationOption.DoubleOption OPTION_RWALK_RESET_PROB
            = new ConfigurationOption.DoubleOption("rwalk.prob.reset",
                    "Probability of performing reset instead of step", null, false);

    protected static final ConfigurationOption.DoubleOption OPTION_RWALK_FRESH_PROB
            = new ConfigurationOption.DoubleOption("rwalk.prob.fresh",
                    "Probability of using a fresh data value", null, false);

    protected static final ConfigurationOption.LongOption OPTION_RWALK_MAX_RUNS
            = new ConfigurationOption.LongOption("rwalk.max.runs",
                    "Maximum number of random walks", null, false);

    protected static final ConfigurationOption.IntegerOption OPTION_RWALK_MAX_DEPTH
            = new ConfigurationOption.IntegerOption("rwalk.max.depth",
                    "Maximum length of each random walk", null, false);

    protected static final ConfigurationOption.StringOption OPTION_TEACHERS
            = new ConfigurationOption.StringOption("teachers",
                    "Teachers. Format: type:class + type:class + ...", null, false);

    protected static final ConfigurationOption.StringOption OPTION_SOLVER
            = new  ConfigurationOption.StringOption("solver",
                    "Constraints Solver. Options: " + ConstraintSolverFactory.ID_SIMPLE +
                            ", " + ConstraintSolverFactory.ID_Z3 + ".",
                            ConstraintSolverFactory.ID_SIMPLE, true);

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

    protected String learner = "slstar";

    private static final Random RANDOM = new Random();

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

        // random
        Long seed = RANDOM.nextLong();
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

        this.learner = OPTION_LEARNER.parse(config);

        String[] parsed = OPTION_TEACHERS.parse(config).split("\\+");
        for (String s : parsed) {
            Pair<String, TypedTheory> pair = parseTeacherConfig(s);
            teacherClasses.put(pair.getFirst(), pair.getSecond());
        }

        this.solver = ConstraintSolverFactory.createSolver(
                OPTION_SOLVER.parse(config));
    }

    private Pair<String, TypedTheory> parseTeacherConfig(String config)
            throws ConfigurationException {
        try {
            String[] parts = config.trim().split(":");
            Class<?> cl = Class.forName(parts[1].trim());

            TypedTheory th = (TypedTheory) cl.newInstance();
            String t = parts[0].trim();
            // Do this later !!!
            // th.setType(t);
            th.setUseSuffixOpt(this.useSuffixOpt);

            return new Pair<String, TypedTheory>(t, th);

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
    }
}
