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

import java.util.LinkedHashSet;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;

/**
 *
 * @author falk
 *
 */
public abstract class GuardExpression {

    public abstract GuardExpression relabel(VarMapping relabelling);

    public abstract boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val);

    public Set<SymbolicDataValue> getSymbolicDataValues() {
        Set<SymbolicDataValue> set = new LinkedHashSet<>();
        getSymbolicDataValues(set);
        return set;
    }

    protected abstract void getSymbolicDataValues(Set<SymbolicDataValue> vals);
}
