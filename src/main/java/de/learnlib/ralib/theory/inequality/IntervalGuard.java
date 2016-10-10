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
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.testng.Assert;

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
    
    public EqualityGuard toEqGuard() {
        assert !isIntervalGuard();
        SymbolicDataExpression r = null;
        if (isSmallerGuard()) {
            r = rightLimit;
        }
        else {
            r = leftLimit;
        }
        return new EqualityGuard(this.parameter,r);
    }
    
    public DisequalityGuard toDeqGuard() {
        assert !isIntervalGuard();
        SymbolicDataExpression r = null;
        if (isSmallerGuard()) {
            r = rightLimit;
        }
        else {
            r = leftLimit;
        }
        return new DisequalityGuard(this.parameter,r);
    }
    
    public SDTGuard negate() {
    	if (this.isBiggerGuard() || this.isSmallerGuard()) {
    		EqualityGuard equ = toEqGuard();
    		IntervalGuard intv;
    		if (this.isBiggerGuard()) {
    			intv = new IntervalGuard(this.parameter, null, this.leftLimit);
    		} else {
    			intv = new IntervalGuard(this.parameter, this.rightLimit, null);
    		}
    		return new SDTOrGuard(this.parameter, equ, intv);
    	} else {
    		return new SDTOrGuard(this.parameter,
    				new EqualityGuard(this.parameter, this.leftLimit),
    				new IntervalGuard(this.parameter, null, this.leftLimit),
    				new EqualityGuard(this.parameter, this.rightLimit),
    				new IntervalGuard(this.parameter, this.rightLimit, null));
    	}
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

    public Set<SymbolicDataValue> getAllRegs() {
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
    
    // merge bigger with something
    protected Set<SDTGuard> bMergeIntervals(IntervalGuard other) {
        Set<SDTGuard> guards = new LinkedHashSet<>();
        SymbolicDataExpression l = this.getLeftExpr();
        if (other.isBiggerGuard()) {
            //          System.out.println("other " + other + " is bigger");
            guards.add(this);
            guards.add(other);
        } else if (other.isSmallerGuard()) {
//            System.out.println("other " + other + " is smaller");
//            System.out.println("see if " + l + " equals " + other.getRightReg() + "?");
            if (l.equals(other.getRightExpr())) {
//                System.out.println("yes, adding disequalityguard");
                guards.add(new DisequalityGuard(this.parameter, l));
            } else {
//                System.out.println("no, merging into interval guard");
//                guards.add(new IntervalGuard(this.parameter, l, other.getRightReg()));
                guards.add(this);
                guards.add(other);
            }
        } else {
//            System.out.println("other " + other + " is interv");

            if (l.equals(other.getRightExpr())) {
                guards.add(new IntervalGuard(this.parameter, other.getLeftExpr(), null));
                guards.add(new DisequalityGuard(this.parameter, l));
            } else if (l.equals(other.getLeftExpr())) {
                guards.add(this);
            } else {
                guards.add(this);
                guards.add(other);
            }

        }
        return guards;
    }

    // merge smaller with something
    protected Set<SDTGuard> sMergeIntervals(IntervalGuard other) {
        Set<SDTGuard> guards = new LinkedHashSet<>();
        SymbolicDataExpression r = this.getRightExpr();
        if (other.isBiggerGuard()) {
            return other.bMergeIntervals(this);
        } else if (other.isSmallerGuard()) {
            guards.add(this);
            guards.add(other);
        } else {
            if (r.equals(other.getLeftExpr())) {
                guards.add(new IntervalGuard(this.parameter, null, other.getRightExpr()));
                guards.add(new DisequalityGuard(this.parameter, r));
            } else if (r.equals(other.getRightExpr())) {
                guards.add(this);
            } else {
                guards.add(this);
                guards.add(other);
            }
        }
        return guards;
    }

    // merge interval with something
    private Set<SDTGuard> iMergeIntervals(IntervalGuard other) {
        Set<SDTGuard> guards = new LinkedHashSet<>();
        SymbolicDataExpression l = this.getLeftExpr();
        SymbolicDataExpression r = this.getRightExpr();
        if (other.isBiggerGuard()) {
            return other.bMergeIntervals(this);
        } else if (other.isSmallerGuard()) {
            return other.sMergeIntervals(this);
        } else {
        	SymbolicDataExpression  oL = other.getLeftExpr();
        	SymbolicDataExpression  oR = other.getRightExpr();
            if (l.equals(oR)) {
                if (r.equals(oL)) {
                    guards.add(new DisequalityGuard(this.parameter, l));
                    guards.add(new DisequalityGuard(this.parameter, r));
                } else {
                    guards.add(new IntervalGuard(this.parameter, oL, r));
                    guards.add(new DisequalityGuard(this.parameter, l));
                }
            } else {
                if (r.equals(oL)) {
                    guards.add(new IntervalGuard(this.parameter, l, oR));
                    guards.add(new DisequalityGuard(this.parameter, r));
                } else {
                    guards.add(this);
                    guards.add(other);
                }
            }
        }
        return guards;
    }

    private Set<SDTGuard> mergeIntervals(IntervalGuard other) {
//        System.out.println("other i-guard: " + other);
        if (this.isBiggerGuard()) {
//            System.out.println(this + " is bigger, left limit is: " + this.leftLimit);
            Set<SDTGuard> gs = this.bMergeIntervals(other);
//            System.out.println("returningB: " + gs);
            return gs;
        }
        if (this.isSmallerGuard()) {
//            System.out.println(this + " is smaller, right limit is: " + this.rightLimit);
            Set<SDTGuard> gs = this.sMergeIntervals(other);
//            System.out.println("returningS: " + gs);
            return gs;
        }

//        System.out.println("is interv");
        return this.iMergeIntervals(other);

    }

    private Set<SDTGuard> mergeWithEquality(EqualityGuard other) {
        Set<SDTGuard> guards = new LinkedHashSet<>();
        if (!(other.getRegister().equals(this.leftLimit) || other.getRegister().equals(this.rightLimit))) {
            guards.add(this);
            guards.add(other);
        } else {
            guards.add(new SDTOrGuard(this.parameter, this, other));
        }
        return guards;
    }

    @Override
    public Set<SDTGuard> mergeWith(SDTGuard other, List<SymbolicDataValue> regPotential) {
        Set<SDTGuard> guards = new LinkedHashSet<>();
        if (other instanceof IntervalGuard) {
            guards.addAll(this.mergeIntervals((IntervalGuard) other));
        } else if (other instanceof DisequalityGuard) {
            DisequalityGuard dGuard = (DisequalityGuard) other;
            if ((this.isBiggerGuard() && this.leftLimit.equals(dGuard.getExpression()))
                    || (this.isSmallerGuard() && this.rightLimit.equals(dGuard.getExpression()))) {

                guards.add((DisequalityGuard) other);
            }
            else {
                guards.add(this);
                guards.add(other);
            }
            // special case for equality guards
        } else //if (other instanceof EqualityGuard) 
        {
            //return this.mergeWithEquality((EqualityGuard) other);
            //} 
            //else {
//            System.out.println("guard " + other + " not deq or interval");
            guards.add(this);
            guards.add(other);
        }
//        System.out.println("merged guards are: " + guards);
        return guards;
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
    public List<SDTGuard> unwrap() {
        return Collections.singletonList((SDTGuard) this);
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
