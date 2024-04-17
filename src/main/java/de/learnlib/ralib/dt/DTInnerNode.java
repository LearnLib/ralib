package de.learnlib.ralib.dt;

import java.util.*;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;

public class DTInnerNode extends DTNode {

	private SymbolicSuffix suffix;

	private Set<DTBranch> branches;

	public DTInnerNode(SymbolicSuffix suffix) {
		super();
		this.suffix = suffix;
		branches = new LinkedHashSet<DTBranch>();
	}

	public DTInnerNode(SymbolicSuffix suffix, Set<DTBranch> branches) {
		super();
		this.suffix = suffix;
		this.branches = branches;
	}

	public DTInnerNode(DTInnerNode n) {
		suffix = n.suffix;
		branches = new LinkedHashSet<DTBranch>();
		for (DTBranch b : n.branches) {
			DTBranch nb = new DTBranch(b);
			b.getChild().setParent(this);
			branches.add(nb);
		}
	}

	protected Pair<DTNode, PathResult> sift(MappedPrefix prefix, TreeOracle oracle, boolean ioMode) {
		PathResult r = PathResult.computePathResult(oracle, prefix, getSuffixes(), ioMode);
		for (DTBranch b : branches) {
			if (b.matches(r)) {
				return new ImmutablePair<DTNode, PathResult>(b.getChild(), r);
			}
		}

		return null;
	}

	public void addBranch(DTBranch b) {
		branches.add(b);
	}

	public Set<DTBranch> getBranches() {
		return branches;
	}

	public SymbolicSuffix getSuffix() {
		return suffix;
	}

	List<SymbolicSuffix> getSuffixes() {
		LinkedList<SymbolicSuffix> suffixes = new LinkedList<>();
		getSuffixes(suffixes);
		return suffixes;
	}

	void getSuffixes(LinkedList<SymbolicSuffix> suffixes) {
		suffixes.addFirst(suffix);
		if (parent != null) {
			parent.getSuffixes(suffixes);
		}
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public DTInnerNode copy() {
		return new DTInnerNode(this);
	}

	@Override
	public String toString() {
		return "(" +  suffix.toString() + ")";
	}
}
