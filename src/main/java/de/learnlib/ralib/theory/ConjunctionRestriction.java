package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class ConjunctionRestriction extends AbstractSuffixValueRestriction {

	private Collection<AbstractSuffixValueRestriction> conjuncts;

	public ConjunctionRestriction(SuffixValue parameter, Collection<? extends AbstractSuffixValueRestriction> conjuncts) {
		super(parameter);
		this.conjuncts = new ArrayList<>();
		boolean hasFalse = false;
		for (AbstractSuffixValueRestriction restr : conjuncts) {
			if (this.conjuncts.contains(restr)) {
				continue;
			}
			if (restr.isFalse()) {
				hasFalse = true;
				break;
			} else if (!(restr.isTrue())) {
				this.conjuncts.add(restr);
			}
		}
		if (hasFalse) {
			this.conjuncts.clear();
		}
	}

	public ConjunctionRestriction(ConjunctionRestriction other, int shift) {
		super(other, shift);
		conjuncts = new ArrayList<>();
		other.conjuncts.forEach(r -> conjuncts.add(r.shift(shift)));
	}

	@Override
	public ConjunctionRestriction shift(int shiftStep) {
		return new ConjunctionRestriction(this, shiftStep);
	}

	@Override
	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		Collection<AbstractSuffixValueRestriction> conc = new ArrayList<>();
		conjuncts.forEach(r -> conc.add(r.concretize(mapping)));
		return new ConjunctionRestriction(parameter, conc);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals) {
		Expression[] exprs = new Expression[conjuncts.size()];
		int i = 0;
		for (AbstractSuffixValueRestriction r : conjuncts) {
			exprs[i++] = r.toGuardExpression(vals);
		}
		return ExpressionUtil.and(exprs);
	}

	@Override
	public boolean isTrue() {
		return conjuncts.isEmpty();
	}

	@Override
	public boolean isFalse() {
		return !conjuncts.isEmpty();
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
	public <T extends TypedValue> AbstractSuffixValueRestriction relabel(Bijection<T> bijection) {
		Collection<AbstractSuffixValueRestriction> relabeled = new ArrayList<>();
		conjuncts.forEach(r -> relabeled.add(r.relabel(bijection)));
		return create(parameter, relabeled);
	}

	@Override
	public String toString() {
		Iterator<AbstractSuffixValueRestriction> it = conjuncts.iterator();
		String str = "";
		while (it.hasNext()) {
			str = str + it.next().toString();
			if (it.hasNext()) {
				str = str + " AND ";
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
		ConjunctionRestriction other = (ConjunctionRestriction) obj;
		return other.conjuncts.equals(conjuncts);
	}

	public static AbstractSuffixValueRestriction create(SuffixValue parameter, Collection<? extends AbstractSuffixValueRestriction> conjuncts) {
		if (conjuncts.isEmpty()) {
			return new TrueRestriction(parameter);
		}
		if (conjuncts.size() == 1) {
			return conjuncts.iterator().next();
		}
		ConjunctionRestriction conjunction = new ConjunctionRestriction(parameter, conjuncts);
		if (conjunction.isTrue()) {
			return new TrueRestriction(parameter);
		}
		return conjunction;
	}

	public static AbstractSuffixValueRestriction create(SuffixValue parameter, AbstractSuffixValueRestriction ... conjuncts) {
		return create(parameter, Arrays.asList(conjuncts));
	}
}
