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

package de.learnlib.ralib.data.util;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.util.PermutationIterator;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.VarMapping;
import java.util.Iterator;
import java.util.Map;

/**
 * Iterates all possible re-mappings between two VarMappings of the same size.
 * 
 * @author falk
 */
public class PIVRemappingIterator implements Iterable<VarMapping>, Iterator<VarMapping> {

    private final PIV replace;
    
    private final PIV by;
    
    private final PermutationIterator[] iterators;
    
    private final Parameter[][] replaceParams;
    
    private final Parameter[][] byParams;
    
    private boolean init = false;
    
    public PIVRemappingIterator(PIV replace, PIV by) {
        assert replace.typedSize().equals(by.typedSize());
        
        this.replace = replace;
        this.by = by;
        
        Map<DataType, Parameter[]> rep_ta = replace.asTypedArrays();
        Map<DataType, Parameter[]> by_ta = by.asTypedArrays();
        
        iterators = new PermutationIterator[rep_ta.size()];
        replaceParams = new Parameter[rep_ta.size()][];
        byParams = new Parameter[by_ta.size()][];
        
        int idx = 0;
        for (DataType t : rep_ta.keySet()) {
            replaceParams[idx] = rep_ta.get(t);
            byParams[idx] = by_ta.get(t);        
            iterators[idx] = new PermutationIterator(replaceParams[idx].length);
            iterators[idx].next();
            idx++;
        }
    } 

    @Override
    public Iterator<VarMapping> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        if (!init) {
            return true;
        } 
            
        for (PermutationIterator pi : iterators) {
            if (pi.hasNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public VarMapping next() {
        if (!init) {
            init = true;
        } else {
            advance();
        }
        VarMapping map = createMapping();
        return map;
    }

    private void advance() {
        for (int idx=iterators.length-1; idx>=0; idx--) {
            PermutationIterator pi = iterators[idx];
            if (pi.hasNext()) {
                pi.next();
                return;
            }
            iterators[idx] = new PermutationIterator(replaceParams[idx].length);
            iterators[idx].next();
        }        
    }
    
    private VarMapping createMapping() {
        VarMapping ret = new VarMapping();
        for (int idx=0; idx<iterators.length; idx++) {            
            int[] permutation = iterators[idx].current();
            for (int pi=0; pi<replaceParams[idx].length; pi++) {
                Parameter repP = replaceParams[idx][pi];
                Parameter byP = byParams[idx][permutation[pi]];
                ret.put(repP, byP);
                ret.put(replace.get(repP), by.get(byP));
            }
        }
        return ret;
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported."); 
    }
    
}
