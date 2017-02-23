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
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author falk
 */
public class Disjunction extends GuardExpression {

    private final GuardExpression[] disjuncts;

    public Disjunction(GuardExpression ... disjuncts) {
        this.disjuncts = disjuncts;
    }
    
    @Override
    protected Expression<Boolean> toExpression(Map<SymbolicDataValue, Variable> map) {        
        Expression<Boolean>[] ret = new Expression[disjuncts.length];
        int i = 0;
        for (GuardExpression ge : disjuncts) {
            ret[i++] = ge.toExpression(map);
        }
        return ExpressionUtil.or(ret);
    }

    @Override
    public GuardExpression relabel(VarMapping relabelling) {
        GuardExpression[] newExpr = new GuardExpression[disjuncts.length];
        int i = 0;
        for (GuardExpression ge : disjuncts) {
            newExpr[i++] = ge.relabel(relabelling);
        }
        return new Disjunction(newExpr);
    }

    @Override
    public boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val) {
        int i = 0;
        for (GuardExpression ge : disjuncts) {
            if (ge.isSatisfied(val)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return StringUtils.join(disjuncts, " || ");
    }

    @Override
    protected void getSymbolicDataValues(Set<SymbolicDataValue> vals) {
        for (GuardExpression ge : disjuncts) {
            ge.getSymbolicDataValues(vals);
        }        
    }

    public GuardExpression[] getDisjuncts() {
        return disjuncts;
    }
    
}
