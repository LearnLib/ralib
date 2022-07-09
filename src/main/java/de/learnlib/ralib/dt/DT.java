package de.learnlib.ralib.dt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rattt.DiscriminationTree;
import de.learnlib.ralib.learning.rattt.RaTTT;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * Implementation of discrimination tree.
 * 
 * @author fredrik
 */
public class DT implements DiscriminationTree {
	private DTInnerNode root;
	
	private final ParameterizedSymbol[] inputs;
	private TreeOracle oracle;
	private boolean ioMode;
	private final Constants consts;
	
	public DT(TreeOracle oracle, boolean ioMode, Constants consts, ParameterizedSymbol ... inputs) {
		this.oracle = oracle;
		this.ioMode = ioMode;
		this.inputs = inputs;
		this.consts = consts;
		
		Word<PSymbolInstance> epsilon = Word.epsilon();
		SymbolicSuffix suffEps = new SymbolicSuffix(epsilon, epsilon);
		
		root = new DTInnerNode(suffEps);
	}
	
	public DT(DTInnerNode root, TreeOracle oracle, boolean ioMode, Constants consts, ParameterizedSymbol ... inputs) {
		this.root = root;
		this.oracle = oracle;
		this.ioMode = ioMode;
		this.inputs = inputs;
		this.consts = consts;
	}
	
	@Override
	public DTLeaf sift(Word<PSymbolInstance> prefix, boolean add) {
		return sift(prefix, add, null);
	}
	
	public void initialize() {
		MappedPrefix mp = new MappedPrefix(RaTTT.EMPTY_PREFIX, new PIV());
		TreeQueryResult tqr = oracle.treeQuery(mp.getPrefix(), root.getSuffix());
		mp.addTQR(root.getSuffix(), tqr);
		DTLeaf leaf = new DTLeaf(mp, oracle);
		DTBranch b = new DTBranch(tqr.getSdt(), leaf);
		root.addBranch(b);
		leaf.setParent(root);
		leaf.start(this, ioMode, inputs);
	}
	
	private DTLeaf sift(Word<PSymbolInstance> prefix, boolean add, ShortPrefix shortPrefix) {
		DTNode node  = root;
		DTLeaf leaf = null;
		TreeQueryResult tqr = null;
		
		MappedPrefix mp = new MappedPrefix(prefix, new PIV());
		
		// traverse tree from root to leaf
		do {
			DTInnerNode inner = (DTInnerNode)node;
			SymbolicSuffix suffix = inner.getSuffix();
			Pair<DTNode,TreeQueryResult> siftRes = inner.sift(prefix, oracle);

			if (siftRes == null) {
				// discovered new location en passant
				leaf = new DTLeaf(oracle);
				if (tqr == null) {
					tqr = oracle.treeQuery(prefix, suffix);
					mp.addTQR(suffix, tqr);
				}
				//leaf.addShortPrefix(new MappedPrefix(prefix, tqr.getPiv()));
				leaf.setAccessSequence(mp);
				DTBranch branch = new DTBranch(tqr.getSdt(), leaf);
				inner.addBranch(branch);
				leaf.setParent(inner);
				leaf.start(this, ioMode, inputs);
				leaf.updateBranching(this);
				return leaf;
			}
			
			node = siftRes.getKey();
			tqr = siftRes.getValue();
			mp.addTQR(suffix, tqr);
		} while (!node.isLeaf() && node != null);
		
		assert(node.isLeaf());
		
		leaf = (DTLeaf)node;
		if (add && !leaf.getAccessSequence().equals(prefix)) {
			if (shortPrefix != null)
				leaf.addShortPrefix(shortPrefix);
			else
				leaf.addPrefix(mp);
		}
		return leaf;
	}
	
