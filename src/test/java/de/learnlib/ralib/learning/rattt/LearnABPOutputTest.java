package de.learnlib.ralib.learning.rattt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.api.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.equivalence.IORandomWalk;
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
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class LearnABPOutputTest extends RaLibTestSuite {
	@Test
	public void learnABPOutput() {

		long seed = -1297170870937649002L;
		final Random random = new Random(seed);

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/abp.output.xml");

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

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo =
                new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        RaLambda rattt = new RaLambda(mto, hypFactory, mlo, consts, true, actions);
        rattt.setSolver(solver);

        IOEquivalenceTest ioEquiv = new IOEquivalenceTest(
                model, teachers, consts, true, actions);

        IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);

        DefaultQuery<PSymbolInstance, Boolean> ce  = null;

        IORandomWalk randomWalk = new IORandomWalk(random,
        		sul,
        		false,
        		0.1,
        		0.8,
        		10000,
        		100,
        		consts,
        		false,
                false,
        		teachers,
        		inputs);

        int check = 0;
        while (true && check < 100) {

            check++;
            rattt.learn();
            Hypothesis hyp = rattt.getHypothesis();

            ce = null;

            boolean nullCe = false;
            for (int i=0; i<3; i++) {

                DefaultQuery<PSymbolInstance, Boolean> ce2 = null;

                ce2 = randomWalk.findCounterExample(hyp, null);

                if (ce2 == null) {
                    nullCe = true;
                    break;
                }

               ce2 = loops.optimizeCE(ce2.getInput(), hyp);
               ce2 = asrep.optimizeCE(ce2.getInput(), hyp);
               ce2 = pref.optimizeCE(ce2.getInput(), hyp);
               ce = (ce == null || ce.getInput().length() > ce2.getInput().length()) ?
                        ce2 : ce;
            }

            if (nullCe) {
            	ce = ioEquiv.findCounterExample(hyp, null);
            	if (ce == null)
            		break;
            }

            Assert.assertTrue(model.accepts(ce.getInput()));
            Assert.assertTrue(!hyp.accepts(ce.getInput()));

            rattt.addCounterexample(ce);
        }

        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
        ce = ioEquiv.findCounterExample(hyp, null);

        Assert.assertNull(ce);
	}
}
