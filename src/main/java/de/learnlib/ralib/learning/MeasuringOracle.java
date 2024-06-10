package de.learnlib.ralib.learning;

import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class MeasuringOracle extends MultiTheoryTreeOracle {

//	private final TreeOracle oracle;

	private final Measurements result;

//	public MeasuringOracle(TreeOracle oracle, Measurements m) {
//		this.oracle = oracle;
//		this.result = m;
//	}

	public MeasuringOracle(DataWordOracle oracle, Map<DataType, Theory> teachers, Constants constants,
            ConstraintSolver solver, Measurements m) {
		super(oracle, teachers, constants, solver);
		result = m;
	}

	public MeasuringOracle(MultiTheoryTreeOracle mto, Measurements m) {
		super(mto);
		result = m;
	}

	@Override
	public TreeQueryResult treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
		result.treeQueries++;
		SymbolicWord key = new SymbolicWord(prefix, suffix);
		if (result.treeQueryWords.containsKey(key))
			result.treeQueryWords.put(key, result.treeQueryWords.get(key) + 1);
		else
			result.treeQueryWords.put(key, 1);
		return super.treeQuery(prefix, suffix);
	}

	@Override
	public Branching getInitialBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv,
			SymbolicDecisionTree... sdts) {
		return super.getInitialBranching(prefix, ps, piv, sdts);
	}

	@Override
	public Branching updateBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, Branching current, PIV piv,
			SymbolicDecisionTree... sdts) {
		return super.updateBranching(prefix, ps, current, piv, sdts);
	}

	@Override
	public Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
			SymbolicDecisionTree sdt, PIV piv) {
		return super.instantiate(prefix, suffix, sdt, piv);
	}
}
