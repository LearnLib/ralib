/*
 * Copyright (C) 2025 The LearnLib Contributors
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
package de.learnlib.ralib.smt;

import java.util.HashMap;
import java.util.Properties;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3SolverProvider;

/**
 *
 * @author falk
 */
public class ConstraintSolver {

    private static HashMap<Expression<Boolean>, Boolean> cache = new HashMap<>();

    private final static gov.nasa.jpf.constraints.api.ConstraintSolver solver =
            new NativeZ3SolverProvider().createSolver(new Properties());

    public boolean isSatisfiable(Expression<Boolean> expr, Mapping<? extends SymbolicDataValue, DataValue> val) {
        Expression<Boolean> test = SMTUtil.toExpression(expr, val);
        Boolean r = cache.get(test);
        if (r == null) {
            SolverContext ctx = solver.createContext();
            r = ctx.isSatisfiable(test) == gov.nasa.jpf.constraints.api.ConstraintSolver.Result.SAT;
            cache.put(test, r);
            ctx.dispose();
        }
        return r;
    }
}
