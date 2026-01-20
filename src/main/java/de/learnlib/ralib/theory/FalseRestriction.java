package de.learnlib.ralib.theory;

import java.util.Objects;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class FalseRestriction extends SuffixValueRestriction {

	public FalseRestriction(SuffixValue parameter) {
		super(parameter, ExpressionUtil.FALSE);
	}

	public FalseRestriction(FalseRestriction other, int shift) {
		super(other, shift);
	}

	@Override
	public SuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		return this;
	}

	@Override
	public SuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> ... valuations) {
		return this;
	}

	@Override
	public SuffixValueRestriction or(SuffixValueRestriction other) {
		return other;
	}

	@Override
	public SuffixValueRestriction and(SuffixValueRestriction other) {
		return this;
	}

	@Override
	public FalseRestriction shift(int shiftStep) {
		return new FalseRestriction(this, shiftStep);
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
