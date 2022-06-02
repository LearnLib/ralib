package de.learnlib.ralib.dt;

import java.util.HashSet;
import java.util.Set;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class DTLeaf extends DTNode {
	
	private Set<ShortPrefix> shortPrefixes;
	
	private Set<Word<PSymbolInstance>> otherPrefixes;
	
	public DTLeaf() {
		shortPrefixes = new HashSet<ShortPrefix>();
		otherPrefixes = new HashSet<Word<PSymbolInstance>>();
	}
	
	public DTLeaf(ShortPrefix as) {
		shortPrefixes = new HashSet<ShortPrefix>();
		otherPrefixes = new HashSet<Word<PSymbolInstance>>();
		shortPrefixes.add(as);
	}
	
	public void addPrefix(Word<PSymbolInstance> p) {
		otherPrefixes.add(p);
	}
	
	public void addShortPrefix(Word<PSymbolInstance> prefix, PIV registers) {
		if (otherPrefixes.contains(prefix))
			otherPrefixes.remove(prefix);
		shortPrefixes.add(new ShortPrefix(prefix, registers));
	}
	
	public Set<ShortPrefix> getShortPrefixes() {
		return shortPrefixes;
	}
	
	public Set<Word<PSymbolInstance>> getPrefixes() {
		return otherPrefixes;
	}
	
	public boolean isLeaf() {
		return true;
	}
}
