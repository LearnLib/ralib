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

import de.learnlib.ralib.data.VarMappingIterator;
import de.learnlib.ralib.trees.SymbolicSuffix;
import java.util.List;

/**
 *
 * @author falk
 */
class Component {
        
    private Row primeRow;
 
    private List<Row> otherRows;
    
    /**
     * tries to add a row to this component.
     * checks if row is equivalent to rows in this component.
     * 
     * @param r
     * @return true if successful
     */
    boolean addPrefix(Row r) {
        //
        if (!primeRow.couldBeEquivalentTo(r)) {
            return false;
        }
        
        VarMappingIterator iterator;
        
        throw new UnsupportedOperationException("not implemented yet");

    }
    
    void addSuffix(SymbolicSuffix suffix) {
        
    }
    
    
    
}
