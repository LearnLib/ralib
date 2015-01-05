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
import de.learnlib.ralib.theory.Branching;
import de.learnlib.ralib.theory.TreeOracle;
import de.learnlib.ralib.theory.TreeQueryResult;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
//TODO: this should be moved to test packages
public class LoggingOracle implements TreeOracle {
    
    private final TreeOracle treeoracle;

    public LoggingOracle(TreeOracle treeoracle) {
        this.treeoracle = treeoracle;
    }
    
    @Override
    public TreeQueryResult treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        //System.out.println("QUERY (tree query): " + prefix + " and " + suffix);
        return treeoracle.treeQuery(prefix, suffix);
    }    
        
//    public Word<PSymbolInstance> getDefaultExtension(
//            Word<PSymbolInstance> prefix, ParameterizedSymbol ps) {
//
//        System.out.println("QUERY (default extension): " + prefix + " and " + ps);
//        return treeoracle.getDefaultExtension(prefix, ps);
//    }
    
    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, SymbolicDecisionTree ... sdts) {
        
        System.out.println("QUERY (initial branching): " + prefix + " and " + ps);
        return treeoracle.getInitialBranching(prefix, ps, piv, sdts);
    }

    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, Branching current, 
            PIV piv, SymbolicDecisionTree ... sdts) {
        
        System.out.println("QUERY (update branching): " + prefix + 
                " and " + ps + " with " + sdts.length + " sdts");
        Branching b = treeoracle.updateBranching(prefix, ps, current, piv, sdts);
        //System.out.println(b.getBranches().size());
        return b;
    }



}
