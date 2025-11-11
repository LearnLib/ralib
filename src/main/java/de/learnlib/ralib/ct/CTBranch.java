package de.learnlib.ralib.ct;

import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.util.RemappingIterator;
import de.learnlib.ralib.smt.ConstraintSolver;

/**
 * Branch forming an edge between two nodes in a {@link ClassificationTree}.
 * The branch is labeled by the representative path of the first prefix which
 * was sifted through the branch.
 * 
 * @author fredrik
 * 
 * @see CTNode
 * @see CTPath
 */
public class CTBranch {
	private final CTNode child;

	private final CTPath reprPath;

	public CTBranch(CTPath path, CTNode child) {
		this.reprPath = path;
		this.child = child;
	}

	public CTPath getRepresentativePath() {
		return reprPath;
	}

	public CTNode getChild() {
		return child;
	}

	/**
	 * Check whether the SDTs of {@code other} are equivalent to the SDTs of {@code this}.
	 * under the same {@link Bijection}. If so, returns the {@code Bijection} under which the {@code SDT}s
	 * are equivalent.
	 * 
	 * @param other the path to compare for equivalence
	 * @param solver constraint solver to use when comparing for equivalence
	 * @return {@code Bijection} under which the {@code SDT}s of {@code other} are equivalent to those of {@code this}, or {@code null} if the {@code SDT}s are not equivalent.
	 * @see SDT
	 */
	public Bijection<DataValue> matches(CTPath other, ConstraintSolver solver) {
		if (!reprPath.typeSizesMatch(other)) {
			return null;
		}

		Set<DataValue> regs = reprPath.getMemorable();
		Set<DataValue> otherRegs = other.getMemorable();

		RemappingIterator<DataValue> it = new RemappingIterator<>(otherRegs, regs);

		for (Bijection<DataValue> vars : it) {
			if (reprPath.isEquivalent(other, vars, solver)) {
				return vars;
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return child.toString();
	}
}
