package de.learnlib.ralib.ct;

import java.util.List;

import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;

public abstract class CTNode {
	private final CTNode parent;

	public CTNode(CTNode parent) {
		this.parent = parent;
	}

	public CTNode getParent() {
		return parent;
	}

	public abstract List<SymbolicSuffix> getSuffixes();

	public abstract boolean isLeaf();

	protected abstract CTLeaf sift(Prefix prefix, TreeOracle oracle, ConstraintSolver solver, boolean ioMode);
}
