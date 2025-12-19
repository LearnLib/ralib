package de.learnlib.ralib.theory.equality;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;

public class EqualRestriction extends AbstractSuffixValueRestriction {
	private final SuffixValue equalParam;

	public EqualRestriction(SuffixValue param, SuffixValue equalParam) {
		super(param);
		this.equalParam = equalParam;
	}

	public EqualRestriction(EqualRestriction other) {
		super(other);
		equalParam = new SuffixValue(other.equalParam.getDataType(), other.equalParam.getId());
	}

	public EqualRestriction(EqualRestriction other, int shift) {
		super(other, shift);
		equalParam = new SuffixValue(other.equalParam.getDataType(), other.equalParam.getId()+shift);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals) {
		assert vals.contains(equalParam);

		return new NumericBooleanExpression(parameter, NumericComparator.EQ, equalParam);
	}

	@Override
	public AbstractSuffixValueRestriction shift(int shiftStep) {
		return new EqualRestriction(this, shiftStep);
	}

	@Override
	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		assert mapping.containsKey(equalParam);
		return SuffixValueRestriction.equalityRestriction(parameter, mapping.get(equalParam));
	}

	@Override
	public AbstractSuffixValueRestriction merge(AbstractSuffixValueRestriction other, Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
		assert other.getParameter().equals(parameter);
		if (prior.get(equalParam) instanceof FreshSuffixValue) {
			if (other instanceof EqualRestriction &&
					((EqualRestriction) other).equalParam.equals(equalParam)) {
				// equality only if the same equality and that parameter is fresh
				return this;
			}
			if (other instanceof FreshSuffixValue) {
				// choose equality over fresh
				return this;
			}
		}
		return new UnrestrictedSuffixValue(parameter);
	}

	@Override
	public String toString() {
		return "(" + parameter.toString() + "=" + equalParam.toString() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EqualRestriction other))
			return false;
		return super.equals(obj) && equalParam.equals(other.equalParam);
	}

	@Override
	public int hashCode() {
		int hash = super.hashCode();
		return 89 * hash + Objects.hash(equalParam);
	}

	public SuffixValue getEqualParameter() {
		return equalParam;
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		return equalParam.equals(r);
	}

	@Override
	public boolean isTrue() {
		return false;
	}

	@Override
	public boolean isFalse() {
		return false;
	}
}
