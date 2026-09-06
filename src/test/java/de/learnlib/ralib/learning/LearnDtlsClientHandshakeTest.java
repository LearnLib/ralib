package de.learnlib.ralib.learning;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibLearningExperimentRunner;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.SampledTestsEQOracle;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class LearnDtlsClientHandshakeTest extends RaLibTestSuite {

	private static final DataType EPOCH_I = new DataType("epoch_i");
	private static final InputSymbol HELLO_VERIFY_REQUEST = new InputSymbol("HELLO_VERIFY_REQUEST", EPOCH_I);
	private static final InputSymbol SERVER_HELLO = new InputSymbol("PSK_SERVER_HELLO", EPOCH_I);
	private static final InputSymbol SERVER_HELLO_DONE = new InputSymbol("SERVER_HELLO_DONE", EPOCH_I);
	private static final InputSymbol CHANGE_CIPHER_SPEC = new InputSymbol("CHANGE_CIPHER_SPEC", EPOCH_I);
	private static final InputSymbol FINISHED = new InputSymbol("FINISHED", EPOCH_I);
	private static final InputSymbol APPLICATION = new InputSymbol("APPLICATION", EPOCH_I);

	@Test
    public void testLearnMbedTLSClientHandshake() {
        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/dtls/mbedtls-client-handshake.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();


        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(EPOCH_I, new IntegerEqualityTheory(EPOCH_I));
        SimulatorSUL sul = new SimulatorSUL(model, teachers, consts);
        SimulatorOracle dwOracle = new SimulatorOracle(model);

        ConstraintSolver solver = TestUtil.getZ3Solver();

        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        SampledTestsEQOracle oracle = new SampledTestsEQOracle(Arrays.asList(clientTest()), sul);

        runner.setEqOracle(oracle);
        runner.setIoMode(true);
        for (RaLearningAlgorithmName alg : Arrays.asList(RaLearningAlgorithmName.RALAMBDA, RaLearningAlgorithmName.RALAMBDAEQ, RaLearningAlgorithmName.RASTAR)) {
	        Hypothesis result = runner.run(alg, dwOracle, teachers, consts, solver, actions);
	        DefaultQuery<PSymbolInstance, Boolean> ce = oracle.findCounterExample(result, null);
	        Assert.assertNull(ce);
        }
    }

	@Test
    public void testLearnWolfSSLClientHandshake() {
        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/dtls/wolfssl-client-handshake.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();


        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(EPOCH_I, new IntegerEqualityTheory(EPOCH_I));
        SimulatorSUL sul = new SimulatorSUL(model, teachers, consts);
        SimulatorOracle dwOracle = new SimulatorOracle(model);

        ConstraintSolver solver = TestUtil.getZ3Solver();

        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        SampledTestsEQOracle oracle = new SampledTestsEQOracle(Arrays.asList(clientTest()), sul);

        runner.setEqOracle(oracle);
        runner.setIoMode(true);
        for (RaLearningAlgorithmName alg : Arrays.asList(RaLearningAlgorithmName.RALAMBDA, RaLearningAlgorithmName.RALAMBDAEQ, RaLearningAlgorithmName.RASTAR)) {
	        Hypothesis result = runner.run(alg, dwOracle, teachers, consts, solver, actions);
	        DefaultQuery<PSymbolInstance, Boolean> ce = oracle.findCounterExample(result, null);
	        Assert.assertNull(ce);
        }
    }

    private Word<PSymbolInstance> clientTest() {
    	return Word.fromSymbols(new PSymbolInstance(HELLO_VERIFY_REQUEST, new DataValue(EPOCH_I, BigDecimal.ZERO)),
    			new PSymbolInstance(SERVER_HELLO, new DataValue(EPOCH_I, BigDecimal.ZERO)),
    			new PSymbolInstance(SERVER_HELLO_DONE, new DataValue(EPOCH_I, BigDecimal.ZERO)),
    			new PSymbolInstance(CHANGE_CIPHER_SPEC, new DataValue(EPOCH_I, BigDecimal.ZERO)),
    			new PSymbolInstance(FINISHED, new DataValue(EPOCH_I, BigDecimal.ONE)),
    			new PSymbolInstance(APPLICATION, new DataValue(EPOCH_I, BigDecimal.ONE))
    			);
    }
}
