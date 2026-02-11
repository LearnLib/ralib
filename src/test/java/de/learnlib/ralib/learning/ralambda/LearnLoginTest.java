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
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class LearnLoginTest extends RaLibTestSuite {

    @Test
    public void testLearnLogin() {
        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);

        SLLambda sllambda = new SLLambda(mto, teachers,
        		consts, false, solver, I_LOGIN, I_LOGOUT, I_REGISTER);

        sllambda.learn();
        RegisterAutomaton hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                        new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ZERO)),
                new PSymbolInstance(I_LOGIN,
                        new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ZERO)));

        sllambda.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        sllambda.learn();
        hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        Assert.assertEquals(hyp.getStates().size(), 3);
        Assert.assertEquals(hyp.getTransitions().size(), 11);
    }

    @Test
    public void testLearnLoginRandom() {
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
			    "[{TQ: 74, Resets: 1997, Inputs: 0}," +
			    " {TQ: 72, Resets: 1984, Inputs: 0}," +
			    " {TQ: 74, Resets: 1417, Inputs: 0}," +
			    " {TQ: 72, Resets: 1449, Inputs: 0}," +
			    " {TQ: 74, Resets: 1403, Inputs: 0}," +
			    " {TQ: 72, Resets: 2120, Inputs: 0}," +
			    " {TQ: 74, Resets: 1984, Inputs: 0}," +
			    " {TQ: 72, Resets: 1263, Inputs: 0}," +
			    " {TQ: 79, Resets: 1243, Inputs: 0}," +
			    " {TQ: 74, Resets: 1220, Inputs: 0}]");
        Assert.assertEquals(Arrays.toString(measuresStar),
			    "[{TQ: 65, Resets: 1807, Inputs: 0}," +
			    " {TQ: 65, Resets: 1788, Inputs: 0}," +
			    " {TQ: 65, Resets: 1693, Inputs: 0}," +
			    " {TQ: 64, Resets: 1727, Inputs: 0}," +
			    " {TQ: 65, Resets: 1680, Inputs: 0}," +
			    " {TQ: 65, Resets: 1929, Inputs: 0}," +
			    " {TQ: 65, Resets: 1793, Inputs: 0}," +
			    " {TQ: 65, Resets: 1720, Inputs: 0}," +
			    " {TQ: 65, Resets: 3103, Inputs: 0}," +
			    " {TQ: 65, Resets: 1682, Inputs: 0}]");
    }

}
