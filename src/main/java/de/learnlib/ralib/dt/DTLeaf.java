package de.learnlib.ralib.dt;

import java.util.HashSet;
import java.util.Set;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class DTLeaf extends DTNode {
	
	//private Set<MappedPrefix> shortPrefixes;
	private PrefixSet shortPrefixes;
	
	//private Set<MappedPrefix> otherPrefixes;
	private PrefixSet otherPrefixes;
	
	public DTLeaf() {
		super();
	//	shortPrefixes = new HashSet<MappedPrefix>();
	//	otherPrefixes = new HashSet<MappedPrefix>();
		shortPrefixes = new PrefixSet();
		otherPrefixes = new PrefixSet();
	}
	
	public DTLeaf(Word<PSymbolInstance> p) {
		super();
	//	shortPrefixes = new HashSet<MappedPrefix>();
	//	otherPrefixes = new HashSet<MappedPrefix>();
		shortPrefixes = new PrefixSet();
		otherPrefixes = new PrefixSet();
		shortPrefixes.add(new MappedPrefix(p, new PIV()));
	}
	
	public DTLeaf(MappedPrefix as) {
		super();
	//	shortPrefixes = new HashSet<MappedPrefix>();
	//	otherPrefixes = new HashSet<MappedPrefix>();
		shortPrefixes = new PrefixSet();
		otherPrefixes = new PrefixSet();
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
	
	public boolean removeShortPrefix(MappedPrefix p) {
		return shortPrefixes.remove(p);
	}
	
	public boolean removeShortPrefix(Word<PSymbolInstance> p) {
		return shortPrefixes.remove(p);
	}
	
	public void clear() {
		shortPrefixes = new PrefixSet();
		otherPrefixes = new PrefixSet();
	}
	
	//public Set<MappedPrefix> getShortPrefixes() {
	public PrefixSet getShortPrefixes() {
		return shortPrefixes;
	}
	
	//public Set<MappedPrefix> getPrefixes() {
	public PrefixSet getPrefixes() {
		return otherPrefixes;
	}
	
	public boolean isLeaf() {
		return true;
	}
}
