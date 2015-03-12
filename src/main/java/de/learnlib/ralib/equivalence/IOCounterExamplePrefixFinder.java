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

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class IOCounterExamplePrefixFinder {

    private final ParameterizedSymbol[] inputs;
    private final Constants consts;
    private final Map<DataType, Theory> teachers;
    private final IOOracle sulOracle;
    private Hypothesis hypothesis;

    public IOCounterExamplePrefixFinder(ParameterizedSymbol[] inputs, Constants consts, Map<DataType, Theory> teachers, IOOracle sulOracle) {
        this.inputs = inputs;
        this.consts = consts;
        this.teachers = teachers;
        this.sulOracle = sulOracle;
    }

    public Word<PSymbolInstance> findPrefix(
            Word<PSymbolInstance> ce, RegisterAutomaton hyp) {

        this.hypothesis = (Hypothesis) hyp;

        int prefixLength = 2;

        while (prefixLength < ce.length()) {

            Word<PSymbolInstance> prefix = ce.prefix(prefixLength);

            Word<PSymbolInstance> candidate = sulOracle.trace(prefix);

            System.out.println(candidate);
            if (!hypothesis.accepts(candidate)) {
                System.out.println("Found Prefix CE!!!");
                return candidate;
            }

            prefixLength += 2;
        }

        return ce;
    }
}
