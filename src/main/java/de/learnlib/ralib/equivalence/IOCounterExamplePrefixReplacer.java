/*
 * Copyright (C) 2015 falk.
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
            Word<PSymbolInstance> candidate = sulOracle.trace(prefix.concat(suffix));

            System.out.println(candidate);
            if (!hypothesis.accepts(candidate)) {
                System.out.println("Reduced Prefix!!!");
                ce = candidate;
            }
            suffixLength -= 2;
        }
        return ce;
    }

}
