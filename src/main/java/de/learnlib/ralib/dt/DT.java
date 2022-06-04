package de.learnlib.ralib.dt;

import org.apache.commons.lang3.tuple.Pair;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class DT {
	private DTInnerNode root;
	
	public DT() {
		Word<PSymbolInstance> epsilon = Word.epsilon();
		
		root = new DTInnerNode(new SymbolicSuffix(epsilon, epsilon));
	}
	
	public DT(TreeOracle oracle) {
		Word<PSymbolInstance> epsilon = Word.epsilon();
		SymbolicSuffix suffEps = new SymbolicSuffix(epsilon, epsilon);
		
		root = new DTInnerNode(suffEps);
		
		TreeQueryResult tqr = oracle.treeQuery(epsilon, suffEps);
		root.addBranch(tqr.getSdt(), new DTLeaf(epsilon));
	}
	
	public DT(DTInnerNode root) {
		this.root = root;
	}
	
	public DTLeaf sift(Word<PSymbolInstance> prefix, TreeOracle oracle, boolean add) {
		DTNode node  = root;
		DTLeaf leaf = null;
		TreeQueryResult tqr = null;
		do {
			DTInnerNode inner = (DTInnerNode)node;
			Pair<DTNode,TreeQueryResult> child = inner.sift(prefix, oracle);

			if (child == null) {
				// discovered new location en passant
				// not completed
				leaf = new DTLeaf();
				leaf.addShortPrefix(new MappedPrefix(prefix, tqr.getPiv()));
				DTBranch branch = new DTBranch(tqr.getSdt(), leaf);
				inner.addBranch(branch);
				return leaf;
			}
			
			node = child.getKey();
			tqr = child.getValue();
		} while (!node.isLeaf() && node != null);
		
		if (node.isLeaf()) {
			leaf = (DTLeaf)node;
			if (add)
				leaf.addPrefix(new MappedPrefix(prefix, tqr.getPiv()));
			return leaf;
		}
		return null;
	}
}