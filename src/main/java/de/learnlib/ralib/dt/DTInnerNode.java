package de.learnlib.ralib.dt;

import java.util.HashSet;
import java.util.Set;

import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class DTInnerNode extends DTNode {
	
	private SymbolicSuffix suffix;
	
	private Set<DTBranch> branches;
	
	public DTInnerNode(SymbolicSuffix suffix) {
		this.suffix = suffix;
		branches = new HashSet<DTBranch>();
	}
	
	public DTInnerNode(SymbolicSuffix suffix, Set<DTBranch> branches) {
		this.suffix = suffix;
		this.branches = branches;
	}
	
	public DTNode sift(Word<PSymbolInstance> prefix, TreeOracle oracle) {
		
		TreeQueryResult tqr = oracle.treeQuery(prefix, suffix);
		
		for (DTBranch b : branches) {
			if (b.matches(tqr))
				return b.getChild();
		}
		
		return null;
	}
	
	public void addBranch(DTBranch b) {
		branches.add(b);
	}
	
	public void addBranch(SymbolicDecisionTree sdt, DTNode child) {
		branches.add(new DTBranch(sdt, child));
	}
	
	public boolean isLeaf() {
		return false;
	}
}
