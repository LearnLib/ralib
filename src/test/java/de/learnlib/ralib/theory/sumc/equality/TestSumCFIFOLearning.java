package de.learnlib.ralib.theory.sumc.equality;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
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
import de.learnlib.ralib.example.sumc.equality.SumCFIFOSUL;
import de.learnlib.ralib.example.sumc.equality.SumCFreshFIFOSUL;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.CanonizingSULOracle;
import de.learnlib.ralib.sul.DeterminizerDataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.tools.theories.IntegerSumCEqualityTheory;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;

public class TestSumCFIFOLearning extends RaLibTestSuite {
	@Test
	public void learnSumCFIFO() {

		long seed = -4750580074638681519L;

		logger.log(Level.FINE, "SEED={0}", seed);
		final Random random = new Random(seed);

		int capacity = 3;
		int sumc = 1;
		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		DataValue[] sumConsts = new DataValue[] { new DataValue<Integer>(SumCFIFOSUL.INT_TYPE, sumc), };

		IntegerSumCEqualityTheory theory = new IntegerSumCEqualityTheory();
		teachers.put(SumCFIFOSUL.INT_TYPE, theory);
		theory.setCheckForFreshOutputs(false);
		theory.setUseSuffixOpt(true);
		theory.setType(SumCFIFOSUL.INT_TYPE);
		SumCFIFOSUL sul = new SumCFIFOSUL(capacity, sumc);

		JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
		Constants consts = new Constants(new SumConstants(sumConsts));
		theory.setConstants(consts);
		MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(sul, SumCFIFOSUL.ERROR, teachers, consts,
				jsolv, sul.getInputSymbols());

		IOOracle ioOracle = new CanonizingSULOracle(new DeterminizerDataWordSUL(teachers, consts, sul),
				SpecialSymbols.ERROR, new SymbolicTraceCanonizer(teachers, consts));

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
				1000, // 1000 runs
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
	public void learnSumCFreshFIFO() {

		long seed = -4750580074638681519L;

		logger.log(Level.FINE, "SEED={0}", seed);
		final Random random = new Random(seed);

		int capacity = 3;
		int sumc = 1;
		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		DataValue[] sumConsts = new DataValue[] { new DataValue<Integer>(SumCFreshFIFOSUL.INT_TYPE, sumc), };

		IntegerSumCEqualityTheory theory = new IntegerSumCEqualityTheory();
		teachers.put(SumCFreshFIFOSUL.INT_TYPE, theory);
		theory.setCheckForFreshOutputs(true);
		theory.setUseSuffixOpt(true);
		theory.setType(SumCFreshFIFOSUL.INT_TYPE);
		SumCFreshFIFOSUL sul = new SumCFreshFIFOSUL(capacity, sumc);

		JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
		Constants consts = new Constants(new SumConstants(sumConsts));
		theory.setConstants(consts);
		MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(sul, SumCFreshFIFOSUL.ERROR, teachers, consts,
				jsolv, sul.getInputSymbols());

		IOOracle ioOracle = new CanonizingSULOracle(new DeterminizerDataWordSUL(teachers, consts, sul),
				SumCFreshFIFOSUL.ERROR, new SymbolicTraceCanonizer(teachers, consts));

		MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, jsolv);

		TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> TestUtil.createBasicSimulatorMTO(hyp, teachers,
				consts, jsolv);

		RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, teachers, jsolv, sul.getActionSymbols());

		IORandomWalk iowalk = new IORandomWalk(random, new DeterminizerDataWordSUL(teachers, consts, sul), false, // do not
																	// draw
																	// symbols
																	// uniformly
				0.1, // reset probability
				0.5, // prob. of choosing a fresh data value
				1000, // 1000 runs
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
}
