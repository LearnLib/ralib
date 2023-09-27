package de.learnlib.ralib.learning.ralambda;

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
import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.SymbolicSuffix;
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
import net.automatalib.words.Word;

public class LearnSipIOTest extends RaLibTestSuite {
	@Test
	public void learnSipIO() {

        long seed = -1386796323025681754L;
        //long seed = (new Random()).nextLong();
        logger.log(Level.FINE, "SEED={0}", seed);
        final Random random = new Random(seed);

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

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo =
                new MultiTheorySDTLogicOracle(consts, solver);

        for (ParameterizedSymbol ps : actions) {
        	if (!DTLeaf.isInput(ps) && ps.getArity() > 0) {
//        	if (ps.getArity() > 0) {
        		mto.treeQuery(Word.epsilon(), new SymbolicSuffix(ps));
        		break;
        	}
        }

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        RaLambda ralambda = new RaLambda(mto, hypFactory, mlo, consts, true, actions);
        ralambda.setSolver(solver);

            IOEquivalenceTest ioEquiv = new IOEquivalenceTest(
                    model, teachers, consts, true, actions);

        IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);

        int check = 0;
        while (true && check < 100) {
            check++;
            ralambda.learn();
            Hypothesis hyp = ralambda.getHypothesis();

            DefaultQuery<PSymbolInstance, Boolean> ce =
                    ioEquiv.findCounterExample(hyp, null);

            if (ce == null) {
                break;
            }

            ce = loops.optimizeCE(ce.getInput(), hyp);
            ce = asrep.optimizeCE(ce.getInput(), hyp);
            ce = pref.optimizeCE(ce.getInput(), hyp);

            Assert.assertTrue(model.accepts(ce.getInput()));
            Assert.assertTrue(!hyp.accepts(ce.getInput()));

            ralambda.addCounterexample(ce);
        }

        RegisterAutomaton hyp = ralambda.getHypothesis();
        logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
        DefaultQuery<PSymbolInstance, Boolean> ce =
            ioEquiv.findCounterExample(hyp, null);

        Assert.assertNull(ce);
	}
}