	@Override
	public void split(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, DTLeaf leaf) {

		// add new inner node
		DTBranch branch = leaf.getParentBranch();
		DTInnerNode node = new DTInnerNode(suffix);
		node.setParent(leaf.getParent());
		branch.setChild(node);		// point branch to the new inner node
		
		// add the new leaf
		TreeQueryResult tqr = oracle.treeQuery(prefix, suffix);
		MappedPrefix mp = leaf.get(prefix);
		mp.addTQR(suffix, tqr);
		DTLeaf newLeaf = new DTLeaf(mp, oracle);
		newLeaf.setParent(node);
		DTBranch newBranch = new DTBranch(tqr.getSdt(), newLeaf);
		node.addBranch(newBranch);
		ShortPrefix sp = (ShortPrefix)leaf.getShortPrefixes().get(prefix);
		
		// update old leaf
		boolean removed = leaf.removeShortPrefix(prefix);
		assert(removed == true);	// must not split a prefix that isn't there
		
		DTBranch b = new DTBranch(
				oracle.treeQuery(leaf.getPrimePrefix().getPrefix(), suffix).getSdt(),
				leaf);
		leaf.setParent(node);
		node.addBranch(b);
		
		// resift all transitions targeting this location
		resift(leaf);

		newLeaf.start(this, sp.getBranching());
		newLeaf.updateBranching(this);

		if (removed)
			leaf.updateBranching(this);
	}
	
	public void addSuffix(SymbolicSuffix suffix, DTLeaf leaf) {
		
		DTBranch branch = leaf.getParentBranch();
		DTInnerNode node = new DTInnerNode(suffix);
		node.setParent(leaf.getParent());
		branch.setChild(node);
		leaf.setParent(node);
		
		TreeQueryResult tqr = oracle.treeQuery(leaf.getPrimePrefix().getPrefix(), suffix);
		DTBranch newBranch = new DTBranch(tqr.getSdt(), leaf);
		node.addBranch(newBranch);
		
		leaf.getPrimePrefix().addTQR(suffix, tqr);
		leaf.addTQRs(tqr.getPiv(), suffix);
		leaf.updateBranching(this);
	}
	
	/**
	 * resift all prefixes of a leaf, in order to add them to the correct leaf
	 * 
	 * @param leaf
	 * @param oracle
	 */
	private void resift(DTLeaf leaf) {
		// Potential optimization:
		// can keep TQRs up to the parent, as they should still be the same
		
		PrefixSet shortPrefixes = leaf.getShortPrefixes();
		PrefixSet prefixes = leaf.getPrefixes();
		
		leaf.clear();
		for (MappedPrefix s : shortPrefixes.get()) {
			sift(s.getPrefix(), true, (ShortPrefix)s);
		}
		for (MappedPrefix p : prefixes.get()) {
			sift(p.getPrefix(), true);
		}
	}
	
	public boolean checkVariableConsistency() {
		return checkConsistency(this.root);
	}
	
	private boolean checkConsistency(DTNode node) {
		if (node.isLeaf()) {
			DTLeaf leaf = (DTLeaf)node;
			return leaf.checkVariableConsistency(this, this.consts);
		}
		boolean ret = true;
		DTInnerNode inner = (DTInnerNode)node;
		for (DTBranch b : inner.getBranches()) {
			ret = ret && checkConsistency(b.getChild());
		}
		return ret;
	}
	
	/**
	 * check whether sifting a word into the dt leads to a refinement of the dt,
	 * i.e whether the location corresponding to word is already in the branching of the source location of word
	 * 
	 * @param word
	 * @return true if sifting word into dt leads to refinement
	 */
	public boolean isRefinement(Word<PSymbolInstance> word) {
		Word<PSymbolInstance> prefix = word.prefix(word.length()-1);
		DTLeaf prefixLeaf = getLeaf(prefix);
		assert prefixLeaf != null;
		
		return prefixLeaf.isRefinemement(this, word);
	}

	/**
	 * get leaf containing prefix as
	 * 
	 * @param as
	 * @param node
	 * @return leaf containing as, or null
	 */
	public DTLeaf getLeaf(Word<PSymbolInstance> as) {
		return getLeaf(as, root);
	}
	
