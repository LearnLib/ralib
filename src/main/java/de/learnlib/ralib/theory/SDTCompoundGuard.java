/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.*;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SDTCompoundGuard extends SDTGuard {

    private final List<SDTIfGuard> guards;

    public SDTCompoundGuard(SuffixValue param, SDTIfGuard... ifGuards) {
        super(param);
        this.guards = new ArrayList<>();
        this.guards.addAll(Arrays.asList(ifGuards));
    }

    private List<Expression<Boolean>> toExprList() {
        List<Expression<Boolean>> exprs = new ArrayList<>();
        for (SDTIfGuard guard : this.guards) {
            exprs.add(guard.toExpr());
        }
        return exprs;
    }

    private Expression<Boolean> toExpr(List<Expression<Boolean>> eqList, int i) {
        if (eqList.size() == i + 1) {
            return eqList.get(i);
        } else {
            return new PropositionalCompound(eqList.get(i), LogicalOperator.AND, toExpr(eqList, i + 1));
        }
    }
    
    public Expression<Boolean> toExpr() {
        return toExpr(this.toExprList(),0);
    }

    @Override
    public IfGuard toTG(Map<SymbolicDataValue, Variable> variables) {
        Expression<Boolean> expr = toExpr(this.toExprList(), 0);
        DataExpression<Boolean> cond = new DataExpression<>(expr, variables);
        return new IfGuard(cond);
    }

}
