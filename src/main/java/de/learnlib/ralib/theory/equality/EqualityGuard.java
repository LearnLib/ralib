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
import de.learnlib.ralib.theory.SDTIfGuard;
import java.util.Objects;

/**
 *
 * @author falk
 */
public class EqualityGuard extends SDTIfGuard {

    public EqualityGuard(SuffixValue param, SymbolicDataValue reg) {
        super(param, reg, Relation.EQUALS);
    }

//    @Override
//    public Equality createCopy(VarMapping renaming) {
//        return new Equality(this.getParameter(), renaming.get(this.getRegister()));
//    }
    @Override
    public String toString() {
//        String ret = "";
//        for (Register reg : this.getRegisters()) {
//            ret = ret + " " + this.getParameter() + "=" + reg;
//        }
//        return ret;
//        //}
        return "(" + this.getParameter().toString() + "=" + this.getRegister().toString() + ")";

    }

//    private List<Expression<Boolean>> toExprList() {
//        List<Expression<Boolean>> eqs = new ArrayList<>();
//        Variable p = new Variable(BuiltinTypes.SINT32, "p");
//        for (Register reg : this.getRegisters()) {
//            String xname = "x" + reg.getId();
//            Variable x = new Variable(BuiltinTypes.SINT32, xname);
//            Expression<Boolean> expression = new 
//        NumericBooleanExpression(x, NumericComparator.EQ, p);
//            eqs.add(expression);
//        }
//        return eqs;
//    }
//    
//    private Expression<Boolean> toExpr(List<Expression<Boolean>> eqList, int i) {
//        if (eqList.size() == i+1) {
//            return eqList.get(i);
//        }
//        else {
//            return new PropositionalCompound(eqList.get(i), LogicalOperator.AND, toExpr(eqList,i+1));
//        }
//    }
    public DisequalityGuard toDeqGuard() {
        return new DisequalityGuard(parameter, register);
    }

    @Override
    public SDTIfGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
        SymbolicDataValue r = null;
        sv = (sv == null) ? parameter : sv;

        if (register.isConstant()) {
            return new EqualityGuard(sv, register);
        } else {
//            if (register.isSuffixValue()) {
//            r = (SymbolicDataValue) relabelling.get(register);
//            }
//            else if (register.isRegister()) {
//            r = (Register) relabelling.get(register);
//            }
            r = (SymbolicDataValue) relabelling.get(register);
        }
        r = (r == null) ? register : r;
        return new EqualityGuard(sv, r);
//        }
    }

    @Override
    public SDTIfGuard relabelLoosely(VarMapping relabelling) {
        return this.relabel(relabelling);
//        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
//        SymbolicDataValue r = null;
//        sv = (sv == null) ? parameter : sv;
//        
//        if (register.isConstant()) {
//            return new EqualityGuard(sv,register);
//        }
//        else {
//            r = (SymbolicDataValue)relabelling.get(register);
//            }
//            
//        r = (r == null) ? parameter : r;
//        return new EqualityGuard(sv, r);
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
        return new AtomicGuardExpression<>(this.register, Relation.EQUALS, parameter);
    } 

}
