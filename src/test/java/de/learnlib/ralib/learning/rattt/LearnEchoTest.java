package de.learnlib.ralib.learning.rattt;

import static de.learnlib.ralib.example.repeater.RepeaterSUL.IPUT;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.OECHO;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.ONOK;
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
import de.learnlib.ralib.example.repeater.RepeaterSUL;
import de.learnlib.ralib.learning.Hypothesis;
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

public class LearnEchoTest extends RaLibTestSuite {

    @Test
    public void learnEchoTest() {

        Constants consts = new Constants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory theory = new IntegerEqualityTheory(TINT);
        theory.setUseSuffixOpt(true);
        teachers.put(TINT, theory);

        RepeaterSUL sul = new RepeaterSUL(-1, 4);
        IOOracle ioOracle = new SULOracle(sul, RepeaterSUL.ERROR);
	IOCache ioCache = new IOCache(ioOracle);
	IOFilter oracle = new IOFilter(ioCache, sul.getInputSymbols());

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(oracle, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        RaLambda learner = new RaLambda(mto, hypFactory, mlo, consts, true, sul.getActionSymbols());
        learner.learn();

        Word<PSymbolInstance> ce = Word.fromSymbols(
       	        new PSymbolInstance(IPUT, new DataValue(TINT, 0)),
       	        new PSymbolInstance(OECHO, new DataValue(TINT, 0)),
       	        new PSymbolInstance(IPUT, new DataValue(TINT, 1)),
       	        new PSymbolInstance(OECHO, new DataValue(TINT, 1)),
       	        new PSymbolInstance(IPUT, new DataValue(TINT, 2)),
       	        new PSymbolInstance(OECHO, new DataValue(TINT, 2)),
       	        new PSymbolInstance(IPUT, new DataValue(TINT, 3)),
       	        new PSymbolInstance(OECHO, new DataValue(TINT, 3)),
       	        new PSymbolInstance(IPUT, new DataValue(TINT, 4)),
       	        new PSymbolInstance(ONOK));

        learner.addCounterexample(new DefaultQuery<>(ce, true));
        learner.learn();

        Hypothesis hyp = learner.getHypothesis();

        Assert.assertEquals(hyp.getStates().size(), 11);
        Assert.assertTrue(hyp.accepts(ce));
    }
}
