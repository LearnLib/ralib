package de.learnlib.ralib.smt;

import java.util.HashMap;
import java.util.Map;

import de.learnlib.ralib.data.*;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class ReplacingValuesVisitor extends
        DuplicatingVisitor<Map<? extends Constant<?>, ? extends Expression<?>>> {

    @Override
    public <E> Expression<E> visit(Constant<E> v, Map<? extends Constant<?>, ? extends Expression<?>> data) {
        Expression<?> newVar = data.get(v);
        return (newVar != null) ? newVar.requireAs(v.getType()) : v;
    }

    public <T> Expression<T> apply(Expression<T> expr, Mapping<? extends Constant<?>, ? extends SDTGuardElement> rename) {
        Map<Constant<?>, Expression<?>> map = new HashMap<>();
        rename.forEach((k, v) -> map.put( k, v.asExpression() ));
        return visit(expr, map).requireAs(expr.getType());
    }

    public <T> Expression<T> applyRegs(Expression<T> expr, Mapping<? extends Constant<?>, SymbolicDataValue.Register> rename) {
        Map<Constant<?>, Expression<?>> map = new HashMap<>(rename);
        return visit(expr, map).requireAs(expr.getType());
    }

    public <T> Expression<T> apply(Expression<T> expr, RegisterAssignment rename) {
        Map<Constant<?>, Expression<?>> map = new HashMap<>(rename);
        return visit(expr, map).requireAs(expr.getType());
    }
}
