package de.learnlib.ralib.dt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.PrefixContainer;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class MappedPrefix implements PrefixContainer {
	private Word<PSymbolInstance> prefix;
	private final PIV memorable = new PIV();
	private final RegisterGenerator regGen = new RegisterGenerator();
	private final Map<SymbolicSuffix, TreeQueryResult> tqrs = new LinkedHashMap<SymbolicSuffix, TreeQueryResult>();
	
	public MappedPrefix(Word<PSymbolInstance> prefix) {
		this.prefix = prefix;
		//memorable = new PIV();
	}
	
	public MappedPrefix(Word<PSymbolInstance> prefix, PIV piv) {
		this.prefix = prefix;
		//this.memorable = memorable;
		updateMemorable(piv);
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
	
	void addTQR(SymbolicSuffix s, TreeQueryResult tqr) {
		tqrs.put(s, tqr);
		updateMemorable(tqr.getPiv());
	}
	
	Map<SymbolicSuffix, TreeQueryResult> getTQRs() {
		return tqrs;
	}
	
	public Word<PSymbolInstance> getPrefix() {
		return this.prefix;
	}
	
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
}
