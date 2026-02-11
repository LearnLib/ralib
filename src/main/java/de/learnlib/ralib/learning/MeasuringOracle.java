package de.learnlib.ralib.learning;

import java.util.Map;

import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class MeasuringOracle implements TreeOracle {

	private final TreeOracle oracle;

	private final Measurements result;

	public MeasuringOracle(TreeOracle oracle, Measurements m) {
		this.oracle = oracle;
		this.result = m;
	}

	@Override
	public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
		result.treeQueries++;
		SymbolicWord key = new SymbolicWord(prefix, suffix);
		if (result.treeQueryWords.containsKey(key))
			result.treeQueryWords.put(key, result.treeQueryWords.get(key) + 1);
		else
			result.treeQueryWords.put(key, 1);
		return oracle.treeQuery(prefix, suffix);
	}

	@Override
	public Branching getInitialBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, SDT... sdts) {
		return oracle.getInitialBranching(prefix, ps, sdts);
	}

	@Override
	public Branching updateBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, Branching current,
			SDT... sdts) {
		return oracle.updateBranching(prefix, ps, current, sdts);
	}

//	@Override
//	public Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
//			SDT sdt) {
//		return oracle.instantiate(prefix, suffix, sdt);
//	}

	@Override
	public SymbolicSuffixRestrictionBuilder getRestrictionBuilder() {
		return oracle.getRestrictionBuilder();
	}
}
