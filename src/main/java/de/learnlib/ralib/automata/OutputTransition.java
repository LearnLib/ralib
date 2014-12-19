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

package de.learnlib.ralib.automata;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Map.Entry;

/**
 *
 * @author falk
 */
public class OutputTransition extends Transition {
    
    private final OutputMapping output;

    public OutputTransition(OutputMapping output, ParameterizedSymbol label, RALocation source, RALocation destination, Assignment assignment) {
        super(label, source, destination, assignment);
        this.output = output;
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
        for (Entry<Parameter, Register> e : output.getOutput()) {
            if (!parameters.get(e.getKey()).equals(
                    registers.get(e.getValue()))) {
                return false;
            }
        }
            
        return true;
    }

    @Override
    public String toString() {
        return "(" + source + ", " + label + ", " + output + ", " + assignment + ", " + destination + ")";
    }    
}
