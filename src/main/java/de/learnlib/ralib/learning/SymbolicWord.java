package de.learnlib.ralib.learning;

import java.util.Arrays;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class SymbolicWord {
	private Word<PSymbolInstance> prefix;
	private SymbolicSuffix suffix;
	private Word<PSymbolInstance> concreteSuffix = null;

	public SymbolicWord(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
		this.prefix = prefix;
		this.suffix = suffix;
	}

	public Word<PSymbolInstance> getPrefix() {
		return prefix;
	}

	public SymbolicSuffix getSuffix() {
		return suffix;
	}

	public Word<PSymbolInstance> concretize(Word<PSymbolInstance> word, Hypothesis hyp) {
		int len = word.length() - suffix.length();
//		Word<PSymbolInstance> cp = word.prefix(len);
//		Word<PSymbolInstance> cs = word.suffix(suffix.length());

		Word<PSymbolInstance> concereteSuffix = prefix.concat(Word.epsilon());
		for (int idx = len; idx < word.length(); idx++) {
			Word<PSymbolInstance> cp = word.prefix(idx);

		}

		return null;
	}

	public Mapping<SymbolicDataValue, DataValue<?>> computeValuation(Word<PSymbolInstance> concreteSuffix, PIV piv) {
    	Mapping<SymbolicDataValue, DataValue<?>> vals = new Mapping<>();

    	SuffixValueGenerator svGen = new SuffixValueGenerator();
    	Word<ParameterizedSymbol> actions = suffix.getActions();
    	int length = actions.length();

    	assert concreteSuffix.length() == length;

    	for (int i = 0; i < length; i++) {
    		ParameterizedSymbol ps = actions.getSymbol(i);
    		PSymbolInstance psi = concreteSuffix.getSymbol(i);
    		int arity = ps.getArity();
    		DataType[] dts = ps.getPtypes();
    		DataValue[] dvs = psi.getParameterValues();

    		assert psi.getBaseSymbol().getArity() == arity;
    		assert Arrays.deepEquals(psi.getBaseSymbol().getPtypes(), dts);

    		for (int j = 0; j < arity; j++ ) {
    			DataType dt = dts[j];
    			SuffixValue sv = svGen.next(dt);
    			vals.put(sv, dvs[j]);
    		}
    	}

    	VarValuation vars = DataWords.computeVarValuation(DataWords.computeParValuation(prefix), piv);
    	vals.putAll(vars);

    	return vals;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SymbolicWord other = (SymbolicWord)obj;
		if (!prefix.equals(other.getPrefix()))
			return false;
//		if (!other.getSuffix().getActions().equals(suffix.getActions()))
		if (!other.getSuffix().equals(suffix))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash * getPrefix().hashCode();
		hash = 31 * hash * getSuffix().hashCode();
		return hash;
	}

	public String toString() {
		return "{" + prefix.toString() + ", " + suffix.toString() + "}";
	}
}
