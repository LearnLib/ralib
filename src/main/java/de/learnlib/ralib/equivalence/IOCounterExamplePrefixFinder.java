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
package de.learnlib.ralib.equivalence;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class IOCounterExamplePrefixFinder implements IOCounterExampleOptimizer {

    private final IOOracle sulOracle;

    public IOCounterExamplePrefixFinder(IOOracle sulOracle) {
        this.sulOracle = sulOracle;
    }

    @Override
    public DefaultQuery<PSymbolInstance, Boolean> optimizeCE
        (Word<PSymbolInstance> ce, Hypothesis hypothesis) {
       return new DefaultQuery<>(findPrefix(ce, hypothesis), true);
    }
    
    private Word<PSymbolInstance> findPrefix(
            Word<PSymbolInstance> ce, Hypothesis hypothesis) {

        int prefixLength = 2;

        while (prefixLength < ce.length()) {

            Word<PSymbolInstance> prefix = ce.prefix(prefixLength);
            Word<PSymbolInstance> candidate = sulOracle.trace(prefix);

           //System.out.println(candidate);
            if (!hypothesis.accepts(candidate)) {
                //System.out.println("Found Prefix CE!!!");
                return candidate;
            }
            prefixLength += 2;
        }
        return ce;
    }

}
