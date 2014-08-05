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

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.Guard;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author falk
 * @param <G>
 */
public abstract class SymbolicDecisionTree<G extends Guard> {
    
    private final boolean accepting;
    
    private final Set<SymbolicDataValue> registers;
    
    private final Map<G, SymbolicDecisionTree> children;

    SymbolicDecisionTree(boolean accepting, 
            Set<SymbolicDataValue> registers,
            Map<G, SymbolicDecisionTree> children) {
        this.accepting = accepting;
        this.registers = registers;
        this.children = children;
    }
    
    public Set<SymbolicDataValue> getRegisters() {
        return this.registers;
    }
    
    public boolean isAccepting() {
        return this.accepting;
    }

    public abstract boolean isEquivalent(SymbolicDecisionTree other);
    
    public abstract SymbolicDecisionTree createCopy(VarMapping renaming);

}
