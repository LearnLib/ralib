package de.learnlib.ralib.learning.rattt;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.api.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.MeasuringOracle;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.ralambda.RaLambda;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class TestQueryCount extends RaLibTestSuite {

	@Test
	public void testQueryCount() {

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(PriorityQueueSUL.DOUBLE_TYPE,
                new DoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE));

        final Constants consts = new Constants();

        PriorityQueueSUL sul = new PriorityQueueSUL(2);

        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
        IOOracle ioOracle = new SULOracle(sul, PriorityQueueSUL.ERROR);

        Measurements measurements = new Measurements();
        MeasuringOracle mto = new MeasuringOracle(TestUtil.createMTO(
                ioOracle, teachers, consts, jsolv, sul.getInputSymbols()),
        		measurements);

        MultiTheorySDTLogicOracle mlo
                = new MultiTheorySDTLogicOracle(consts, jsolv);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp)
                -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, jsolv);

        QueryStatistics queryStats = new QueryStatistics(measurements, ioOracle);
        RaLambda learner = new RaLambda(mto, hypFactory, mlo,
                consts, true, sul.getActionSymbols());
        learner.setStatisticCounter(queryStats);

        learner.learn();

        Word<PSymbolInstance> ce1 = Word.fromSymbols(
        		new PSymbolInstance(PriorityQueueSUL.OFFER, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(PriorityQueueSUL.OK),
        		new PSymbolInstance(PriorityQueueSUL.OFFER, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.valueOf(2))),
        		new PSymbolInstance(PriorityQueueSUL.OK),
        		new PSymbolInstance(PriorityQueueSUL.OFFER, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.valueOf(3))),
        		new PSymbolInstance(PriorityQueueSUL.OK));

        DefaultQuery<PSymbolInstance, Boolean> ceQuery = new DefaultQuery<>(ce1, false);

        learner.addCounterexample(ceQuery);
        learner.learn();

        long memQueries1 = learner.getQueryStatistics().getMemQueries();
        Assert.assertEquals(memQueries1, 2);

        Word<PSymbolInstance> ce2 = Word.fromSymbols(
        		new PSymbolInstance(PriorityQueueSUL.OFFER, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(PriorityQueueSUL.OK),
        		new PSymbolInstance(PriorityQueueSUL.OFFER, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(PriorityQueueSUL.OK),
        		new PSymbolInstance(PriorityQueueSUL.POLL),
        		new PSymbolInstance(PriorityQueueSUL.OUTPUT, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(PriorityQueueSUL.POLL),
        		new PSymbolInstance(PriorityQueueSUL.OUTPUT, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.ONE)));

        ceQuery = new DefaultQuery<>(ce2, true);

        learner.addCounterexample(ceQuery);
        learner.learn();

        long memQueries2 = learner.getQueryStatistics().getMemQueries();
        Assert.assertEquals(memQueries2, 36);

        Word<PSymbolInstance> ce3 = Word.fromSymbols(
        		new PSymbolInstance(PriorityQueueSUL.OFFER, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(PriorityQueueSUL.OK),
        		new PSymbolInstance(PriorityQueueSUL.OFFER, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(PriorityQueueSUL.OK),
        		new PSymbolInstance(PriorityQueueSUL.OFFER, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(PriorityQueueSUL.NOK),
        		new PSymbolInstance(PriorityQueueSUL.POLL),
        		new PSymbolInstance(PriorityQueueSUL.OUTPUT, new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.ONE)));

        ceQuery = new DefaultQuery<>(ce3, true);

        learner.addCounterexample(ceQuery);
        learner.learn();

        long memQueries3 = learner.getQueryStatistics().getMemQueries();
        Assert.assertEquals(memQueries3, 44);
	}
}
