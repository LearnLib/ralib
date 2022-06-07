package de.learnlib.ralib.dt;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rattt.DiscriminationTree;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class DT implements DiscriminationTree {
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
		return sift(prefix, oracle, add, false);
	}
	
	private DTLeaf sift(Word<PSymbolInstance> prefix, TreeOracle oracle, boolean add, boolean isShort) {
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
			if (add) {
				if (isShort)
					leaf.addShortPrefix(new MappedPrefix(prefix, tqr.getPiv()));
				else
					leaf.addPrefix(new MappedPrefix(prefix, tqr.getPiv()));
			}
			return leaf;
		}
		return null;
	}
	
	public void split(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, DTLeaf leaf, TreeOracle oracle) {

		// add new inner node
		DTBranch branch = leaf.getParentBranch();
		DTInnerNode node = new DTInnerNode(suffix);
		branch.setChild(node);
		
		// add the new leaf
		TreeQueryResult tqr = oracle.treeQuery(prefix, suffix);
		DTLeaf newLeaf = new DTLeaf(new MappedPrefix(prefix, tqr.getPiv()));
		newLeaf.setParent(node);
		DTBranch newBranch = new DTBranch(tqr.getSdt(), newLeaf);
		node.addBranch(newBranch);
		
		// update old leaf
		leaf.removeShortPrefix(prefix);
		if (leaf.getShortPrefixes().iterator().hasNext()) {	// sanity check
			// maybe choose the shortest prefix?
			MappedPrefix as = leaf.getShortPrefixes().iterator().next();
			DTBranch b = new DTBranch(
					oracle.treeQuery(as.getPrefix(), suffix).getSdt(),
					leaf);
			leaf.setParent(node);
			node.addBranch(b);
		}

		resift(leaf, oracle);
	}
	
	private void resift(DTLeaf leaf, TreeOracle oracle) {
		PrefixSet shortPrefixes = leaf.getShortPrefixes();
		PrefixSet prefixes = leaf.getPrefixes();
		
		leaf.clear();		
		for (MappedPrefix s : shortPrefixes.get()) {
			sift(s.getPrefix(), oracle, true, true);
		}
		for (MappedPrefix p : prefixes.get()) {
			sift(p.getPrefix(), oracle, true);
		}
	}
	
	public DTLeaf getLeaf(Word<PSymbolInstance> as) {
		return getLeaf(as, root);
	}
	
	private DTLeaf getLeaf(Word<PSymbolInstance> as, DTNode node) {
		if (node.isLeaf()) {
			DTLeaf leaf = (DTLeaf)node;
			if (leaf.getShortPrefixes().contains(as))
				return leaf;
		}
		else {
			DTInnerNode in = (DTInnerNode)node;
			for (DTBranch b : in.getBranches()) {
				DTLeaf l = getLeaf(as, b.getChild());
				if (l != null)
					return l;
			}
		}
		return null;
	}
}