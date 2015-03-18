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
import de.learnlib.ralib.data.VarMapping;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Stealth
 */
public class SDTTrueGuard extends SDTGuard {

    public SDTTrueGuard(SymbolicDataValue.SuffixValue param) {
        super(param);
    }

    @Override
    public String toString() {
        return "TRUE: " + parameter.toString();
    }

    @Override
    public Expression<Boolean> toExpr(Constants consts) {
        return ExpressionUtil.TRUE;
    }

    @Override
    public IfGuard toTG(Map<SymbolicDataValue, Variable> variables, Constants consts) {
        return new IfGuard(DataExpression.TRUE);
    }

    @Override
    public SDTGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
        if (sv != null) {
            return new SDTTrueGuard(sv);
        }
        return this;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SDTTrueGuard other = (SDTTrueGuard) obj;
        return Objects.equals(this.parameter, other.parameter);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.parameter);
        hash = 59 * hash + Objects.hashCode(this.getClass());
        
        return hash;
    }
}
