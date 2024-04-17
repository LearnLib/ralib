package de.learnlib.ralib.dt;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.PermutationIterator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.PrefixContainer;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class MappedPrefix implements PrefixContainer {
	private Word<PSymbolInstance> prefix;
	private final PIV memorable = new PIV();
	private final RegisterGenerator regGen = new RegisterGenerator();
	private final Map<SymbolicSuffix, TreeQueryResult> tqrs = new LinkedHashMap<SymbolicSuffix, TreeQueryResult>();
	public final Set<Parameter> missingParameter = new LinkedHashSet<>();

	public MappedPrefix(Word<PSymbolInstance> prefix) {
		this.prefix = prefix;
	}

	public MappedPrefix(Word<PSymbolInstance> prefix, PIV piv) {
		this.prefix = prefix;
		updateMemorable(piv);
	}

	MappedPrefix(MappedPrefix mp) {
		this.prefix = mp.getPrefix();
		updateMemorable(mp.getParsInVars());
		tqrs.putAll(mp.getTQRs());
	}

	public Set<VarMapping<Parameter, Parameter>> equivalentRenamings(Set<Parameter> params) {

		assert memorable.keySet().containsAll(params);

		Parameter[] params_arr = new Parameter[params.size()];
		params_arr = params.toArray(params_arr);
		PermutationIterator permutations = new PermutationIterator(params_arr.length);
		Set<VarMapping<Parameter, Parameter>> renamings = new LinkedHashSet<>();

		LOC: while (permutations.hasNext()) {
			int[] perm = permutations.next();
			VarMapping<Parameter, Parameter> paramRenaming = new VarMapping<>();
			VarMapping<Register, Register> registerRenaming = new VarMapping<>();
			for (int i = 0; i < params_arr.length; i++) {
				Parameter po = params_arr[i];
				Parameter pr = params_arr[perm[i]];
				paramRenaming.put(params_arr[i], params_arr[perm[i]]);
				registerRenaming.put(memorable.get(po), memorable.get(pr));
			}
			for (TreeQueryResult tqr : tqrs.values()) {
				if (!tqr.getSdt().isEquivalent(tqr.getSdt(), registerRenaming))
					continue LOC;
			}
			renamings.add(paramRenaming);
		}
		return renamings;
	}
	void updateMemorable(PIV piv) {
		for (Entry<Parameter, Register> e : piv.entrySet()) {
			Register r = memorable.get(e.getKey());
			if (r == null) {
				r = regGen.next(e.getKey().getType());
				memorable.put(e.getKey(), r);
			}
		}
	}

	/*
	 * Performs a tree query for the (new) suffix and stores it in its internal map.
	 * Returns the result.
	 */
	TreeQueryResult computeTQR(SymbolicSuffix suffix, TreeOracle oracle) {
        TreeQueryResult tqr = oracle.treeQuery(prefix, suffix);
	    addTQR(suffix, tqr);
	    return tqr;
	}

	void addTQR(SymbolicSuffix s, TreeQueryResult tqr) {
	    if (tqrs.containsKey(s) || tqr == null) return;
		tqrs.put(s, tqr);
		updateMemorable(tqr.getPiv());
	}

	public Map<SymbolicSuffix, TreeQueryResult> getTQRs() {
		return tqrs;
	}

	@Override
	public Word<PSymbolInstance> getPrefix() {
		return this.prefix;
	}

	@Override
	public String toString() {
		return "{" + prefix.toString() + ", " + memorable.toString() + "}";
	}

	@Override
	public PIV getParsInVars() {
		return memorable;
	}

	SymbolicSuffix getSuffixForMemorable(Parameter p) {
		for (Entry<SymbolicSuffix, TreeQueryResult> e : tqrs.entrySet()) {
			if (e.getValue().getPiv().containsKey(p))
				return e.getKey();
		}
		throw new IllegalStateException("This line is not supposed to be reached.");
	}

	Set<SymbolicSuffix> getAllSuffixesForMemorable(Parameter p) {
		Set<SymbolicSuffix> suffixes = new LinkedHashSet<SymbolicSuffix>();
		for (Entry<SymbolicSuffix, TreeQueryResult> e : tqrs.entrySet()) {
			if (e.getValue().getPiv().containsKey(p))
				suffixes.add(e.getKey());
		}
		return suffixes;
	}
}
