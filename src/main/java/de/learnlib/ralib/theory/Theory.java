/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.theory;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.mapper.ValueMapper;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;


/**
 *
 * @author falk
 * @param <T>
 */
public interface Theory<T> {
      

    /**
     * Returns a fresh data value.
     * 
     * @param vals
     * @return a fresh data value of type T 
     */
    public DataValue<T> getFreshValue(List<DataValue<T>> vals);
    
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
     * @param constants 
     * @param suffixValues map of already instantiated suffix 
     * data values (sv -> dv)
     * @param oracle the tree oracle in control of this query
     * 
     * @return a symbolic decision tree and updated piv 
     */    
    public SDT treeQuery(
            Word<PSymbolInstance> prefix,             
            GeneralizedSymbolicSuffix suffix,
            WordValuation values, 
            PIV piv,
            Constants constants,
            SuffixValuation suffixValues,
            SDTConstructor oracle,
            IOOracle traceOracle);
 
    /**
     * returns all next data values to be tested (for vals).
     * 
     * @param vals
     * @return 
     */
    public Collection<DataValue<T>> getAllNextValues(List<DataValue<T>> vals);
    
    /**
     * returns an ordered list of potential values
     * @param vals
     * @return
     */
    public default List<DataValue<T>> getPotential(List<DataValue<T>> vals) {
    	return Collections.emptyList();
    }
    
    public default ValueMapper<T> getValueMapper() {
    	return null;
    }
    
    /**
     * Finds a model for a guard (data values). Tries to reuse
     * known (old) data values.
     * 
     * @param prefix
     * @param ps
     * @param piv
     * @param pval
     * @param constants
     * @param guard
     * @param param
     * @param oldDvs
     * @return 
     */
    public DataValue instantiate(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, ParValuation pval,
            Constants constants,
            SDTGuard guard, Parameter param, Set<DataValue<T>> oldDvs, boolean useSolver);
    
    /**
     * return set of relations that hold between left and right
     * 
     * @param left
     * @param right
     * @return 
     */
    public List<EnumSet<DataRelation>> getRelations(List<DataValue<T>> left, DataValue<T> right);
    
    /**
     * return set of all recognized relations
     */
    public EnumSet<DataRelation> recognizedRelations();
    
    /**
     * Returns disjunction and conjunction logic for sdt guards.
     */
    public SDTGuardLogic getGuardLogic();
}
