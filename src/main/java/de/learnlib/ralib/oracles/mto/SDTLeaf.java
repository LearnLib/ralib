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

package de.learnlib.ralib.oracles.mto;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.SDTGuard;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Leaf implementation of an SDT.
 * 
 * @author falk
 */
public class SDTLeaf extends SDT {

    public static final SDTLeaf ACCEPTING = new SDTLeaf(true);
    
    public static final SDTLeaf REJECTING = new SDTLeaf(false);

    private final boolean accepting;
    
    private SDTLeaf(boolean accepting) {
        super(null);
        this.accepting = accepting;
    }
            
    @Override
    public boolean isEquivalent(SymbolicDecisionTree other, VarMapping renaming) {
        return (getClass() == other.getClass() &&
                isAccepting() == other.isAccepting());
    }

    @Override
    public boolean canUse(SDT other) {
        if (!(other instanceof SDTLeaf)) {
            return false;
        }
        else {
            return this.isEquivalent(other, new VarMapping());
        }
    }
    
    @Override
    public String toString() {
        return this.isAccepting() ? "+" : "-";
    }

    @Override
    public SymbolicDecisionTree relabel(VarMapping relabeling) {
        return this;
    }

    @Override
    public boolean isAccepting() {
        return accepting;
    }
    
    
    @Override
    void toString(StringBuilder sb, String indentation) {
        sb.append(indentation).append("[").append(isAccepting() ? "+" : "-").append("]").append("\n");
    }
    
    @Override
    List<List<SDTGuard>> getPaths(List<SDTGuard> path) {        
        List<List<SDTGuard>> ret = new ArrayList<>();
        if (this.isAccepting()) {
            ret.add(path);
        }
        return ret;
    }
           
    @Override
    public Set<SymbolicDataValue.Register> getRegisters() {
        return new HashSet<>();    
    }
}