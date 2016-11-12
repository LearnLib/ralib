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
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;

/**
 *
 * @author falk
 */
public class IntervalGuard extends SDTGuard {

    protected final SymbolicDataExpression leftLimit;
    protected final SymbolicDataExpression rightLimit;

    public IntervalGuard(SuffixValue param, SymbolicDataExpression ll, SymbolicDataExpression rl) {
        super(param);
        leftLimit = ll;
        rightLimit = rl;
    }

    public IntervalGuard flip() {
        return new IntervalGuard(parameter, rightLimit, leftLimit);
    }

    public boolean isSmallerGuard() {
        return leftLimit == null;
    }

    public boolean isBiggerGuard() {
        return rightLimit == null;
    }

    public boolean isIntervalGuard() {
        return (leftLimit != null && rightLimit != null);
    }

    @Override
    public String toString() {
        if (leftLimit == null) {
            return "(" + this.getParameter().toString() + "<" + this.rightLimit.toString() + ")";
        }
        if (rightLimit == null) {
            return "(" + this.getParameter().toString() + ">" + this.leftLimit.toString() + ")";
        }
        return "(" + leftLimit.toString() + "<" + this.getParameter().toString() + "<" + this.rightLimit.toString() + ")";
    }

    public Set<SymbolicDataValue> getAllSDVsFormingGuard() {
        Set<SymbolicDataValue> regs = new LinkedHashSet<>();
        if (leftLimit != null) {
            regs.add(leftLimit.getSDV());
        }
        if (rightLimit != null) {
            regs.add(rightLimit.getSDV());
        }
        return regs;
    }

    public SymbolicDataExpression getLeftExpr() {
        return leftLimit;
    }
    
    public SymbolicDataValue getLeftSDV() {
        return leftLimit.getSDV();
    }
    
    public SymbolicDataValue getRightSDV() {
    	return rightLimit.getSDV();
    }

    public SymbolicDataExpression getRightExpr() {
        return rightLimit;
    }

    @Override
    public GuardExpression toExpr() {
    	GuardExpression smaller = null;
    	GuardExpression bigger = null;
        if (rightLimit != null) {
        	if (rightLimit instanceof SymbolicDataValue)
        		smaller = new AtomicGuardExpression(parameter, Relation.SMALLER, rightLimit.getSDV());
        	else
        		if (rightLimit instanceof SumCDataExpression)
        			smaller =  new SumCAtomicGuardExpression(parameter, null,  Relation.SMALLER, rightLimit.getSDV(), ((SumCDataExpression) rightLimit).getConstant());
        }
        if (leftLimit!= null) {
        	if (leftLimit instanceof SymbolicDataValue)
        		bigger = new AtomicGuardExpression(parameter, Relation.BIGGER, leftLimit.getSDV());
        	else
        		if (leftLimit instanceof SumCDataExpression)
        			bigger = new SumCAtomicGuardExpression(parameter, null,  Relation.BIGGER, leftLimit.getSDV(), ((SumCDataExpression) leftLimit).getConstant());
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
            if (rightLimit.isConstant() || !relabelling.containsKey(rightLimit.getSDV())) {
                r = rightLimit;
            } else {
                r = rightLimit.swapSDV((SymbolicDataValue) relabelling.get(rightLimit.getSDV()));
            }
        }
        if (!isSmallerGuard()) {
            if (leftLimit.isConstant() || !relabelling.containsKey(leftLimit.getSDV())) {
                l = leftLimit;
            } else {
                l = leftLimit.swapSDV((SymbolicDataValue) relabelling.get(leftLimit.getSDV()));
            }
        }
        return new IntervalGuard(sv, l, r);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(parameter);
        hash = 59 * hash + Objects.hashCode(leftLimit);
        hash = 59 * hash + Objects.hashCode(rightLimit);
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
        if (!Objects.equals(this.rightLimit, other.rightLimit)) {
            return false;
        }
        if (!Objects.equals(this.leftLimit, other.leftLimit)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }

	@Override
	public SDTGuard replace(Replacement replacing) {
		SymbolicDataExpression rl = replacing.containsKey(this.rightLimit) ? 
				replacing.get(this.rightLimit) : this.rightLimit;
		SymbolicDataExpression ll = replacing.containsKey(this.leftLimit) ? 
				replacing.get(this.leftLimit) : this.leftLimit;
		
		
		return new IntervalGuard(getParameter(), ll, rl);
	}

}
