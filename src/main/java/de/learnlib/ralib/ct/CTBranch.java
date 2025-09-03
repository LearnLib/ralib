package de.learnlib.ralib.ct;

import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.util.RemappingIterator;
import de.learnlib.ralib.smt.ConstraintSolver;

public class CTBranch {
	private final CTNode child;

	private final CTPath reprPath;

	public CTBranch(CTPath path, CTNode child) {
		this.reprPath = path;
		this.child = child;
	}

	public CTPath getPath() {
		return reprPath;
	}

	public CTNode getChild() {
		return child;
	}

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

	public CTPath getRepresentativePath() {
		return reprPath;
	}
	
	@Override
	public String toString() {
		return child.toString();
	}
}
