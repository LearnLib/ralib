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

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.SymbolicDataExpression;
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

import org.testng.Assert;

/**
 *
 * @author falk
 */
public class EqualityGuard extends SDTIfGuard {

    public EqualityGuard(SuffixValue param, SymbolicDataExpression reg) {
        super(param, reg, Relation.EQUALS);
    }

    @Override
    public String toString() {
        return "(" + this.getParameter().toString()
                + "=" + this.getRegister().toString() + ")";

    }

    public DisequalityGuard toDeqGuard() {
        return new DisequalityGuard(parameter, registerExpr);
    }

    @Override
    public SDTIfGuard relabel(VarMapping relabelling) {
    	  SymbolicDataValue.SuffixValue sv
          = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
		  sv = (sv == null) ? parameter : sv;
		  SymbolicDataExpression r = null;
		
		  if (registerExpr.isConstant()) {
		      return new DisequalityGuard(sv, registerExpr);
		  } else {
		  	if (relabelling.containsKey(registerExpr.getSDV())) {
		  		r = registerExpr.swapSDV((SymbolicDataValue) relabelling.get(registerExpr));
		  	} else {
		  		r = registerExpr;
		  	}
		  }
        return new EqualityGuard(sv, r);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(parameter);
        hash = 59 * hash + Objects.hashCode(registerExpr);
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
        if (!Objects.equals(this.registerExpr, other.registerExpr)) {
            return false;
        }
        if (!Objects.equals(this.relation, other.relation)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }

    @Override
    public GuardExpression toExpr() {
    	Assert.assertTrue(registerExpr instanceof SymbolicDataValue);
        return new AtomicGuardExpression<>(this.registerExpr.getSDV(),
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
            //System.out.println("attempt to merge " + this + " with " + other);
            guards.addAll(other.mergeWith(this, regPotential));

        }
        return guards;
    }

}
