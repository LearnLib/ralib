package de.learnlib.ralib.smt;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class ReplacingVarsVisitor extends
        DuplicatingVisitor<VarMapping<? extends SymbolicDataValue, ? extends SymbolicDataValue>> {

    @Override
    public <E> Expression<?> visit(Variable<E> v, VarMapping<? extends SymbolicDataValue, ? extends SymbolicDataValue> data) {
        SymbolicDataValue newVar = data.get(v);
        return (newVar != null) ? newVar : v;
    }

    public <T> Expression<T> apply(Expression<T> expr, VarMapping<? extends SymbolicDataValue, ? extends SymbolicDataValue> rename) {
        return visit(expr, rename).requireAs(expr.getType());
    }
}
