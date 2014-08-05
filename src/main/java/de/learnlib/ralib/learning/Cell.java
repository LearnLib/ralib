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
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.TreeQueryResult;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;

/**
 *
 * @author falk
 */
class Cell {
    
    private final SymbolicSuffix suffix;
    
    private final SymbolicDecisionTree sdt;
    
    private final ParsInVars parsInVars;

    Cell(SymbolicSuffix suffix, TreeQueryResult result) {
        this.suffix = suffix;
        this.sdt = result.getSdt();
        this.parsInVars = result.getParsInVars();
    }

    ParsInVars getMemorable() {
        return this.parsInVars;       
    }
    
    Cell createCopy(VarMapping renaming) {
        throw new UnsupportedOperationException("not implemented yet.");                
    }
    
    boolean isEquivalent(Cell other) {
        throw new UnsupportedOperationException("not implemented yet.");                
    }
    
}
