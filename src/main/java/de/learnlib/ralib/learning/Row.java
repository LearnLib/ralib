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

import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
class Row {
    
    private final Word<PSymbolInstance> prefix;

    private final Map<SymbolicSuffix, Cell> cells;
        
    private final Set<SymbolicDataValue> memorable = new HashSet<>();

    private Row(Word<PSymbolInstance> prefix, Map<SymbolicSuffix, Cell> cells) {
        this.prefix = prefix;
        this.cells = cells;
        
        for (Cell c : cells.values()) {
            memorable.addAll(c.getMemorable());
        }
    }
    
    
    
    ParsInVars getParsInVars() {
        throw new UnsupportedOperationException("not implemented yet.");        
    } 
    
    Set<SymbolicDataValue> getMemorable() {        
        throw new UnsupportedOperationException("not implemented yet.");        
    }

    /**
     * relabels all the SDTs in the cells.
     * 
     * @param relabelling
     * @return - a new row with relabeled cells
     */
    Row relabel(VarMapping relabelling) {
        LinkedHashMap<SymbolicSuffix, Cell> cellsNew = new LinkedHashMap<>();
        for (Entry<SymbolicSuffix, Cell> e : this.cells.entrySet()) {
            cellsNew.put(e.getKey(), e.getValue().relabel(relabelling));
        }
        return new Row(prefix, cellsNew);
                     
    }
    
    /**
     * checks rows for equality (disregarding of the prefix!).
     * It is assumed that both rows have the same set of suffixes.
     * 
     * @param other
     * @return true if rows are equal
     */
    boolean equals(Row other) { 
        for (SymbolicSuffix s : this.cells.keySet()) {
            Cell c1 = this.cells.get(s);
            Cell c2 = other.cells.get(s);
            if (!c1.equals(c2)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 
     * @param r
     * @return 
     */
    boolean couldBeEquivalentTo(Row other) {
        if (this.getMemorable().size() != other.getMemorable().size()) {
            return false;
        }
        
        for (SymbolicSuffix s : this.cells.keySet()) {
            Cell c1 = this.cells.get(s);
            Cell c2 = other.cells.get(s);
            if (!c1.couldBeEquivalentTo(c2)) {
                return false;
            }
        }
        return true;
    }    
    
    /**
     * computes a new row object from a prefix and a set of symbolic suffixes.
     * 
     * @param oracle
     * @param prefix
     * @param suffixes
     * @return 
     */
    static Row computeRow(Oracle oracle, 
            Word<PSymbolInstance> prefix, List<SymbolicSuffix> suffixes) {
    
        LinkedHashMap<SymbolicSuffix, Cell> cells = new LinkedHashMap<>();
        for (SymbolicSuffix s : suffixes) {
            cells.put(s, Cell.computeCell(oracle, prefix, s));
        }
        return new Row(prefix, cells);
    }

}
