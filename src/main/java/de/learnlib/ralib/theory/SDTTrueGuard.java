/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.inequality.SmallerGuard;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Map;

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
        return "TRUE";
    }

    @Override
    public Expression<Boolean> toExpr() {
        return ExpressionUtil.TRUE;
    }

    @Override
    public IfGuard toTG(Map<SymbolicDataValue, Variable> variables) {
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
    public boolean equals(SDTGuard other) {
        if (!(other instanceof SDTTrueGuard)) {
            return false;
        }
        else {
            SDTTrueGuard _other = (SDTTrueGuard) other;
            return (_other.getParameter().equals(this.getParameter()));
    }
    }

}
