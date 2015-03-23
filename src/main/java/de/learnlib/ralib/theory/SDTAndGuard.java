/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SDTAndGuard extends SDTMultiGuard {

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
        final SDTAndGuard other = (SDTAndGuard) obj;
        
        if (!Objects.equals(this.guardSet, other.guardSet)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    } 
    
    public SDTAndGuard(SuffixValue param, SDTIfGuard... ifGuards) {
        super(param, ConDis.AND, ifGuards);
    }

    private List<Expression<Boolean>> toExprList(Constants consts) {
        List<Expression<Boolean>> exprs = new ArrayList<>();
        for (SDTIfGuard guard : this.guards) {
            exprs.add(guard.toExpr(consts));
        }
        return exprs;
    }

    private Expression<Boolean> toExpr(List<Expression<Boolean>> eqList, int i) {
        //assert !eqList.isEmpty();
        if (eqList.size() == i + 1) {
            return eqList.get(i);
        } else {
//            System.out.println("here is the xpr: " + eqList.toString());
            return new PropositionalCompound(eqList.get(i), LogicalOperator.AND, toExpr(eqList, i + 1));
        }
    }
    
    @Override
    public Expression<Boolean> toExpr(Constants consts) {
        List<Expression<Boolean>> thisList = this.toExprList(consts);
        if (thisList.isEmpty()) {
            return ExpressionUtil.TRUE;
        }
        if (thisList.size() == 1) {
            return thisList.get(0);
        }
        else {
 //           System.out.println("here is the list: " + thisList.toString());
            return toExpr(thisList, 0);
        }
    }
    
    @Override
    public SDTGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
        sv = (sv == null) ? getParameter() : sv;
        
        List<SDTIfGuard> gg = new ArrayList<>();
        for (SDTIfGuard g : this.guards) {
            gg.add(g.relabel(relabelling));
        }
        //throw new IllegalStateException("not supposed to happen");
        return new SDTAndGuard(sv, gg.toArray(new SDTIfGuard[]{}));
    }    
    
    @Override
    public SDTGuard relabelLoosely(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
        sv = (sv == null) ? getParameter() : sv;
        
        List<SDTIfGuard> gg = new ArrayList<>();
        for (SDTIfGuard g : this.guards) {
            gg.add(g.relabelLoosely(relabelling));
        }
        //throw new IllegalStateException("not supposed to happen");
        return new SDTAndGuard(sv, gg.toArray(new SDTIfGuard[]{}));
    }    
}
