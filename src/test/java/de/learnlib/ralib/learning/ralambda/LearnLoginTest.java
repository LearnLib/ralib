package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibLearningExperimentRunner;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;

import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class LearnLoginTest extends RaLibTestSuite {

    @Test
    public void learnLoginTest() {
        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        RaLambda ralambda = new RaLambda(mto, hypFactory, slo,
                consts, I_LOGIN, I_LOGOUT, I_REGISTER);
        ralambda.setSolver(solver);

        ralambda.learn();
        RegisterAutomaton hyp = ralambda.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                        new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ZERO)),
                new PSymbolInstance(I_LOGIN,
                        new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ZERO)));

        ralambda.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        ralambda.learn();
        hyp = ralambda.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        Assert.assertEquals(hyp.getStates().size(), 3);
        Assert.assertEquals(hyp.getTransitions().size(), 11);
    }

    @Test
    public void learnLoginRandomTest() {
        final int SEEDS = 10;

        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);
        ConstraintSolver solver = new ConstraintSolver();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        Measurements[] measuresLambda = new Measurements[SEEDS];
        Measurements[] measuresStar = new Measurements[SEEDS];

        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        runner.setMaxDepth(6);
        for (int seed = 0; seed < SEEDS; seed++) {
	    runner.setSeed(seed);
	    Hypothesis hyp = runner.run(RaLearningAlgorithmName.RALAMBDA, dwOracle, teachers, consts, solver, new ParameterizedSymbol [] {I_LOGIN, I_LOGOUT, I_REGISTER});
	    measuresLambda[seed] = runner.getMeasurements();
	    runner.resetMeasurements();

	    Assert.assertEquals(hyp.getStates().size(), 4);
	    Assert.assertEquals(hyp.getTransitions().size(), 14);
	    logger.log(Level.FINE, "HYP: {0}", hyp);

	    hyp = runner.run(RaLearningAlgorithmName.RASTAR, dwOracle, teachers, consts, solver, new ParameterizedSymbol [] {I_LOGIN, I_LOGOUT, I_REGISTER});
	    measuresStar[seed] = runner.getMeasurements();
	    runner.resetMeasurements();
        }
        Assert.assertEquals(Arrays.toString(measuresLambda),
			    "[{TQ: 64, Resets: 2511, Inputs: 0}," +
			    " {TQ: 64, Resets: 2485, Inputs: 0}," +
			    " {TQ: 64, Resets: 1958, Inputs: 0}," +
			    " {TQ: 64, Resets: 1959, Inputs: 0}," +
			    " {TQ: 64, Resets: 1886, Inputs: 0}," +
			    " {TQ: 64, Resets: 2750, Inputs: 0}," +
			    " {TQ: 64, Resets: 2487, Inputs: 0}," +
			    " {TQ: 64, Resets: 1737, Inputs: 0}," +
			    " {TQ: 68, Resets: 1758, Inputs: 0}," +
			    " {TQ: 64, Resets: 1604, Inputs: 0}]");
        Assert.assertEquals(Arrays.toString(measuresStar),
			    "[{TQ: 65, Resets: 2339, Inputs: 0}," +
			    " {TQ: 65, Resets: 2313, Inputs: 0}," +
			    " {TQ: 65, Resets: 2208, Inputs: 0}," +
			    " {TQ: 64, Resets: 2205, Inputs: 0}," +
			    " {TQ: 65, Resets: 2136, Inputs: 0}," +
			    " {TQ: 65, Resets: 2578, Inputs: 0}," +
			    " {TQ: 65, Resets: 2315, Inputs: 0}," +
			    " {TQ: 65, Resets: 2192, Inputs: 0}," +
			    " {TQ: 65, Resets: 3623, Inputs: 0}," +
			    " {TQ: 65, Resets: 2059, Inputs: 0}]");
    }

}
