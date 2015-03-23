/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class SDTMultiGuard extends SDTGuard {
    
    protected enum ConDis {
        AND, OR
    }

    protected final List<SDTIfGuard> guards;
    protected final Set<SDTIfGuard> guardSet;
    protected final ConDis condis;
    
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

    public SDTMultiGuard(SuffixValue param, ConDis condis, SDTIfGuard... ifGuards) {
        super(param);
        this.condis = condis;
        this.guards = new ArrayList<>();
        this.guards.addAll(Arrays.asList(ifGuards));
        this.guardSet = new LinkedHashSet<>(guards);
    }

    @Override
    public IfGuard toTG(Map<SymbolicDataValue, Variable> variables, Constants consts) {
        Expression<Boolean> expr = this.toExpr(consts);
        DataExpression<Boolean> cond = new DataExpression<>(expr, variables);
        return new IfGuard(cond);
    }

    @Override
    public String toString() {
        String p = this.condis.toString() + "COMPOUND: " +parameter.toString();
        if (this.guards.isEmpty()) {
            return p + "empty";
        }
        return p +  this.guards.toString();
    }
    
    
}
