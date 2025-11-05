package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

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
import de.learnlib.ralib.learning.SymbolicSuffix;
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

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

//        RaLambda ralambda = new RaLambda(mto, hypFactory, slo, consts, false, false, I_PUSH, I_POP);
//        ralambda.setSolver(solver);
        SLLambda sllambda = new SLLambda(mto, hypFactory, slo, teachers, consts, false, solver, I_PUSH, I_POP);

//        ralambda.learn();
//        RegisterAutomaton hyp = ralambda.getHypothesis();
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

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

//        RaLambda ralambda = new RaLambda(mto, hypFactory, slo, consts, false, false, I_PUSH, I_POP);
//        ralambda.setSolver(solver);
        SLLambda sllambda = new SLLambda(mto, hypFactory, slo, teachers, consts, false, solver, I_PUSH, I_POP);

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

//        Collection<SymbolicSuffix> suffixes = ralambda.getDT().getSuffixes();
//        Set<Word<ParameterizedSymbol>> suffixActions = suffixes.stream().map(s -> s.getActions()).collect(Collectors.toSet());
//        Set<Word<ParameterizedSymbol>> expectedSuffixActions = ImmutableSet.of(
//            Word.fromSymbols(),
//            Word.fromSymbols(I_PUSH),
//            Word.fromSymbols(I_PUSH, I_PUSH),
//            Word.fromSymbols(I_POP),
//            Word.fromSymbols(I_POP, I_POP));

        Assert.assertEquals(hyp.getStates().size(), 4);
        Assert.assertEquals(hyp.getTransitions().size(), 10);
//        Assert.assertEquals(suffixActions, expectedSuffixActions);
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

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

//        RaLambda ralambda = new RaLambda(mto, hypFactory, slo, consts, false, false, I_PUSH, I_POP);
//        ralambda.setSolver(solver);
        SLLambda sllambda = new SLLambda(mto, hypFactory, slo, teachers, consts, false, solver, I_PUSH, I_POP);

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
        
        System.out.println(Arrays.toString(measuresLambda));

        Assert.assertEquals(Arrays.toString(measuresLambda),
			    "[{TQ: 60, Resets: 1199, Inputs: 0}," +
			    " {TQ: 65, Resets: 1859, Inputs: 0}," +
			    " {TQ: 60, Resets: 1178, Inputs: 0}," +
			    " {TQ: 62, Resets: 1516, Inputs: 0}," +
			    " {TQ: 70, Resets: 2717, Inputs: 0}," +
			    " {TQ: 62, Resets: 1443, Inputs: 0}," +
			    " {TQ: 56, Resets: 1148, Inputs: 0}," +
			    " {TQ: 41, Resets: 1089, Inputs: 0}," +
			    " {TQ: 78, Resets: 2478, Inputs: 0}," +
			    " {TQ: 44, Resets: 1139, Inputs: 0}]");
        Assert.assertEquals(Arrays.toString(measuresStar),
			    "[{TQ: 51, Resets: 1589, Inputs: 0}," +
			    " {TQ: 50, Resets: 12577, Inputs: 0}," +
			    " {TQ: 63, Resets: 1317, Inputs: 0}," +
			    " {TQ: 50, Resets: 10669, Inputs: 0}," +
			    " {TQ: 39, Resets: 11088, Inputs: 0}," +
			    " {TQ: 62, Resets: 1310, Inputs: 0}," +
			    " {TQ: 60, Resets: 1298, Inputs: 0}," +
			    " {TQ: 49, Resets: 1207, Inputs: 0}," +
			    " {TQ: 53, Resets: 11461, Inputs: 0}," +
			    " {TQ: 49, Resets: 1301, Inputs: 0}]");
    }
}
