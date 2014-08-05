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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author falk
 */
class Row {
    
    private final Map<SymbolicSuffix, Cell> cells = new LinkedHashMap<>();
    
    private final Set<SymbolicDataValue> memorable = new HashSet<>();
    
    ParsInVars getParsInVars() {
        throw new UnsupportedOperationException("not implemented yet.");        
    } 
    
    Set<SymbolicDataValue> getMemorable() {        
        throw new UnsupportedOperationException("not implemented yet.");        
    }

    Row createCopy(VarMapping renaming) {
        throw new UnsupportedOperationException("not implemented yet.");                
    }
    
    boolean isEquivalent(Row other) {
        throw new UnsupportedOperationException("not implemented yet.");                
    }
}
