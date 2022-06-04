package de.learnlib.ralib.dt;

import java.util.HashSet;
import java.util.Set;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class DTLeaf extends DTNode {
	
	private Set<MappedPrefix> shortPrefixes;
	
	private Set<MappedPrefix> otherPrefixes;
	
	public DTLeaf() {
		super();
		shortPrefixes = new HashSet<MappedPrefix>();
		otherPrefixes = new HashSet<MappedPrefix>();
	}
	
	public DTLeaf(Word<PSymbolInstance> p) {
		super();
		shortPrefixes = new HashSet<MappedPrefix>();
		otherPrefixes = new HashSet<MappedPrefix>();
		shortPrefixes.add(new MappedPrefix(p, new PIV()));
	}
	
	public DTLeaf(MappedPrefix as) {
		super();
		shortPrefixes = new HashSet<MappedPrefix>();
		otherPrefixes = new HashSet<MappedPrefix>();
		shortPrefixes.add(as);
	}
	
	public void addPrefix(Word<PSymbolInstance> p) {
		otherPrefixes.add(new MappedPrefix(p));
	}
	
	public void addPrefix(MappedPrefix p) {
		otherPrefixes.add(p);
	}
	
	public void addShortPrefix(Word<PSymbolInstance> prefix, PIV registers) {
		addShortPrefix(new MappedPrefix(prefix, registers));
	}
	
	public void addShortPrefix(MappedPrefix prefix) {
		if (otherPrefixes.contains(prefix))
			otherPrefixes.remove(prefix);
		shortPrefixes.add(prefix);
	}
	
	public Set<MappedPrefix> getShortPrefixes() {
		return shortPrefixes;
	}
	
	public Set<MappedPrefix> getPrefixes() {
		return otherPrefixes;
	}
	
	public boolean isLeaf() {
		return true;
	}
}
