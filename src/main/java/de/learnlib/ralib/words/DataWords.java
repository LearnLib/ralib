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

package de.learnlib.ralib.words;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.words.Word;

/**
 * static helper methods for data words.
 * 
 * @author falk
 */
public final class DataWords {

    /**
     * returns sequence of data values of a specific type in a data word.
     * 
     * @param <T>
     * @param word
     * @param t
     * @return 
     */
    public static <T> DataValue<T>[] valsOf(Word<PSymbolInstance> word, DataType t) {
        List<DataValue<T>> vals = new ArrayList<>();
        for (PSymbolInstance psi : word) {
            for (DataValue d : psi.getParameterValues()) {
                if (d.getType().equals(t)) {
                    vals.add(d);
                }
            }
        }
        return vals.toArray(new DataValue[] {});
    }
    
    /**
     * returns sequence of all data values in a data word.
     * 
     * @param word
     * @return 
     */
    public static DataValue[] valsOf(Word<PSymbolInstance> word) {
        DataValue[] vals = new DataValue[DataWords.paramLength(actsOf(word))];
        int i = 0;
        for (PSymbolInstance psi : word) {
            for (DataValue p : psi.getParameterValues()) {
                vals[i++] = p;
            }
        }
        return vals;
    }
 
    /**
     * returns set of unique data values of some type in a data word.
     * 
     * @param <T>
     * @param word
     * @param t
     * @return 
     */
    public static <T> Set<DataValue<T>> valSet(Word<PSymbolInstance> word, DataType t) {
        Set<DataValue<T>> vals = new HashSet<>();
        for (PSymbolInstance psi : word) {
            for (DataValue d : psi.getParameterValues()) {
                if (d.getType().equals(t)) {
                    vals.add(d);
                }
            }
        }
        return vals;
    }
    
    /**
     * 
     * @param <T>
     * @param in
     * @return 
     */
    public static <T> Set<DataValue<T>> joinValsToSet(Collection<DataValue<T>> ... in) {
        Set<DataValue<T>> vals = new HashSet<>();    
        for (Collection<DataValue<T>> s : in) {
            vals.addAll(s);
        }
        return vals;
    }
    
    /**
     * returns set of all unique data values in a data word.
     * 
     * @param word
     * @return 
     */
    public static Set<DataValue> valSet(Word<PSymbolInstance> word) {
        Set<DataValue> valset = new HashSet<>();
        for (PSymbolInstance psi : word) {
            valset.addAll(Arrays.asList(psi.getParameterValues()));
        }
        return valset;
    }    
     
    /**
     * returns sequence of actions in a data word.
     * @param word
     * @return 
     */
    public static Word<ParameterizedSymbol> actsOf(
            Word<PSymbolInstance> word) {
        ParameterizedSymbol[] symbols = new ParameterizedSymbol[word.length()];        
        int idx = 0;
        for (PSymbolInstance psi : word) {
            symbols[idx++] = psi.getBaseSymbol();
        }        
        return Word.fromSymbols(symbols);
    }
    
    /**
     * instantiates a data word from a sequence of actions and
     * a valuation.
     * 
     * @param actions
     * @param dataValues
     * @return 
     */
    public static Word<PSymbolInstance> instantiate(
            Word<ParameterizedSymbol> actions, 
            Map<Integer, ? extends DataValue> dataValues) {

        PSymbolInstance[] symbols = new PSymbolInstance[actions.length()];
        int idx = 0;
        int pid = 1;
        for (ParameterizedSymbol ps : actions) {
            DataValue[] pvalues = new DataValue[ps.getArity()];
            for (int i = 0; i < ps.getArity(); i++) {
                pvalues[i] = dataValues.get(pid++);
            }
            symbols[idx++] = new PSymbolInstance(ps, pvalues);
        }
        return Word.fromSymbols(symbols);
    }

    /**
     * returns the number of data values in a sequence of actions.
     * 
     * @param word
     * @return 
     */
    public static int paramLength(Word<ParameterizedSymbol> word) {
        int length = 0;
        for (ParameterizedSymbol psi : word) {
            length += psi.getArity();
        }
        return length;
    }
        
    private DataWords() {        
    }    
}
