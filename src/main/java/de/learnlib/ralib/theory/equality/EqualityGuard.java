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
package de.learnlib.ralib.theory.equality;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author falk
 */
public class EqualityGuard extends SDTIfGuard {

    public EqualityGuard(SuffixValue param, SymbolicDataValue reg) {
        super(param, reg, Relation.EQUALS);
    }

    @Override
    public String toString() {
        return "(" + this.getParameter().toString()
                + "=" + this.getRegister().toString() + ")";

    }

    public DisequalityGuard toDeqGuard() {
        return new DisequalityGuard(parameter, register);
    }

    @Override
    public SDTIfGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv
                = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
        SymbolicDataValue r = null;
        sv = (sv == null) ? parameter : sv;

        if (register.isConstant()) {
            return new EqualityGuard(sv, register);
        } else {
            r = (SymbolicDataValue) relabelling.get(register);
        }
        r = (r == null) ? register : r;
        return new EqualityGuard(sv, r);
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
        final EqualityGuard other = (EqualityGuard) obj;
        if (!Objects.equals(this.register, other.register)) {
            return false;
        }
        if (!Objects.equals(this.relation, other.relation)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }

    @Override
    public GuardExpression toExpr() {
        return new AtomicGuardExpression<>(this.register,
                Relation.EQUALS, parameter);
    }

    @Override
    public Set<SDTGuard> mergeWith(SDTGuard other, List<SymbolicDataValue> regPotential) {
        Set<SDTGuard> guards = new LinkedHashSet<>();
        if (other instanceof DisequalityGuard) {
            if (!(other.equals(this.toDeqGuard()))) {
                guards.add(this);
                guards.add(other);
            }
        } else if (other instanceof EqualityGuard) {
            if (!(this.equals(other))) {
                guards.add(other);
            }
            guards.add(this);
        } else if (other instanceof SDTOrGuard) {
            for (SDTGuard s : ((SDTOrGuard)other).getGuards()) {
                guards.addAll(this.mergeWith(s, regPotential));
            }
        }else {
            System.out.println("attempt to merge " + this + " with " + other);
            guards.addAll(other.mergeWith(this, regPotential));

        }
        return guards;
    }

}
