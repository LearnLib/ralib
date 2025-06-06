package de.learnlib.ralib.dt;

import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class ShortPrefix extends MappedPrefix {

	private final Map<ParameterizedSymbol, Branching> branching = new LinkedHashMap<ParameterizedSymbol, Branching>();

	public ShortPrefix(Word<PSymbolInstance> prefix) {
		super(prefix, new Bijection<>());
	}

	public ShortPrefix(MappedPrefix mp) {
		super(mp, mp.getRemapping());
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
