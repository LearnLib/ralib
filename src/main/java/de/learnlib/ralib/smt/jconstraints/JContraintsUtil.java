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
package de.learnlib.ralib.smt.jconstraints;

import java.util.HashMap;
import java.util.Map;

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

/**
 *
 * @author falk
 */
public class JContraintsUtil {

    public static Expression<Boolean> toExpression(Expression<Boolean> expr, Mapping<SymbolicDataValue, DataValue> val) {
        Map<SymbolicDataValue, Variable> map = new HashMap<>();
        //Expression<Boolean> guardExpr = toExpression(expr, map);
        Expression<Boolean> valExpr = toExpression(val, map);
        return ExpressionUtil.and(expr, valExpr);
    }

    public static Expression<Boolean> toExpression(Mapping<SymbolicDataValue, DataValue> val, Map<SymbolicDataValue, Variable> map) {
        Expression<Boolean>[] elems = new Expression[val.size()];
        int i = 0;
        for (Map.Entry<SymbolicDataValue, DataValue> entry : val.entrySet()) {
            elems[i++] = new NumericBooleanExpression(getOrCreate(entry.getKey(), map), NumericComparator.EQ, toConstant(entry.getValue()));
        }
        return ExpressionUtil.and(elems);
    }

    private static Variable getOrCreate(SymbolicDataValue dv,
            Map<SymbolicDataValue, Variable> map) {
        Variable ret = map.get(dv);
        if (ret == null) {
            // FIXME: superfluous!
            ret = dv;
            map.put(dv, ret);
        }
        return ret;
    }

    public static Constant toConstant(DataValue v) {
        return new Constant( BuiltinTypes.DECIMAL, (v.getValue()));
    }

    public static Variable toVariable(DataValue v) {
        return new Variable(BuiltinTypes.DECIMAL, v.toString());
    }
}
