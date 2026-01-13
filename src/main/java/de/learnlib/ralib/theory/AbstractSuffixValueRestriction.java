package de.learnlib.ralib.theory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.theory.equality.EqualRestriction;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

public abstract class AbstractSuffixValueRestriction {
	protected final SuffixValue parameter;

	public AbstractSuffixValueRestriction(SuffixValue parameter) {
		this.parameter = parameter;
	}

	public AbstractSuffixValueRestriction(AbstractSuffixValueRestriction other) {
		parameter = new SuffixValue(other.parameter.getDataType(), other.parameter.getId());
	}

	public AbstractSuffixValueRestriction(AbstractSuffixValueRestriction other, int shift) {
		parameter = new SuffixValue(other.parameter.getDataType(), other.parameter.getId()+shift);
	}

	public SuffixValue getParameter() {
		return parameter;
	}

	public abstract AbstractSuffixValueRestriction shift(int shiftStep);

	public abstract AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping);

	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> ... mappings) {
		Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
		for (Mapping<? extends SymbolicDataValue, DataValue> m : mappings) {
			mapping.putAll(m);
		}
		return concretize(mapping);
	}

	public abstract Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals);

	public abstract AbstractSuffixValueRestriction merge(AbstractSuffixValueRestriction other, Map<SuffixValue, AbstractSuffixValueRestriction> prior);

	public abstract boolean revealsRegister(SymbolicDataValue r);

	public abstract <T extends TypedValue> AbstractSuffixValueRestriction relabel(Bijection<T> bijection);

	/**
	 * Generate a generic restriction using Fresh, Unrestricted and Equal restriction types
	 *
	 * @param sv
	 * @param prefix
	 * @param suffix
	 * @param consts
	 * @return
	 */
	public static AbstractSuffixValueRestriction genericRestriction(SuffixValue sv, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts) {
		DataValue[] prefixVals = DataWords.valsOf(prefix);
		DataValue[] suffixVals = DataWords.valsOf(suffix);
		DataType[] prefixTypes = DataWords.typesOf(DataWords.actsOf(prefix));
		DataType[] suffixTypes = DataWords.typesOf(DataWords.actsOf(suffix));
		DataValue val = suffixVals[sv.getId()-1];
		int firstSymbolArity = suffix.length() > 0 ? suffix.getSymbol(0).getBaseSymbol().getArity() : 0;

		boolean unrestricted = false;
		for (int i = 0; i < prefixVals.length; i++) {
			DataValue dv = prefixVals[i];
			DataType dt = prefixTypes[i];
            if (dt.equals(sv.getDataType()) && dv.equals(val)) {
                unrestricted = true;
                break;
            }
		}
		if (consts.containsValue(val)) {
			unrestricted = true;
		}
		boolean equalsSuffixValue = false;
		int equalSV = -1;
		for (int i = 0; i < sv.getId()-1 && !equalsSuffixValue; i++) {
			DataType dt = suffixTypes[i];
			if (dt.equals(sv.getDataType()) && suffixVals[i].equals(val)) {
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
			AbstractSuffixValueRestriction restr = new EqualRestriction(sv, new SuffixValue(suffixVals[equalSV].getDataType(), equalSV+1));
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

	public static SymbolicSuffix concretize(SymbolicSuffix suffix, Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		Set<SuffixValue> suffixVals = suffix.getValues();
		Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
		for (SuffixValue s : suffixVals) {
			AbstractSuffixValueRestriction restr = suffix.getRestriction(s);
			restrictions.put(s, restr.concretize(mapping));
		}
		return new SymbolicSuffix(suffix.getActions(), restrictions);
	}

	public abstract boolean isTrue();

	public abstract boolean isFalse();

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
		AbstractSuffixValueRestriction other = (AbstractSuffixValueRestriction) obj;
		return Objects.equals(parameter, other.parameter);
	}

	public static AbstractSuffixValueRestriction genericRestriction(SDTGuard guard, Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
    	SuffixValue suffixValue = guard.getParameter();
    	// case fresh
    	if (guard instanceof SDTGuard.SDTTrueGuard || guard instanceof SDTGuard.DisequalityGuard) {
    		return new FreshSuffixValue(suffixValue);
    	// case equal to previous suffix value
    	} else if (guard instanceof SDTGuard.EqualityGuard) {
    		SDTGuardElement param = ((SDTGuard.EqualityGuard) guard).register();
    		if (param instanceof SuffixValue) {
    			AbstractSuffixValueRestriction restr = prior.get(param);
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

	public static Map<SuffixValue, AbstractSuffixValueRestriction> shift(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, int shift) {
		Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
		for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> e : restrictions.entrySet()) {
			SuffixValue s = new SuffixValue(e.getKey().getDataType(), e.getKey().getId() + shift);
			AbstractSuffixValueRestriction r = e.getValue().shift(shift);
			ret.put(s, r);
		}
		return ret;
	}
}
