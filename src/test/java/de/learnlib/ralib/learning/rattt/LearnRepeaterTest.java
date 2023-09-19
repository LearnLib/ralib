package de.learnlib.ralib.learning.rattt;

import static de.learnlib.ralib.example.repeater.RepeaterSUL.IPUT;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.OECHO;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.TINT;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.api.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.repeater.Repeater;
import de.learnlib.ralib.example.repeater.RepeaterSUL;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class LearnRepeaterTest extends RaLibTestSuite {
	@Test
	public void learnRepeater() {

        Constants consts = new Constants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory theory = new IntegerEqualityTheory(TINT);
        theory.setUseSuffixOpt(true);
        teachers.put(TINT, theory);

        RepeaterSUL sul = new RepeaterSUL();
        IOOracle ioOracle = new SULOracle(sul, RepeaterSUL.ERROR);
	    IOCache ioCache = new IOCache(ioOracle);
	    IOFilter oracle = new IOFilter(ioCache, sul.getInputSymbols());

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                oracle, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo =
                new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        Measurements measurements = new Measurements();
        QueryStatistics queryStats = new QueryStatistics(measurements, ioOracle);

        RaTTT learner = new RaTTT(mto, hypFactory, mlo, consts, true, sul.getActionSymbols());
        learner.setStatisticCounter(queryStats);
        learner.setSolver(solver);

        learner.learn();

        Repeater repeater = new Repeater();
        Assert.assertEquals(repeater.repeat(0), (Integer)0);
        Assert.assertEquals(repeater.repeat(0), (Integer)0);
        Assert.assertNull(repeater.repeat(0));

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(IPUT, new DataValue(TINT, 0)),
        		new PSymbolInstance(OECHO, new DataValue(TINT, 0)),
        		new PSymbolInstance(IPUT, new DataValue(TINT, 0)),
        		new PSymbolInstance(OECHO, new DataValue(TINT, 0)),
        		new PSymbolInstance(IPUT, new DataValue(TINT, 0)),
        		new PSymbolInstance(OECHO, new DataValue(TINT, 0)));

        learner.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, false));

        learner.learn();

        System.out.println(queryStats.toString());

        Assert.assertTrue(true);
	}
}
