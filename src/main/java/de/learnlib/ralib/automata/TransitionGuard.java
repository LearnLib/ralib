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
package de.learnlib.ralib.automata;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.*;
import gov.nasa.jpf.constraints.api.Expression;

/**
 * Transition Guard.
 *
 * @author falk
 */
@Deprecated
public class TransitionGuard {

    private final GuardExpression condition;

    public TransitionGuard() {
        this.condition = TrueGuardExpression.TRUE;
    }

    public TransitionGuard(GuardExpression condition) {
        assert condition != null;
        this.condition = condition;
    }

    /**
     * Checks if the guard is satisfied for the given assignments of
     * registers, parameters, and named constants.
     *
     * @param registers
     * @param parameters
     * @param consts
     * @return
     */
    public boolean isSatisfied(VarValuation registers, ParValuation parameters, Constants consts) {
        Mapping<SymbolicDataValue, DataValue> val = new Mapping<>();
        val.putAll(registers);
        val.putAll(parameters);
        val.putAll(consts);

        return condition.isSatisfied(val);
    }

    @Override
    public String toString() {
        return condition.toString();
    }

    /**
     * @return the condition
     */
    public GuardExpression getCondition() {
        return condition;
    }

}
