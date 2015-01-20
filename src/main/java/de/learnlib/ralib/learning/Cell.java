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

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Collection;
import net.automatalib.words.Word;

/**
 * A cell of an observation table.
 * 
 * @author falk
 */
final class Cell {
    
    private final Word<PSymbolInstance> prefix;
     
    private final SymbolicSuffix suffix;
    
    private final SymbolicDecisionTree sdt;
    
    private final PIV parsInVars;

    private Cell(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, SymbolicDecisionTree sdt, PIV parsInVars) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.sdt = sdt;
        this.parsInVars = parsInVars;
    }

    Collection<Parameter> getMemorable() {
        return parsInVars.keySet();
    }
    
    PIV getParsInVars() {
        return parsInVars;
    }
    
    /**
     * checks whether the sdts of the two cells are equal
     * 
     * @param other
     * @return 
     */
    boolean isEquivalentTo(Cell other, VarMapping renaming) {
        if (!couldBeEquivalentTo(other)) {
            return false;
        }
        
        return this.suffix.equals(other.suffix) &&
                this.parsInVars.relabel(renaming).equals(other.parsInVars) &&
                this.sdt.isEquivalent(other.sdt, renaming);
    }
    
    /**
     * 
     * @param r
     * @return 
     */
    boolean couldBeEquivalentTo(Cell other) {
        return this.parsInVars.typedSize().equals(other.parsInVars.typedSize());
        //TODO: call preliminary checks on SDTs
    }   
    
    /**
     * computes a cell for a prefix and a symbolic suffix.
     * 
     * @param oracle
     * @param prefix
     * @param suffix
     * @return 
     */
    static Cell computeCell(TreeOracle oracle, 
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
       
        TreeQueryResult tqr = oracle.treeQuery(prefix, suffix);
        return new Cell(prefix, suffix, tqr.getSdt(), 
                new PIV(prefix, tqr.getParsInVars()));
    }

    SymbolicSuffix getSuffix() {
        return this.suffix;
    }

    Word<PSymbolInstance> getPrefix() {
        return this.prefix;
    }
    
    SymbolicDecisionTree getSDT() {
        return this.sdt;
    }
    
    @Override
    public String toString() {
        return "Cell: " + this.prefix + " / " + this.suffix + " : " + this.sdt.toString();
    }

    Cell relabel(VarMapping relabelling) {
        return new Cell(prefix, suffix, 
                sdt.relabel(relabelling), 
                parsInVars.relabel(relabelling));
    }

    boolean isAccepting() {
        return this.sdt.isAccepting();
    }

}
