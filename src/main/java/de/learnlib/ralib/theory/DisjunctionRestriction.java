package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class DisjunctionRestriction extends AbstractSuffixValueRestriction {

	private Collection<AbstractSuffixValueRestriction> disjuncts;

	public DisjunctionRestriction(SuffixValue parameter, Collection<? extends AbstractSuffixValueRestriction> disjuncts) {
		super(parameter);
		this.disjuncts = new ArrayList<>();
		boolean hasTrue = false;
		for (AbstractSuffixValueRestriction restr : disjuncts) {
			if (restr.isTrue()) {
				hasTrue = true;
				break;
			} else if (!(restr.isFalse())) {
				this.disjuncts.add(restr);
			}
		}
		if (hasTrue) {
			this.disjuncts.clear();
		}
	}

	public DisjunctionRestriction(SuffixValue parameter, AbstractSuffixValueRestriction ... disjuncts) {
		this(parameter, Arrays.asList(disjuncts));
//		super(parameter);
//		this.disjuncts = Arrays.asList(disjuncts);
	}

	public DisjunctionRestriction(DisjunctionRestriction other, int shift) {
		super(other, shift);
		disjuncts = new ArrayList<>();
		other.disjuncts.forEach(r -> disjuncts.add(r.shift(shift)));
	}

	@Override
	public DisjunctionRestriction shift(int shiftStep) {
		return new DisjunctionRestriction(this, shiftStep);
	}

	@Override
	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		Collection<AbstractSuffixValueRestriction> conc = new ArrayList<>();
		disjuncts.forEach(r -> conc.add(r.concretize(mapping)));
		return create(parameter, conc);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals) {
		Expression[] exprs = new Expression[disjuncts.size()];
		int i = 0;
		for (AbstractSuffixValueRestriction r : disjuncts) {
			exprs[i++] = r.toGuardExpression(vals);
		}
		return ExpressionUtil.or(exprs);
	}

	@Override
	public boolean isTrue() {
		return disjuncts.isEmpty();
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	@Override
	public AbstractSuffixValueRestriction merge(AbstractSuffixValueRestriction other,
			Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
		throw new RuntimeException("Unsupported operation");
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		Iterator<AbstractSuffixValueRestriction> it = disjuncts.iterator();
		String str = "";
		while (it.hasNext()) {
			str = str + it.next().toString();
			if (it.hasNext()) {
				str = str + " OR ";
			}
		}
		return str;
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
		DisjunctionRestriction other = (DisjunctionRestriction) obj;
		return other.disjuncts.equals(disjuncts);
	}

	public static AbstractSuffixValueRestriction create(SuffixValue parameter, Collection<? extends AbstractSuffixValueRestriction> disjuncts) {
		if (disjuncts.isEmpty()) {
			return new TrueRestriction(parameter);
		}
		if (disjuncts.size() == 1) {
			return disjuncts.iterator().next();
		}
		DisjunctionRestriction disjunction = new DisjunctionRestriction(parameter, disjuncts);
		if (disjunction.isTrue()) {
			return new TrueRestriction(parameter);
		}
		return disjunction;
	}

	public static AbstractSuffixValueRestriction create(SuffixValue parameter, AbstractSuffixValueRestriction ... disjuncts) {
		return create(parameter, Arrays.asList(disjuncts));
	}
}
