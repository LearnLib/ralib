package de.learnlib.ralib.theory.sumc.inequality;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibLearningTest;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.equivalence.HypVerify;
import de.learnlib.ralib.equivalence.IOCounterExampleLoopRemover;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.example.sumc.inequality.OneWayFreshTCPSUL;
import de.learnlib.ralib.example.sumc.inequality.OneWayTCPSUL;
import de.learnlib.ralib.example.sumc.inequality.TCPExample.Option;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.CanonizingSULOracle;
import de.learnlib.ralib.sul.DeterminizerDataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleSumCInequalityTheory;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;

public class TestOneWayTCPLearning extends RaLibLearningTest{
	
	@Test
	public void testOneWayTCPLearning() {
		long seed = -4750580074638681520L;

		logger.log(Level.FINE, "SEED={0}", seed);
		final Random random = new Random(seed);
		
    	Double win = 100.0;
    	DoubleSumCInequalityTheory theory = new DoubleSumCInequalityTheory();
    	Constants consts = new Constants();
    	consts.setSumC(new SumConstants(new DataValue<Double>(OneWayTCPSUL.DOUBLE_TYPE, 1.0), 
    			new DataValue<Double>(OneWayTCPSUL.DOUBLE_TYPE, win)));
    	theory.setType(OneWayTCPSUL.DOUBLE_TYPE);
    	theory.setConstants(consts);
        		
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(OneWayTCPSUL.DOUBLE_TYPE, theory);

        OneWayTCPSUL sul = new OneWayTCPSUL(win);
        sul.configure(Option.WIN_CONNECTING_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
		
		IOOracle ioOracle = new BasicSULOracle(sul, OneWayTCPSUL.ERROR); 
		
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, OneWayTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());

		MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, jsolv);

		TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> TestUtil.createBasicSimulatorMTO(hyp, teachers,
				consts, jsolv);
        
		RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, teachers, jsolv, sul.getActionSymbols());

		IORandomWalk iowalk = new IORandomWalk(random, sul, false, // do not
																	// draw
																	// symbols
																	// uniformly
				0.1, // reset probability
				0.5, // prob. of choosing a fresh data value
				10000, // 10000 runs
				100, // max depth
				consts, false, // reset runs
				teachers, sul.getInputSymbols());

		IOCounterExampleLoopRemover loops = new IOCounterExampleLoopRemover(ioOracle);
		IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
		IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);
		DefaultQuery<PSymbolInstance, Boolean> ce = null;

		int check = 0;
		while (true && check < 100) {
			check++;
			rastar.learn();
			Hypothesis hyp = rastar.getHypothesis();
			if (ce != null) {
				// the last CE should not be a CE for the current hypothesis
				Assert.assertFalse(HypVerify.isCEForHyp(ce, hyp));
			}

			ce = iowalk.findCounterExample(hyp, null);

			// System.out.println("CE: " + ce);
			if (ce == null) {
				break;
			}

			ce = loops.optimizeCE(ce.getInput(), hyp);
			Assert.assertTrue(HypVerify.isCEForHyp(ce, hyp));
			ce = asrep.optimizeCE(ce.getInput(), hyp);
			Assert.assertTrue(HypVerify.isCEForHyp(ce, hyp));
			ce = pref.optimizeCE(ce.getInput(), hyp);
			Assert.assertTrue(HypVerify.isCEForHyp(ce, hyp));
			rastar.addCounterexample(ce);
		}

		Assert.assertNull(ce);
		RegisterAutomaton hyp = rastar.getHypothesis();

		logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
		logger.log(Level.FINE, "Resets: {0}", sul.getResets());
		logger.log(Level.FINE, "Inputs: {0}", sul.getInputs());
	}
	
	@Test
	public void testOneWayFreshTCPLearning() {
		long seed = -4750580074638681519L;

		logger.log(Level.FINE, "SEED={0}", seed);
		final Random random = new Random(seed);
		
    	Double win = 100.0;
    	DoubleSumCInequalityTheory theory = new DoubleSumCInequalityTheory();
    	Constants consts = new Constants();
    	consts.setSumC(new SumConstants(new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0), 
    			new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, win)));
    	theory.setType(OneWayFreshTCPSUL.DOUBLE_TYPE);
    	theory.setConstants(consts);
    	theory.setCheckForFreshOutputs(true);
        		
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(OneWayFreshTCPSUL.DOUBLE_TYPE, theory);

        OneWayFreshTCPSUL sul = new OneWayFreshTCPSUL(win);
        sul.configure(Option.WIN_CONNECTING_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
		
		IOOracle ioOracle = new CanonizingSULOracle(new DeterminizerDataWordSUL(teachers, consts, sul),
				OneWayFreshTCPSUL.ERROR, new SymbolicTraceCanonizer(teachers, consts));
		
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, OneWayFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());

		MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, jsolv);

		TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> TestUtil.createBasicSimulatorMTO(hyp, teachers,
				consts, jsolv);
        
		RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, teachers, jsolv, sul.getActionSymbols());

		IORandomWalk iowalk = new IORandomWalk(random, new DeterminizerDataWordSUL(teachers, consts, sul), false, // do not
																	// draw
																	// symbols
																	// uniformly
				0.1, // reset probability
				0.1, // prob. of choosing a fresh data value
				10000, // 10000 runs
				20, // max depth
				consts, false, // reset runs
				teachers, sul.getInputSymbols());

		IOCounterExampleLoopRemover loops = new IOCounterExampleLoopRemover(ioOracle);
		IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
		IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);
		DefaultQuery<PSymbolInstance, Boolean> ce = null;

		int check = 0;
		while (true && check < 100) {
			check++;
			rastar.learn();
			Hypothesis hyp = rastar.getHypothesis();
			if (ce != null) {
				// the last CE should not be a CE for the current hypothesis
				Assert.assertFalse(HypVerify.isCEForHyp(ce, hyp));
			}

			ce = iowalk.findCounterExample(hyp, null);

			// System.out.println("CE: " + ce);
			if (ce == null) {
				break;
			}

			ce = loops.optimizeCE(ce.getInput(), hyp);
			Assert.assertTrue(HypVerify.isCEForHyp(ce, hyp));
			ce = asrep.optimizeCE(ce.getInput(), hyp);
			Assert.assertTrue(HypVerify.isCEForHyp(ce, hyp));
			ce = pref.optimizeCE(ce.getInput(), hyp);
			Assert.assertTrue(HypVerify.isCEForHyp(ce, hyp));
			rastar.addCounterexample(ce);
		}

		Assert.assertNull(ce);
		RegisterAutomaton hyp = rastar.getHypothesis();

		logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
		logger.log(Level.FINE, "Resets: {0}", sul.getResets());
		logger.log(Level.FINE, "Inputs: {0}", sul.getInputs());
	}
}
