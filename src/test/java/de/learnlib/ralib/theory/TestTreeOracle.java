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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.Query;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class TestTreeOracle extends RaLibTestSuite {

    @Test
    public void testTreeOracle() {

        // define types
        final DataType userType = new DataType("userType");
        final DataType passType = new DataType("passType");

        // define parameterized symbols
        final ParameterizedSymbol register = new InputSymbol(
                "register", userType, passType);

        final ParameterizedSymbol login = new InputSymbol(
                "login", userType, passType);

        //final ParameterizedSymbol change = new InputSymbol(
        //        "change", new DataType[] {passType});

        //final ParameterizedSymbol logout = new InputSymbol(
        //        "logout", new DataType[] {userType});

        // create prefix: register(falk[userType], secret[passType])
        final Word<PSymbolInstance> prefix = Word.fromLetter(
                new PSymbolInstance(register,
                    new DataValue(userType, BigDecimal.ONE),
                    new DataValue(passType, BigDecimal.ZERO)));

        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(login,
                    new DataValue(userType, BigDecimal.ONE),
                    new DataValue(passType, BigDecimal.ZERO))
                    );

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)

        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);
        //System.out.println("Prefix: " + prefix);
        //System.out.println("Suffix: " + symSuffix);

        // hacked oracle
        DataWordOracle dwOracle = new DataWordOracle() {
            @Override
            public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {

                // given a collection of queries, process each one (with Bool replies)
                for (Query q : clctn) {
                    Word<PSymbolInstance> trace = q.getInput();

                    if (trace.length() != 2) {
                        q.answer(false);
                        continue;
                    }

                    // get the first two symbols in the trace
                    PSymbolInstance a1 = trace.getSymbol(0);
                    PSymbolInstance a2 = trace.getSymbol(1);
                    DataValue[] a1Params = a1.getParameterValues();
                    DataValue[] a2Params = a2.getParameterValues();

                    q.answer( a1.getBaseSymbol().equals(register) &&
                            a2.getBaseSymbol().equals(login) &&
                            Arrays.equals(a1Params, a2Params));
                }
            }
        };

        Theory userTheory = new IntegerEqualityTheory(userType);
        Theory passTheory = new IntegerEqualityTheory(passType);

        Map<DataType, Theory> theories = new LinkedHashMap();
        theories.put(userType, userTheory);
        theories.put(passType, passTheory);

        MultiTheoryTreeOracle treeOracle = new MultiTheoryTreeOracle(
                dwOracle, theories,
                new Constants(), new ConstraintSolver());

        SDT res = treeOracle.treeQuery(prefix, symSuffix);

        String expectedTree = "[r1, r2]-+\n" +
                "        []-(s1=1[userType])\n" +
                "         |    []-(s2=0[passType])\n" +
                "         |     |    [Leaf+]\n" +
                "         |     +-(s2!=0[passType])\n" +
                "         |          [Leaf-]\n" +
                "         +-(s1!=1[userType])\n" +
                "              []-TRUE: s2\n" +
                "                    [Leaf-]\n";

        String tree = res.toString();
        Assert.assertEquals(tree, expectedTree);

        logger.log(Level.FINE, "final SDT: \n{0}", tree);
    }

}
