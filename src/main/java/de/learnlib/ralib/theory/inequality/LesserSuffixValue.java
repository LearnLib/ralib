package de.learnlib.ralib.theory.inequality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.theory.equality.EqualRestriction;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class LesserSuffixValue extends AbstractSuffixValueRestriction {

	public LesserSuffixValue(SuffixValue param) {
		super(param);
	}

	public LesserSuffixValue(LesserSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public AbstractSuffixValueRestriction shift(int shiftStep) {
		return new LesserSuffixValue(this, shiftStep);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals) {
		List<Expression<Boolean>> expr = new ArrayList<>();
		for (SymbolicDataValue sdv : vals) {
			return new NumericBooleanExpression(parameter, NumericComparator.LT, sdv);
		}
		Expression<Boolean> exprArr[] = new Expression[expr.size()];
		exprArr = expr.toArray(exprArr);
		return ExpressionUtil.and(exprArr);
	}

	@Override
	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		if (mapping.isEmpty()) {
//			return new FreshSuffixValue(parameter);
			return this;
		}
		DataValue d = Collections.min(mapping.values());
		Expression<Boolean> expr = new NumericBooleanExpression(parameter, NumericComparator.LT, d);
		return new SuffixValueRestriction(parameter, expr);
	}

	@Override
	public AbstractSuffixValueRestriction merge(AbstractSuffixValueRestriction other, Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
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
	public boolean isTrue() {
		return false;
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 37 * hash + Objects.hashCode(getClass());
		return hash;
	}
	
	@Override
	public <K extends TypedValue, V extends TypedValue> AbstractSuffixValueRestriction relabel(Mapping<K, V> renaming) {
		return this;
	}

	@Override
	public String toString() {
		return "Lesser(" + parameter.toString() + ")";
	}
}
