package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class FreshSuffixValue extends SuffixValueRestriction {
	public FreshSuffixValue(SuffixValue param) {
		super(param);
	}

	public FreshSuffixValue(FreshSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals) {
		List<Expression<Boolean> > expr = new ArrayList<>();
		for (SymbolicDataValue sdv : vals) {
			Expression<Boolean>  g = new NumericBooleanExpression(parameter, NumericComparator.NE, sdv);
			expr.add(g);
		}
		Expression<Boolean> [] exprArr = new Expression[expr.size()];
		expr.toArray(exprArr);
		return ExpressionUtil.and(exprArr);
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new FreshSuffixValue(this, shiftStep);
	}

	@Override
	public SuffixValueRestriction merge(SuffixValueRestriction other, Map<SuffixValue, SuffixValueRestriction> prior) {
		if (other instanceof FreshSuffixValue) {
			return this;
		}
		return other.merge(this, prior);
	}

	@Override
	public String toString() {
		return "Fresh(" + parameter.toString() + ")";
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		return false;
	}
}
