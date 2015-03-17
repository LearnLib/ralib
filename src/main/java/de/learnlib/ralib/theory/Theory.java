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

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.List;
import net.automatalib.words.Word;


/**
 *
 * @author falk
 * @param <T>
 */
public interface Theory<T> {
      
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
     * @param regGenerator
     * @param vals
     * 
     * @return a symbolic decision tree and updated piv 
     */
    
    public DataValue<T> getFreshValue(List<DataValue<T>> vals);
    
    public SDT treeQuery(
            Word<PSymbolInstance> prefix,             
            SymbolicSuffix suffix,
            WordValuation values, 
            PIV pir,
            Constants constants,
            SuffixValuation suffixValues,
            SDTConstructor oracle);
        
       
    public DataValue instantiate(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, ParValuation pval,
            Constants constants,
            SDTGuard guard, Parameter param);

//    public MultiTheoryBranching updateBranching(Word<PSymbolInstance> prefix, 
//            ParameterizedSymbol ps, MultiTheoryBranching current, 
//            PIV piv, SDTConstructor oracle, SDT... sdts);
    
}
