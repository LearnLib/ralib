package de.learnlib.ralib.smt;

import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class VarsValuationVisitor extends DuplicatingVisitor<Map<? extends Variable<?>, ? extends Expression<?>>> {

	@Override
	public <E> Expression<E> visit(Variable<E> v, Map<? extends Variable<?>, ? extends Expression<?>> data) {
		Expression<?> val = data.get(v);
		return (val != null) ? val.requireAs(v.getType()) : v;
	}

	public <T> Expression<T> apply(Expression<T> expr, Mapping<? extends SymbolicDataValue, ? extends DataValue> valuation) {
		Map<Variable<?>, Expression<?>> map = new LinkedHashMap<>();
		valuation.forEach((k,v) -> map.put(k, v.asExpression()));
		return visit(expr, map).requireAs(expr.getType());
	}
}
