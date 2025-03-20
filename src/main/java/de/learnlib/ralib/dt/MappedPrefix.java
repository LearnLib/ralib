package de.learnlib.ralib.dt;

import java.util.*;
import java.util.Map.Entry;

import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.util.RemappingIterator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.PrefixContainer;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class MappedPrefix implements PrefixContainer {

	private final Word<PSymbolInstance> prefix;
	private final RegisterGenerator regGen = new RegisterGenerator();

	private Bijection<DataValue> remapping;
	private final Map<SymbolicSuffix, SDT> tqrs = new LinkedHashMap<SymbolicSuffix, SDT>();
	public final Set<DataValue> missingParameter = new LinkedHashSet<>();

	public MappedPrefix(Word<PSymbolInstance> prefix, Bijection<DataValue> remapping) {
		this.prefix = prefix;
		this.remapping = remapping;
	}

	MappedPrefix(MappedPrefix mp, Bijection<DataValue> remapping) {
		this.prefix = mp.getPrefix();
		tqrs.putAll(mp.getTQRs());
		this.remapping = remapping;
	}

	public Set<Bijection<DataValue>> equivalentRenamings(Set<DataValue> params) {

		assert new HashSet<>(memorableValues()).containsAll(params);

		Set<Bijection<DataValue>> renamings = new LinkedHashSet<>();
		RemappingIterator<DataValue> iter = new RemappingIterator<>(params, params);
		LOC: for (Bijection<DataValue> b : iter) {
			for (SDT tqr : tqrs.values()) {
				if (!tqr.isEquivalent(tqr, b))
					continue LOC;
			}
			renamings.add(b);
		}

		return renamings;
	}

	/*
	 * Performs a tree query for the (new) suffix and stores it in its internal map.
	 * Returns the result.
	 */
	TreeQueryResult computeTQR(SymbolicSuffix suffix, TreeOracle oracle) {
        TreeQueryResult tqr = oracle.treeQuery(prefix, suffix);
	    addTQR(suffix, tqr.sdt());
	    return tqr;
	}

	void addTQR(SymbolicSuffix s, SDT tqr) {
	    if (tqrs.containsKey(s) || tqr == null) return;
		tqrs.put(s, tqr);
		//updateMemorable(tqr.getPiv());
	}

	public Map<SymbolicSuffix, SDT> getTQRs() {
		return tqrs;
	}

	@Override
	public Word<PSymbolInstance> getPrefix() {
		return this.prefix;
	}

	public Bijection<DataValue> getRemapping() {
		return remapping;
	}

	public void updateRemapping(Bijection<DataValue> remapping) {
		this.remapping = remapping;
	}

	public List<DataValue> memorableValues() {
		return this.tqrs.values().stream()
				.flatMap(sdt -> sdt.getDataValues().stream())
				.distinct()
				.sorted()
				.toList();
	}

	@Override
	public RegisterAssignment getAssignment() {
		RegisterAssignment ra = new RegisterAssignment();
		SymbolicDataValueGenerator.RegisterGenerator regGen =
				new SymbolicDataValueGenerator.RegisterGenerator();

		this.memorableValues().forEach(
				dv -> ra.put(dv, regGen.next(dv.getDataType()))
		);

		return ra;
	}

	@Override
	public String toString() {
		return "{" + prefix.toString() + ", " + Arrays.toString(memorableValues().toArray()) + "}";
	}

	SymbolicSuffix getSuffixForMemorable(DataValue d) {
		return tqrs.entrySet().stream()
				.filter(e -> e.getValue().getDataValues().contains(d))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("This line is not supposed to be reached."))
				.getKey();
	}

	List<SymbolicSuffix> getAllSuffixesForMemorable(DataValue d) {
		return tqrs.entrySet().stream()
				.filter(e -> e.getValue().getDataValues().contains(d))
				.map(Entry::getKey)
				.toList();
	}
}
