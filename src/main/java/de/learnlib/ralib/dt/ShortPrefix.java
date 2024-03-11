package de.learnlib.ralib.dt;

import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.oracles.Branching;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.symbol.ParameterizedSymbol;
import net.automatalib.word.Word;

public class ShortPrefix extends MappedPrefix {

	private final Map<ParameterizedSymbol, Branching> branching = new LinkedHashMap<ParameterizedSymbol, Branching>();

	public ShortPrefix(Word<PSymbolInstance> prefix) {
		super(prefix);
	}

	public ShortPrefix(Word<PSymbolInstance> prefix, PIV piv) {
		super(prefix, piv);
	}

	public ShortPrefix(MappedPrefix mp) {
		super(mp);
	}

	public Map<ParameterizedSymbol, Branching> getBranching() {
		return branching;
	}

	public Branching getBranching(ParameterizedSymbol ps) {
		return branching.get(ps);
	}

	void putBranching(ParameterizedSymbol ps, Branching b) {
		branching.put(ps, b);
	}
}
