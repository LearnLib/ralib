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

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.io.IOOracle;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class IOCounterExamplePrefixReplacer implements IOCounterExampleOptimizer {

    private final IOOracle sulOracle;

    public IOCounterExamplePrefixReplacer(IOOracle sulOracle) {
        this.sulOracle = sulOracle;
    }

    @Override
    public DefaultQuery<PSymbolInstance, Boolean> optimizeCE(
            Word<PSymbolInstance> ce, Hypothesis hyp) {

        return new DefaultQuery<>(replacePrefix(ce, hyp), true);
    }

    private Word<PSymbolInstance> replacePrefix(
            Word<PSymbolInstance> ce, Hypothesis hypothesis) {

        int suffixLength = ce.length() - 2;
        while (suffixLength > 2) {

            int prefixLength = ce.length() - suffixLength;

            Word<PSymbolInstance> prefix = ce.prefix(prefixLength);
            Word<PSymbolInstance> suffix = ce.suffix(suffixLength);

            prefix = hypothesis.transformAccessSequence(prefix);
            if (prefix == null) {
                return ce;
            }
            Word<PSymbolInstance> candidate = sulOracle.trace(prefix.concat(suffix));

            //System.out.println(candidate);
            if (candidate != null && !hypothesis.accepts(candidate)) {
                //System.out.println("Reduced Prefix!!!");
                ce = candidate;
                //System.out.println("New CE: " + ce);
            }
            suffixLength -= 2;
        }
        return ce;
    }

}
