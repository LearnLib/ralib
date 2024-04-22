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

import static de.learnlib.ralib.tools.AbstractToolWithRandomWalk.OPTION_LOGGING_LEVEL;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonExporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaLearningAlgorithm;
import de.learnlib.ralib.learning.ralambda.RaDT;
import de.learnlib.ralib.learning.ralambda.RaLambda;
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
import de.learnlib.ralib.tools.classanalyzer.ClasssAnalyzerDataWordSUL;
import de.learnlib.ralib.tools.classanalyzer.MethodConfig;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import de.learnlib.util.statistic.SimpleProfiler;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class ClassAnalyzer extends AbstractToolWithRandomWalk {

    private static final ConfigurationOption.StringOption OPTION_TARGET
            = new ConfigurationOption.StringOption("target",
                    "target class name", null, false);

    private static final ConfigurationOption.StringOption OPTION_METHODS
            = new ConfigurationOption.StringOption("methods",
                    "target method signatures. format: m1(class:type,class:type)class:type + m2() + ...", null, false);

    protected static final ConfigurationOption.IntegerOption OPTION_MAX_DEPTH
            = new ConfigurationOption.IntegerOption("max.depth",
                    "Maximum depth to explore", -1, true);

    private static final ConfigurationOption[] OPTIONS = new ConfigurationOption[]{
        OPTION_LEARNER,
        OPTION_LOGGING_LEVEL,
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
        OPTION_RWALK_SEED_TRANSITIONS
    };

    private DataWordSUL sulLearn;

    private DataWordSUL sulTest;

    private DataWordSUL trackingSulTest;

    private IORandomWalk randomWalk = null;

    private RaLearningAlgorithm rastar;

    private IOCounterexampleLoopRemover ceOptLoops;

    private IOCounterExamplePrefixReplacer ceOptAsrep;

    private IOCounterExamplePrefixFinder ceOptPref;

    private IOOracle back;

    private Map<DataType, Theory> teachers;

    private final Constants consts = new Constants();

    private Class<?> target = null;

    private final Map<String, DataType> types = new LinkedHashMap<>();

    private final Map<ParameterizedSymbol, MethodConfig> methods = new LinkedHashMap<>();

    private long resets = 0;
    private long inputs = 0;

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

            String[] mcStrings = OPTION_METHODS.parse(config).split("\\+");
            for (String mcs : mcStrings) {
                MethodConfig mc = new MethodConfig(mcs, this.target, this.types);
                this.methods.put(mc.getInput(), mc);
                inList.add(mc.getInput());
                actList.add(mc.getInput());
                if (!mc.isVoid() && !mc.getRetType().equals(SpecialSymbols.BOOLEAN_TYPE)) {
                    actList.add(mc.getOutput());
                }
            }

            Integer md = OPTION_MAX_DEPTH.parse(config);

            sulLearn = new ClasssAnalyzerDataWordSUL(target, methods, md);
            if (this.timeoutMillis > 0L) {
                this.sulLearn = new TimeOutSUL(this.sulLearn, this.timeoutMillis);
            }

            ParameterizedSymbol[] inputSymbols = inList.toArray(new ParameterizedSymbol[]{});

            actList.add(SpecialSymbols.ERROR);
            actList.add(SpecialSymbols.NULL);
            actList.add(SpecialSymbols.VOID);
            actList.add(SpecialSymbols.TRUE);
            actList.add(SpecialSymbols.FALSE);
            actList.add(SpecialSymbols.DEPTH);
            ParameterizedSymbol[] actions = actList.toArray(new ParameterizedSymbol[]{});

//            final Constants consts = new Constants();

            String cstString = OPTION_CONSTANTS.parse(config);
            if (cstString != null) {
            	final SymbolicDataValueGenerator.ConstantGenerator cgen = new SymbolicDataValueGenerator.ConstantGenerator();
            	DataValue<?>[] cstArray = super.parseDataValues(cstString, types);
            	Arrays.stream(cstArray).forEach(c -> consts.put(cgen.next(c.getType()), c));
            }

            // create teachers
            teachers = new LinkedHashMap<DataType, Theory>();
            // create teachers
            for (String tName : teacherClasses.keySet()) {
                DataType t = types.get(tName);
                TypedTheory theory = teacherClasses.get(t.getName());
                theory.setType(t);
                if (this.useSuffixOpt) {
                    theory.setUseSuffixOpt(this.useSuffixOpt);
                }
                teachers.put(t, theory);
            }

            back = new SULOracle(sulLearn, SpecialSymbols.ERROR);
            IOCache ioCache = new IOCache(back);
            IOFilter ioOracle = new IOFilter(ioCache, inputSymbols);

            sulTest = new ClasssAnalyzerDataWordSUL(target, methods, md);
            trackingSulTest = sulTest;
            if (this.timeoutMillis > 0L) {
                this.sulTest = new TimeOutSUL(this.sulTest, this.timeoutMillis);
            }

            //TODO add gathering of statistics
            if (OPTION_CACHE_TESTS.parse(config)) {
                SULOracle testBack = new SULOracle(sulTest,  SpecialSymbols.ERROR);
                IOCache testCache = new IOCache(testBack, ioCache);
                this.sulTest = new CachingSUL(trackingSulTest, testCache);
            }


            if (useFresh) {
                for (Theory t : teachers.values()) {
                    ((TypedTheory) t).setCheckForFreshOutputs(true, ioCache);
                }
            }

            MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioOracle, teachers, consts, solver);
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

            //this.rastar = new RaStar(mto, hypFactory, mlo, consts, true, actions);

            switch (this.learner) {
                case AbstractToolWithRandomWalk.LEARNER_SLSTAR:
                    this.rastar = new RaStar(mto, hypFactory, mlo, consts, true, actions);
                    break;
                case AbstractToolWithRandomWalk.LEARNER_SLLAMBDA:
                    this.rastar = new RaLambda(mto, hypFactory, mlo, consts, true, actions);
                    ((RaLambda)this.rastar).setSolver(solver);
                    break;
                case AbstractToolWithRandomWalk.LEARNER_RADT:
                    this.rastar = new RaDT(mto, hypFactory, mlo, consts, true, actions);
                    break;
                default:
                    throw new ConfigurationException("Unknown Learning algorithm: " + this.learner);
            }

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

                this.randomWalk.setError(SpecialSymbols.ERROR);
            }

            this.ceOptLoops = new IOCounterexampleLoopRemover(back);
            this.ceOptAsrep = new IOCounterExamplePrefixReplacer(back);
            this.ceOptPref = new IOCounterExamplePrefixFinder(back);

        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            ex.printStackTrace();
            throw new ConfigurationException(ex.getMessage());
        }

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
        while (maxRounds < 0 || rounds < maxRounds) {

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

            resets = trackingSulTest.getResets();
            inputs = trackingSulTest.getInputs();

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

            SimulatorSUL hypSul = new SimulatorSUL(hyp, teachers, consts);
            IOOracle iosul = new SULOracle(hypSul, SpecialSymbols.ERROR);
            Word<PSymbolInstance> hypTrace = iosul.trace(ce.getInput());
            System.out.println("### HYP TRACE: " + hypTrace);

            assert !hypTrace.equals(sysTrace);
            rastar.addCounterexample(ce);
        }

        System.out.println("=============================== STOP ===============================");
        System.out.println("FINAL HYP:------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");
        SimpleProfiler.logResults();

        System.out.println("ce lengths (original): "
                + Arrays.toString(ceLengths.toArray()));

        if (useCeOptimizers) {
            System.out.println("ce lengths (shortened): "
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
