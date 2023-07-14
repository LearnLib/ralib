package de.learnlib.ralib.learning.rattt;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
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
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class LearnLoginTest extends RaLibTestSuite {

    @Test
    public void learnLoginExample() {

        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        RaLambda rattt = new RaLambda(mto, hypFactory, slo,
                consts, I_LOGIN, I_LOGOUT, I_REGISTER);
        rattt.setSolver(solver);

        rattt.learn();
        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                        new DataValue(T_UID, 0), new DataValue(T_PWD, 0)),
                new PSymbolInstance(I_LOGIN,
                        new DataValue(T_UID, 0), new DataValue(T_PWD, 0)));

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        Assert.assertEquals(hyp.getStates().size(), 3);
        Assert.assertEquals(hyp.getTransitions().size(), 11);
    }


    @Test
    public void learnLoginExampleRandom() {
        final int SEEDS = 10;

        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);
        ConstraintSolver solver = new SimpleConstraintSolver();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        Measurements[] measuresTTT = new Measurements[SEEDS];
        Measurements[] measuresStar = new Measurements[SEEDS];

        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        runner.setMaxDepth(6);
        for (int seed=0; seed<SEEDS; seed++) {
        	runner.setSeed(seed);
	        Hypothesis hyp = runner.run(RaLearningAlgorithmName.RALAMBDA, dwOracle, teachers, consts, solver, new ParameterizedSymbol [] {I_LOGIN, I_LOGOUT, I_REGISTER});
	        measuresTTT[seed] = runner.getMeasurements();
	        runner.resetMeasurements();

	        Assert.assertEquals(hyp.getStates().size(), 4);
	        Assert.assertEquals(hyp.getTransitions().size(), 14);
	        logger.log(Level.FINE, "HYP: {0}", hyp);

	        hyp = runner.run(RaLearningAlgorithmName.RASTAR, dwOracle, teachers, consts, solver, new ParameterizedSymbol [] {I_LOGIN, I_LOGOUT, I_REGISTER});
	        measuresStar[seed] = runner.getMeasurements();
	        runner.resetMeasurements();
        }
        System.out.println("Queries (TTT): " + Arrays.toString(measuresTTT));
        System.out.println("Queries (Star): " + Arrays.toString(measuresStar));
    }

}
