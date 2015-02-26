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


import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import java.util.Set;

/**
 * This interface describes the methods that are needed in a symbolic decision
 * tree during learning.
 * 
 * @author falk
 */
public interface SymbolicDecisionTree {
    
    /**
     * checks if the the tree (under renaming) is equivalent to other tree 
     * 
     * @param other
     * @param renaming
     * @return 
     */
    public boolean isEquivalent(SymbolicDecisionTree other, VarMapping renaming);
    
    /**
     * apply relabeling to tree and return a renamed tree.
     * 
     * @param relabeling
     * @return 
     */
    public SymbolicDecisionTree relabel(VarMapping relabeling);
    
    /**
     * true if all paths in this tree are accepting
     * 
     * @return 
     */
    
    public Set<SymbolicDataValue.Register> getRegisters();
    
    public boolean isAccepting();
}
