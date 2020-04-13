package de.learnlib.ralib.equivalence;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class IOCounterExampleSingleTransitionRemover implements IOCounterExampleOptimizer {

	private IOOracle sulOracle;

	public IOCounterExampleSingleTransitionRemover(IOOracle sulOracle) {
		this.sulOracle = sulOracle;
	}

	public DefaultQuery<PSymbolInstance, Boolean> optimizeCE(Word<PSymbolInstance> ce, Hypothesis hyp) {
		Word<PSymbolInstance> reducedCe = ce;
		int transIndex = 0;
		while (transIndex < reducedCe.length()) {
			Word<PSymbolInstance> testCe = reducedCe.subWord(0, transIndex)
					.concat(reducedCe.subWord(transIndex + 2, reducedCe.length()));
			Word<PSymbolInstance> tracedCe = sulOracle.trace(testCe);
			DefaultQuery<PSymbolInstance, Boolean> ceQuery = new DefaultQuery<PSymbolInstance, Boolean>(tracedCe,
					Boolean.TRUE);
			if (HypVerify.isCEForHyp(ceQuery, hyp)) {
				reducedCe = tracedCe;
			} else {
				transIndex += 2;
			}
		}
		return new DefaultQuery<>(reducedCe, Boolean.TRUE);
	}

}
