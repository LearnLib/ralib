package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.array.DoubleArrayListDataWordOracle.DOUBLE_TYPE;
import static de.learnlib.ralib.example.array.DoubleArrayListDataWordOracle.PUSH;
import static de.learnlib.ralib.example.array.DoubleArrayListDataWordOracle.REMOVE;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.array.DoubleArrayListDataWordOracle;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.ConstraintSolverFactory;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class LearnBoundedListIneqTest extends RaLibTestSuite {

	@Test
	public void learnArrayListTest() {

		Constants consts = new Constants();

		DoubleArrayListDataWordOracle dwOracle = new DoubleArrayListDataWordOracle(2);

		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		DoubleInequalityTheory dit = new DoubleInequalityTheory(DOUBLE_TYPE);
		teachers.put(DOUBLE_TYPE, dit);
        ConstraintSolver solver = ConstraintSolverFactory.createZ3ConstraintSolver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, consts, solver);

        SDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        RaLambda learner = new RaLambda(mto, hypFactory, mlo, teachers, consts, PUSH, REMOVE);
        learner.learn();

        DataValue<BigDecimal> dv1 = new DataValue<>(DOUBLE_TYPE, BigDecimal.ONE);
        DataValue<BigDecimal> dv2 = new DataValue<>(DOUBLE_TYPE, BigDecimal.valueOf(2));
        DataValue<BigDecimal> dv3 = new DataValue<>(DOUBLE_TYPE, BigDecimal.valueOf(3));

        PSymbolInstance push1 = new PSymbolInstance(PUSH, dv1);
        PSymbolInstance push2 = new PSymbolInstance(PUSH, dv2);
        PSymbolInstance push3 = new PSymbolInstance(PUSH, dv3);
        PSymbolInstance rem1 = new PSymbolInstance(REMOVE, dv1);
        PSymbolInstance rem2 = new PSymbolInstance(REMOVE, dv2);

        Word<PSymbolInstance> ce = Word.fromSymbols(push1, push2, push3);

        learner.addCounterexample(new DefaultQuery<>(ce, false));
        learner.learn();

        ce = Word.fromSymbols(push1, rem1);
        learner.addCounterexample(new DefaultQuery<>(ce, true));
        learner.learn();

        ce = Word.fromSymbols(push1, push2, rem1);
        learner.addCounterexample(new DefaultQuery<>(ce, true));
        learner.learn();

        ce = Word.fromSymbols(push2, push1, rem2);
        learner.addCounterexample(new DefaultQuery<>(ce, true));
        learner.learn();

        Hypothesis hyp = learner.getHypothesis();

        Assert.assertEquals(hyp.getStates().size(), 5);
        Assert.assertEquals(hyp.getTransitions().size(), 18);
	}
}
