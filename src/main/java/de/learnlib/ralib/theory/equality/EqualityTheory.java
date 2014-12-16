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

package de.learnlib.ralib.theory.equality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.TreeOracle;
import de.learnlib.ralib.theory.TreeQueryResult;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 * @param <T>
 */
public abstract class EqualityTheory<T> implements Theory<T> {

    //
    
    @Override
    public List<DataValue<T>> getPotential(
            Collection<DataValue<T>> vals) {        
        Set<DataValue<T>> set = new LinkedHashSet<>(vals);
        return new ArrayList<>(set);
    }
    
    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, 
            SymbolicSuffix suffix,
            WordValuation values, 
            ParsInVars piv,
            VarValuation suffixValues,
            TreeOracle oracle) {

        // 1. check degree of freedom for this parameter
        int prefixLength = DataWords.paramLength(DataWords.actsOf(prefix));
        int pId = values.size() + 1;
        //int suffixPId = pId - prefixLength;
        SymbolicDataValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();
        
        boolean free = suffix.getFreeValues().contains(sv);
        List<DataValue<T>> potential = getPotential(
                DataWords.<T>joinValsToSet(
                    DataWords.<T>valSet(prefix, type),
                    suffixValues.<T>values(type)));
                        
        if (!free) {
            DataValue d = suffixValues.get(sv);
            if (d == null) {
                d = getFreshValue( potential );
                //suffixValues.put(sv, d);
            }
            values.put(pId, d);
            
            // call next ...
        } 
        
        // 2. get set of all values ...        
        //suffix.
        
        // 3. compute tree for default case
        
        // 4. create special cases and group cases
        
        // 5. create guards
        
        // 6. create tree
                
        // 7. clean up
        
        throw new UnsupportedOperationException("not implemented");
    }
    
}
