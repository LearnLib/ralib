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
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 * @author falk
 */
public interface Theory {


    /**
     * Returns a fresh data value.
     *
     * @param vals
     * @return a fresh data value of type T
     */
    DataValue getFreshValue(List<DataValue> vals);

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
     * @param constants
     * @param suffixValues map of already instantiated suffix
     * data values (sv -> dv)
     * @param oracle the tree oracle in control of this query
     *
     * @return a symbolic decision tree and updated piv
     */
    SDT treeQuery(
            Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix,
            WordValuation values,
            Constants constants,
            SuffixValuation suffixValues,
            SDTConstructor oracle);


    /**
     * returns all next data values to be tested (for vals).
     *
     * @param vals
     * @return
     */
    Collection<DataValue> getAllNextValues(List<DataValue> vals);

    /**
     * TBD
     *
     * @param prefix
     * @param ps
     * @param pval
     * @param constants
     * @param guard
     * @param param
     * @param oldDvs
     * @return
     */
    DataValue instantiate(Word<PSymbolInstance> prefix,
                          ParameterizedSymbol ps, SuffixValuation pval,
                          Constants constants,
                          SDTGuard guard, SuffixValue param, Set<DataValue> oldDvs);

    SuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts);

    SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior);

    boolean guardRevealsRegister(SDTGuard guard, SymbolicDataValue registers);

}
