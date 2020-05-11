package de.learnlib.ralib;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.testng.Assert;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.HypVerify;
import de.learnlib.ralib.equivalence.IOCounterExampleLoopRemover;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.CanonizingSULOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.DeterminizerDataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/*
 * It should be possible to use an experiment builder as an alternative to inheritance/long param calls
 */
public class RaLibLearningTestSuite extends RaLibTestSuite {
	
	private Consumer<RegisterAutomaton> hypValidator;
	
	public RaLibLearningTestSuite() {
		this.hypValidator = (hyp) -> {};
	}
	
	/**
	 * 
	 * @return
	 */
	protected long[] getSeeds() {
		return new long[] { 0 };
	}
	
	protected void setHypValidator(Consumer<RegisterAutomaton> hypValidator) {
		this.hypValidator = hypValidator;
	}

	/**
	 * Launches learning experiments for IO, one of each seed in
	 * {@link RaLibLearningTestSuite#getSeeds()}.
	 * 
	 * @param sul
	 *            the system to be tested
	 * @param teachers
	 *            theories fully configured
	 * @param consts
	 *            constants fully defined
	 * @param freshSupport
	 *            enable fresh value support for SUL
	 * @param solver
	 *            constraint solver to use
	 * @param actionSymbols
	 *            inputs and output symbols applied during the learning process
	 * @param errorSymbol
	 *            special error symbol
	 * @param equOracleBuilder
	 */
	protected void runIOLearningExperiments(DataWordSUL sul, Map<DataType, Theory> teachers, Constants consts,
			boolean freshSupport,
			// boolean canonizationSupport,
			ConstraintSolver solver, ParameterizedSymbol[] actionSymbols, ParameterizedSymbol errorSymbol, 
			IOEquivalenceOracleBuilder equOracleBuilder) {
		ParameterizedSymbol[] inputSymbols = Arrays.stream(actionSymbols).filter(s -> (s instanceof InputSymbol))
				.toArray(ParameterizedSymbol[]::new);

		for (long seed : getSeeds()) {
			logger.log(Level.FINE, "SEED={0}", seed);
			Random random = new Random(seed);
			MultiTheoryTreeOracle mto = null;
			if (freshSupport) {
				mto = TestUtil.createMTOWithFreshValueSupport(sul, errorSymbol, teachers, consts, solver, inputSymbols);
			} else {
				mto = TestUtil.createBasicMTO(sul, errorSymbol, teachers, consts, solver, inputSymbols);
			}

			MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

			TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> TestUtil.createBasicSimulatorMTO(hyp, teachers,
					consts, solver);
			
			// the oracle used for CE optimization
			IOOracle ioOracle = null;
			if (freshSupport) {
				ioOracle = new CanonizingSULOracle(new DeterminizerDataWordSUL(teachers, consts, sul), errorSymbol,
						new SymbolicTraceCanonizer(teachers, consts));
			} else {
				ioOracle = new BasicSULOracle(sul, errorSymbol);
			}

			RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, teachers, solver, actionSymbols);
			
			IOEquivalenceOracle equOracle = equOracleBuilder.build(random, sul, teachers, consts, inputSymbols);

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

				ce = equOracle.findCounterExample(hyp, null);

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
			hypValidator.accept(hyp);

			logger.log(Level.FINE, "LAST:------------------------------------------------");
			logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
			logger.log(Level.FINE, "Resets: {0}", sul.getResets());
			logger.log(Level.FINE, "Inputs: {0}", sul.getInputs());
		}
	}
	
	
	protected void runIOLearningExperiments(DataWordSUL sul, Map<DataType, Theory> teachers, Constants consts,
			boolean freshSupport,
			// boolean canonizationSupport,
			ConstraintSolver solver, ParameterizedSymbol[] actionSymbols, ParameterizedSymbol errorSymbol) {
		runIOLearningExperiments(sul, teachers, consts, freshSupport, solver, 
				actionSymbols, errorSymbol, new IOEquivalenceOracleBuilder());
	}
	
}
