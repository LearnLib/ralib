package de.learnlib.ralib.theory.equality;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SDTGuardElement;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.ElementRestriction;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;

public class EqualRestriction extends AbstractSuffixValueRestriction implements ElementRestriction {
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
	public boolean containsElement(Expression<BigDecimal> element) {
		return equalParam.equals(element);
	}

	@Override
	public Set<Expression<BigDecimal>> getElements() {
		return Set.of(equalParam);
	}

	@Override
	public AbstractSuffixValueRestriction replaceElement(Expression<BigDecimal> replace, Expression<BigDecimal> by) {
		if (!(by instanceof SuffixValue)) {
			throw new IllegalArgumentException("Not a valid type for this restriction");
		}

		if (equalParam.asExpression().equals(replace)) {
			return new EqualRestriction(getParameter(), (SuffixValue) by);
		}
		return this;
	}

	@Override
	public List<ElementRestriction> getRestrictions(Expression<BigDecimal> element) {
		return Arrays.asList(this);
	}

	@Override
	public EqualRestriction cast() {
		return this;
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

	@Override
	public boolean containsFresh() {
		return false;
	}

	@Override
	public <K extends TypedValue, V extends TypedValue> AbstractSuffixValueRestriction relabel(Mapping<K, V> renaming) {
		for (Map.Entry<K, V> e : renaming.entrySet()) {
			if (e.getKey().equals(equalParam)) {
				assert e.getValue() instanceof SDTGuardElement;
				return new EqualityRestriction(parameter, Set.of((SDTGuardElement) e.getValue()));
			}
		}
		return this;
	}
}
