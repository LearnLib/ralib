package de.learnlib.ralib.dt;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class PrefixSet {
	private Set<MappedPrefix> prefixes;

	public PrefixSet() {
		prefixes = new LinkedHashSet<MappedPrefix>();
	}

	public PrefixSet(PrefixSet ps) {
		prefixes = new LinkedHashSet<MappedPrefix>(ps.get());
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

	public Collection<Word<PSymbolInstance>> getWords() {
		Collection<Word<PSymbolInstance>> words = new LinkedHashSet<Word<PSymbolInstance>>();
		for (MappedPrefix p : prefixes)
			words.add(p.getPrefix());
		return words;
	}

	public int length() {
		return prefixes.size();
	}

	public boolean isEmpty() {
		return prefixes.isEmpty();
	}

	@Override
	public String toString() {
		return prefixes.toString();
	}
}
