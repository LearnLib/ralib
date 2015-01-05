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

package de.learnlib.ralib.theory;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * A tree oracle is the connection between the learning algorithm 
 * and theories for data values.
 * 
 * @author falk
 */
public interface TreeOracle {
    
    /**
     * performs a tree query, returning a SymbolicDecisionTree
     * an an Assignment of registers of this tree with parameters
     * of the prefix.
     * 
     * @param prefix
     * @param suffix
     * @return 
     */
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix);    
     
    /**
     * Computes a Branching from a set of SymbolicDecisionTrees.
     * 
     * @param prefix
     * @param ps
     * @param piv
     * @param sdts
     * @return 
     */
    public Branching getInitialBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, SymbolicDecisionTree ... sdts);

    /**
     * Updates and extends an existing Branching 
     * from a set of SymbolicDecisionTrees.
     * 
     * @param prefix
     * @param ps
     * @param current
     * @param piv
     * @param sdts
     * @return 
     */
    public Branching updateBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, Branching current, 
            PIV piv, SymbolicDecisionTree ... sdts);
     
}
