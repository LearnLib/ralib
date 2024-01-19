package de.learnlib.ralib.theory.equality;

import java.util.Set;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.SuffixValueRestriction;

public class EqualRestriction extends SuffixValueRestriction {
	private final SuffixValue equalParam;

	public EqualRestriction(SuffixValue param, SuffixValue equalParam) {
		super(param);
		this.equalParam = equalParam;
	}

	public EqualRestriction(EqualRestriction other) {
		super(other);
		equalParam = new SuffixValue(other.equalParam.getType(), other.equalParam.getId());
	}

	public EqualRestriction(EqualRestriction other, int shift) {
		super(other, shift);
		equalParam = new SuffixValue(other.equalParam.getType(), other.equalParam.getId()+shift);
	}

	@Override
	public GuardExpression toGuardExpression(Set<SymbolicDataValue> vals) {
		assert vals.contains(equalParam);

		return new AtomicGuardExpression<SuffixValue, SuffixValue>(parameter, Relation.EQUALS, equalParam);
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new EqualRestriction(this, shiftStep);
	}

	@Override
	public String toString() {
		return "(" + parameter.toString() + "=" + equalParam.toString() + ")";
	}

	public SuffixValue getEqualParameter() {
		return equalParam;
	}

}
