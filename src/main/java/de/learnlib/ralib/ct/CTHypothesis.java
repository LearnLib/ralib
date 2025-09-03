package de.learnlib.ralib.ct;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class CTHypothesis extends Hypothesis {
	
	private final BiMap<CTLeaf, RALocation> leaves;
	
	public CTHypothesis(Constants consts, int leaves) {
		super(consts);
		this.leaves = HashBiMap.create(leaves);
	}
	
	public CTHypothesis(Constants consts, Map<CTLeaf, RALocation> leaves) {
		super(consts);
		this.leaves = HashBiMap.create(leaves.size());
		this.leaves.putAll(leaves);
	}
	
	public void putLeaves(Map<CTLeaf, RALocation> leaves) {
		this.leaves.putAll(leaves);
	}
	
	@Override
	public @Nullable RALocation getSuccessor(RALocation state, ParameterizedSymbol input) {
		return super.getSuccessor(state, input);
	}
	
	public RALocation getLocation(CTLeaf leaf) {
		return leaves.get(leaf);
	}
	
	public CTLeaf getLeaf(RALocation location) {
		return leaves.inverse().get(location);
	}
}
