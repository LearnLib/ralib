package de.learnlib.ralib.oracles;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.sul.InputCounter;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;


/**
 * Logs input and reset cost for every tree query. 
 */
public class TreeOracleStatsLoggerWrapper implements TreeOracle{

	private TreeOracle treeOracle;
	private InputCounter counter;
	public TreeOracleStatsLoggerWrapper(TreeOracle treeOracle, InputCounter inputCounter) {
		this.treeOracle = treeOracle;
		this.counter = inputCounter;
	}
	
	@Override
	public TreeQueryResult treeQuery(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix) {
		long preResets = counter.getResets();
		long preInputs = counter.getInputs();
		TreeQueryResult queryResult = treeOracle.treeQuery(prefix, suffix);
		System.out.println("Num inputs: " + (counter.getInputs() - preInputs));
		System.out.println("Num resets: " + (counter.getResets() - preResets));
		return queryResult;
	}

	@Override
	public Branching getInitialBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv,
			SymbolicDecisionTree... sdts) {
		return treeOracle.getInitialBranching(prefix, ps, piv, sdts);
	}

	@Override
	public Branching updateBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, Branching current, PIV piv,
			SymbolicDecisionTree... sdts) {
		return treeOracle.updateBranching(prefix, ps, current, piv, sdts);
	}

}
