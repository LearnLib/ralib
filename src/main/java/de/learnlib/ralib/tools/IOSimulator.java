/*
 * Copyright (C) 2014-2026 The LearnLib Contributors
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonExporter;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.*;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.MeasuringOracle;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.RaLearningAlgorithm;
import de.learnlib.ralib.learning.ralambda.SLCT;
import de.learnlib.ralib.learning.ralambda.SLLambda;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.CachingSUL;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import de.learnlib.util.statistic.SimpleProfiler;

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
        OPTION_LEARNER,
        OPTION_LOGGING_LEVEL,
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
        OPTION_RWALK_RESET,
        OPTION_RWALK_SEED_TRANSITIONS
    };

    private RegisterAutomaton model;

    private DataWordSUL sulLearn;

    private DataWordSUL sulTest;

    private IORandomWalk randomWalk = null;

    private IOEquivalenceTest eqTest;

    private RaLearningAlgorithm rastar;

    private IOCounterexampleLoopRemover ceOptLoops;

    private IOCounterExamplePrefixReplacer ceOptAsrep;

    private IOCounterExamplePrefixFinder ceOptPref;

    private boolean useEqTest;

//    private long resets = 0;
//    private long inputs = 0;

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

        final ParameterizedSymbol ERROR = new OutputSymbol("_io_err");

        IOOracle back = new SULOracle(sulLearn, ERROR);
        IOCache ioCache = new IOCache(back);
        IOFilter ioOracle = new IOFilter(ioCache, inputSymbols);

        this.sulTest = new SimulatorSUL(model, teachers, consts);
        if (this.timeoutMillis > 0L) {
            this.sulTest = new TimeOutSUL(this.sulTest, this.timeoutMillis);
        }

        DataWordSUL trackingSULTest = this.sulTest;

        if (OPTION_CACHE_TESTS.parse(config)) {
            SULOracle testBack = new SULOracle(sulTest, ERROR);
            IOCache testCache = new IOCache(testBack, ioCache);
            this.sulTest = new CachingSUL(trackingSULTest, testCache);
        }

        if (useFresh) {
            for (Theory t : teachers.values()) {
                ((TypedTheory) t).setCheckForFreshOutputs(true, ioCache);
            }
        }

        Measurements measurements = new Measurements();
        MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(ioOracle, teachers, consts, solver), measurements);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

        final long timeout = this.timeoutMillis;
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

        switch (this.learner) {
            case AbstractToolWithRandomWalk.LEARNER_SLSTAR:
                this.rastar = new RaStar(mto, hypFactory, mlo, consts, true, actions);
                break;
            case AbstractToolWithRandomWalk.LEARNER_SLLAMBDA:
                this.rastar = new SLLambda(mto, teachers, consts, true, solver, actions);
                break;
            case AbstractToolWithRandomWalk.LEARNER_RADT:
            	this.rastar = new SLCT(mto, hypFactory, mlo, consts, true, solver, actions);
            	break;
            default:
                throw new ConfigurationException("Unknown Learning algorithm: " + this.learner);
        }
        QueryStatistics queryStats = new QueryStatistics(measurements, sulLearn, trackingSULTest);
        this.rastar.setStatisticCounter(queryStats);

        this.eqTest = new IOEquivalenceTest(model, teachers, consts, true, actions);

        this.useEqTest = OPTION_USE_EQTEST.parse(config);

        if (findCounterexamples) {
            boolean drawUniformly = OPTION_RWALK_DRAW.parse(config);
            double resetProbabilty = OPTION_RWALK_RESET_PROB.parse(config);
            double freshProbability = OPTION_RWALK_FRESH_PROB.parse(config);
            long maxTestRuns = OPTION_RWALK_MAX_RUNS.parse(config);
            int maxDepth = OPTION_RWALK_MAX_DEPTH.parse(config);
            boolean resetRuns = OPTION_RWALK_RESET.parse(config);
            boolean seedTransitions = OPTION_RWALK_SEED_TRANSITIONS.parse(config);

            this.randomWalk = new IORandomWalk(random,
                    sulTest,
                    drawUniformly, // do not draw symbols uniformly
                    resetProbabilty, // reset probability
                    freshProbability, // prob. of choosing a fresh data value
                    maxTestRuns, // 1000 runs
                    maxDepth, // max depth
                    consts,
                    resetRuns, // reset runs
                    seedTransitions,
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
        System.out.println("Sys. Locations: " + model.getStates().size());
        System.out.println("Sys. Transitions: " + model.getTransitions().size());
        System.out.println("Sys. Registers: " + model.getRegisters().size());
        System.out.println("Constants: " + consts.size());

        SimpleProfiler.start(__RUN__);
        SimpleProfiler.start(__LEARN__);

        boolean eqTestfoundCE = false;
        ArrayList<Integer> ceLengths = new ArrayList<>();
        ArrayList<Integer> ceLengthsShortened = new ArrayList<>();
        Hypothesis hyp = null;

        QueryStatistics queryStats = rastar.getQueryStatistics();

        int rounds = 0;
        while (maxRounds < 0 || rounds < maxRounds) {
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
                ce = this.eqTest.findCounterExample(hyp, new ArrayList<>());

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
                ce = null;
            }

            for (int i = 0; i < 3; i++) {

                DefaultQuery<PSymbolInstance, Boolean> ce2 = null;

                ce2 = (findCounterexamples ? this.randomWalk.findCounterExample(hyp, new ArrayList<>()) : ce);

                SimpleProfiler.stop(__SEARCH__);
                System.out.println("CE: " + ce2);
                if (ce2 == null) {
                    break;
                }

                if (useCeOptimizers) {
		    queryStats.setPhase(QueryStatistics.CE_OPTIMIZE);
                    ce2 = ceOptLoops.optimizeCE(ce2.getInput(), hyp);
                    System.out.println("Shorter CE: " + ce2);
                    ce2 = ceOptAsrep.optimizeCE(ce2.getInput(), hyp);
                    System.out.println("New Prefix CE: " + ce2);
                    ce2 = ceOptPref.optimizeCE(ce2.getInput(), hyp);
                    System.out.println("Prefix of CE is CE: " + ce2);
                    queryStats.setPhase(QueryStatistics.TESTING);
                }

 		if (ce == null || ce.getInput().length() > ce2.getInput().length()) {
		    ce = ce2;
		}
            }

            if (ce == null) {
                break;
            }

            SimpleProfiler.start(__LEARN__);
            //ceLengths.add(ce.getInput().length());

            //ceLengthsShortened.add(ce.getInput().length());

            assert model.accepts(ce.getInput());
            assert !hyp.accepts(ce.getInput());

            rastar.addCounterexample(ce);
        }

        System.out.println("=============================== STOP ===============================");
        SimpleProfiler.logResults();

        System.out.println("Learner: " + rastar.getClass().getSimpleName());

        for (Entry<DataType, Theory> e : teachers.entrySet()) {
            System.out.println("Theory: " + e.getKey() + " -> " + e.getValue().getClass().getName());
        }

        if (useEqTest) {
            System.out.println("Last EQ Test found a counterexample: " + eqTestfoundCE);
        }

        System.out.println("ce lengths (original): " + Arrays.toString(ceLengths.toArray()));

        if (useCeOptimizers) {
            System.out.println("ce lengths (shortened): " +
                    Arrays.toString(ceLengthsShortened.toArray()));
        }

        // model
        if (hyp != null) {
            System.out.println("Hyp. Locations: " + hyp.getStates().size());
            System.out.println("Hyp. Transitions: " + hyp.getTransitions().size());

            // input locations + transitions
            System.out.println("Hyp. Input Locations: " + hyp.getInputStates().size());
            System.out.println("Hyp. Input Transitions: " + hyp.getInputTransitions().size());

            System.out.println("Hyp. Registers: " + hyp.getRegisters().size());

            if (this.exportModel) {
                System.out.println("exporting model to model.xml");
                try {
                    FileOutputStream fso = new FileOutputStream("model.xml");
                    RegisterAutomatonExporter.write(hyp, consts, fso);

                } catch (FileNotFoundException ex) {
                    System.out.println("... export failed");
                }
            }
        }

        // statistics
        System.out.println(queryStats.toString());
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
