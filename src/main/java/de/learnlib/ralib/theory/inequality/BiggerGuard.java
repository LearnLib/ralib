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

import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.Relation;
import de.learnlib.ralib.theory.SDTGuard;
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
public class BiggerGuard extends SDTIfGuard {
    
    public BiggerGuard(SuffixValue param, SymbolicDataValue reg) {
        super(param,reg,Relation.BIGGER);
    }
    
    
   
    @Override
    public String toString() {
        return "(" + this.getParameter().toString() + ">" + this.getRegister().toString() + ")";
        //
        //return super.toString();
    }

    public Expression<Boolean> toExpr() {
        String xname = "x" + this.getRegister().getId();
        Variable p = new Variable(BuiltinTypes.SINT32, "y");
        Variable x = new Variable(BuiltinTypes.SINT32,xname);
        return new NumericBooleanExpression(x, NumericComparator.GT, p);
    }
    
    @Override
    public IfGuard toTG(Map<SymbolicDataValue, Variable> variables) {
        Expression<Boolean> expr = this.toExpr();
        DataExpression<Boolean> cond = new DataExpression<>(expr, variables);
        return new IfGuard(cond);
    }
    
    public boolean contradicts(SDTIfGuard other) {
        boolean samePR = (this.getParameter().getId() == other.getParameter().getId() && 
                this.getRegister().getId() == other.getRegister().getId());
        return samePR && (other instanceof SmallerGuard);
    }

    @Override
    public SDTIfGuard relabel(VarMapping relabelling) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    
}
