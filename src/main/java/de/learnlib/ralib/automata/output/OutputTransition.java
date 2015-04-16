/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.automata.output;

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
import java.util.Map.Entry;

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