	DTLeaf getLeaf(Word<PSymbolInstance> as, DTNode node) {
		if (node.isLeaf()) {
			DTLeaf leaf = (DTLeaf)node;
			if (leaf.getPrimePrefix().getPrefix().equals(as) || leaf.getShortPrefixes().contains(as) || leaf.getPrefixes().contains(as))
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
	
	public ParameterizedSymbol[] getInputs() {
		return inputs;
	}
	
	boolean getIoMode() {
		return ioMode;
	}
	
	TreeOracle getOracle() {
		return oracle;
	}
	
	public Collection<DTLeaf> getLeaves() {
		Collection<DTLeaf> leaves = new ArrayList<DTLeaf>();
		getLeaves(root, leaves);
		return leaves;
	}
	
	private void getLeaves(DTNode node, Collection<DTLeaf> leaves) {
		if (node.isLeaf())
			leaves.add((DTLeaf)node);
		else {
			DTInnerNode inner = (DTInnerNode)node;
			for (DTBranch b : inner.getBranches())
				getLeaves(b.getChild(), leaves);
		}
	}

	private void getSuffixes(DTNode node,  Collection<SymbolicSuffix> suffixes) {
		if (!node.isLeaf()) {
			DTInnerNode inner = (DTInnerNode)node;
			suffixes.add(inner.getSuffix());
			for (DTBranch b : inner.getBranches())
				getSuffixes(b.getChild(), suffixes);
		}
	}

	public Collection<SymbolicSuffix> getSuffixes() {
		Collection<SymbolicSuffix> suffixes = new LinkedHashSet<>();
		getSuffixes(root, suffixes);
		return suffixes;
	}

	private Collection<Word<PSymbolInstance>> getAllPrefixes() {
		Collection<Word<PSymbolInstance>> prefs = new ArrayList<Word<PSymbolInstance>>();
		getAllPrefixes(prefs, root);
		return prefs;
	}
	
	private void getAllPrefixes(Collection<Word<PSymbolInstance>> prefs, DTNode node) {
		if (node.isLeaf()) {
			DTLeaf leaf = (DTLeaf)node;
			prefs.addAll(leaf.getAllPrefixes());
		}
		else {
			DTInnerNode inner = (DTInnerNode)node;
			for (DTBranch b : inner.getBranches())
				getAllPrefixes(prefs, b.getChild());
		}
	}
	
	public Map<Word<PSymbolInstance>, LocationComponent> getComponents() {
		Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
		collectComponents(components, root);
		return components;
	}
	
	private void collectComponents(Map<Word<PSymbolInstance>, LocationComponent> comp, DTNode node) {
		if (node.isLeaf()) {
			DTLeaf leaf = (DTLeaf)node;
			comp.put(leaf.getAccessSequence(), leaf);
		}
		else {
			DTInnerNode inner = (DTInnerNode)node;
			for (DTBranch b : inner.getBranches()) {
				collectComponents(comp, b.getChild());
			}
		}
	}
	
	/**
	 * find the lowest common ancestor of two leaves
	 * 
	 * @param l1
	 * @param l2
	 * @return the lowest common ancestor of l1 and l2
	 */
	public DTInnerNode findLCA(DTLeaf l1, DTLeaf l2) {
		Deque<DTInnerNode> path1 = new ArrayDeque<DTInnerNode>();
		Deque<DTInnerNode> path2 = new ArrayDeque<DTInnerNode>();
		
		if (l1.getParent() == l2.getParent())
			return l1.getParent();
		
		DTInnerNode parent = l1.getParent();
		while(parent != null) {
			path1.add(parent);
			parent = parent.getParent();
		}
		parent = l2.getParent();
		while(parent != null) {
			path2.add(parent);
			parent = parent.getParent();
		}
		
		DTInnerNode node = path1.pop();
		path2.pop();
		while(!path1.isEmpty() && !path2.isEmpty() && 
			  path1.peek() == path2.peek()) {
			node = path1.pop();
			path2.pop();
		}
		return node;
	}
	
}