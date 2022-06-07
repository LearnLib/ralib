package de.learnlib.ralib.dt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class PrefixSet {
	private Set<MappedPrefix> prefixes;
	
	public PrefixSet() {
		prefixes = new HashSet<MappedPrefix>();
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
	
	public boolean remove(Word<PSymbolInstance> p) {
		return prefixes.removeIf(mp -> mp.getPrefix().equals(p));
	}
	
	public Set<MappedPrefix> get() {
		return prefixes;
	}
	
	public Iterator<MappedPrefix> iterator() {
		return prefixes.iterator();
	}
	
	public boolean contains(MappedPrefix p) {
		return prefixes.contains(p);
	}
	
	public boolean contains(Word<PSymbolInstance> p) {
		Iterator<MappedPrefix> it = prefixes.iterator();
		
		while (it.hasNext()) {
			MappedPrefix mp = it.next();
			if (mp.getPrefix().equals(p))
				return true;
		}
		return false;
	}
	
	public String toString() {
		return prefixes.toString();
	}
}
