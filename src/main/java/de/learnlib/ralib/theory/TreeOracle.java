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

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.VarsToInternalRegs;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.sul.DataWordOracle;
import de.learnlib.ralib.trees.SDTLeaf;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Collections;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class TreeOracle {
        
    private final DataWordOracle oracle;
    
    private final Map<DataType, Theory> teachers;

    public TreeOracle(DataWordOracle oracle, Map<DataType, Theory> teachers) {
        this.oracle = oracle;
        this.teachers = teachers;
    }
    
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        
        return treeQuery(prefix, suffix, 
                new WordValuation(), new ParsInVars(), new VarValuation());
    }
    
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            WordValuation values, ParsInVars piv, 
            VarValuation suffixValues) {
        
        if (values.size() == DataWords.paramLength(suffix.getActions())) {
            
            Word<PSymbolInstance> concSuffix = DataWords.instantiate(
                    suffix.getActions(), values);
            
            Word<PSymbolInstance> trace = prefix.concat(concSuffix);
            DefaultQuery<PSymbolInstance, Boolean> query = 
                    new DefaultQuery<>(prefix, concSuffix);
            oracle.processQueries(Collections.singletonList(query));
            
            return new TreeQueryResult(piv, new VarsToInternalRegs(),
                    query.getOutput() ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING);
        }
        
        SymbolicDataValue sd = suffix.getDataValue(values.size() + 1);
        Theory teach = teachers.get(sd.getType());
        return teach.treeQuery(prefix, suffix, values, piv, suffixValues, this);
    }    
    
//    public Word<PSymbolInstance> getDefaultExtension(
//            Word<PSymbolInstance> prefix, ParameterizedSymbol ps, )
    
}
