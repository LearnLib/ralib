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

package de.learnlib.ralib.data;

import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class PIV extends VarMapping<Parameter, Register> {

    public PIV() {
        
    }
    
    public PIV(Word<PSymbolInstance> prefix, ParsInVars parsInVars) {

        for (Map.Entry<Register, DataValue<?>> e : parsInVars) {            
            System.out.println("PIV: " + e.getKey() + " : " + e.getValue());
        }
        
        HashSet<DataValue<?>> vals = new HashSet<>(parsInVars.values());
        
        int idx = 1;
        for (PSymbolInstance psi : prefix) {        
            for (DataValue dv : psi.getParameterValues()) {                
                if (vals.contains(dv)) {
                    vals.remove(dv);
                    put( new Parameter(dv.getType(), idx), parsInVars.getOneKey(dv));
                }
                idx++;
            }
        }
        
    }
      
    public PIV relabel(VarMapping relabelling) {
        PIV ret = new PIV();
        for (Map.Entry<Parameter, Register> e : this) {
            Parameter p = (Parameter) relabelling.get(e.getKey());
            Register r = (Register) relabelling.get(e.getValue());            
            ret.put(p == null ? e.getKey() : p, r == null ? e.getValue() : r);
        }
        return ret;
    }
    
    public Map<DataType, Integer> typedSize() {
        Map<DataType, Integer> ret = new HashMap<>();
        for (Parameter p : keySet()) {
            Integer i = ret.get(p.getType());
            i = (i == null) ? 1 : i+1;
            ret.put(p.getType(), i);
        }
        return ret;
    } 

    public Map<DataType, Parameter[]> asTypedArrays() {
        Map<DataType, List<Parameter>> tmp = new HashMap<>();
        for (Parameter p : keySet()) {
            List<Parameter> list = tmp.get(p.getType());
            if (list == null) {
                list = new ArrayList<>();
                tmp.put(p.getType(), list);
            }
            list.add(p);
        } 
        
        Map<DataType, Parameter[]> ret = new HashMap<>();
        for (Map.Entry<DataType, List<Parameter>> e : tmp.entrySet()) {
            ret.put(e.getKey(), e.getValue().toArray(new Parameter[] {}));
        }
        return ret;
    }
    
}
