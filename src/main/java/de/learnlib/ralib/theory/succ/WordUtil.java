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
package de.learnlib.ralib.theory.succ;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

/**
 *
 * @author falk
 */
public class WordUtil {

    private static final Constant C_ONE
            = new Constant<>(BuiltinTypes.SINT32, 1);

    private static final Constant C_WDW_SIZE
            = new Constant<>(BuiltinTypes.SINT32, 100);

    private static final Constant C_HS_SIZE
            = new Constant<>(BuiltinTypes.SINT32, 21474836);

    private final ConstraintSolver solver;

    public WordUtil(ConstraintSolver solver) {
        this.solver = solver;
    }

    public int[] instantiate(SuccessorDataValue[] dvs) {
        Variable[] vars = new Variable[dvs.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = new Variable(BuiltinTypes.SINT32, "v" + i);
        }

        Expression<Boolean> instCheck = ExpressionUtil.TRUE;
        for (int i = 1; i < vars.length; i++) {
            Expression<Boolean> dvCheck = constr(dvs[i], vars, i);
            instCheck = ExpressionUtil.and(instCheck, dvCheck);
        }

        Valuation val = new Valuation();
        System.out.println(instCheck);
        Result res = solver.solve(instCheck, val);
        if (res != Result.SAT) {
            return null;
        }

        System.out.println(val);
        
        int[] ret = new int[dvs.length];
        for (int i = 0; i < vars.length; i++) {
            ret[i] = (Integer) val.getValue(vars[i]);
        }
        return ret;
    }

    public Expression<Boolean> constr(SuccessorDataValue dv, Variable[] vars, int iDv) {
        Expression<Boolean>[] checks = new Expression[iDv];
        for (int i = 0; i < iDv; i++) {
            checks[i] = constr(dv.getMinTerms(i), vars[i], vars[iDv]);
        }
        return ExpressionUtil.and(checks);
    }

    public Expression<Boolean> constr(
            SuccessorMinterms mt, Variable prev, Variable cur) {

        switch (mt) {
            case EQUAL: 
                return eq(prev, cur);
            case SUCC: 
                return succ(prev, cur);
            case IN_WINDOW: 
                return ExpressionUtil.and(
                        inWdw(prev, cur),
                        not(eq(prev, cur)),
                        not(succ(prev, cur)));
            case IN_HALFSPACE:
                return ExpressionUtil.and(
                        inHSpace(prev, cur),
                        not(inWdw(prev, cur)));            
            default:
                return not(inHSpace(prev, cur));
        }

    }

    private Expression<Boolean> not(Expression<Boolean> e) {
        return new Negation(e);
    }

    private Expression<Boolean> succ(Variable prev, Variable cur) {
        return eq(cur, sum(prev, C_ONE));
    }

    private Expression<Boolean> inWdw(Variable prev, Variable cur) {
        return ExpressionUtil.and(
                le(prev, cur),
                le(cur, sum(prev, C_WDW_SIZE))
        );
    }

    private Expression<Boolean> inHSpace(Variable prev, Variable cur) {
        return ExpressionUtil.and(
                le(prev, cur),
                le(cur, sum(prev, C_HS_SIZE))
        );
    }

    private Expression<Boolean> le(Expression left, Expression right) {
        return new NumericBooleanExpression(left, NumericComparator.LE, right);
    }

    private Expression<Boolean> eq(Expression left, Expression right) {
        return new NumericBooleanExpression(left, NumericComparator.EQ, right);
    }

    private Expression sum(Expression left, Expression right) {
        return new NumericCompound<>(left, NumericOperator.PLUS, right);
    }
}
