package de.learnlib.ralib.dt;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class PrefixSet {
	private Set<MappedPrefix> prefixes;
	
	public PrefixSet() {
		prefixes = new LinkedHashSet<MappedPrefix>();
	}
	
	public void add(MappedPrefix p) {
		prefixes.add(p);
	}
	
	public void add(Word<PSymbolInstance> p, PIV piv) {
		prefixes.add(new MappedPrefix(p, piv));
	}
	
	public boolean remove(MappedPrefix p) {
		return prefixes.remove(p);
	}
	
	public boolean removeIf(Predicate<? super MappedPrefix> filter) {
		return prefixes.removeIf(filter);
	}
	
	public boolean remove(Word<PSymbolInstance> p) {
		return prefixes.removeIf(mp -> mp.getPrefix().equals(p));
	}
	
	public Set<MappedPrefix> get() {
		return prefixes;
	}
	
	public MappedPrefix get(Word<PSymbolInstance> p) {
		for (MappedPrefix mp : prefixes) {
			if (mp.getPrefix().equals(p))
				return mp;
		}
		return null;
	}
	
	public Iterator<MappedPrefix> iterator() {
		return prefixes.iterator();
	}
	
	public boolean contains(MappedPrefix p) {
		return prefixes.contains(p);
	}
	
	public boolean contains(Word<PSymbolInstance> word) {
		return prefixes.stream().anyMatch(mp -> mp.getPrefix().equals(word));
	}
	
//	public boolean contains(Word<PSymbolInstance> p) {
//		Iterator<MappedPrefix> it = prefixes.iterator();
//		
//		while (it.hasNext()) {
//			MappedPrefix mp = it.next();
//			if (mp.getPrefix().equals(p))
//				return true;
//		}
//		return false;
//	}
	
	public int length() {
		return prefixes.size();
	}
	
	public String toString() {
		return prefixes.toString();
	}
}
