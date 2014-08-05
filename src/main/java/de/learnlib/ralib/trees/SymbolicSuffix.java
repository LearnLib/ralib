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

package de.learnlib.ralib.trees;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.automatalib.words.Word;

/**
 * A symbolic suffix is a sequence of actions with 
 * a constraint over the parameters in the sequence.
 * 
 * @author falk
 */
public class SymbolicSuffix {
    
    /**
     * symbolic values that may connect to a prefix
     */
    private final Set<SymbolicDataValue> freeValues;
    
    /**
     * Map of positions to data values
     */
    private final Map<Integer, SymbolicDataValue> dataValues;
            
    /**
     * actions
     */
    private final Word<ParameterizedSymbol> actions;
    
    /**
     * creates a symbolic suffix from a prefix and a suffix
     * data word.
     * Shared data values between prefix and suffix become
     * free values. Equalities between data values in the 
     * suffix data word are only preserved for un-free data 
     * values. Preserving equalities for free data values 
     * would lead to undesired effects (Falk).
     * 
     * @param prefix
     * @param suffix 
     */
    public SymbolicSuffix(Word<PSymbolInstance> prefix, 
            Word<PSymbolInstance> suffix) {
        
        this.actions = DataWords.actsOf(suffix);
        
        this.dataValues = new HashMap<>();
        this.freeValues = new HashSet<>();
        
        Map<DataValue, SymbolicDataValue> groups = new HashMap<>();
        Set<DataValue> valsetPrefix = DataWords.valSet(prefix);
        int idx = 1;
        int symc = 1;
        for (DataValue d : DataWords.valsOf(suffix)) {
            if (valsetPrefix.contains(d)) {
                SymbolicDataValue sym = SymbolicDataValue.suffix(
                        d.getType(), symc++);
                this.freeValues.add(sym);
                this.dataValues.put(idx, sym);
            } else {
                SymbolicDataValue ref = groups.get(d);
                if (ref == null) {
                    ref = SymbolicDataValue.suffix( 
                            d.getType(), symc++);
                    groups.put(d, ref);
                } 
                this.dataValues.put(idx, ref);
            }            
            idx++;
        }
    }

    public SymbolicDataValue getDataValue(int i) {
        return this.dataValues.get(i);
    }
    
    public Set<SymbolicDataValue> getFreeValues() {
        return this.freeValues;
    }

    public Word<ParameterizedSymbol> getActions() {
        return actions;
    }
    
    @Override
    public String toString() {
        Word<PSymbolInstance> dw = 
                DataWords.instantiate(actions, dataValues);
        
        return Arrays.toString(freeValues.toArray()) + 
                "((" + dw.toString() + "))";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SymbolicSuffix other = (SymbolicSuffix) obj;
        if (this.freeValues != other.freeValues && (this.freeValues == null || !this.freeValues.equals(other.freeValues))) {
            return false;
        }
        if (this.dataValues != other.dataValues && (this.dataValues == null || !this.dataValues.equals(other.dataValues))) {
            return false;
        }
        if (this.actions != other.actions && (this.actions == null || !this.actions.equals(other.actions))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.freeValues != null ? this.freeValues.hashCode() : 0);
        hash = 37 * hash + (this.dataValues != null ? this.dataValues.hashCode() : 0);
        hash = 37 * hash + (this.actions != null ? this.actions.hashCode() : 0);
        return hash;
    }
        
}
