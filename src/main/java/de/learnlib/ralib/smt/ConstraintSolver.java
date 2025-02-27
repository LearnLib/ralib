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
package de.learnlib.ralib.smt;

import java.util.Properties;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3SolverProvider;

/**
 *
 * @author falk
 */
public class ConstraintSolver {

    private final gov.nasa.jpf.constraints.api.ConstraintSolver solver =
            new NativeZ3SolverProvider().createSolver(new Properties());

    public boolean isSatisfiable(Expression<Boolean> expr, Mapping<SymbolicDataValue, DataValue> val) {
        gov.nasa.jpf.constraints.api.ConstraintSolver.Result r = solver.isSatisfiable( SMTUtil.toExpression(expr, val));
        return r == gov.nasa.jpf.constraints.api.ConstraintSolver.Result.SAT;
    }
}
