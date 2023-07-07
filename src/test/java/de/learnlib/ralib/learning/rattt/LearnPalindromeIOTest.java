package de.learnlib.ralib.learning.rattt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
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
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class LearnPalindromeIOTest extends RaLibTestSuite {

    @Test
    public void learnPalindromeIO() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/palindrome.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();
        logger.log(Level.FINE, "SYS: {0}", model);

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            TypedTheory<Integer> theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);
            teachers.put(t, theory);
        });

        ConstraintSolver solver = new SimpleConstraintSolver();

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(
                consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        RaTTT rattt = new RaTTT(mto, hypFactory, mlo, consts, true, actions);
        rattt.setSolver(solver);

        IOEquivalenceTest ioEquiv = new IOEquivalenceTest(
                model, teachers, consts, true, actions);

        int check = 0;
        while (true && check < 10) {

            check++;
            rattt.learn();
            Hypothesis hyp = rattt.getHypothesis();
            logger.log(Level.FINE, "HYP: {0}", hyp);


            DefaultQuery<PSymbolInstance, Boolean> ce =
                    ioEquiv.findCounterExample(hyp, null);

            logger.log(Level.FINE, "CE: {0}", ce);
            if (ce == null) {
                break;
            }

            Assert.assertTrue(model.accepts(ce.getInput()));
            Assert.assertTrue(!hyp.accepts(ce.getInput()));

            rattt.addCounterexample(ce);
        }

        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
        DefaultQuery<PSymbolInstance, Boolean> ce =
            ioEquiv.findCounterExample(hyp, null);

        Assert.assertNull(ce);
//        Assert.assertEquals(hyp.getStates().size(), 5);
        Assert.assertEquals(hyp.getTransitions().size(), 16);
    }
}
