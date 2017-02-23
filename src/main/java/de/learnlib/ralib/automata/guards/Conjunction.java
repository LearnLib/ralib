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
public class Conjunction extends GuardExpression {

    private final GuardExpression[] conjuncts;

    public Conjunction(GuardExpression ... conjuncts) {
        this.conjuncts = conjuncts;
    }
    
    @Override
    protected Expression<Boolean> toExpression(Map<SymbolicDataValue, Variable> map) {        
        Expression<Boolean>[] ret = new Expression[conjuncts.length];
        int i = 0;
        for (GuardExpression ge : conjuncts) {
            ret[i++] = ge.toExpression(map);
        }
        return ExpressionUtil.and(ret);
    }

    @Override
    public GuardExpression relabel(VarMapping relabelling) {
        GuardExpression[] newExpr = new GuardExpression[conjuncts.length];
        int i = 0;
        for (GuardExpression ge : conjuncts) {
            newExpr[i++] = ge.relabel(relabelling);
        }
        return new Conjunction(newExpr);
    }

    @Override
    public boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val) {
        int i = 0;
        for (GuardExpression ge : conjuncts) {
            if (!ge.isSatisfied(val)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return StringUtils.join(conjuncts, " && ");
    }

    @Override
    protected void getSymbolicDataValues(Set<SymbolicDataValue> vals) {
        for (GuardExpression ge : conjuncts) {
            ge.getSymbolicDataValues(vals);
        }
    }

    public GuardExpression[] getConjuncts() {
        return conjuncts;
    }    
}
