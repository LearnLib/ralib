/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.theory.equality;

import java.util.Objects;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.SDTIfGuard;

/**
 *
 * @author falk
 */
public class EqualityGuard extends SDTIfGuard {

    public EqualityGuard(SuffixValue param, SymbolicDataValue reg) {
        super(param, reg, Relation.EQUALS);
    }

    public EqualityGuard(EqualityGuard other) {
    	super(other);
    }

    @Override
    public String toString() {
        return "(" + this.getParameter().toString()
                + "=" + this.getRegister().toString() + ")";

    }

    @Override
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
    public EqualityGuard copy() {
    	return new EqualityGuard(this);
    }

}
