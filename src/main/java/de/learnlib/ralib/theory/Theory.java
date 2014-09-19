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

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.VarsToInternalRegs;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Collection;
import java.util.List;
import net.automatalib.words.Word;


/**
 *
 * @author falk
 * @param <T>
 */
public interface Theory<T> {
      
    public DataValue<T> getFreshValue(List<DataValue<T>> vals);
    
    /**
     * computes the potential for a set of data values of this type.
     * 
     * @param vals
     * @return 
     */
    public List<DataValue<T>> getPotential(Collection<DataValue<T>> vals);
    
    /** 
     * Implements a tree query for this theory. This tree query
     * will only work on one parameter and then call the 
     * TreeOracle for the next parameter.
     * 
     * This method should contain (a) creating all values for the
     * current parameter and (b) merging the corresponding 
     * sub-trees.
     * 
     * @param prefix prefix word. 
     * @param suffix suffix word.
     * @param values found values for complete word (pos -> dv)
     * @param piv memorable data values of the prefix (dv <-> itr) 
     * @param suffixValues map of already instantiated suffix 
     * data values (sv -> dv)
     * @param oracle the tree oracle in control of this query
     * 
     * @return a symbolic decision tree and updated piv 
     */
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix,             
            SymbolicSuffix suffix,
            WordValuation values, 
            ParsInVars piv,
            VarValuation suffixValues,
            TreeOracle oracle);
        
    
   
    /**
     * This method computes the initial branching for
     * an SDT. It re-uses existing valuations where 
     * possible.
     * 
     * @param merged An SDT
     * @param vtir mapping from memorable values to
     * internal registers of the tree
     * @param parval already used valuations of 
     * parameters of the tree
     * @return a branching
     */
    public Branching getInitialBranching(
            SymbolicDecisionTree merged, 
            VarsToInternalRegs vtir,
            ParValuation ... parval);
    
}
