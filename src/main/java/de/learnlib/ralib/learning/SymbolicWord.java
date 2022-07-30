package de.learnlib.ralib.learning;

import de.learnlib.ralib.words.PSymbolInstance;
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
