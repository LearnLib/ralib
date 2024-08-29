package de.learnlib.ralib.learning.ralambda;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.api.query.DefaultQuery;
import de.learnlib.ralib.RaLibLearningExperimentRunner;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.MeasuringOracle;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class LearnDtlsServerTest extends RaLibTestSuite {
    private RegisterAutomatonImporter loader;
    private ParameterizedSymbol[] inputs;
    private ParameterizedSymbol[] actions;
    private Constants consts;

    public LearnDtlsServerTest() {
        loader = TestUtil.getLoader("/de/learnlib/ralib/automata/xml/dtls-server.xml");
        inputs = loader.getInputs().toArray(new ParameterizedSymbol[] {});

        actions = loader.getActions().toArray(new ParameterizedSymbol[] {});

        consts = loader.getConstants();
    }

    private PSymbolInstance input(String name, Integer... params) {
        for (ParameterizedSymbol action : actions) {
            if (action instanceof InputSymbol && action.getName().equals(name)) {
                if (action.getPtypes().length != params.length) {
                    throw new RuntimeException(action.toString());
                }
                DataValue[] pVals = new DataValue[params.length];
                for (int i = 0; i < params.length; i++) {
                    pVals[i] = new DataValue(action.getPtypes()[i], params[i]);
                }
                return new PSymbolInstance(action, pVals);
            }
        }
        throw new RuntimeException(
                String.format("Could not find symbol %s. Available symbols %s", name, Arrays.toString(actions)));
    }

    private PSymbolInstance output(String name, Integer... params) {
        for (ParameterizedSymbol action : actions) {
            if (action instanceof OutputSymbol && action.getName().equals(name)) {
                if (action.getPtypes().length != params.length) {
                    throw new RuntimeException(action.toString());
                }
                DataValue[] pVals = new DataValue[params.length];
                for (int i = 0; i < params.length; i++) {
                    pVals[i] = new DataValue(action.getPtypes()[i], params[i]);
                }
                return new PSymbolInstance(action, pVals);
            }
        }
        throw new RuntimeException(
                String.format("Could not find symbol %s. Available symbols %s", name, Arrays.toString(actions)));
    }

    @Test(enabled = false)
    public void learnDtlsServerTest() {
        RegisterAutomatonImporter loader = TestUtil.getLoader("/de/learnlib/ralib/automata/xml/dtls-server.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(new ParameterizedSymbol[] {});

        ParameterizedSymbol[] actions = loader.getActions().toArray(new ParameterizedSymbol[] {});

        final Constants consts = loader.getConstants();

        DataType epoch = loader.getDataTypes().iterator().next();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(epoch, new IntegerEqualityTheory(epoch));
        SimulatorOracle dwOracle = new SimulatorOracle(model);

        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();

        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        IOEquivalenceTest eqOracle = new IOEquivalenceTest(model, teachers, consts, false, actions);
        runner.setEqOracle(eqOracle);
        runner.setIoMode(true);
        Hypothesis result = runner.run(RaLearningAlgorithmName.RALAMBDA, dwOracle, teachers, consts, jsolv, actions);
        DefaultQuery<PSymbolInstance, Boolean> ce = eqOracle.findCounterExample(result, null);
        Assert.assertNull(ce);
    }

    @Test
    public void learnRaLambdaDtlsServerTest() {
        RegisterAutomatonImporter loader = TestUtil.getLoader("/de/learnlib/ralib/automata/xml/dtls-server.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(new ParameterizedSymbol[] {});

        ParameterizedSymbol[] actions = loader.getActions().toArray(new ParameterizedSymbol[] {});

        final Constants consts = loader.getConstants();

        DataType epoch = loader.getDataTypes().iterator().next();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(epoch, new IntegerEqualityTheory(epoch));
        SimulatorSUL sul = new SimulatorSUL(model, teachers, consts);
        SimulatorSUL testSul = new SimulatorSUL(model, teachers, consts);

        JConstraintsConstraintSolver solver = TestUtil.getZ3Solver();
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);
        Measurements measurements = new Measurements();
        MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(ioFilter, teachers, consts, solver),
                measurements);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp),
                teachers, consts, solver);

        RaLambda ralambda = new RaLambda(mto, hypFactory, mlo, consts, true, actions);

        QueryStatistics queryStats = new QueryStatistics(measurements, sul, testSul);
        ralambda.setSolver(solver);
        ralambda.setStatisticCounter(queryStats);
        ralambda.learn();
        Hypothesis hyp = ralambda.getHypothesis();
        PSymbolInstance d = new PSymbolInstance(new OutputSymbol("dummy"));
        Word<PSymbolInstance> ce = ioOracle.trace(Word.fromSymbols(input("PSK_CLIENT_HELLO", 0), d,
                input("PSK_CLIENT_HELLO", 0), d, input("PSK_CLIENT_KEY_EXCHANGE", 0), d, input("CHANGE_CIPHER_SPEC", 0),
                d, input("FINISHED", 1), d, input("APPLICATION", 1), d));
        for (Word<PSymbolInstance> candidate : ce.prefixes(false)) {
            if (!hyp.accepts(ce)) {
                ralambda.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(candidate, true));
                ralambda.learn();
                hyp = ralambda.getHypothesis();
            }
        }
        System.out.println(ralambda.getQueryStatistics());
    }

    @Test
    public void learnRaStarDtlsServerTest() {
        RegisterAutomatonImporter loader = TestUtil.getLoader("/de/learnlib/ralib/automata/xml/dtls-server.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(new ParameterizedSymbol[] {});

        ParameterizedSymbol[] actions = loader.getActions().toArray(new ParameterizedSymbol[] {});

        final Constants consts = loader.getConstants();

        DataType epoch = loader.getDataTypes().iterator().next();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(epoch, new IntegerEqualityTheory(epoch));
        SimulatorSUL sul = new SimulatorSUL(model, teachers, consts);
        SimulatorSUL testSul = new SimulatorSUL(model, teachers, consts);

        JConstraintsConstraintSolver solver = TestUtil.getZ3Solver();
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);
        Measurements measurements = new Measurements();
        MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(ioFilter, teachers, consts, solver),
                measurements);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp),
                teachers, consts, solver);

        RaStar ralambda = new RaStar(mto, hypFactory, mlo, consts, true, actions);

        QueryStatistics queryStats = new QueryStatistics(measurements, sul, testSul);
        ralambda.setStatisticCounter(queryStats);
        ralambda.learn();
        Hypothesis hyp = ralambda.getHypothesis();
        PSymbolInstance d = new PSymbolInstance(new OutputSymbol("dummy"));
        Word<PSymbolInstance> ce = ioOracle.trace(Word.fromSymbols(input("PSK_CLIENT_HELLO", 0), d,
                input("PSK_CLIENT_HELLO", 0), d, input("PSK_CLIENT_KEY_EXCHANGE", 0), d, input("CHANGE_CIPHER_SPEC", 0),
                d, input("FINISHED", 1), d, input("APPLICATION", 1), d));
        for (Word<PSymbolInstance> candidate : ce.prefixes(false)) {
            if (!hyp.accepts(ce)) {
                ralambda.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(candidate, true));
                ralambda.learn();
                hyp = ralambda.getHypothesis();
            }
        }
        System.out.println(ralambda.getQueryStatistics());
    }

}
