package de.learnlib.ralib.theory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import de.learnlib.ralib.theory.equality.UnmappedEqualityRestriction;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class ConjunctionRestriction extends AbstractSuffixValueRestriction implements RestrictionContainer, ElementRestriction {

	private Collection<AbstractSuffixValueRestriction> conjuncts;

	public ConjunctionRestriction(SuffixValue parameter, Collection<? extends AbstractSuffixValueRestriction> conjuncts) {
		super(parameter);
		this.conjuncts = new ArrayList<>();
		boolean hasFalse = false;
		for (AbstractSuffixValueRestriction restr : conjuncts) {
			if (restr instanceof ConjunctionRestriction cr) {
				for (AbstractSuffixValueRestriction r : cr.conjuncts) {
					if (!this.conjuncts.contains(r)) {
						this.conjuncts.add(r);
					}
				}
			}
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

	protected Collection<AbstractSuffixValueRestriction> getConjuncts() {
		return conjuncts;
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
	public <K extends TypedValue, V extends TypedValue> AbstractSuffixValueRestriction relabel(Mapping<K, V> renaming) {
		Collection<AbstractSuffixValueRestriction> relabeled = new ArrayList<>();
		conjuncts.forEach(r -> relabeled.add(r.relabel(renaming)));
//		for (AbstractSuffixValueRestriction r : conjuncts) {
//			relabeled.add(r.relabel(renaming));
//		}
		return create(parameter, relabeled);
	}

	@Override
	public List<ElementRestriction> getRestrictions(Expression<BigDecimal> element) {
		List<ElementRestriction> restrictions = new ArrayList<>();
		for (AbstractSuffixValueRestriction r : conjuncts) {
			if (r instanceof ElementRestriction er && er.containsElement(element)) {
				restrictions.addAll(er.getRestrictions(element));
			}
		}
		return restrictions;
	}

	@Override
	public boolean containsElement(Expression<BigDecimal> element) {
		for (AbstractSuffixValueRestriction r : conjuncts) {
			if (r instanceof ElementRestriction er && er.containsElement(element)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<Expression<BigDecimal>> getElements() {
		Set<Expression<BigDecimal>> ret = new LinkedHashSet<>();
		for (AbstractSuffixValueRestriction r : conjuncts) {
			if (r instanceof ElementRestriction er) {
				ret.addAll(er.getElements());
			}
		}
		return ret;
	}

	@Override
	public AbstractSuffixValueRestriction replaceElement(Expression<BigDecimal> replace, Expression<BigDecimal> by) {
		Collection<AbstractSuffixValueRestriction> replaced = new ArrayList<>();
		for (AbstractSuffixValueRestriction r : conjuncts) {
			if (r instanceof ElementRestriction er && er.containsElement(replace)) {
				replaced.add(er.replaceElement(replace, by));
			} else {
				replaced.add(r);
			}
		}
		return create(getParameter(), replaced);
	}

	@Override
	public boolean contains(AbstractSuffixValueRestriction restr) {
		for (AbstractSuffixValueRestriction r : conjuncts) {
			if (r.equals(restr)) {
				return true;
			}
			if (r instanceof RestrictionContainer rc && rc.contains(restr)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsUnmapped() {
		for (AbstractSuffixValueRestriction r : conjuncts) {
			if (r instanceof UnmappedEqualityRestriction) {
				return true;
			}
		}
		return false;
	}

	@Override
	public AbstractSuffixValueRestriction replace(AbstractSuffixValueRestriction replace, AbstractSuffixValueRestriction with) {
		Collection<AbstractSuffixValueRestriction> replaced = new ArrayList<>();
		for (AbstractSuffixValueRestriction r : conjuncts) {
			if (r.equals(replace)) {
				if (with instanceof ConjunctionRestriction cr) {
					replaced.addAll(cr.conjuncts);
				} else {
					replaced.add(with);
				}
			} else if (r instanceof RestrictionContainer rc && rc.contains(replace)) {
				AbstractSuffixValueRestriction nrc = rc.replace(replace, with);
				if (nrc instanceof ConjunctionRestriction cr) {
					replaced.addAll(cr.getConjuncts());
				} else {
					replaced.add(nrc);
				}
			} else {
				replaced.add(r);
			}
		}
		return create(getParameter(), replaced);
	}

	@Override
	public ConjunctionRestriction cast() {
		return this;
	}

//	@Override
//	public AbstractSuffixValueRestriction relabel(SDTRelabeling relabeling) {
//		Collection<AbstractSuffixValueRestriction> relabeled = new ArrayList<>();
//		conjuncts.forEach(r -> relabeled.add(r.relabel(relabeling)));
//		return create(parameter, relabeled);
//	}

	@Override
	public String toString() {
		Iterator<AbstractSuffixValueRestriction> it = conjuncts.iterator();
		String str = "(";
		while (it.hasNext()) {
			str = str + it.next().toString();
			if (it.hasNext()) {
				str = str + " AND ";
			}
		}
		return str + ")";
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

	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 61 * hash + conjuncts.hashCode();
		return hash;
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
