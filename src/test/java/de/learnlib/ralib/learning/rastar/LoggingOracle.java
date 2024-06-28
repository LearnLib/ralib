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
package de.learnlib.ralib.learning.rastar;

import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
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

    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, SymbolicDecisionTree ... sdts) {

        //System.out.println("QUERY (initial branching): " + prefix + " and " + ps);
        return treeoracle.getInitialBranching(prefix, ps, piv, sdts);
    }

    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, Branching current,
            PIV piv, SymbolicDecisionTree ... sdts) {

        //System.out.println("QUERY (update branching): " + prefix +
        //        " and " + ps + " with " + sdts.length + " sdts");
        Branching b = treeoracle.updateBranching(prefix, ps, current, piv, sdts);
        //System.out.println(b.getBranches().size());
        return b;
    }

    @Override
    public Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix,
    		SymbolicSuffix suffix, SymbolicDecisionTree sdt, PIV piv) {
    	return treeoracle.instantiate(prefix, suffix, sdt, piv);
    }

    @Override
    public SymbolicSuffixRestrictionBuilder getRestrictionBuilder() {
    	return new SymbolicSuffixRestrictionBuilder(new Constants());
    }
}
