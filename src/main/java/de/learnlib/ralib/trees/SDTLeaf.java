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
import java.util.HashMap;
import java.util.HashSet;

/**
 * Leaf implementation of an SDT.
 * 
 * @author falk
 */
public class SDTLeaf extends SymbolicDecisionTree {

    public static final SDTLeaf ACCEPTING = new SDTLeaf(true);
    
    public static final SDTLeaf REJECTING = new SDTLeaf(false);

    private SDTLeaf(boolean accepting) {
        super(accepting, new HashSet<SymbolicDataValue>(), null);
    }
            
    @Override
    public boolean isEquivalent(SymbolicDecisionTree other) {

        assert getRegisters().isEmpty() && 
                other.getRegisters().isEmpty();
        
        return (getClass() == other.getClass() &&
                isAccepting() == other.isAccepting());
    }

    @Override
    public boolean canUse(SymbolicDecisionTree other) {
        if (other instanceof SDT) {
            return false;
        }
        else {
            return this.isEquivalent(other);
        }
    }
    
//    @Override
//    public SymbolicDecisionTree createCopy(VarMapping renaming) {
//        return this;
//    }
    
    @Override
    public String toString() {
        return this.isAccepting() ? "+" : "-";
    }
    
}