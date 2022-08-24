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

	public class Measurements {
		public int queries = 0;
		
		@Override
		public String toString() {
			return "{" + queries + "}";
		}
	}
	
	private final TreeOracle oracle;
	
	private Measurements result;
	
	public MeasuringOracle(TreeOracle oracle) {
		this.oracle = oracle;
		result = new Measurements();
		resetMeasurements();
	}
	
	@Override
	public TreeQueryResult treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
		result.queries++;
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

	public Measurements getMeasurements() {
		return result;
	}
	
	public void resetMeasurements() {
		result.queries = 0;
	}
}
