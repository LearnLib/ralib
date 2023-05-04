package de.learnlib.ralib.learning;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class Measurements {
	public int treeQueries = 0;
	public long resets = 0;
	public long inputs = 0;
	public final Map<SymbolicWord, Integer> treeQueryWords = new LinkedHashMap<SymbolicWord, Integer>();
	public final Collection<Word<PSymbolInstance>> ces = new LinkedHashSet<Word<PSymbolInstance>>();

	public Measurements() {
	}

	public Measurements(Measurements m) {
		treeQueries = m.treeQueries;
		resets = m.resets;
		treeQueryWords.putAll(m.treeQueryWords);
		ces.addAll(m.ces);
	}

	public void reset() {
		treeQueries = 0;
		resets = 0;
		treeQueryWords.clear();
		ces.clear();
	}

	@Override
	public String toString() {
		return "{TQ: " + treeQueries + ", Resets: " + resets + ", Inputs: " + inputs + "}";
	}
}
