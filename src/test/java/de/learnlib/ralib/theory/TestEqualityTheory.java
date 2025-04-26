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

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class TestEqualityTheory extends RaLibTestSuite {

    @Test
    public void testLoginExample1() {

        DataWordOracle oracle = new SimulatorOracle(AUTOMATON);

        Map<DataType, Theory> theories = new LinkedHashMap();
        theories.put(T_UID, new IntegerEqualityTheory(T_UID));
        theories.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        MultiTheoryTreeOracle treeOracle = new MultiTheoryTreeOracle(
                oracle, theories, new Constants(), new ConstraintSolver());

        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, BigDecimal.ONE),
                    new DataValue(T_PWD, BigDecimal.ONE)),
                new PSymbolInstance(I_LOGOUT),
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, BigDecimal.ONE),
                    new DataValue(T_PWD, BigDecimal.ONE)));

        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                    new DataValue(T_UID, BigDecimal.ONE),
                    new DataValue(T_PWD, BigDecimal.ONE)));

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, longsuffix);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        SDT res = treeOracle.treeQuery(prefix, symSuffix);
        SDT sdt = res;

        String expectedTree = "[r1, r2]-+\n" +
                "        []-(s1=1[T_uid])\n" +
                "         |    []-(s2=1[T_pwd])\n" +
                "         |     |    []-(s3=1[T_uid])\n" +
                "         |     |     |    []-(s4=1[T_pwd])\n" +
                "         |     |     |     |    [Leaf+]\n" +
                "         |     |     |     +-(s4!=1[T_pwd])\n" +
                "         |     |     |          [Leaf-]\n" +
                "         |     |     +-(s3!=1[T_uid])\n" +
                "         |     |          []-TRUE: s4\n" +
                "         |     |                [Leaf-]\n" +
                "         |     +-(s2!=1[T_pwd])\n" +
                "         |          []-TRUE: s3\n" +
                "         |                []-TRUE: s4\n" +
                "         |                      [Leaf-]\n" +
                "         +-(s1!=1[T_uid])\n" +
                "              []-TRUE: s2\n" +
                "                    []-TRUE: s3\n" +
                "                          []-TRUE: s4\n" +
                "                                [Leaf-]\n";

        String tree = sdt.toString();
        Assert.assertEquals(tree, expectedTree);
        logger.log(Level.FINE, "final SDT: \n{0}", tree);

        Branching b = treeOracle.getInitialBranching(prefix, I_LOGIN, sdt);
        Assert.assertEquals(b.getBranches().toString(),
                "{register[1[T_uid], 1[T_pwd]] login[1[T_uid], 1[T_pwd]]=((1 == 'p1') && (1 == 'p2')), " +
                        "register[1[T_uid], 1[T_pwd]] login[1[T_uid], 2[T_pwd]]=((1 == 'p1') && (1 != 'p2')), " +
                        "register[1[T_uid], 1[T_pwd]] login[2[T_uid], 2[T_pwd]]=((1 != 'p1') && true)}");

        Assert.assertEquals(b.getBranches().size(), 3);
        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }

}
