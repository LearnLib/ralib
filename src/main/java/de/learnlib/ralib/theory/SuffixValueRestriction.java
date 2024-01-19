package de.learnlib.ralib.theory;

import java.util.Map;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.equality.EqualRestriction;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public abstract class SuffixValueRestriction {
	protected final SuffixValue parameter;

	public SuffixValueRestriction(SuffixValue parameter) {
		this.parameter = parameter;
	}

	public SuffixValueRestriction(SuffixValueRestriction other) {
		parameter = new SuffixValue(other.parameter.getType(), other.parameter.getId());
	}

	public SuffixValueRestriction(SuffixValueRestriction other, int shift) {
		parameter = new SuffixValue(other.parameter.getType(), other.parameter.getId()+shift);
	}

	public SuffixValue getParameter() {
		return parameter;
	}

	public abstract SuffixValueRestriction shift(int shiftStep);

	public abstract GuardExpression toGuardExpression(Map<SymbolicDataValue, DataValue<?>> vals);

	public static SuffixValueRestriction generateRestriction(SuffixValue sv, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts) {
		DataValue[] prefixVals = DataWords.valsOf(prefix);
		DataValue[] suffixVals = DataWords.valsOf(suffix);
		DataValue val = suffixVals[sv.getId()-1];
//		DataValue val = suffixVals[idx];
//		SuffixValue sv = new SuffixValue(val.getType(), idx+1);

		boolean equalsPrefixValue = false;
		for (DataValue dv : prefixVals) {
			if (dv.equals(val))
				equalsPrefixValue = true;
		}
		boolean equalsSuffixValue = false;
		int equalSV = -1;
		for (int i = 0; i < suffixVals.length && !equalsSuffixValue; i++) {
			if (suffixVals[i].equals(val)) {
				equalsSuffixValue = true;
				equalSV = i;
			}
		}

		// case equal to previous suffix value
		if (equalsSuffixValue && !equalsPrefixValue) {
			return new EqualRestriction(sv, new SuffixValue(suffixVals[equalSV].getType(), equalSV+1));
		}
		// case fresh
		else if (!equalsSuffixValue && !equalsPrefixValue) {
			return new FreshSuffixValue(sv);
		}
		// case unrestricted
		else {
			return new UnrestrictedSuffixValue(sv);
		}
	}
}
