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
package de.learnlib.ralib.automata.output;

import java.util.Map.Entry;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.words.OutputSymbol;

/**
 * Output transitions are a convenient way of
 * modeling systems with output.
 *
 * @author falk
 */
public class OutputTransition extends Transition {

    private final OutputMapping output;

    public OutputTransition(TransitionGuard guard, OutputMapping output,
            OutputSymbol label, RALocation source, RALocation destination,
            Assignment assignment) {
        super(label, guard, source, destination, assignment);
        this.output = output;
    }

    public OutputTransition(OutputMapping output, OutputSymbol label, RALocation source, RALocation destination, Assignment assignment) {
        this( new TransitionGuard(), output, label, source, destination, assignment);
    }

    public boolean canBeEnabled(VarValuation registers, Constants consts) {
        // FIXME: this is not in general safe to do!! (We assume the guard to not have parameters)
        return this.guard.isSatisfied(registers, new ParValuation(), consts);
    }

    @Override
    public boolean isEnabled(VarValuation registers, ParValuation parameters, Constants consts) {

        // check freshness of parameters ...
        for (Parameter p : output.getFreshParameters()) {
            DataValue pval = parameters.get(p);
            if (registers.containsValue(pval) || consts.containsValue(pval)) {
                return false;
            }
            for (Entry<Parameter, DataValue<?>> e : parameters) {
                if (!p.equals(e.getKey()) && pval.equals(e.getValue())) {
                    return false;
                }
            }
        }

        // check other parameters
        for (Entry<Parameter, SymbolicDataValue> e : output.getOutput()) {
            if (e.getValue() instanceof Register) {
                if (!parameters.get(e.getKey()).equals(
                        registers.get( (Register) e.getValue()))) {
                    return false;
                }
            } else if (e.getValue() instanceof Constant) {
                if (!parameters.get(e.getKey()).equals(
                        consts.get( (Constant) e.getValue()))) {
                    return false;
                }
            } else {
                throw new IllegalStateException("Source for parameter has to be register or constant.");
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "(" + source + ", " + label + ", " + guard + ", " + output +
                ", " + assignment + ", " + destination + ")";
    }

    /**
     * @return the output
     */
    public OutputMapping getOutput() {
        return output;
    }

    @Override
    public OutputSymbol getLabel() {
        return (OutputSymbol) super.getLabel();
    }


}
