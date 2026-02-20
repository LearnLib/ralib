package de.learnlib.ralib.theory;

import java.util.Objects;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class TrueRestriction extends SuffixValueRestriction {

	public TrueRestriction(SuffixValue parameter) {
		super(parameter, ExpressionUtil.TRUE);
	}

	public TrueRestriction(TrueRestriction other, int shift) {
		super(other, shift);
	}

	@Override
	public TrueRestriction shift(int shiftStep) {
		return new TrueRestriction(this, shiftStep);
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
		return this;
	}

	@Override
	public SuffixValueRestriction and(SuffixValueRestriction other) {
		return other;
	}

	@Override
	public <K extends TypedValue, V extends TypedValue> AbstractSuffixValueRestriction relabel(Mapping<K, V> renaming) {
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

	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 37 * hash + Objects.hashCode(getClass());
		return hash;
	}
}
