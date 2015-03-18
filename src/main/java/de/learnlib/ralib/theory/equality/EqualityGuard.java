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

import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.Relation;
import de.learnlib.ralib.theory.SDTIfGuard;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import java.util.Map;
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
    public IfGuard toTG(Map<SymbolicDataValue, Variable> variables, Constants consts) {
        Expression<Boolean> expr = this.toExpr(consts);
        DataExpression<Boolean> cond = new DataExpression<>(expr, variables);
        return new IfGuard(cond);
                
                }
 
    @Override
    public SDTIfGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
        SymbolicDataValue r = null;
        sv = (sv == null) ? parameter : sv;
        
        if (register.isConstant()) {
            return new EqualityGuard(sv,register);
        }
        else {
            if (register.isSuffixValue()) {
            r = (SymbolicDataValue) relabelling.get(register);
            }
            else if (register.isRegister()) {
            r = (Register) relabelling.get(register);
            }
        r = (r == null) ? parameter : r;
        return new EqualityGuard(sv, r);
        }
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
    public Expression<Boolean> toExpr(Constants consts) {
        SymbolicDataValue r = this.getRegister();
         String pname = "y" + this.getParameter().getId();
        Variable p = new Variable(BuiltinTypes.SINT32, pname);
        
        if (r instanceof SymbolicDataValue.Constant) {
            DataValue<Integer> dv = (DataValue<Integer>) consts.get((Constant)r);
            Integer dv_i = dv.getId();
            gov.nasa.jpf.constraints.expressions.Constant c = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.SINT32,dv_i);
            return new NumericBooleanExpression(c, NumericComparator.EQ, p);
        }
        else {
            String xname = "";
            if (r instanceof SymbolicDataValue.Register) {
            xname = "x" + r.getId();
            }
            else if (r instanceof SymbolicDataValue.SuffixValue) {
            xname = "y" + r.getId();
            }
        Variable x = new Variable(BuiltinTypes.SINT32,xname);
        return new NumericBooleanExpression(x, NumericComparator.EQ, p);
        }
    } 

    
}
