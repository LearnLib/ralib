package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.repeater.RepeaterSUL.IPUT;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.OECHO;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.TINT;

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
import de.learnlib.ralib.example.repeater.RepeaterSUL;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class TestSuffixOptimization extends RaLibTestSuite {

    @Test
    public void learnRepeaterSuffixOptTest() {

        Constants consts = new Constants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory theory = new IntegerEqualityTheory(TINT);
        theory.setUseSuffixOpt(true);
        teachers.put(TINT, theory);

        RepeaterSUL sul = new RepeaterSUL(-1, 2);
        IOOracle ioOracle = new SULOracle(sul, RepeaterSUL.ERROR);
	IOCache ioCache = new IOCache(ioOracle);
	IOFilter oracle = new IOFilter(ioCache, sul.getInputSymbols());

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto =
	    new MultiTheoryTreeOracle(oracle, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo =
	    new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
	    new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        Measurements measurements = new Measurements();
        QueryStatistics stats = new QueryStatistics(measurements, sul);

        RaLambda learner = new RaLambda(mto, hypFactory, mlo, consts, true, sul.getActionSymbols());
        learner.setStatisticCounter(stats);
        learner.setSolver(solver);

        learner.learn();

        Word<PSymbolInstance> ce =
	    Word.fromSymbols(new PSymbolInstance(IPUT, new DataValue(TINT, BigDecimal.ZERO)),
			     new PSymbolInstance(OECHO, new DataValue(TINT, BigDecimal.ZERO)),
			     new PSymbolInstance(IPUT, new DataValue(TINT, BigDecimal.ONE)),
			     new PSymbolInstance(OECHO, new DataValue(TINT, BigDecimal.ONE)),
			     new PSymbolInstance(IPUT, new DataValue(TINT, new BigDecimal(2))),
			     new PSymbolInstance(OECHO, new DataValue(TINT, new BigDecimal(2))));

        learner.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, false));

        learner.learn();

	Hypothesis hyp = learner.getHypothesis();
        Assert.assertEquals(hyp.getStates().size(), 7);

	String str = stats.toString();
	Assert.assertTrue(str.contains("Counterexamples: 1"));
        Assert.assertTrue(str.contains("CE max length: 6"));
        Assert.assertTrue(str.contains("CE Analysis: {TQ: 0, Resets: 2, Inputs: 5}"));
        Assert.assertTrue(str.contains("Processing / Refinement: {TQ: 0, Resets: 1, Inputs: 4}"));
        Assert.assertTrue(str.contains("Other: {TQ: 0, Resets: 1, Inputs: 1}"));
        Assert.assertTrue(str.contains("Total: {TQ: 0, Resets: 4, Inputs: 10}"));
    }
}
