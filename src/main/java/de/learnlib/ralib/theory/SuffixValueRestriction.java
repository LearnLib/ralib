package de.learnlib.ralib.theory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualRestriction;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

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

	public abstract GuardExpression toGuardExpression(Set<SymbolicDataValue> vals);

	public abstract SuffixValueRestriction merge(SuffixValueRestriction other, Map<SuffixValue, SuffixValueRestriction> prior);

	public abstract boolean revealsRegister(SymbolicDataValue r);

	/**
	 * Generate a generic restriction using Fresh, Unrestricted and Equal restriction types
	 *
	 * @param sv
	 * @param prefix
	 * @param suffix
	 * @param consts
	 * @return
	 */
	public static SuffixValueRestriction genericRestriction(SuffixValue sv, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts) {
		DataValue<?>[] prefixVals = DataWords.valsOf(prefix);
		DataValue<?>[] suffixVals = DataWords.valsOf(suffix);
		DataType[] prefixTypes = DataWords.typesOf(DataWords.actsOf(prefix));
		DataType[] suffixTypes = DataWords.typesOf(DataWords.actsOf(suffix));
		DataValue<?> val = suffixVals[sv.getId()-1];
		int firstSymbolArity = suffix.length() > 0 ? suffix.getSymbol(0).getBaseSymbol().getArity() : 0;

		boolean unrestricted = false;
		for (int i = 0; i < prefixVals.length; i++) {
			DataValue<?> dv = prefixVals[i];
			DataType dt = prefixTypes[i];
			if (dt.equals(sv.getType()) && dv.equals(val)) {
				unrestricted = true;
			}
		}
		if (consts.containsValue(val)) {
			unrestricted = true;
		}
		boolean equalsSuffixValue = false;
		int equalSV = -1;
		for (int i = 0; i < sv.getId()-1 && !equalsSuffixValue; i++) {
			DataType dt = suffixTypes[i];
			if (dt.equals(sv.getType()) && suffixVals[i].equals(val)) {
				if (sv.getId() <= firstSymbolArity) {
					unrestricted = true;
				} else {
					equalsSuffixValue = true;
					equalSV = i;
				}
			}
		}

		// case equal to previous suffix value
		if (equalsSuffixValue && !unrestricted) {
			SuffixValueRestriction restr = new EqualRestriction(sv, new SuffixValue(suffixVals[equalSV].getType(), equalSV+1));
			return restr;
		}
		// case fresh
		else if (!equalsSuffixValue && !unrestricted) {
			return new FreshSuffixValue(sv);
		}
		// case unrestricted
		else {
			return new UnrestrictedSuffixValue(sv);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(parameter);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SuffixValueRestriction other = (SuffixValueRestriction) obj;
		return Objects.equals(parameter, other.parameter);
	}

	public static SuffixValueRestriction genericRestriction(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
    	SuffixValue suffixValue = guard.getParameter();
    	// case fresh
    	if (guard instanceof SDTTrueGuard || guard instanceof DisequalityGuard) {
    		return new FreshSuffixValue(suffixValue);
    	// case equal to previous suffix value
    	} else if (guard instanceof EqualityGuard) {
    		SymbolicDataValue param = ((EqualityGuard) guard).getRegister();
    		if (param instanceof SuffixValue) {
    			SuffixValueRestriction restr = prior.get(param);
    			if (restr instanceof FreshSuffixValue) {
    				return new EqualRestriction(suffixValue, (SuffixValue)param);
    			} else if (restr instanceof EqualRestriction) {
    				return new EqualRestriction(suffixValue, ((EqualRestriction)restr).getEqualParameter());
    			} else {
    				return new UnrestrictedSuffixValue(suffixValue);
    			}
    		} else {
    			return new UnrestrictedSuffixValue(suffixValue);
    		}
    	// case unrestricted
    	} else {
    		return new UnrestrictedSuffixValue(suffixValue);
    	}
	}
}
