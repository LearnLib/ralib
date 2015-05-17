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
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author falk
 * @param <Left>
 * @param <Right>
 */
public class AtomicGuardExpression<Left extends SymbolicDataValue, Right extends SymbolicDataValue> extends GuardExpression {

    private final Left left; 
        
    private final Relation relation;

    private final Right right;
    
    public AtomicGuardExpression(Left left, Relation relation, Right right) {
        this.left = left;
        this.relation = relation;
        this.right = right;
    }

    @Override
    protected Expression toExpression(Map<SymbolicDataValue, Variable> map) {
        Variable lv = getOrCreate(left, map);
        Variable rv = getOrCreate(right, map);
        
        switch (relation) {
            case EQUALS: 
                return new NumericBooleanExpression(lv, NumericComparator.EQ, rv);
            case NOT_EQUALS: 
                return new NumericBooleanExpression(lv, NumericComparator.NE, rv);
            case SMALLER:
                return new NumericBooleanExpression(lv, NumericComparator.LT, rv);
            case BIGGER:
                return new NumericBooleanExpression(lv, NumericComparator.GT, rv);
                
            default:
                throw new UnsupportedOperationException(
                        "Relation " + relation + " is not supoorted in guards");
        }        
    }

    private Variable getOrCreate(SymbolicDataValue dv, Map<SymbolicDataValue, Variable> map) {
        Variable ret = map.get(dv);
        if (ret == null) {
            ret = new Variable(BuiltinTypes.DOUBLE, dv.toString());
            map.put(dv, ret);
        }
        return ret;
    }
    
    @Override
    public boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val) {        
        
        DataValue lv = val.get(left);
        DataValue rv = val.get(right);
        
        System.out.println(this);
        System.out.println(val.toString());
        
        assert lv != null && rv != null;
                        
        switch (relation) {
            case EQUALS: 
                return lv.equals(rv);
            case NOT_EQUALS: 
                return !lv.equals(rv);
            case SMALLER:
            case BIGGER:
                return compareComparable(lv, rv);
                
            default:
                throw new UnsupportedOperationException(
                        "Relation " + relation + " is not supoorted in guards");
        }
    }
    
    private boolean compareComparable(DataValue l, DataValue r) {
        if (!l.getType().equals(r.getType())) {
            return false;
        }
        
        Comparable lc = (Comparable) l.getId();
        int result = lc.compareTo(r.getId());        
        switch (relation) {
            case SMALLER:
                return result < 0;
            case BIGGER:
                return result > 0;
                
            default:
                throw new UnsupportedOperationException(
                        "Relation " + relation + " is not supoorted in guards");            
        }         
    }
            
    @Override
    public GuardExpression relabel(VarMapping relabelling) {
        SymbolicDataValue newLeft = (SymbolicDataValue) relabelling.get(left);
        if (newLeft == null) {
            newLeft = left;
        }
        SymbolicDataValue newRight = (SymbolicDataValue) relabelling.get(right);
        if (newRight == null) {
            newRight = right;
        }
        
        return new AtomicGuardExpression(newLeft, relation, newRight);
    }

    
    @Override
    public String toString() {
        return "(" + left + relation + right + ")";
    }

    public Left getLeft() {
        return left;
    }

    public Right getRight() {
        return right;
    }    

    @Override
    protected void getSymbolicDataValues(Set<SymbolicDataValue> vals) {
        vals.add(left);
        vals.add(right);
    }
}
