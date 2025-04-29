package de.learnlib.ralib.learning;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class SymbolicWord {
	private final Word<PSymbolInstance> prefix;
	private final SymbolicSuffix suffix;

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
        return other.getSuffix().equals(suffix);
    }

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash * getPrefix().hashCode();
		hash = 31 * hash * getSuffix().hashCode();
		return hash;
	}

        @Override
	public String toString() {
		return "{" + prefix.toString() + ", " + suffix.toString() + "}";
	}
}
