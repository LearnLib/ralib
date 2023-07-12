package de.learnlib.ralib.learning.rattt;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

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

import de.learnlib.oracles.DefaultQuery;
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
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class LearnStackTest extends RaLibTestSuite {
	@Test
	public void learnStackExample() {

		Constants consts = new Constants();
	    RegisterAutomaton sul = AUTOMATON;
	    DataWordOracle dwOracle = new SimulatorOracle(sul);

	    final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	    teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

	    ConstraintSolver solver = new SimpleConstraintSolver();

	    Measurements mes = new Measurements();

	    MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(
	              dwOracle, teachers, new Constants(), solver),
	    		mes);

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        RaLambda rattt = new RaLambda(mto, hypFactory, slo, consts, false, false, I_PUSH, I_POP);

        rattt.learn();
        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 0)));

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 2)));

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        Assert.assertEquals(hyp.getStates().size(), 4);
        Assert.assertEquals(hyp.getTransitions().size(), 10);
	}

	@Test
	public void learnStackExampleSwitchedCE() {
		Constants consts = new Constants();
	    RegisterAutomaton sul = AUTOMATON;
	    DataWordOracle dwOracle = new SimulatorOracle(sul);

	    final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	    teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

	    ConstraintSolver solver = new SimpleConstraintSolver();

	    MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
	              dwOracle, teachers, new Constants(), solver);

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        RaLambda rattt = new RaLambda(mto, hypFactory, slo, consts, false, false, I_PUSH, I_POP);

        rattt.learn();
        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 2)));


        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 0)));


        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 1)));

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));
        rattt.learn();
        hyp = rattt.getHypothesis();

        Collection<SymbolicSuffix> suffixes = rattt.getDT().getSuffixes();
        Set<Word<ParameterizedSymbol>> suffixActions = suffixes.stream().map(s -> s.getActions()).collect(Collectors.toSet());
        Set<Word<ParameterizedSymbol>> expectedSuffixActions = ImmutableSet.of(
            Word.fromSymbols(),
            Word.fromSymbols(I_PUSH),
            Word.fromSymbols(I_PUSH, I_PUSH),
            Word.fromSymbols(I_POP),
            Word.fromSymbols(I_POP, I_POP));

        Assert.assertEquals(hyp.getStates().size(), 4);
        Assert.assertEquals(hyp.getTransitions().size(), 10);
        Assert.assertEquals(suffixActions, expectedSuffixActions);
	}

	@Test
    public void learnStackExampleLongCE() {
        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                  dwOracle, teachers, new Constants(), solver);

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        RaLambda rattt = new RaLambda(mto, hypFactory, slo, consts, false, false, I_PUSH, I_POP);

        rattt.learn();
        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
                new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
                new PSymbolInstance(I_POP, new DataValue(T_INT, 0)),
                new PSymbolInstance(I_POP, new DataValue(T_INT, 0))
                );


        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        Assert.assertTrue(hyp.accepts(ce));
    }

	@Test
	public void learnStackExampleRandom() {
		final int SEEDS = 10;
		Constants consts = new Constants();
		RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);
        ConstraintSolver solver = new SimpleConstraintSolver();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory theory = new IntegerEqualityTheory(T_INT);
        theory.setUseSuffixOpt(false);
        teachers.put(T_INT, theory);

        Measurements[] measuresTTT = new Measurements[SEEDS];
        Measurements[] measuresStar = new Measurements[SEEDS];

        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        runner.setMaxDepth(6);
        for (int seed=0; seed<SEEDS; seed++) {
        	runner.setSeed(seed);
        	Hypothesis hypTTT = runner.run(RaLearningAlgorithmName.RALAMBDA, dwOracle, teachers, consts, solver, new ParameterizedSymbol[]{I_PUSH, I_POP});
        	measuresTTT[seed] = runner.getMeasurements();
        	runner.resetMeasurements();

        	Assert.assertEquals(hypTTT.getStates().size(), 4);
        	Assert.assertEquals(hypTTT.getTransitions().size(), 10);

        	Hypothesis hypStar = runner.run(RaLearningAlgorithmName.RASTAR, dwOracle, teachers, consts, solver, new ParameterizedSymbol[] {I_PUSH, I_POP});
        	measuresStar[seed] = runner.getMeasurements();
        	runner.resetMeasurements();
        }

        System.out.println("Queries (RaTTT): " + Arrays.toString(measuresTTT));
        System.out.println("Queries (RaStar): " + Arrays.toString(measuresStar));
	}
}
