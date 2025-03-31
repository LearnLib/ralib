package de.learnlib.ralib.learning;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.words.PSymbolInstance;

public interface RaLearningAlgorithm {

	/**
	 * Creates a hypothesis consistent with the counterexamples added thus far.
	 */
    void learn();

	/**
	 * Provides a counterexample to the learner. Should be followed by {@link RaLearningAlgorithm#learn()}, which
	 * will prompt the learner to refine the hypothesis based on the supplied
	 * counterexample.
	 *
	 * @param ce the query which exposes diverging behavior, as posed to the real
	 *           SUL (i.e. with the SULs output).
	 *
	 */
    void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce);

	/**
	 * Returns the last created hypothesis.
	 */
    Hypothesis getHypothesis();

	void setStatisticCounter(QueryStatistics queryStats);

	QueryStatistics getQueryStatistics();

	RaLearningAlgorithmName getName();
}
