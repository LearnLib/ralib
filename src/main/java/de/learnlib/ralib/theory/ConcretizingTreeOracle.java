package de.learnlib.ralib.theory;

import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.SLLambdaRestrictionBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class ConcretizingTreeOracle implements TreeOracle {

	private final TreeOracle oracle;
	private final Constants consts;

	public ConcretizingTreeOracle(TreeOracle oracle, Constants consts) {
		this.oracle = oracle;
		this.consts = consts;
	}

	@Override
	public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
		return oracle.treeQuery(prefix, suffix);
	}

	public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, Set<DataValue> memorable) {
		RegisterValuation regs = RegisterValuation.fromMemorable(prefix, memorable);
		ParameterValuation params = ParameterValuation.fromPSymbolWord(prefix);
		Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
		mapping.putAll(regs);
		mapping.putAll(params);
		mapping.putAll(consts);
		SymbolicSuffix concreteSuffix = SLLambdaRestrictionBuilder.concretize(suffix, mapping);
		return oracle.treeQuery(prefix, concreteSuffix);
//		return treeQuery(prefix, suffix);
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

	@Override
	public SymbolicSuffixRestrictionBuilder getRestrictionBuilder() {
		return oracle.getRestrictionBuilder();
	}

}
