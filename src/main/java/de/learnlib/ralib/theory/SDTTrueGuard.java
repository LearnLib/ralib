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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    public List<SDTGuard> unwrap() {
        List<SDTGuard> s = new ArrayList();
        s.add(this);
        return s;
    }

    @Override
    public GuardExpression toExpr() {
        return TrueGuardExpression.TRUE;
    }

    @Override
    public SDTGuard relabel(VarMapping relabelling) {
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
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.getClass());

        return hash;
    }

    @Override
    public boolean isSingle() {
        return true;
    }
    
    @Override
    public SDTGuard getSingle() {
        return this;
    }
    
//    @Override
//    public SDTGuard mergeWith(Set<SDTGuard> _merged) {
//        return new SDTOrGuard(this.parameter, _merged.toArray(new SDTGuard[]{}));
//        
//    }
}
