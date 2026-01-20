package de.learnlib.ralib.ct;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 * Inner node of a {@link ClassificationTree}, containing a {@link SymbolicSuffix}.
 * Maintains a set of branches to child nodes.
 *
 * @author fredrik
 * @see CTBranch
 * @see CTNode
 */
public class CTInnerNode extends CTNode {
	private final SymbolicSuffix suffix;
	private final List<CTBranch> branches;

	public CTInnerNode(CTInnerNode parent, SymbolicSuffix suffix) {
		super(parent);
		this.suffix = suffix;
		branches = new ArrayList<>();
	}

	public SymbolicSuffix getSuffix() {
		return suffix;
	}

	public List<CTBranch> getBranches() {
		return branches;
	}

	protected CTBranch getBranch(CTNode child) {
		for (CTBranch b : getBranches()) {
			if (b.getChild() == child) {
				return b;
			}
		}
		return null;
	}
	
//	@Override
//	protected List<CTBranch> getParentBranches() {
//		List<CTBranch> branches = new ArrayList<>();
//		if (getParent() != null) {
//			branches.addAll(getParent().getParentBranches());
//			CTBranch b = ((CTInnerNode) getParent()).getBranch(this);
//			branches.add(b);
//		}
//		return branches;
//	}

	@Override
	protected CTLeaf sift(Prefix prefix, TreeOracle oracle, ConstraintSolver solver, boolean ioMode) {
		CTPath path = CTPath.computePath(oracle, prefix, getSuffixes(), ioMode);

		// find a matching branch and sift to child
		for (CTBranch b : branches) {
			Bijection<DataValue> vars = b.matches(path, solver);
			if (vars != null) {
				prefix = new Prefix(prefix, vars, path, prefix.getBijections());
				prefix.putBijection(getSuffix(), vars);
				return b.getChild().sift(prefix, oracle, solver, ioMode);
			}
		}

		// no child with equivalent SDTs, create a new leaf
		prefix = new Prefix(prefix, path);
//		if (getParent() != null) {
			prefix.putBijection(getSuffix());
//		}
		CTLeaf leaf = new CTLeaf(prefix, this);
		CTBranch branch = new CTBranch(path, leaf);
		branches.add(branch);
		return leaf;
	}
	
//	private CTBranch getParentBranch() {
//		CTInnerNode parent = getParent();
//		if (parent == null) {
//			return null;
//		}
//		return parent.getBranch(this);
//	}

	/**
	 * Replace {@code leaf} with a new {@link CTInnerNode} containing {@code suffix}.
	 * The prefixes in {@code leaf} will be sifted into this new inner node.
	 * If this sifting creates a new {@link CTLeaf}, the first prefix to be sifted
	 * into that leaf node will be made the representative prefix.
	 *
	 * @param leaf
	 * @param suffix
	 * @param oracle
	 * @param solver
	 * @param ioMode
	 * @param inputs
	 * @return a mapping of prefixes in {@code leaf} to their new leaf nodes
	 */
	protected Map<Word<PSymbolInstance>, CTLeaf> refine(CTLeaf leaf, SymbolicSuffix suffix, TreeOracle oracle, ConstraintSolver solver, boolean ioMode, ParameterizedSymbol[] inputs) {
		CTBranch b = getBranch(leaf);
		assert b != null : "Node is not the parent of leaf " + leaf;
		List<SymbolicSuffix> suffixes = getSuffixes();
		if (suffix.toString().equals("((?ISendFrame[] !OFrame[int, int]))[true, (s2 == c2)]") && suffixes.size() == 6) {
			int code1 = suffix.hashCode();
			int code2 = suffixes.getLast().hashCode();
			int code3 = code1 + code2;
		}
		assert !getSuffixes().contains(suffix) : "Duplicate suffix: " + suffix;

		Set<ShortPrefix> shorts = leaf.getShortPrefixes();

		// replace leaf with a new inner node, with same path as leaf
		CTInnerNode newNode = new CTInnerNode(this, suffix);
		CTBranch newBranch = new CTBranch(b.getRepresentativePath(), newNode);
		branches.remove(b);
		branches.add(newBranch);

		// sift leaf's RP into the new inner node
		Map<Word<PSymbolInstance>, CTLeaf> leaves = new LinkedHashMap<>();
		CTLeaf l = sift(leaf.getRepresentativePrefix(), oracle, solver, ioMode);
		leaves.put(leaf.getRepresentativePrefix(), l);

		// resift leaf's prefixes into this node (except the RP, which was already sifted)
		Set<Prefix> prefixes = new LinkedHashSet<>(leaf.getPrefixes());
		prefixes.remove(leaf.getRepresentativePrefix());

		for (Prefix u : prefixes) {
			l = sift(u, oracle, solver, ioMode);
			leaves.put(u, l);
		}

		// make sure all short prefixes of leaf are still short
		for (ShortPrefix u : shorts) {
			if (!(u instanceof ShortPrefix)) {
				leaves.get(u).elevatePrefix(u, oracle, inputs);
			}
		}

		return leaves;
	}

	@Override
	public List<SymbolicSuffix> getSuffixes() {
		List<SymbolicSuffix> suffixes = new ArrayList<>();
		if (getParent() != null) {
			suffixes.addAll(getParent().getSuffixes());
		}
		suffixes.add(suffix);
		return suffixes;
//		suffixes.add(suffix);
//		if (getParent() == null) {
//			return suffixes;
//		}
//
//		suffixes.addAll(getParent().getSuffixes());
//		return suffixes;
	}

	/**
	 * @return {@code false}
	 */
	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public String toString() {
		return "(" + suffix + ")";
	}

}
