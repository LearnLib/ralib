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
package de.learnlib.ralib.automata.guards;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;

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
