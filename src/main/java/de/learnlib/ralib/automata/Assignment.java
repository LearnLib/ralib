/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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

import java.util.List;
import java.util.Map.Entry;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;

/**
 * A parallel assignment for registers.
 *
 * @author falk
 */
public class Assignment {

    private final VarMapping<Register, ? extends SymbolicDataValue> assignment;

    public Assignment(VarMapping<Register, ? extends SymbolicDataValue> assignment) {
        this.assignment = assignment;
    }

    public RegisterValuation compute(RegisterValuation registers, ParameterValuation parameters, Constants consts) {
        RegisterValuation val = new RegisterValuation();
        List<String> rNames = assignment.keySet().stream().map(k -> k.getName()).toList();
        for (Entry<Register, DataValue> e : registers.entrySet()) {
            if (!rNames.contains(e.getKey().getName())) {
                val.put(e.getKey(), e.getValue());
            }
        }
        for (Entry<Register, ? extends SymbolicDataValue> e : assignment) {
            SymbolicDataValue valp = e.getValue();
            if (valp.isRegister()) {
                val.put(e.getKey(), registers.get((Register) valp));
            }
            else if (valp.isParameter()) {
                val.put(e.getKey(), parameters.get((Parameter) valp));
            }
            //TODO: check if we want to copy constant values into vars
            else if (valp.isConstant()) {
                val.put(e.getKey(), consts.get((Constant) valp));
            }
            else {
                throw new IllegalStateException("Illegal assignment: " +
                        e.getKey() + " := " + valp);
            }
        }
        return val;
    }

    @Override
    public String toString() {
        return assignment.toString(":=");
    }

    public VarMapping<Register, ? extends SymbolicDataValue> getAssignment() {
        return assignment;
    }

}
