package de.learnlib.ralib;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.testng.Assert;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.MeasuringOracle;
import de.learnlib.ralib.learning.RaLearningAlgorithm;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.learning.rattt.RaTTT;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 * Class for running RA learning experiments. 
 */
public class RaLibLearningExperimentRunner {

	private long seed;
	private Logger logger;
	
	/**
	 * RA Learning Algorithm default settings
	 */
	private boolean useOldAnalyzer = false;
	
	
	/**
	 * Equivalence Oracle default settings
	 */
	private long maxRuns = 1000; // maximum number of runs
	private double resetProbability = 0.1; // reset probability
	private double freshProbability = 0.5; // prob. of choosing a fresh data value
	private int maxDepth = 20; // max depth
	
	private final Measurements measures = new Measurements();

	public RaLibLearningExperimentRunner(Logger logger) {
		seed = 0;
		this.logger = logger;
	}
	
	public Measurements getMeasurements() {
		return new Measurements(measures);
	}
	
	public void resetMeasurements() {
		measures.reset();
	}
	
	public void setMaxRuns(long maxRuns) {
		this.maxRuns = maxRuns;
	}

	public void setResetProbability(double resetProbability) {
		this.resetProbability = resetProbability;
	}

	public void setFreshProbability(double freshProbability) {
		this.freshProbability = freshProbability;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}
	
	public void setUseOldAnalyzer(boolean useOldAnalyzer) {
		this.useOldAnalyzer = useOldAnalyzer;
	}

	/**
	 * Launches a learning experiments for acceptor RAs.
	 * 
	 */
	public Hypothesis run(RaLearningAlgorithmName algorithmName, DataWordOracle dataOracle,
			Map<DataType, Theory> teachers, Constants consts, ConstraintSolver solver,
			ParameterizedSymbol[] actionSymbols) {

		try {
			logger.log(Level.INFO, "SEED={0}", seed);
			Random random = new Random(seed);
			CacheDataWordOracle ioCache = new CacheDataWordOracle(dataOracle);
			MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(ioCache, teachers, consts, solver), measures);

			MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

			TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> TestUtil.createSimulatorMTO(hyp, teachers, consts,
					solver);

			RaLearningAlgorithm learner = null;
			switch (algorithmName) {
			case RASTAR:
				learner = new RaStar(mto, hypFactory, mlo, consts, false, actionSymbols);
				break;
			case RATTT:
				learner = new RaTTT(mto, hypFactory, mlo, consts, false, useOldAnalyzer, actionSymbols);
				break;
			default:
				throw new UnsupportedOperationException(String.format("Algorithm %s not supported", algorithmName));
			}
			DefaultQuery<PSymbolInstance, Boolean> ce = null;
			IOEquivalenceOracle eqOracle = new RandomWalk(random, ioCache, 
					resetProbability, // reset probability
					freshProbability, // prob. of choosing a fresh data value
					maxRuns, // number of runs
					maxDepth, // max depth
					teachers,
					consts, Arrays.asList(actionSymbols));

			int check = 0;
			while (true && check < 100) {
				check++;
				learner.learn();
				Hypothesis hyp = learner.getHypothesis();
				ce = eqOracle.findCounterExample(hyp, null);
				if (ce == null) {
					break;
				}
				logger.log(Level.FINEST, "CE: {0}", ce);
				assertCE(ce, true, hyp);
				learner.addCounterexample(ce);
				measures.ces.add(ce.getInput());
			}
			
			measures.memQueries = ioCache.getQueryCount();
			
			Assert.assertNull(ce);
			Hypothesis hyp = learner.getHypothesis();

			logger.log(Level.FINE, "LAST:------------------------------------------------");
			logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
			logger.log(Level.FINE, "Queries: {0}", ioCache.getQueryCount());
			return hyp;
		} catch (Exception e) {
			Assert.fail(String.format("Learning experiment failed for seed %d. Cause: %s", seed, e), e);
		}
		
		return null;
	}

	private void assertCE(DefaultQuery<PSymbolInstance, Boolean> ce, boolean isCE, RegisterAutomaton ra) {
		Assert.assertEquals(ce.getOutput().equals(ra.accepts(ce.getInput())), !isCE);
	}
}