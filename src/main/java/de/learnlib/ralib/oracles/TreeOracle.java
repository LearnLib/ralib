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

import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 * A tree oracle is the connection between the learning algorithm
 * and theories for data values.
 *
 * @author falk
 */
public interface TreeOracle {

    /**
     * performs a tree query, returning a SDT
     * an an Assignment of registers of this tree with parameters
     * of the prefix.
     *
     * @param prefix
     * @param suffix
     * @return
     */
    SDT treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix);

    /**
     * Computes a Branching from a set of SDTs.
     *
     * @param prefix
     * @param ps
     * @param sdts
     * @return
     */
    Branching getInitialBranching(Word<PSymbolInstance> prefix,
                                  ParameterizedSymbol ps, SDT... sdts);

    /**
     * Updates and extends an existing Branching
     * from a set of SDTs.
     *
     * @param prefix
     * @param ps
     * @param current
     * @param sdts
     * @return
     */
    Branching updateBranching(Word<PSymbolInstance> prefix,
                              ParameterizedSymbol ps, Branching current, SDT... sdts);

    Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix,
                                                    SymbolicSuffix suffix, SDT sdt);

    SymbolicSuffixRestrictionBuilder getRestrictionBuilder();

}
