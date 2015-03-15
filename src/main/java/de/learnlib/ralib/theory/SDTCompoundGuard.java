/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.inequality.SmallerGuard;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SDTCompoundGuard extends SDTGuard {

    private final List<SDTIfGuard> guards;
    
    public List<SDTIfGuard> getGuards() {
        return guards;
    }
    
    public Set<SymbolicDataValue> getAllRegs() {
        Set<SymbolicDataValue> allRegs = new HashSet<SymbolicDataValue>();
        for (SDTIfGuard g : guards) {
            allRegs.add(g.getRegister());
        }
        return allRegs;
    }

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
        if (eqList.size() == 0) {
            return ExpressionUtil.TRUE;
        }
        else if (eqList.size() == i + 1) {
            return eqList.get(i);
        } else {
            return new PropositionalCompound(eqList.get(i), LogicalOperator.AND, toExpr(eqList, i + 1));
        }
    }
    
    @Override
    public Expression<Boolean> toExpr() {
        List<Expression<Boolean>> thisList = this.toExprList();
        if (thisList.isEmpty()) {
            return ExpressionUtil.TRUE;
        }
        else {
            return toExpr(thisList, 0);
        }
    }
    
    @Override
    public boolean equals(SDTGuard other) {
        if (!(other instanceof SDTCompoundGuard)) {
            return false;
        }
        else {
            SDTCompoundGuard _other = (SDTCompoundGuard) other;
            return (_other.getParameter().equals(this.getParameter()) &&
                    _other.getGuards().equals(this.getGuards()));
    }
    }

    @Override
    public IfGuard toTG(Map<SymbolicDataValue, Variable> variables) {
        Expression<Boolean> expr = toExpr(this.toExprList(), 0);
        DataExpression<Boolean> cond = new DataExpression<>(expr, variables);
        return new IfGuard(cond);
    }

    @Override
    public String toString() {
        return this.guards.toString();
    }
    
    @Override
    public SDTGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
        sv = (sv == null) ? getParameter() : sv;
        
        List<SDTIfGuard> gg = new ArrayList<>();
        for (SDTIfGuard g : this.guards) {
            gg.add(g.relabel(relabelling));
        }
        return new SDTCompoundGuard(sv, gg.toArray(new SDTIfGuard[]{}));
    }    
}
