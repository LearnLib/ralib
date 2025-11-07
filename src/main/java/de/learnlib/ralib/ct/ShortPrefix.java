package de.learnlib.ralib.ct;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class ShortPrefix extends Prefix {

	private final Map<ParameterizedSymbol, Branching> branching;

	private final TreeOracle oracle;

	private final ParameterizedSymbol[] inputs;

	public ShortPrefix(Prefix u, Bijection<DataValue> rpBijection, TreeOracle oracle, ParameterizedSymbol ... inputs) {
		super(u, rpBijection);
		this.oracle = oracle;
		this.inputs = inputs;
		branching = new LinkedHashMap<>();

		for (ParameterizedSymbol ps : inputs) {
			SDT[] sdts = getSDTs(ps);
			Branching b = oracle.getInitialBranching(this, ps, sdts);
			branching.put(ps, b);
		}
	}

	public ShortPrefix(Prefix u, TreeOracle oracle, ParameterizedSymbol ... inputs) {
		this(u, u.getRpBijection(), oracle, inputs);
	}

	public Branching getBranching(ParameterizedSymbol ps) {
		return branching.get(ps);
	}

	public void updateBranching() {
		for (ParameterizedSymbol ps : inputs) {
			SDT[] sdts = getSDTs(ps);
			Branching b = oracle.updateBranching(this, ps, branching.get(ps), sdts);
			branching.put(ps, b);
		}
	}

	public Set<Word<PSymbolInstance>> getInitialReprPrefixes(ParameterizedSymbol ps) {
		return branching.get(ps).getBranches().keySet();
	}
}
