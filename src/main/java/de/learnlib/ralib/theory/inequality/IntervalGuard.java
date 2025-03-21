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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.SDTGuard;

/**
 *
 * @author falk
 */
public class IntervalGuard extends SDTGuard {

    private final SymbolicDataValue leftLimit;
    private final SymbolicDataValue rightLimit;
    private final boolean leftClosed;
    private final boolean rightClosed;

    public IntervalGuard(SuffixValue param, SymbolicDataValue leftLimit, SymbolicDataValue rightLimit, boolean leftClosed, boolean rightClosed) {
    	super(param);
    	this.leftLimit = leftLimit;
    	this.rightLimit = rightLimit;
    	this.leftClosed = leftClosed;
    	this.rightClosed = rightClosed;
    }

    public IntervalGuard(SuffixValue param, SymbolicDataValue leftLimit, SymbolicDataValue rightLimit) {
        this(param, leftLimit, rightLimit, false, false);
    }

    public IntervalGuard(IntervalGuard other) {
    	super(other);
    	leftLimit = other.leftLimit;//.copy();
    	rightLimit = other.rightLimit;//.copy();
    	leftClosed = other.leftClosed;
    	rightClosed = other.rightClosed;
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

    public boolean isLeftClosed() {
    	return leftClosed;
    }

    public boolean isRightClosed() {
    	return rightClosed;
    }

    @Override
    public Set<SymbolicDataValue> getComparands(SymbolicDataValue dv) {
    	Set<SymbolicDataValue> comparands = new LinkedHashSet<>();
    	if (dv.equals(parameter)) {
    		if (leftLimit != null) {
    			comparands.add(leftLimit);
    		}
    		if (rightLimit != null) {
    			comparands.add(rightLimit);
    		}
    	} else if (dv.equals(leftLimit) || dv.equals(rightLimit)) {
    		comparands.add(parameter);
    	}
    	return comparands;
    }

    @Override
    public String toString() {
        if (leftLimit == null) {
            return "(" + this.getParameter().toString() + (rightClosed ? "<=" : "<") + this.rightLimit.toString() + ")";
        }
        if (rightLimit == null) {
            return "(" + this.getParameter().toString() + (leftClosed ? ">=" : ">") + this.leftLimit.toString() + ")";
        }
        return "(" + leftLimit.toString() +
        		(leftClosed ? "<=" : "<") +
        		this.getParameter().toString() +
        		(rightClosed ? "<=" : "<") +
        		this.rightLimit.toString() +
        		")";
    }

    public Set<SymbolicDataValue> getAllRegs() {
        Set<SymbolicDataValue> regs = new LinkedHashSet<>();
        if (leftLimit != null) {
            regs.add(leftLimit);
        }
        if (rightLimit != null) {
            regs.add(rightLimit);
        }
        return regs;
    }

    public SymbolicDataValue getLeftReg() {
        assert !isSmallerGuard();
        return leftLimit;
    }

    public SymbolicDataValue getRightReg() {
        assert !isBiggerGuard();
        return rightLimit;
    }

    @Override
    public GuardExpression toExpr() {
        if (leftLimit == null) {
        	return new AtomicGuardExpression<SuffixValue, SymbolicDataValue>(parameter,
        			(rightClosed ? Relation.SMALLER_OR_EQUAL : Relation.SMALLER),
        			rightLimit);
        }
        if (rightLimit == null) {
        	return new AtomicGuardExpression<SuffixValue, SymbolicDataValue>(parameter,
        			(leftClosed ? Relation.BIGGER_OR_EQUAL : Relation.BIGGER),
        			leftLimit);
        } else {
            GuardExpression smaller = new AtomicGuardExpression<SuffixValue, SymbolicDataValue>(parameter,
            		(rightClosed ? Relation.SMALLER_OR_EQUAL : Relation.SMALLER),
            		rightLimit);
            GuardExpression bigger = new AtomicGuardExpression<SuffixValue, SymbolicDataValue>(parameter,
            		(leftClosed ? Relation.BIGGER_OR_EQUAL : Relation.BIGGER),
            		leftLimit);
            return new Conjunction(smaller, bigger);
        }
    }

    @Override
    public SDTGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv
                = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
        SymbolicDataValue r = null;
        SymbolicDataValue l = null;
        sv = (sv == null) ? parameter : sv;

        if (!isBiggerGuard()) {
            if (rightLimit.isConstant()) {
                r = rightLimit;
            } else {
                r = (SymbolicDataValue) relabelling.get(rightLimit);
            }
            r = (r == null) ? rightLimit : r;
        }
        if (!isSmallerGuard()) {
            if (leftLimit.isConstant()) {
                l = leftLimit;
            } else {
                l = (SymbolicDataValue) relabelling.get(leftLimit);
            }
            l = (l == null) ? leftLimit : l;
        }
        return new IntervalGuard(sv, l, r, leftClosed, rightClosed);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(parameter);
        hash = 59 * hash + Objects.hashCode(leftLimit);
        hash = 59 * hash + Objects.hashCode(rightLimit);
        hash = 59 * hash + Objects.hashCode(leftClosed);
        hash = 59 * hash + Objects.hashCode(rightClosed);
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
        if (!Objects.equals(this.leftClosed, other.leftClosed)) {
        	return false;
        }
        if (!Objects.equals(this.rightClosed, other.rightClosed)) {
        	return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }

    @Override
    public List<SDTGuard> unwrap() {
        return Collections.singletonList((SDTGuard) this);
    }

    @Override
    public IntervalGuard copy() {
    	return new IntervalGuard(this);
    }

    public static IntervalGuard lessGuard(SuffixValue param, SymbolicDataValue r) {
    	return new IntervalGuard(param, null, r, false, false);
    }

    public static IntervalGuard lessOrEqualGuard(SuffixValue param, SymbolicDataValue r) {
    	return new IntervalGuard(param, null, r, false, true);
    }

    public static IntervalGuard greaterGuard(SuffixValue param, SymbolicDataValue r) {
    	return new IntervalGuard(param, r, null, false, false);
    }

    public static IntervalGuard greaterOrEqualGuard(SuffixValue param, SymbolicDataValue r) {
    	return new IntervalGuard(param, r, null, true, false);
    }
}
