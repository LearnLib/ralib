package de.learnlib.ralib.theory;

import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
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
}
