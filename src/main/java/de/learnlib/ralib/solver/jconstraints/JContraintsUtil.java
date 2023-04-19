/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.solver.jconstraints;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.FalseGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author falk
 */
public class JContraintsUtil {
    
    public static Type<?> getJCType(Class<?> cls) {
        if (cls == Integer.class)  {
            return BuiltinTypes.DECIMAL;
        } else if (cls == Float.class) {
            return BuiltinTypes.DECIMAL;
        } else if (cls == Long.class) {
            return BuiltinTypes.DECIMAL;
        } else {
            return BuiltinTypes.DECIMAL;
        }
    }

    public static Expression<Boolean> toExpression(
            LogicalOperator op,
            Map<SymbolicDataValue, Variable> map,
            GuardExpression... expr) {
        
        Expression<Boolean>[] elems = new Expression[expr.length];
        int i = 0;
        for (GuardExpression e : expr) {
            elems[i++] = toExpression(e, map);
        }
        switch (op) {
            case AND:
                return ExpressionUtil.and(elems);
            case OR:
                return ExpressionUtil.or(elems);
            default:
                throw new RuntimeException("Unsupported Operator: " + op);
        }
    }
    
    public static Expression<Boolean> toExpression(GuardExpression expr, Mapping<SymbolicDataValue, DataValue<?>> val) {
        Map<SymbolicDataValue, Variable> map = new HashMap<>();
        Expression<Boolean> guardExpr = toExpression(expr, map);
        Expression<Boolean> valExpr = toExpression(val, map);
        return ExpressionUtil.and(guardExpr, valExpr);
    }
    
    public static Expression<Boolean> toExpression(Mapping<SymbolicDataValue, DataValue<?>> val, Map<SymbolicDataValue, Variable> map) {
        Expression<Boolean>[] elems = new Expression[val.size()];
        int i = 0;
        for (Map.Entry<SymbolicDataValue, DataValue<?>> entry : val.entrySet()) {
            elems[i++] = new NumericBooleanExpression(getOrCreate(entry.getKey(), map), NumericComparator.EQ, toConstant(entry.getValue()));
        }
        return ExpressionUtil.and(elems);
    }

    public static Expression<Boolean> toExpression(GuardExpression expr) {
        return toExpression(expr, new HashMap<>());
    }   
    
    public static Expression<Boolean> toExpression(GuardExpression expr,
            Map<SymbolicDataValue, Variable> map) {
        
        if (expr instanceof AtomicGuardExpression) {
            return toExpression((AtomicGuardExpression) expr, map);
        } else if (expr instanceof TrueGuardExpression) {
            return ExpressionUtil.TRUE;
        } else if (expr instanceof FalseGuardExpression) {
            return ExpressionUtil.FALSE;
        } else if (expr instanceof Conjunction) {
            Conjunction con = (Conjunction) expr;
            return toExpression(LogicalOperator.AND, map, con.getConjuncts());
        } else if (expr instanceof Disjunction) {
            Disjunction dis = (Disjunction) expr;
            return toExpression(LogicalOperator.OR, map, dis.getDisjuncts());
        } else if (expr instanceof Negation) {
            Negation neg = (Negation) expr;
            return new gov.nasa.jpf.constraints.expressions.Negation(
                    toExpression(neg.getNegated(), map));
        }

        throw new RuntimeException("Unsupported Guard Expression: "
                + expr.getClass().getName());
    }
    

    public static Expression<Boolean> toExpression(AtomicGuardExpression expr,
            Map<SymbolicDataValue, Variable> map) {

        Variable lv = getOrCreate(expr.getLeft(), map);
        Variable rv = getOrCreate(expr.getRight(), map);

        switch (expr.getRelation()) {
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
                        "Relation " + expr.getRelation() + " is not supoorted in guards");
        }
    }

    private static Variable getOrCreate(SymbolicDataValue dv,
            Map<SymbolicDataValue, Variable> map) {
        Type jcType = getJCType(dv.getType().getBase());
        Variable ret = map.get(dv);
        if (ret == null) {
            ret = new Variable(jcType, dv.toString());
            map.put(dv, ret);
        }
        return ret;
    }
    
    public static Constant toConstant(DataValue v) {
        return new Constant( getJCType(v.getType().getBase()), (v.getId()));
    }

    public static Variable toVariable(DataValue v) {
        return new Variable(BuiltinTypes.DECIMAL, v.toString());
    }
}
