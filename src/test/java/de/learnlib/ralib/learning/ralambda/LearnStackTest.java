package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

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
import de.learnlib.ralib.learning.MeasuringOracle;
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

public class LearnStackTest extends RaLibTestSuite {

    @Test
    public void testLearnStack() {

    	Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        Measurements mes = new Measurements();

        MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(
              dwOracle, teachers, new Constants(), solver), mes);

        SLLambda sllambda = new SLLambda(mto, teachers, consts, false, solver, I_PUSH, I_POP);

        sllambda.learn();
        RegisterAutomaton hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ZERO)));

        sllambda.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        sllambda.learn();
        hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(2))));

        sllambda.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        sllambda.learn();
        hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        Assert.assertEquals(hyp.getStates().size(), 4);
        Assert.assertEquals(hyp.getTransitions().size(), 10);
    }

    @Test
    public void testLearnStackSwitchedCE() {
        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
              dwOracle, teachers, new Constants(), solver);

        SLLambda sllambda = new SLLambda(mto, teachers, consts, false, solver, I_PUSH, I_POP);

        sllambda.learn();
        RegisterAutomaton hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);
        hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(2))));

        sllambda.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        sllambda.learn();
        hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ZERO)));

        sllambda.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        sllambda.learn();
        hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ONE)));

        sllambda.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));
        sllambda.learn();
        hyp = sllambda.getHypothesis();

        Assert.assertEquals(hyp.getStates().size(), 4);
        Assert.assertEquals(hyp.getTransitions().size(), 10);
    }

    @Test
    public void testLearnStackLongCE() {
        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                  dwOracle, teachers, new Constants(), solver);

        SLLambda sllambda = new SLLambda(mto, teachers, consts, false, solver, I_PUSH, I_POP);

        sllambda.learn();
        RegisterAutomaton hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);
        hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
                new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
                new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ZERO)),
                new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ZERO)));

        sllambda.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        sllambda.learn();
        hyp = sllambda.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        Assert.assertTrue(hyp.accepts(ce));
    }

    @Test
    public void testLearnStackRandom() {
	final int SEEDS = 10;
	Constants consts = new Constants();
	RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);
        ConstraintSolver solver =new ConstraintSolver();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory theory = new IntegerEqualityTheory(T_INT);
        theory.setUseSuffixOpt(false);
        teachers.put(T_INT, theory);

        Measurements[] measuresLambda = new Measurements[SEEDS];
        Measurements[] measuresStar = new Measurements[SEEDS];

        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        runner.setMaxDepth(6);
        for (int seed = 0; seed < SEEDS; seed++) {
            runner.setSeed(seed);
            Hypothesis hypLambda = runner.run(RaLearningAlgorithmName.RALAMBDA, dwOracle, teachers, consts, solver, new ParameterizedSymbol[]{I_PUSH, I_POP});
            measuresLambda[seed] = runner.getMeasurements();
            runner.resetMeasurements();

            Assert.assertEquals(hypLambda.getStates().size(), 4);
            Assert.assertEquals(hypLambda.getTransitions().size(), 10);

            runner.run(RaLearningAlgorithmName.RASTAR, dwOracle, teachers, consts, solver, new ParameterizedSymbol[] {I_PUSH, I_POP});
            measuresStar[seed] = runner.getMeasurements();
            runner.resetMeasurements();
        }

        Assert.assertEquals(Arrays.toString(measuresLambda),
			    "[{TQ: 93, Resets: 435, Inputs: 0}," +
			    " {TQ: 103, Resets: 737, Inputs: 0}," +
			    " {TQ: 88, Resets: 441, Inputs: 0}," +
			    " {TQ: 98, Resets: 574, Inputs: 0}," +
			    " {TQ: 113, Resets: 936, Inputs: 0}," +
			    " {TQ: 92, Resets: 597, Inputs: 0}," +
			    " {TQ: 82, Resets: 446, Inputs: 0}," +
			    " {TQ: 58, Resets: 418, Inputs: 0}," +
			    " {TQ: 133, Resets: 694, Inputs: 0}," +
			    " {TQ: 63, Resets: 470, Inputs: 0}]");
        Assert.assertEquals(Arrays.toString(measuresStar),
			    "[{TQ: 51, Resets: 838, Inputs: 0}," +
			    " {TQ: 50, Resets: 10681, Inputs: 0}," +
			    " {TQ: 63, Resets: 599, Inputs: 0}," +
			    " {TQ: 50, Resets: 9021, Inputs: 0}," +
			    " {TQ: 39, Resets: 9971, Inputs: 0}," +
			    " {TQ: 62, Resets: 606, Inputs: 0}," +
			    " {TQ: 60, Resets: 603, Inputs: 0}," +
			    " {TQ: 49, Resets: 520, Inputs: 0}," +
			    " {TQ: 53, Resets: 9344, Inputs: 0}," +
			    " {TQ: 49, Resets: 620, Inputs: 0}]");
    }
}
