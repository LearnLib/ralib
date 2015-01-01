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


import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.Guard;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author falk
 */
public abstract class SymbolicDecisionTree {
    
    private final boolean accepting;
    
    private final Set<Register> registers;
    
    private final Map<List<Guard>, SymbolicDecisionTree> children;

    public SymbolicDecisionTree(boolean accepting, 
            Set<Register> registers,
            Map<List<Guard>, SymbolicDecisionTree> children) {
        this.accepting = accepting;
        this.registers = registers;
        this.children = children;
    }
    
    public Set<Register> getRegisters() {
        return this.registers;
    }
    
    public boolean isAccepting() {
        return this.accepting;
    }
    
    protected Map<List<Guard>, SymbolicDecisionTree> getChildren() {
        return this.children;
    }

    public boolean isEquivalent(SymbolicDecisionTree other, VarMapping renaming) {
        return this.canUse(other) && other.canUse(this);
    }
    
    // FIXME: do we need this method in this interface?
    public abstract boolean canUse(SymbolicDecisionTree other);
    
    //public abstract SymbolicDecisionTree createCopy(VarMapping renaming);

    public SymbolicDecisionTree relabel(VarMapping relabelling) {
        if (relabelling.isEmpty()) {
            return this;
        }
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
