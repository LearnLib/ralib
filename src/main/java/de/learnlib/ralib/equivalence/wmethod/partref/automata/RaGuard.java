package de.learnlib.ralib.equivalence.wmethod.partref.automata;

import java.util.LinkedHashSet;
import java.util.Set;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public abstract class RaGuard {

    public abstract Constraint toConstraint(DSymbolInstance in);

    public static Constraint toConstraint(Set<RaGuard> guards, DSymbolInstance in) {
        Set<Constraint> constrs = new LinkedHashSet<>();
        for (RaGuard g : guards) {
            constrs.add(g.toConstraint(in));
        }
        return Constraint.construct(constrs);
    }

    public static Set<RaGuard> fromRaGuard(Expression<Boolean> guard) {
        Set<RaGuard> guards = new LinkedHashSet<>();
        if (guard.equals(ExpressionUtil.TRUE)) {
            guards.add(new RaTrueGuard());
            return guards;
        }
        if (guard instanceof PropositionalCompound pc) {
            assert pc.getOperator().equals(LogicalOperator.AND) : "Invalid logical compound: conjunction expected";
            guards.addAll(fromRaGuard(pc.getLeft()));
            guards.addAll(fromRaGuard(pc.getRight()));
            return guards;
        }
        if (guard instanceof NumericBooleanExpression nbe) {
            guards.add(parseNumericBooleanExpression(nbe));
            return guards;
        }
        throw new IllegalArgumentException("Unexpected expression: " + guard);
    }

    private static RaGuard parseNumericBooleanExpression(NumericBooleanExpression expr) {
        Expression<?> left = expr.getLeft();
        Expression<?> right = expr.getRight();
        NumericComparator comp = expr.getComparator();
        Relation rel = comp.equals(NumericComparator.EQ) ? Relation.EQ : (
                comp.equals(NumericComparator.NE) ? Relation.NEQ : null);
        if (rel == null) {
            throw new IllegalArgumentException("Unexpected comparator: " + comp);
        }

        SymbolicDataValue leftVal = parseVariable(left);
        SymbolicDataValue rightVal = parseVariable(right);
        if (!leftVal.isParameter()) {
            assert rightVal.isParameter() : "Invalid guard";
            SymbolicDataValue temp = leftVal;
            leftVal = rightVal;
            rightVal = temp;
        }
        return new RaEqualityGuard((Parameter) leftVal, rel, rightVal);
    }

    private static SymbolicDataValue parseVariable(Expression<?> var) {
        if (var instanceof SymbolicDataValue val) {
            return val;
        }
        throw new IllegalArgumentException("Unexpected expression (SymbolicDataValue expected): " + var);
    }
}
