/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.automata.guards;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author falk
 * 
 */
public abstract class GuardExpression {
        
    public DataExpression toDataExpression() {
        Map<SymbolicDataValue, Variable> map = new HashMap<>();
        Expression<Boolean> expr = toExpression(map);
        return new DataExpression(expr, map);
    }
    
    protected abstract Expression<Boolean> toExpression(Map<SymbolicDataValue, Variable> map);
 
    public abstract GuardExpression relabel(VarMapping relabelling); 

    public abstract boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val); 
   
    public Set<SymbolicDataValue> getSymbolicDataValues() {
        Set<SymbolicDataValue> set = new HashSet<>();
        getSymbolicDataValues(set);
        return set;
    }
    
    protected abstract void getSymbolicDataValues(Set<SymbolicDataValue> vals);
}
