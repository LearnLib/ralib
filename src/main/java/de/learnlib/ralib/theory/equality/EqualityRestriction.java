package de.learnlib.ralib.theory.equality;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SDTGuardElement;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class EqualityRestriction extends AbstractSuffixValueRestriction {

	private Set<SDTGuardElement> regs;

	public EqualityRestriction(SuffixValue parameter, Set<SDTGuardElement> regs) {
		super(parameter);
		for (SDTGuardElement r : regs) {
			assert r != null;
		}
		this.regs = regs;
	}

	public EqualityRestriction(EqualityRestriction other, int shift) {
		super(other, shift);
		regs = new LinkedHashSet<>();
		for (SDTGuardElement r : other.regs) {
			if (r instanceof SuffixValue s) {
				regs.add(new SuffixValue(s.getDataType(), s.getId() + shift));
			} else {
				regs.add(r);
			}
		}
	}

	private static Expression<Boolean> equalityExpression(SuffixValue param, Set<SDTGuardElement> regs) {
		Expression[] exprs = new Expression[regs.size()];
		int i = 0;
		for (SDTGuardElement r : regs) {
			if (r instanceof DataValue d) {
				exprs[i] = new NumericBooleanExpression(param, NumericComparator.EQ, d);
			} else if (r instanceof SymbolicDataValue s) {
				exprs[i] = new NumericBooleanExpression(param, NumericComparator.EQ, s);
			} else {
				throw new RuntimeException("Unknown SDT guard element class: " + r.getClass());
			}
			i++;
		}
		return ExpressionUtil.and(exprs);
	}

	@Override
	public AbstractSuffixValueRestriction shift(int shiftStep) {
		return new EqualityRestriction(this, shiftStep);
	}

	@Override
	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		Set<SDTGuardElement> regs = new LinkedHashSet<>();
		for (SDTGuardElement r : this.regs) {
			if (r instanceof SymbolicDataValue s && mapping.containsKey(s)) {
				regs.add(mapping.get(s));
			} else {
				regs.add(r);
			}
		}
		return new EqualityRestriction(parameter, regs);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals) {
		Expression[] exprs = new Expression[regs.size()];
		int i = 0;
		for (SDTGuardElement r : regs) {
			if (r instanceof DataValue d) {
				exprs[i] = new NumericBooleanExpression(parameter, NumericComparator.EQ, d);
			} else if (r instanceof SymbolicDataValue s) {
				exprs[i] = new NumericBooleanExpression(parameter, NumericComparator.EQ, s);
			} else {
				throw new RuntimeException("Unknown SDT guard element class: " + r.getClass());
			}
			i++;
		}
		return ExpressionUtil.or(exprs);
	}

	@Override
	public AbstractSuffixValueRestriction merge(AbstractSuffixValueRestriction other,
			Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T extends TypedValue> EqualityRestriction relabel(Bijection<T> bijection) {
		Set<SDTGuardElement> regs = new LinkedHashSet<>();
		for (SDTGuardElement r : this.regs) {
			if (bijection.containsKey(r)) {
				TypedValue t = bijection.get(r);
				assert t instanceof SDTGuardElement;
				SDTGuardElement casted = (SDTGuardElement) t;
				regs.add(casted);
			} else {
				regs.add(r);
			}
		}
		return new EqualityRestriction(parameter, regs);
	}

	@Override
	public boolean isTrue() {
		return false;
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	public Set<SDTGuardElement> getElements() {
		return regs;
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
		if (!regs.equals(((EqualityRestriction) obj).regs)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 37 * hash + ((regs == null) ? 0 : regs.hashCode());
		return hash;
	}

	@Override
	public String toString() {
		if (regs.isEmpty()) {
			return "";
		}

//		final String eqStart = "(" + parameter.toString() + " == ";
//		final String eqEnd = ")";
//		final String delim = ", ";
//		String str = eqStart;
		String str = "";
		int i = 0;
		for (SDTGuardElement r : regs) {
			str = str + "(" + parameter + " == " + r.toString() + ")";
			i++;
			if (i < regs.size()) {
				str = str + " OR ";
			}
		}
		return str;
	}
}
