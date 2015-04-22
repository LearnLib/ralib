/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
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
    public GuardExpression toExpr() {
        return TrueGuardExpression.TRUE;
    }

    @Override
    public SDTGuard relabel(VarMapping relabelling) {
//        System.out.println("relabel " + this + " with " + relabelling.get(getParameter()));        
//        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
//        if (sv != null) {
//            return new SDTTrueGuard(sv);
//        }
        return this;
    }
    
    @Override
    public SDTGuard relabelLoosely(VarMapping relabelling) {
        return this.relabel(relabelling);
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
        //return Objects.equals(this.parameter, other.parameter);
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        //hash = 59 * hash + Objects.hashCode(this.parameter);
        hash = 59 * hash + Objects.hashCode(this.getClass());
        
        return hash;
    }
}
