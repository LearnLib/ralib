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
package de.learnlib.ralib.solver.simple;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.solver.ConstraintSolver;

/**
 *
 * @author falk
 */
public class SimpleConstraintSolver implements ConstraintSolver {

    private final SimpleSolver solver;

    public SimpleConstraintSolver() {
        this.solver = new SimpleSolver();
    }

    @Override
    public boolean isSatisfiable(GuardExpression expr, Mapping<SymbolicDataValue, DataValue<?>> val) {
        List<GuardExpression> conjuncts = new ArrayList<GuardExpression>();
        conjuncts.add(expr);
        SymbolicDataValue[] sdvs = val.keySet().toArray(new SymbolicDataValue[val.size()]);

        for (int i = 0; i < sdvs.length; i++) {
            for (int j = i + 1; j < sdvs.length; j++) {
                if (val.get(sdvs[i]).equals(val.get(sdvs[j]))) {
                    conjuncts.add(new AtomicGuardExpression<SymbolicDataValue, SymbolicDataValue>(sdvs[i],
                            de.learnlib.ralib.automata.guards.Relation.EQUALS, sdvs[j]));
                } else {
                    conjuncts.add(new AtomicGuardExpression<SymbolicDataValue, SymbolicDataValue>(sdvs[i],
                            de.learnlib.ralib.automata.guards.Relation.NOT_EQUALS, sdvs[j]));
                }
            }
        }

        Conjunction exprWithVal = new Conjunction(conjuncts.toArray(new GuardExpression[conjuncts.size()]));

        return solver.isSatisfiable(exprWithVal);
    }

}