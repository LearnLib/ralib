package de.learnlib.ralib.theory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class UnrestrictedSuffixValue extends AbstractSuffixValueRestriction {

	public UnrestrictedSuffixValue(SuffixValue parameter) {
		super(parameter);
	}

	public UnrestrictedSuffixValue(UnrestrictedSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		return new SuffixValueRestriction(parameter, ExpressionUtil.TRUE);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals) {
		return ExpressionUtil.TRUE;
	}

	@Override
	public AbstractSuffixValueRestriction shift(int shiftStep) {
		return new UnrestrictedSuffixValue(this, shiftStep);
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
	public <K extends TypedValue, V extends TypedValue> AbstractSuffixValueRestriction relabel(Mapping<K, V> renaming) {
		return this;
	}

	@Override
	public String toString() {
		return "Unrestricted(" + parameter.toString() + ")";
	}

	@Override
	public AbstractSuffixValueRestriction merge(AbstractSuffixValueRestriction other, Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
		return this;
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		return true;
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
}
