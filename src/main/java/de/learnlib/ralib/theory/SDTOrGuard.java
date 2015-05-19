/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SDTOrGuard extends SDTMultiGuard {

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.condis);
        hash = 59 * hash + Objects.hashCode(this.parameter);
        hash = 59 * hash + Objects.hashCode(this.guardSet);
        hash = 59 * hash + Objects.hashCode(this.getClass());

        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SDTOrGuard other = (SDTOrGuard) obj;

        if (!Objects.equals(this.guardSet, other.guardSet)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }

    public SDTOrGuard(SuffixValue param, SDTGuard... ifGuards) {
        super(param, ConDis.OR, ifGuards);
    }

    private List<GuardExpression> toExprList() {
        List<GuardExpression> exprs = new ArrayList<>();
        for (SDTGuard guard : this.guards) {
            exprs.add(guard.toExpr());
        }
        return exprs;
    }

    @Override
    public GuardExpression toExpr() {
        List<GuardExpression> thisList = this.toExprList();
        if (thisList.isEmpty()) {
            return TrueGuardExpression.TRUE;
        }
        if (thisList.size() == 1) {
            return thisList.get(0);
        } else {
            return new Disjunction(thisList.toArray(new GuardExpression[]{}));
        }
    }

    @Override
    public SDTGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
        sv = (sv == null) ? getParameter() : sv;

        List<SDTGuard> gg = new ArrayList<>();
        for (SDTGuard g : this.guards) {
            gg.add(g.relabel(relabelling));
        }
        return new SDTOrGuard(sv, gg.toArray(new SDTGuard[]{}));
    }
    

    //@Override
    //public SDTGuard mergeWith(Set<SDTGuard> _merged) {
    //    return null;
    //}
//        Set<SDTGuard> merged = new LinkedHashSet<>();
//        merged.addAll(_merged);
//        for (SDTGuard x : this.getGuards()) {
//            if (x instanceof SDTIfGuard) {
//                SDTGuard newGuard = x.mergeWith(merged);
//            }
//        }
//        if (merged.isEmpty()) {
//            return new SDTTrueGuard(this.parameter);
//        } else {
//            SDTGuard[] mergedArr = merged.toArray(new SDTGuard[]{});
//            if (mergedArr.length == 1) {
//                return mergedArr[0];
//            }
//            else {
//                return new SDTOrGuard(this.parameter, mergedArr);
//            }
//
//        }
//    }
}
