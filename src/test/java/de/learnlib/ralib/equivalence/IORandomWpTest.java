package de.learnlib.ralib.equivalence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IORandomWpEquivalenceOracle.Options;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.sllambda.SLLambda;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class IORandomWpTest extends RaLibTestSuite {

    @Test
    public void testRandomWpOnSIP() {
        final int seed = 123456;
        final int minimalSize = 1;
        final int rndLength = 2;
        final boolean uniform = false;
        final double freshProbability = 0.2;
        final long maxRuns = 50;

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/sip.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            IntegerEqualityTheory theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);
            teachers.put(t, theory);
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);

        teachers.values().stream().forEach((t) -> {
            ((EqualityTheory)t).setFreshValues(true, ioCache);
        });

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, teachers, consts, solver);

        SLLambda sllambda = new SLLambda(mto, teachers, consts, true, solver, actions);
        sllambda.learn();

        Hypothesis hyp = sllambda.getHypothesis();

        Options options = new Options(new Random(seed),
                minimalSize,
                rndLength,
                uniform,
                freshProbability,
                maxRuns);
        IORandomWpEquivalenceOracle wpEq = new IORandomWpEquivalenceOracle(sul, teachers, consts, options, inputs);

        DefaultQuery<PSymbolInstance, Boolean> ce = wpEq.findCounterExample(hyp, null);
        while (ce != null) {
            sllambda.addCounterexample(ce);
            sllambda.learn();
            hyp = sllambda.getHypothesis();
            ce = wpEq.findCounterExample(hyp, null);
        }

        IOEquivalenceTest ioEquiv = new IOEquivalenceTest(
                    model, teachers, consts, true, actions);
        Assert.assertNull(ioEquiv.findCounterExample(hyp, null));
    }
}
