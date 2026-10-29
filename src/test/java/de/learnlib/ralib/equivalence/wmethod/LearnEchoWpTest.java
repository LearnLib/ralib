package de.learnlib.ralib.equivalence.wmethod;

import static de.learnlib.ralib.example.repeater.RepeaterSUL.IPUT;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.OECHO;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.ONOK;
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
import de.learnlib.ralib.equivalence.IOWpEquivalenceOracle;
import de.learnlib.ralib.example.repeater.RepeaterSUL;
import de.learnlib.ralib.learning.sllambda.SLLambda;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class LearnEchoWpTest extends RaLibTestSuite {

    @Test
    private void learnEchoWpTest() {
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

        IOWpEquivalenceOracle wpEq = new IOWpEquivalenceOracle(sul, 2, teachers, consts, sul.getInputSymbols());

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(oracle, teachers, consts, solver);

        SLLambda sllambda = new SLLambda(mto, teachers, consts, true, solver, sul.getActionSymbols());

        sllambda.learn();
        RegisterAutomaton hyp = sllambda.getHypothesis();

        DefaultQuery<PSymbolInstance, Boolean> ce = wpEq.findCounterExample(hyp, null);
        while (ce != null) {
            sllambda.addCounterexample(ce);
            sllambda.learn();
            hyp = sllambda.getHypothesis();
            ce = wpEq.findCounterExample(hyp, null);
        }

        Word<PSymbolInstance> word = Word.fromSymbols(
                new PSymbolInstance(IPUT, new DataValue(TINT, BigDecimal.ZERO)),
                new PSymbolInstance(OECHO, new DataValue(TINT, BigDecimal.ZERO)),
                new PSymbolInstance(IPUT, new DataValue(TINT, BigDecimal.ONE)),
                new PSymbolInstance(OECHO, new DataValue(TINT, BigDecimal.ONE)),
                new PSymbolInstance(IPUT, new DataValue(TINT, new BigDecimal(2))),
                new PSymbolInstance(ONOK));

        Assert.assertEquals(hyp.getStates().size(), 7);
        Assert.assertTrue(hyp.accepts(word));
    }
}
