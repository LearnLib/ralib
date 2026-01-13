package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class FreshSuffixValue extends AbstractSuffixValueRestriction {
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
	public AbstractSuffixValueRestriction shift(int shiftStep) {
		return new FreshSuffixValue(this, shiftStep);
	}

	@Override
	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
//		Set<DataValue> vals = new LinkedHashSet<>();
//		vals.addAll(mapping.values());
//		return SuffixValueRestriction.disequalityRestriction(parameter, vals);
		return this;
	}

	@Override
	public AbstractSuffixValueRestriction merge(AbstractSuffixValueRestriction other, Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
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

	@Override
	public boolean isTrue() {
		return false;
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	@Override
	public <T extends TypedValue> AbstractSuffixValueRestriction relabel(Bijection<T> bijection) {
		return this;
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
}
