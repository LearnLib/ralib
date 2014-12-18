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
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.VarValuation;
import java.util.Map.Entry;

/**
 *
 * @author falk
 */
public class Assignment {
    
    private final VarMapping<Register, ? extends SymbolicDataValue> assignment;

    public Assignment(VarMapping<Register, ? extends SymbolicDataValue> assignment) {
        this.assignment = assignment;
    }
    
    public VarValuation compute(VarValuation registers, ParValuation parameters, Constants consts) {
        VarValuation val = new VarValuation();
        for (Entry<Register, ? extends SymbolicDataValue> e : assignment) {
            SymbolicDataValue valp = e.getValue();
            if (valp.isRegister()) {
                val.put(e.getKey(), registers.get( (Register) valp));
            }
            else if (valp.isParameter()) {
                val.put(e.getKey(), parameters.get( (Parameter) valp));
            }
            //else if (valp.isConstant()) {
            //    val.put(e.getKey(), consts.get( (Constant) valp));
            //}
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
    

    
}
