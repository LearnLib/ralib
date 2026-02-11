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

/**
 * Leaf node of a {@link ClassificationTree}, containing a number of prefixes.
 * A leaf node must contain at least one prefix, which is the representative
 * prefix of the leaf. Any number of prefixes may also be short prefixes.
 *
 * @author fredrik
 * @see Prefix
 * @see ShortPrefix
 */
public class CTLeaf extends CTNode implements LocationComponent {
	private Prefix rp;
	private final Set<ShortPrefix> shortPrefixes;
	private final Set<Prefix> prefixes;

	public CTLeaf(Prefix rp, CTInnerNode parent) {
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

	/**
	 * @return all prefixes contained within this leaf
	 */
	public Set<Prefix> getPrefixes() {
		return prefixes;
	}

	/**
	 * Helper method for retrieving the {@link Prefix} representation of a {@code Word<PSymbolInstance>}.
	 *
	 * @param u
	 * @return the {@code Prefix} form of {@code u} if {@code u} is in this leaf, or {@code null} otherwise
	 */
	public Prefix getPrefix(Word<PSymbolInstance> u) {
		for (Prefix p : prefixes) {
			if (p.equals(u)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * @return all short prefixes in this leaf
	 */
	public Set<ShortPrefix> getShortPrefixes() {
		return shortPrefixes;
	}

	/**
	 * @return the representative prefix of this leaf
	 */
	public Prefix getRepresentativePrefix() {
		return rp;
	}

	/**
	 * @return {@code true}
	 */
	@Override
	public boolean isLeaf() {
		return true;
	}

	/**
	 * Returns {@code true} if this leaf is accepting, i.e., if a tree query for this
	 * leaf's representative prefix with the empty suffix (ε) is accepting.
	 *
	 * @return {@code true} if this leaf corresponds to an accepting location
	 */
	public boolean isAccepting() {
		return rp.getPath().isAccepting();
	}

	/**
	 * Adds {@code prefix} to this leaf. If {@code prefix} is a short prefix, this method updates
	 * the branching of {@code prefix} to include initial guards from the conjunction of all SDTs
	 * along its path.
	 *
	 * @param prefix that will be added to this leaf
	 * @param oracle unused
	 * @param solver unused
	 * @param ioMode unused
	 * @return {@code this}
	 */
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

	/**
	 * Elevate {@code u} to a short prefix by converting it to a {@link ShortPrefix}. The branching
	 * of {@code u} will be initialized to reflect the initial guards of the conjunction of SDTs
	 * along its path.
	 *
	 * @param u the prefix to elevate
	 * @param oracle tree oracle to use for updating the branching
	 * @param inputs all input symbols
	 * @return {@code u} as a {@code ShortPrefix}
	 * @see Branching
	 * @see SDT
	 * @see CTPath
	 */
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
		String str = "{RP:[" + rp.toString() + "]";
		for (Prefix u : prefixes) {
			if (u != rp) {
				str = str + ", " + (u instanceof ShortPrefix ? "SP:[" : "[") + u.toString() + "]";
			}
		}
		return str + "}";
	}

	/**
	 * @return this leaf's representative prefix
	 */
	@Override
	public Word<PSymbolInstance> getAccessSequence() {
		return getRepresentativePrefix();
	}

	@Override
	public Bijection<DataValue> getRemapping(PrefixContainer r) {
		assert r instanceof Prefix;
		return ((Prefix) r).getRpBijection();
	}

	/**
	 * Get the branching for symbol {@code action} of the representative prefix.
	 * Requires that the representative prefix is a {@link ShortPrefix}.
	 *
	 * @param action
	 * @return {@code Branching} of {@code action} for the representative prefix of this leaf
	 */
	@Override
	public Branching getBranching(ParameterizedSymbol action) {
		assert rp instanceof ShortPrefix : "Representative prefix is not a short prefix";
		return ((ShortPrefix) rp).getBranching(action);
	}

	/**
	 * @return the representative prefix of this leaf
	 */
	@Override
	public PrefixContainer getPrimePrefix() {
		return getRepresentativePrefix();
	}

	/**
	 * @return {@code Collection} of all prefixes in this leaf other than the representative prefix
	 */
	@Override
	public Collection<PrefixContainer> getOtherPrefixes() {
		Collection<PrefixContainer> prefs = new LinkedList<>();
		prefs.addAll(prefixes);
		prefs.remove(rp);
		return prefs;
	}
}
