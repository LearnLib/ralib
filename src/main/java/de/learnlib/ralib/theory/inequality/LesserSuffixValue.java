package de.learnlib.ralib.theory.inequality;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.theory.equality.EqualRestriction;

public class LesserSuffixValue extends SuffixValueRestriction {

	public LesserSuffixValue(SuffixValue param) {
		super(param);
	}

	public LesserSuffixValue(LesserSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new LesserSuffixValue(this, shiftStep);
	}

	@Override
	public GuardExpression toGuardExpression(Set<SymbolicDataValue> vals) {
		List<GuardExpression> expr = new ArrayList<>();
		for (SymbolicDataValue sdv : vals) {
			return new AtomicGuardExpression<SuffixValue, SymbolicDataValue>(parameter, Relation.SMALLER, sdv);
		}
		GuardExpression exprArr[] = new GuardExpression[expr.size()];
		exprArr = expr.toArray(exprArr);
		return new Conjunction(exprArr);
	}

	@Override
	public SuffixValueRestriction merge(SuffixValueRestriction other, Map<SuffixValue, SuffixValueRestriction> prior) {
		if (other instanceof LesserSuffixValue || other instanceof FreshSuffixValue) {
			return this;
		}
		if (other instanceof GreaterSuffixValue || other instanceof EqualRestriction) {
			return new UnrestrictedSuffixValue(parameter);
		}
		return other.merge(other, prior);
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		return false;
	}

	@Override
	public String toString() {
		return "Lesser(" + parameter.toString() + ")";
	}
}
