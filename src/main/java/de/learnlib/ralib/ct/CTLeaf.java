package de.learnlib.ralib.ct;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.PrefixContainer;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class CTLeaf extends CTNode implements LocationComponent {
	private Prefix rp;
	private final Set<ShortPrefix> shortPrefixes;
	private final Set<Prefix> prefixes;

	public CTLeaf(Prefix rp, CTNode parent) {
		super(parent);
		if (parent == null) {
			throw new IllegalArgumentException("A leaf must have a parent");
		}
		this.rp = rp;
		shortPrefixes = new LinkedHashSet<>();
		prefixes = new LinkedHashSet<>();
		prefixes.add(rp);
		if (rp instanceof ShortPrefix) {
			((ShortPrefix) rp).updateBranching();
		}
	}

	@Override
	public List<SymbolicSuffix> getSuffixes() {
		return getParent().getSuffixes();
	}

	public Set<Prefix> getPrefixes() {
		return prefixes;
	}

	public Prefix getPrefix(Word<PSymbolInstance> u) {
		for (Prefix p : prefixes) {
			if (p.equals(u)) {
				return p;
			}
		}
		return null;
	}

//	public Set<Word<PSymbolInstance>> getWords() {
//		return getWords(prefixes);
//	}

	public Set<ShortPrefix> getShortPrefixes() {
		return shortPrefixes;
	}

//	public Set<Word<PSymbolInstance>> getShortWords() {
//		return getWords(shortPrefixes);
//	}

//	private static Set<Word<PSymbolInstance>> getWords(Set<? extends Prefix> prefixes) {
//		Set<Word<PSymbolInstance>> words = new LinkedHashSet<>();
//		for (Prefix p : prefixes) {
//			words.add(p.getPrefix());
//		}
//		return words;
//	}

	public Prefix getRepresentativePrefix() {
		return rp;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	public boolean isAccepting() {
		return rp.getPath().isAccepting();
	}

	@Override
	protected CTLeaf sift(Prefix prefix, TreeOracle oracle, ConstraintSolver solver, boolean ioMode) {
		prefixes.add(prefix);
		if (prefix instanceof ShortPrefix) {
			ShortPrefix sp = (ShortPrefix) prefix;
			shortPrefixes.add(sp);
			sp.updateBranching();
		}
		return this;
	}

	protected ShortPrefix elevatePrefix(Word<PSymbolInstance> u, TreeOracle oracle, ParameterizedSymbol ... inputs) {
		Prefix prefix = getPrefix(u);
		assert !(prefix instanceof ShortPrefix) : "Prefix is already short: " + prefix;
		prefixes.remove(prefix);
		ShortPrefix sp = new ShortPrefix(prefix, oracle, inputs);
		shortPrefixes.add(sp);
		prefixes.add(sp);

		if (prefix == rp) {
			rp = sp;
		}
		return sp;
	}

	@Override
	public String toString() {
//		return prefixes.toString();
		String str = "{RP:[" + rp.toString() + "]";
		for (Prefix u : prefixes) {
			if (u != rp) {
				str = str + ", " + (u instanceof ShortPrefix ? "SP:[" : "[") + u.toString() + "]";
			}
		}
		return str + "}";
	}

	@Override
	public Word<PSymbolInstance> getAccessSequence() {
		return getRepresentativePrefix();
	}

	@Override
	public Bijection<DataValue> getRemapping(PrefixContainer r) {
		assert r instanceof Prefix;
		return ((Prefix) r).getRpBijection();
	}

	@Override
	public Branching getBranching(ParameterizedSymbol action) {
		assert rp instanceof ShortPrefix : "Representative prefix is not a short prefix";
		return ((ShortPrefix) rp).getBranching(action);
	}

	@Override
	public PrefixContainer getPrimePrefix() {
		return rp;
	}

	@Override
	public Collection<PrefixContainer> getOtherPrefixes() {
		Collection<PrefixContainer> prefs = new LinkedList<>();
		prefs.addAll(prefixes);
		prefs.remove(rp);
		return prefs;
	}
}
