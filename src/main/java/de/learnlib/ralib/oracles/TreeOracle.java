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
package de.learnlib.ralib.oracles;

import java.util.Map;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
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

    public Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix,
    		SymbolicSuffix suffix, SymbolicDecisionTree sdt, PIV piv);

}
