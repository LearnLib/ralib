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
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.theory.SDTIfGuard;
import java.util.Objects;

/**
 *
 * @author falk
 */
public class SmallerGuard extends SDTIfGuard {

    public SmallerGuard(SymbolicDataValue.SuffixValue param, SymbolicDataValue reg) {
        super(param, reg, Relation.SMALLER);
    }

    public BiggerGuard toDeqGuard() {
        return new BiggerGuard(parameter, register);
    }

    public boolean contradicts(SDTIfGuard other) {
        boolean samePR = (this.getParameter().getId() == other.getParameter().getId()
                && this.getRegister().getId() == other.getRegister().getId());
        return samePR && (other instanceof BiggerGuard);
    }

    @Override
    public String toString() {
        return "(" + this.getParameter().toString() + "<" + this.getRegister().toString() + ")";
        //return super.toString();
    }

    @Override
    public GuardExpression toExpr() {
        return new AtomicGuardExpression(register, Relation.BIGGER, parameter);
    }

    @Override
    public SDTIfGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
        SymbolicDataValue r = null;
        sv = (sv == null) ? parameter : sv;

        if (register.isConstant()) {
            return new SmallerGuard(sv, register);
        } else {
//            if (register.isSuffixValue()) {
//            r = (SymbolicDataValue) relabelling.get(register);
//            }
//            else if (register.isRegister()) {
//            r = (SymbolicDataValue.Register) relabelling.get(register);
//            }
            r = (SymbolicDataValue) relabelling.get(register);
        }
        r = (r == null) ? register : r;
        return new SmallerGuard(sv, r);
        //    }
    }

    @Override
    public SDTIfGuard relabelLoosely(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
        return this.relabelLoosely(relabelling);
//        SymbolicDataValue r = null;
//        sv = (sv == null) ? parameter : sv;
//        
//        if (register.isConstant()) {
//            return new SmallerGuard(sv,register);
//        }
//        else {
//            r = (SymbolicDataValue)relabelling.get(register);
//            }
//            
//        r = (r == null) ? parameter : r;
//        return new SmallerGuard(sv, r);
//       
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(parameter);
        hash = 59 * hash + Objects.hashCode(register);
        hash = 59 * hash + Objects.hashCode(relation);
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
        final SmallerGuard other = (SmallerGuard) obj;
        if (!Objects.equals(this.register, other.register)) {
            return false;
        }
        if (!Objects.equals(this.relation, other.relation)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }

}
