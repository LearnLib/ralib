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
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import de.learnlib.ralib.theory.equality.UnmappedEqualityRestriction;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class DisjunctionRestriction extends AbstractSuffixValueRestriction implements RestrictionContainer, ElementRestriction {

	private Collection<AbstractSuffixValueRestriction> disjuncts;

	public DisjunctionRestriction(SuffixValue parameter, Collection<? extends AbstractSuffixValueRestriction> disjuncts) {
		super(parameter);
		this.disjuncts = new ArrayList<>();
		boolean hasTrue = false;
		for (AbstractSuffixValueRestriction restr : disjuncts) {
			if (restr instanceof DisjunctionRestriction cr) {
				for (AbstractSuffixValueRestriction r : cr.disjuncts) {
					if (!this.disjuncts.contains(r)) {
						this.disjuncts.add(r);
					}
				}
			}
			if (this.disjuncts.contains(restr)) {
				continue;
			}
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

	protected Collection<AbstractSuffixValueRestriction> getDisjuncts() {
		return disjuncts;
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
	public boolean containsFresh() {
		return disjuncts.stream().filter(AbstractSuffixValueRestriction::containsFresh).findAny().isPresent();
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
		disjuncts.forEach(r -> relabeled.add(r.relabel(renaming)));
		return create(parameter, relabeled);
	}

	@Override
	public boolean containsElement(Expression<BigDecimal> element) {
		for (AbstractSuffixValueRestriction r : disjuncts) {
			if (r instanceof ElementRestriction er && er.containsElement(element)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<Expression<BigDecimal>> getElements() {
		Set<Expression<BigDecimal>> ret = new LinkedHashSet<>();
		for (AbstractSuffixValueRestriction r : disjuncts) {
			if (r instanceof ElementRestriction er) {
				ret.addAll(er.getElements());
			}
		}
		return ret;
	}

	@Override
	public AbstractSuffixValueRestriction replaceElement(Expression<BigDecimal> replace, Expression<BigDecimal> by) {
		Collection<AbstractSuffixValueRestriction> replaced = new ArrayList<>();
		for (AbstractSuffixValueRestriction r : disjuncts) {
			if (r instanceof ElementRestriction er && er.containsElement(replace)) {
				replaced.add(er.replaceElement(replace, by));
			} else {
				replaced.add(r);
			}
		}
		return create(getParameter(), replaced);
	}

	@Override
	public List<ElementRestriction> getRestrictions(Expression<BigDecimal> element) {
		List<ElementRestriction> restrictions = new ArrayList<>();
		for (AbstractSuffixValueRestriction r : disjuncts) {
			if (r instanceof ElementRestriction er && er.containsElement(element)) {
				restrictions.addAll(er.getRestrictions(element));
			}
		}
		return restrictions;
	}

	@Override
	public boolean contains(AbstractSuffixValueRestriction restr) {
		for (AbstractSuffixValueRestriction r : disjuncts) {
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
		for (AbstractSuffixValueRestriction r : disjuncts) {
			if (r instanceof UnmappedEqualityRestriction) {
				return true;
			}
		}
		return false;
	}

	@Override
	public AbstractSuffixValueRestriction replace(AbstractSuffixValueRestriction replace, AbstractSuffixValueRestriction with) {
		Collection<AbstractSuffixValueRestriction> replaced = new ArrayList<>();
		for (AbstractSuffixValueRestriction r : disjuncts) {
			if (r.equals(replace)) {
				if (with instanceof DisjunctionRestriction dr) {
					replaced.addAll(dr.disjuncts);
				} else {
					replaced.add(with);
				}
			} else if (r instanceof RestrictionContainer rc && rc.contains(replace)) {
				AbstractSuffixValueRestriction nrc = rc.replace(replace, with);
				if (nrc instanceof DisjunctionRestriction dr) {
					replaced.addAll(dr.getDisjuncts());
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
	public DisjunctionRestriction cast() {
		return this;
	}


//	@Override
//	public AbstractSuffixValueRestriction relabel(SDTRelabeling relabeling) {
//		Collection<AbstractSuffixValueRestriction> relabeled = new ArrayList<>();
//		disjuncts.forEach(r -> relabeled.add(r.relabel(relabeling)));
//		return create(parameter, disjuncts);
//	}

	@Override
	public String toString() {
		Iterator<AbstractSuffixValueRestriction> it = disjuncts.iterator();
		String str = "(";
		while (it.hasNext()) {
			str = str + it.next().toString();
			if (it.hasNext()) {
				str = str + " OR ";
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
		DisjunctionRestriction other = (DisjunctionRestriction) obj;
		return other.disjuncts.equals(disjuncts);
	}

	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 61 * hash + disjuncts.hashCode();
		return hash;
	}

	public static AbstractSuffixValueRestriction create(SuffixValue parameter, Collection<? extends AbstractSuffixValueRestriction> disjuncts) {
		disjuncts = disjuncts.stream().distinct().filter(d -> !d.isTrue()).collect(Collectors.toList());
		if (disjuncts == null || disjuncts.isEmpty()) {
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
