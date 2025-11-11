package de.learnlib.ralib.ct;

import java.util.List;

import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;

/**
 * Node of a {@link ClassificationTree}.
 *
 * @author fredrik
 */
public abstract class CTNode {
	private final CTNode parent;

	public CTNode(CTNode parent) {
		this.parent = parent;
	}

	/**
	 *
	 * @return immediate ancestor of this node
	 */
	public CTNode getParent() {
		return parent;
	}

	/**
	 * @return all symbolic suffixes from all the ancestor nodes.
	 */
	public abstract List<SymbolicSuffix> getSuffixes();

	/**
	 *
	 * @return {@code true} if this node is a {@link CTLeaf}
	 */
	public abstract boolean isLeaf();

	/**
	 * Sifts {@code prefix} into the node. If this is a {@link CTLeaf}, adds {@code prefix} to it.
	 * If this is a {@link CTInnerNode}, a tree query is made for {@code prefix} with the node's
	 * {@link SymbolicSuffix}, after which {@code prefix} is sifted to the child whose path
	 * matches the tree query, determined with a call to {@link CTBranch#matches(CTPath, ConstraintSolver)}.
	 * If no such child exists, this method creates a new {@code CTLeaf} and adds {@code prefix}
	 * to it as its representative prefix.
	 *
	 * @param prefix the prefix to sift through this node
	 * @param oracle the {@link TreeOracle} to use when making tree queries
	 * @param solver the {@link ConstraintSolver} to use for comparing paths for equivalence
	 * @param ioMode {@code true} if using IO mode
	 * @return the {@code CTLeaf} node to which {@code prefix} is sifted
	 * @see CTPath
	 */
	protected abstract CTLeaf sift(Prefix prefix, TreeOracle oracle, ConstraintSolver solver, boolean ioMode);
}
