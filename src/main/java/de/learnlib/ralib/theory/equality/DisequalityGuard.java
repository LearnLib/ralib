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
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.Relation;
import de.learnlib.ralib.theory.SDTIfGuard;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import java.util.Map;
/**
 *
 * @author falk
 */
public class DisequalityGuard extends SDTIfGuard {
    
    public DisequalityGuard(SuffixValue param, SymbolicDataValue reg) {
        super(param, reg, Relation.NOT_EQUALS);     
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
        return "(" + this.getParameter().toString() + "!=" + this.getRegister().toString() + ")";
        
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
    
    @Override
    public Expression<Boolean> toExpr() {
        String xname = "x" + this.getRegister().getId();
        String pname = "y" + this.getParameter().getId();
        Variable p = new Variable(BuiltinTypes.SINT32, pname);
        Variable x = new Variable(BuiltinTypes.SINT32,xname);
        return new NumericBooleanExpression(x, NumericComparator.NE, p);
    }
    
    @Override
    public IfGuard toTG(Map<SymbolicDataValue, Variable> variables) {
        Expression<Boolean> expr = this.toExpr();
        DataExpression<Boolean> cond = new DataExpression<>(expr, variables);
        return new IfGuard(cond);
                
                }
 
    @Override
    public SDTIfGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
        SymbolicDataValue r = (Register) relabelling.get(getRegister());
        
        sv = (sv == null) ? getParameter() : sv;
        r = (r == null) ? getRegister() : r;
        
        return new DisequalityGuard(sv, r);
    }    

//    @Override
//    public SDTGuard relabel(VarMapping relabelling) {
//        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
//        
//        sv = (sv == null) ? getParameter() : sv;
//  
//        Set<Register> regs = new HashSet<>();
//        for (Register r : getRegisters()) {
//            Register rNew = (Register) relabelling.get(r);
//            rNew = (rNew == null) ? r : rNew;
//            regs.add(rNew);            
//        }
//        
//        return new DisequalityGuard(sv, regs);
//    }        
    
    
    
    
    
//        private List<Expression<Boolean>> toExprList() {
//        List<Expression<Boolean>> deqs = new ArrayList<>();
//        String pname = "y" + this.getParameter().getId();
//        Variable p = new Variable(BuiltinTypes.SINT32, pname);
//        for (Register reg : this.getRegisters()) {
//            String xname = "x" + reg.getId();
//            Variable x = new Variable(BuiltinTypes.SINT32, xname);
//            Expression<Boolean> expression = new 
//        NumericBooleanExpression(x, NumericComparator.NE, p);
//            deqs.add(expression);
//        }
//        return deqs;
//    }
//    
//    private Expression<Boolean> toExpr(List<Expression<Boolean>> deqList, int i) {
//        if (deqList.size() == i+1) {
//            return deqList.get(i);
//        }
//        else {
//            return new PropositionalCompound(deqList.get(i), LogicalOperator.AND, toExpr(deqList,i+1));
//        }
//    }
//    
//    @Override
//    public Expression<Boolean> toExpr() {
//        List<Expression<Boolean>> eList = this.toExprList();
//        if (!eList.isEmpty()) {
//            return this.toExpr(eList,0);
//        }
//        else {
//            return ExpressionUtil.TRUE;
//        }
//    }
//    
//    private List<IfGuard> toIfs(Set<Expression<Boolean>> ifExprs, Map<SymbolicDataValue, Variable> variables) {
//        List<IfGuard> ifGuards = new ArrayList<>();
//        for (Expression<Boolean> ifExpr : ifExprs) {
//            DataExpression<Boolean> cond = new DataExpression<>(ifExpr, variables);
//            ifGuards.add(new IfGuard(cond));
//        }
//        return ifGuards;
//    }
//        
//    @Override
//    public IfGuard toTG(Map<SymbolicDataValue, Variable> variables) {
//        Expression<Boolean> expr = this.toExpr();
//        DataExpression<Boolean> cond = new DataExpression<>(expr, variables);
//        return new IfGuard(cond);
//                
//                }
//    
//    
//    @Override
//    public String toString() {
//        return "ELSE[" + this.getParameter().toString() + "]";
//    }
//    
//    public DisequalityGuard join(EqualityGuard e) {
//        //log.log(Level.FINEST,"e.param = " + e.getParameter().toString() + ", this.param = " + this.getParameter().toString());
//        assert e.getParameter().equals(this.getParameter());
//        return this;
//    }
//    
// 
}