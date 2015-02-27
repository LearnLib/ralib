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

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.Relation;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.Map;

/**
 *
 * @author falk
 */
public class SmallerGuard extends SDTGuard {
    
    public SmallerGuard(SymbolicDataValue.SuffixValue param, Register reg) {
        super(param,reg,Relation.SMALLER);
    }
    
    
   
    @Override
    public String toString() {
        //return "(" + this.getParameter().toString() + "<" + this.getRegister().toString() + ")";
        return super.toString();
    }

    @Override
    public Expression<Boolean> getGuardExpression(Map<SymbolicDataValue, Variable> variables) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
