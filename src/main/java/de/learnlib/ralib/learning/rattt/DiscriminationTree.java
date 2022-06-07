package de.learnlib.ralib.learning.rattt;

import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public interface DiscriminationTree {
	
	public DTLeaf sift(Word<PSymbolInstance> prefix, TreeOracle oracle, boolean add);
	
	public void split(Word<PSymbolInstance> prefx, SymbolicSuffix suffix, DTLeaf leaf, TreeOracle oracle);
}
