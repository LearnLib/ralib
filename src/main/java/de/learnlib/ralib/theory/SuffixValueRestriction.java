package de.learnlib.ralib.theory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SDTGuardElement;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.smt.ReplacingValuesVisitor;
import de.learnlib.ralib.smt.ReplacingVarsVisitor;
import de.learnlib.ralib.smt.SMTUtil;
import de.learnlib.ralib.smt.VarsValuationVisitor;
import de.learnlib.ralib.theory.equality.EqualityRestriction;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class SuffixValueRestriction extends AbstractSuffixValueRestriction {

	protected final Expression<Boolean> expr;

	protected class DummyVisitor extends DuplicatingVisitor<Map<? extends Variable<BigDecimal>, ? extends Variable<BigDecimal>>> {
		@Override
		public <E> Expression<?> visit(Variable<E> v, Map<? extends Variable<BigDecimal>, ? extends Variable<BigDecimal>> data) {
			Variable<BigDecimal> newVar = data.get(v);
			return (newVar != null) ? newVar : v;
		}

		public <T> Expression<T> apply(Expression<T> expr, Map<? extends Variable<BigDecimal>, ? extends Variable<BigDecimal>> rename) {
			return visit(expr, rename).requireAs(expr.getType());
		}
	};

	protected class DummyDataValue extends Variable<BigDecimal> {
		int id;
		DataType type;

		public DummyDataValue(DataType type, int id) {
			super(BuiltinTypes.DECIMAL, "dummy" + id);
			this.type = type;
			this.id = id;
		}

		public DataType getDataType() {
			return type;
		}

		public int getId() {
			return id;
		}

		@Override
		public boolean equals(Object obj) {
	        if (obj == null) {
	            return false;
	        }
	        if (getClass() != obj.getClass()) {
	            return false;
	        }
	        final DummyDataValue other = (DummyDataValue) obj;
	        if (!Objects.equals(this.type, other.type)) {
	            return false;
	        }
	        return this.id == other.id;
		}

	    @Override
	    public int hashCode() {
	        int hash = 7;
	        hash = 97 * hash + Objects.hashCode(this.id);
	        hash = 97 * hash + Objects.hashCode(this.type);
	        hash = 97 * hash + Objects.hashCode(this.getClass());
	        return hash;
	    }
	};

	public SuffixValueRestriction(SuffixValue parameter, Expression<Boolean> expr) {
		super(parameter);
		this.expr = expr;
	}

	public SuffixValueRestriction(SuffixValueRestriction other, int shift) {
		super(other, shift);
		Set<SuffixValue> suffixVals = new LinkedHashSet<>();
		SMTUtil.getSymbolicDataValues(other.expr)
		    .stream()
		    .filter(s -> s.isSuffixValue())
		    .forEach(s -> suffixVals.add((SuffixValue)s));
		Map<SuffixValue, DummyDataValue> toDummy = new LinkedHashMap<>();
		Map<DummyDataValue, SuffixValue> fromDummy = new LinkedHashMap<>();
		suffixVals.stream().forEach(s -> toDummy.put(s, new DummyDataValue(s.getDataType(), s.getId())));
		toDummy.values().stream().forEach(d -> fromDummy.put(d, new SuffixValue(d.getDataType(), d.getId() + shift)));
		DummyVisitor visitor = new DummyVisitor();
		Expression<Boolean> dummyExpr = visitor.apply(other.expr, toDummy);
		this.expr = visitor.apply(dummyExpr, fromDummy);
	}

	@Override
	public AbstractSuffixValueRestriction shift(int shiftStep) {
		return new SuffixValueRestriction(this, shiftStep);
	}

	@Override
	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		VarsValuationVisitor vvv = new VarsValuationVisitor();
		Expression<Boolean> expr = vvv.apply(this.expr, mapping);
		return new SuffixValueRestriction(parameter, expr);
	}

	public SuffixValueRestriction or(SuffixValueRestriction other) {
		if (!this.parameter.equals(other.parameter)) {
			throw new IllegalArgumentException("Mismatched parameters: " + this.parameter + ", " + other.parameter);
		}
		return new SuffixValueRestriction(parameter, ExpressionUtil.or(this.expr, other.expr));
	}

	public SuffixValueRestriction and(SuffixValueRestriction other) {
		if (!this.parameter.equals(other.parameter)) {
			throw new IllegalArgumentException("Mismatched parameters: " + this.parameter + ", " + other.parameter);
		}
		return new SuffixValueRestriction(parameter, ExpressionUtil.and(this.expr, other.expr));
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals) {
		return expr;
	}

	public SuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> ... valuations) {
		VarsValuationVisitor vvv = new VarsValuationVisitor();
		Mapping<SymbolicDataValue, DataValue> valuation = new Mapping<>();
		for (Mapping<? extends SymbolicDataValue, DataValue> v : valuations) {
			valuation.putAll(v);
		}
		Expression<Boolean> expr = vvv.apply(this.expr, valuation);
		return new SuffixValueRestriction(parameter, expr);
	}

	@Override
	public AbstractSuffixValueRestriction merge(AbstractSuffixValueRestriction other,
			Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
		throw new RuntimeException("Not supported for this type of restriction");
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTrue() {
		return expr.equals(ExpressionUtil.TRUE);
	}

	@Override
	public boolean isFalse() {
		return expr.equals(ExpressionUtil.FALSE);
	}

	@Override
	public <T extends TypedValue> AbstractSuffixValueRestriction relabel(Bijection<T> bijection) {
		if (bijection.isEmpty()) {
			return this;
		}
		T first = bijection.keySet().iterator().next();
		if (first instanceof DataValue) {
			ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
			Mapping<DataValue, DataValue> map = new Mapping<>();
			bijection.forEach((k,v) -> map.put((DataValue) k, (DataValue) v));
			Expression<Boolean> expr = rvv.apply(this.expr, map);
			return new SuffixValueRestriction(parameter, expr);
		} else if (first instanceof SymbolicDataValue) {
			ReplacingVarsVisitor rvv = new ReplacingVarsVisitor();
			VarMapping<SymbolicDataValue, SymbolicDataValue> map = new VarMapping<>();
			bijection.forEach((k,v) -> map.put((SymbolicDataValue) k, (SymbolicDataValue) v));
			Expression<Boolean> expr = rvv.apply(this.expr, map);
			return new SuffixValueRestriction(parameter, expr);
		}
		throw new RuntimeException("Unknown parameter type");
	}

	@Override
	public String toString() {
		return expr.toString();
	}

	@Override
	public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
		SuffixValueRestriction other = (SuffixValueRestriction) obj;
		if (!other.parameter.equals(parameter)) {
			return false;
		}
		return other.expr.equals(expr);
	}

	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 89 * hash + (expr == null ? 0 : Objects.hashCode(expr));
		return hash;
	}

