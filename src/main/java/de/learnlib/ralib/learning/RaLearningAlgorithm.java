package de.learnlib.ralib.learning;

import de.learnlib.api.query.DefaultQuery;
import de.learnlib.ralib.words.PSymbolInstance;

public interface RaLearningAlgorithm {

	/**
	 * Creates a hypothesis consistent with the counterexamples added thus far.
	 */
	public void learn();

	/**
	 * Provides a counterexample to the learner. Should be followed by {@link RaLearningAlgorithm#learn()}, which
	 * will prompt the learner to refine the hypothesis based on the supplied
	 * counterexample.
	 *
	 * @param ce the query which exposes diverging behavior, as posed to the real
	 *           SUL (i.e. with the SULs output).
	 *
	 */
	public void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce);

	/**
	 * Returns the last created hypothesis.
	 */
	public Hypothesis getHypothesis();

	public void setStatisticCounter(QueryStatistics queryStats);

	public QueryStatistics getQueryStatistics();

	public RaLearningAlgorithmName getName();
}
