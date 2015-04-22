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

package de.learnlib.ralib.learning;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final Set<SuffixValue> freeValues;
    
    /**
     * Map of positions to data values
     */
    private final Map<Integer, SuffixValue> dataValues;
            
    /**
     * actions
     */
    private final Word<ParameterizedSymbol> actions;

    public SymbolicSuffix(Word<PSymbolInstance> prefix, 
            Word<PSymbolInstance> suffix) {
        this(prefix, suffix, new Constants());
    }    
    
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
     * @param consts 
     */
    public SymbolicSuffix(Word<PSymbolInstance> prefix, 
            Word<PSymbolInstance> suffix, Constants consts) {
        
//        log.log(Level.FINEST,prefix.toString() + "\n" + suffix.toString());
        
        this.actions = DataWords.actsOf(suffix);
        
        this.dataValues = new LinkedHashMap<>();
        this.freeValues = new LinkedHashSet<>();
        
        Map<DataValue, SuffixValue> groups = new LinkedHashMap<>();
        Set<DataValue> valsetPrefix = DataWords.valSet(prefix);
        int idx = 1;
        
        SuffixValueGenerator valgen = new SuffixValueGenerator();
        
        for (DataValue d : DataWords.valsOf(suffix)) {
            if (prefix.length() == 0 || valsetPrefix.contains(d) || consts.containsValue(d)) {
            //if (valsetPrefix.contains(d) || consts.containsValue(d)) {
                SuffixValue sym = valgen.next(d.getType());
                this.freeValues.add(sym);
                this.dataValues.put(idx, sym);
//                log.log(Level.FINEST,"adding " + sym.toString() + " at " + idx);

            } else {
                SuffixValue ref = groups.get(d);
                if (ref == null) {
                    ref = valgen.next(d.getType());
                    groups.put(d, ref);
                } 
                this.dataValues.put(idx, ref);
            }            
            idx++;
        }
    }

    public SymbolicSuffix(ParameterizedSymbol ps) {
        this.actions = Word.fromSymbols(ps);
        this.dataValues = new LinkedHashMap<>();
        this.freeValues = new LinkedHashSet<>();

        SuffixValueGenerator valgen = new SuffixValueGenerator();
        int idx = 1;
        for (DataType t : ps.getPtypes()) {
            SuffixValue sv = valgen.next(t);
            this.freeValues.add(sv);
            this.dataValues.put(idx++, sv);
        }        
    }
    
    public SymbolicSuffix(Word<PSymbolInstance> prefix, SymbolicSuffix symSuffix) {
        
        this.actions = symSuffix.actions.prepend(
                DataWords.actsOf(prefix).lastSymbol());
        
        this.dataValues = new LinkedHashMap<>();
        this.freeValues = new LinkedHashSet<>();
        
        Word<PSymbolInstance> suffix = prefix.suffix(1);
        prefix = prefix.prefix(prefix.length() - 1);
        
        Map<DataValue, SuffixValue> groups = new LinkedHashMap<>();
        Set<DataValue> valsetPrefix = DataWords.valSet(prefix);
        int idx = 1;
        
        SuffixValueGenerator valgen = new SuffixValueGenerator();
        
        for (DataValue d : DataWords.valsOf(suffix)) {
            if (valsetPrefix.contains(d)) {
                SuffixValue sym = valgen.next(d.getType());
                this.freeValues.add(sym);
                this.dataValues.put(idx, sym);
//                log.log(Level.FINEST,"adding " + sym.toString() + " at " + idx);

            } else {
                SuffixValue ref = groups.get(d);
                if (ref == null) {
                    ref = valgen.next(d.getType());
                    groups.put(d, ref);
                } 
                this.dataValues.put(idx, ref);
            }            
            idx++;
        }
        
        for (int i=1; i<=DataWords.paramLength(symSuffix.actions); i++) {
            SuffixValue symValue = symSuffix.getDataValue(i);
            SuffixValue shifted = valgen.next(symValue.getType());
            this.dataValues.put(idx++, shifted);
            if (symSuffix.freeValues.contains(symValue)) {
                this.freeValues.add(shifted);
            }
        }
    }
    
    
    public SuffixValue getDataValue(int i) {
        return this.dataValues.get(i);
    }
    
    public Set<SuffixValue> getFreeValues() {
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
