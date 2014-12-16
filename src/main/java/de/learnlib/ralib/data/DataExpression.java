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

package de.learnlib.ralib.data;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author falk
 */
public class DataExpression<T> implements Evaluate<T> {

    private final Expression<T> expression;    
    private final Map<SymbolicDataValue, Variable> mapping;

    public DataExpression(Expression<T> expression, 
            Map<SymbolicDataValue, Variable> mapping) {
        this.expression = expression;
        this.mapping = mapping;
    }
        
    @Override
    public T evaluate(VarValuation vars, ParValuation pars, Constants consts) {
        
        Valuation val = new Valuation();        
        setVals(val, mapping, vars);
        setVals(val, mapping, pars);
        setVals(val, mapping, consts);
        
        return this.expression.evaluate(val);
    }
    
    public static Valuation setVals(Valuation valuation, 
            Map<SymbolicDataValue, Variable> mapping,
            Mapping<? extends SymbolicDataValue, DataValue<?>> vals) {
        
        for (Entry<? extends SymbolicDataValue, DataValue<?>> e : vals) {
            // is entry relevant in this context?
            Variable var = mapping.get(e.getKey());
            if (var == null) {
                continue;
            }
            Object value = e.getValue().id;
            valuation.setValue(var, value);
        }        
        return valuation;
    }
    
}
