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
import java.util.Map;
import java.util.Set;

/**
 *
 * @author falk
 */
public class Negation extends GuardExpression {

    private final GuardExpression negated;

    public Negation(GuardExpression negated) {
        this.negated = negated;
    }
    
    @Override
    protected Expression<Boolean> toExpression(Map<SymbolicDataValue, Variable> map) {
        return new gov.nasa.jpf.constraints.expressions.Negation(
                this.negated.toExpression(map));
    }

    @Override
    public GuardExpression relabel(VarMapping relabelling) {
        GuardExpression newNegated = negated.relabel(relabelling);
        return new Negation(newNegated);
    }

    @Override
    public boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val) {
        return !negated.isSatisfied(val);
    }

    @Override
    public String toString() {
        return "(!" + negated + ")";
    }

    @Override
    protected void getSymbolicDataValues(Set<SymbolicDataValue> vals) {
        this.negated.getSymbolicDataValues(vals);
    }
    
}
