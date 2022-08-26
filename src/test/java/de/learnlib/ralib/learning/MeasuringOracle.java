package de.learnlib.ralib.learning;

import java.util.Map;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class MeasuringOracle implements TreeOracle {
	
	private final TreeOracle oracle;
	
	private final Measurements result;
	
	public MeasuringOracle(TreeOracle oracle, Measurements m) {
		this.oracle = oracle;
		this.result = m;
	}
	
	@Override
	public TreeQueryResult treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
		result.treeQueries++;
		SymbolicWord key = new SymbolicWord(prefix, suffix);
		if (result.treeQueryWords.containsKey(key))
			result.treeQueryWords.put(key, result.treeQueryWords.get(key) + 1);
		else
			result.treeQueryWords.put(key, 1);
		return oracle.treeQuery(prefix, suffix);
	}

	@Override
	public Branching getInitialBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv,
			SymbolicDecisionTree... sdts) {
		return oracle.getInitialBranching(prefix, ps, piv, sdts);
	}

	@Override
	public Branching updateBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, Branching current, PIV piv,
			SymbolicDecisionTree... sdts) {
		return oracle.updateBranching(prefix, ps, current, piv, sdts);
	}

	@Override
	public Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
			SymbolicDecisionTree sdt, PIV piv) {
		return oracle.instantiate(prefix, suffix, sdt, piv);
	}
}
