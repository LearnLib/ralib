/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.theory.inequality;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.automata.guards.SumCAtomicGuardExpression;
import de.learnlib.ralib.data.Replacement;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.SDTGuard;

/**
 *
 * @author falk
 */
public class IntervalGuard extends SDTGuard {

    private final SymbolicDataExpression leftEnd;
    private final SymbolicDataExpression rightEnd;
    private final Boolean leftOpen;
    private final Boolean rightOpen;
    
    public IntervalGuard(SuffixValue param, SymbolicDataExpression ll, SymbolicDataExpression rl) {
        super(param);
        this.leftEnd = ll;
        this.rightEnd = rl;
        this.leftOpen = true;
        this.rightOpen = true;
    }
    
    public IntervalGuard(SuffixValue param, SymbolicDataExpression ll, Boolean lo, SymbolicDataExpression rl, Boolean ro) {
        super(param);
        this.leftEnd = ll;
        this.leftOpen = lo;
        this.rightEnd = rl;
        this.rightOpen = ro;
    }

    public boolean isSmallerGuard() {
        return leftEnd == null;
    }

    public boolean isBiggerGuard() {
        return rightEnd == null;
    }

    public boolean isIntervalGuard() {
        return (leftEnd != null && rightEnd != null);
    }
    
    public Boolean getRightOpen() {
    	return rightOpen;
    }
    
    public Boolean getLeftOpen() {
    	return leftOpen;
    }

    @Override
    public String toString() {
        if (leftEnd == null) {
            return "(" + super.getParameter().toString() + "<" + equ(rightOpen) + rightEnd.toString() + ")";
        }
        if (rightEnd == null) {
            return "(" + super.getParameter().toString() + ">" + equ(leftOpen) + leftEnd.toString() + ")";
        }
        return "(" + leftEnd.toString() + "<" + equ(leftOpen) + getParameter().toString() + "<" + 
        equ(rightOpen) + rightEnd.toString() + ")";
    }
    
    private String equ(@Nonnull boolean open) {
    	return open?"":"=";
    }

    public Set<SymbolicDataValue> getAllSDVsFormingGuard() {
        Set<SymbolicDataValue> regs = new LinkedHashSet<>();
        if (leftEnd != null) {
            regs.add(leftEnd.getSDV());
        }
        if (rightEnd != null) {
            regs.add(rightEnd.getSDV());
        }
        return regs;
    }

    public SymbolicDataExpression getLeftExpr() {
        return leftEnd;
    }
    
    public SymbolicDataValue getLeftSDV() {
        return leftEnd.getSDV();
    }
    
    public SymbolicDataValue getRightSDV() {
    	return rightEnd.getSDV();
    }

    public SymbolicDataExpression getRightExpr() {
        return rightEnd;
    }

    @Override
    public GuardExpression toExpr() {
    	GuardExpression smaller = null;
    	GuardExpression bigger = null;
        if (rightEnd != null) {
        	Relation lsrRel = rightOpen? Relation.GREATER : Relation.GREQUALS;
        	if (rightEnd instanceof SymbolicDataValue)
        		smaller = new AtomicGuardExpression(rightEnd.getSDV(), lsrRel, parameter);
        	else
        		if (rightEnd instanceof SumCDataExpression)
        			smaller =  new SumCAtomicGuardExpression(
                                        rightEnd.getSDV(), ((SumCDataExpression) rightEnd).getConstant(), lsrRel, parameter, null);
        }
        if (leftEnd!= null) {
        	Relation grRel = leftOpen? Relation.LESSER : Relation.LSREQUALS;
        	if (leftEnd instanceof SymbolicDataValue)
        		bigger = new AtomicGuardExpression(leftEnd.getSDV(), grRel, parameter);
        	else
        		if (leftEnd instanceof SumCDataExpression)
        			bigger = new SumCAtomicGuardExpression(
                                        leftEnd.getSDV(), ((SumCDataExpression) leftEnd).getConstant(),  grRel, parameter, null);
        } 
        
        GuardExpression ret = smaller != null && bigger != null ? new Conjunction(smaller, bigger) : 
        	smaller != null? smaller : bigger != null ? bigger : null;
        if (ret == null) {
        	throw new RuntimeException ("Could not transform for " + this);
        }
        return ret;
    }
    

    @Override
    public SDTGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv
                = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
        SymbolicDataExpression r = null;
        SymbolicDataExpression l = null;
        sv = (sv == null) ? parameter : sv;

        if (!isBiggerGuard()) {
            if (rightEnd.isConstant() || !relabelling.containsKey(rightEnd.getSDV())) {
                r = rightEnd;
            } else {
                r = rightEnd.swapSDV((SymbolicDataValue) relabelling.get(rightEnd.getSDV()));
            }
        }
        if (!isSmallerGuard()) {
            if (leftEnd.isConstant() || !relabelling.containsKey(leftEnd.getSDV())) {
                l = leftEnd;
            } else {
                l = leftEnd.swapSDV((SymbolicDataValue) relabelling.get(leftEnd.getSDV()));
            }
        }
        return new IntervalGuard(sv, l, leftOpen, r, rightOpen);
    }
    
    @Override
	public SDTGuard replace(Replacement replacing) {
		SymbolicDataExpression rl = replacing.containsKey(rightEnd) ? 
				replacing.get(rightEnd) : rightEnd;
		SymbolicDataExpression ll = replacing.containsKey(this.leftEnd) ? 
				replacing.get(leftEnd) : leftEnd;
		
		
		return new IntervalGuard(getParameter(), ll, leftOpen, rl, rightOpen);
	}

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(parameter);
        hash = 59 * hash + Objects.hashCode(leftEnd);
        hash = 59 * hash + Objects.hashCode(leftOpen);
        hash = 59 * hash + Objects.hashCode(rightEnd);
        hash = 59 * hash + Objects.hashCode(rightOpen);
        hash = 59 * hash + Objects.hashCode(getClass());

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
        final IntervalGuard other = (IntervalGuard) obj;
        if (!Objects.equals(this.rightEnd, other.rightEnd)) {
            return false;
        }
        if (!Objects.equals(this.leftEnd, other.leftEnd)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }
}
