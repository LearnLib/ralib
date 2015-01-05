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

import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * An output mapping encodes the guard of an output transition in a 
 * more straight-forward form in the case of guards with equalities. 
 * 
 * - Fresh parameters have to be unequal to values stored in registers.
 * - A mapping encodes equalities.
 * 
 * @author falk
 */
public class OutputMapping  {
    
    private final Collection<Parameter> fresh;
    
    private final VarMapping<Parameter, Register> piv;
    
    public OutputMapping(Collection<Parameter> fresh, 
            VarMapping<Parameter, Register> piv) {
        this.fresh = fresh;
        this.piv = piv;
    }
    
    public OutputMapping() {
        this(new ArrayList<Parameter>(), new VarMapping<Parameter, Register>());
    }

    public OutputMapping(Parameter fresh) {
        this(Collections.singleton(fresh), new VarMapping<Parameter, Register>());
    }
    
    public OutputMapping(Parameter key, Register value) {
        this(new ArrayList<Parameter>(), new VarMapping<Parameter, Register>(key, value));
    }

    public Collection<Parameter> getFreshParameters() {
        return fresh;
    }
    
    public VarMapping<Parameter, Register> getOutput() {
        return piv;
    }

    @Override
    public String toString() {
        return "F:" + Arrays.toString(fresh.toArray()) + ", M:" + piv.toString();
    }

}
