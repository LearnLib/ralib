package de.learnlib.ralib.theory;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
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
}
