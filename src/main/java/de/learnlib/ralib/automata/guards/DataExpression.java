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

package de.learnlib.ralib.automata.guards;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarValuation;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A data expression encodes a relation between parameters and
 * registers as a logic formula using the jConstraints library.
 * 
 * @author falk
 * @param <T> type to which the expression evaluates
 */
public class DataExpression<T extends Object> {

    private final Expression<T> expression;    
    private final Map<SymbolicDataValue, Variable> mapping;

    public DataExpression(Expression<T> expression, 
            Map<SymbolicDataValue, Variable> mapping) {
        this.expression = expression;
        this.mapping = mapping;
    }
        
    public T evaluate(VarValuation vars, ParValuation pars, Constants consts) {
        
        Valuation val = new Valuation();        
        setVals(val, mapping, vars);
        setVals(val, mapping, pars);
        setVals(val, mapping, consts);
        
        return this.expression.evaluate(val);
    }
    
    private Valuation setVals(Valuation valuation, 
            Map<SymbolicDataValue, Variable> mapping,
            Mapping<? extends SymbolicDataValue, DataValue<?>> vals) {
        
        for (Entry<? extends SymbolicDataValue, DataValue<?>> e : vals) {
            // is entry relevant in this context?
            Variable var = mapping.get(e.getKey());
            if (var == null) {
                continue;
            }
            Object value = e.getValue().getId();
            valuation.setValue(var, value);
        }        
        return valuation;
    }

    @Override
    public String toString() {
        return this.mapping.toString() + "[" + this.expression.toString() + "]";
    }
    

}
