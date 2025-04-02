package de.learnlib.ralib.theory.inequality;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.theory.equality.EqualRestriction;

public class GreaterSuffixValue extends SuffixValueRestriction {

	public GreaterSuffixValue(SuffixValue param) {
		super(param);
	}

	public GreaterSuffixValue(GreaterSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new GreaterSuffixValue(this, shiftStep);
	}

	@Override
	public GuardExpression toGuardExpression(Set<SymbolicDataValue> vals) {
		List<GuardExpression> expr = new ArrayList<>();
		for (SymbolicDataValue sdv : vals) {
			GuardExpression g = new AtomicGuardExpression<SuffixValue, SymbolicDataValue>(parameter, Relation.BIGGER, sdv);
			expr.add(g);
		}
		GuardExpression expArr[] = new GuardExpression[expr.size()];
		expArr = expr.toArray(expArr);
		return new Conjunction(expArr);
	}

	@Override
	public SuffixValueRestriction merge(SuffixValueRestriction other, Map<SuffixValue, SuffixValueRestriction> prior) {
		if (other instanceof GreaterSuffixValue || other instanceof FreshSuffixValue) {
			return this;
		}
		if (other instanceof LesserSuffixValue || other instanceof EqualRestriction) {
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
		return "Greater(" + parameter.toString() + ")";
	}
}