//	public static SymbolicSuffix concretize(SymbolicSuffix suffix, Mapping<? extends SymbolicDataValue, DataValue> ... valuations) {
//		Map<SuffixValue, SuffixValueRestriction> restrictions = new LinkedHashMap<>();
//		for (SuffixValue sv : suffix.getValues()) {
//			AbstractSuffixValueRestriction restr = suffix.getRestriction(sv);
//			if (!(restr instanceof SuffixValueRestriction)) {
//				throw new IllegalArgumentException("Incompatible restriction: " + restr);
//			}
//			SuffixValueRestriction svr = (SuffixValueRestriction) restr;
//			restrictions.put(sv, svr.concretize(valuations));
//		}
//		return new SymbolicSuffix(suffix.getActions(), restrictions);
//	}
//
//	public static SymbolicSuffix concretize(SymbolicSuffix suffix, RegisterValuation regs, Constants consts) {
//		return concretize(suffix, regs, consts);
//	}

	public static AbstractSuffixValueRestriction equalityRestriction(SuffixValue s, Expression<?> ... elements) {
		if (elements.length == 0) {
			return new FalseRestriction(s);
		}

		boolean isSDTGuardElement = true;
		Expression[] eqs = new Expression[elements.length];
		Set<SDTGuardElement> regs = new LinkedHashSet<>();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof SDTGuardElement e) {
				regs.add(e);
			} else {
				isSDTGuardElement = false;
			}
			eqs[i] = new NumericBooleanExpression(s, NumericComparator.EQ, elements[i]);
		}
		if (isSDTGuardElement) {
			return new EqualityRestriction(s, regs);
		}
		return new SuffixValueRestriction(s, ExpressionUtil.or(eqs));
	}

	public static AbstractSuffixValueRestriction equalityRestriction(SuffixValue s, Collection<? extends Expression> elements) {
//		return equalityRestriction(s, elements.toArray(new SymbolicDataValue[elements.size()]));
		Expression[] elems = new Expression[elements.size()];
		int i = 0;
		for (Expression e : elements) {
			elems[i++] = e;
		}
		return equalityRestriction(s, elems);
	}

	public static SuffixValueRestriction disequalityRestriction(SuffixValue s, Expression<?> ... elements) {
		if (elements.length == 0) {
			return new TrueRestriction(s);
		}
		Expression[] eqs = new Expression[elements.length];
		for (int i = 0; i < elements.length; i++) {
			eqs[i] = new NumericBooleanExpression(s, NumericComparator.NE, elements[i]);
		}
		return new SuffixValueRestriction(s, ExpressionUtil.and(eqs));
	}

	public static SuffixValueRestriction disequalityRestriction(SuffixValue s, Collection<? extends Expression> elements) {
		Expression[] elems = new Expression[elements.size()];
		int i = 0;
		for (Expression e : elements) {
			elems[i++] = e;
		}
		return disequalityRestriction(s, elems);
	}

	public static SuffixValueRestriction fresh(SuffixValue s, Collection<? extends Expression> elements) {
		return disequalityRestriction(s, elements);
	}
}
