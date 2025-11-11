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

/**
 * Data structure for storing branching information of a short prefix,
 * in addition to the SDTs from the suffixes along its path stored
 * in {@link Prefix}.
 * 
 * @author fredrik
 */
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

	/**
	 * Update the branching according to the SDTs computed for the suffixes along the path to
	 * the leaf in which this short prefix is stored.
	 */
	public void updateBranching() {
		for (ParameterizedSymbol ps : inputs) {
			SDT[] sdts = getSDTs(ps);
			Branching b = oracle.updateBranching(this, ps, branching.get(ps), sdts);
			branching.put(ps, b);
		}
	}
}
