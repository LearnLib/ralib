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

import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.example.sdts.LoginExampleTreeOracle;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class CellTest extends RaLibTestSuite {

    @Test
    public void testCellCreation() {

        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                    new DataValue(T_UID, BigDecimal.ONE),
                    new DataValue(T_PWD, BigDecimal.ONE)),
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, new BigDecimal(2)),
                    new DataValue(T_PWD, new BigDecimal(2))));

        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, BigDecimal.ONE),
                    new DataValue(T_PWD, BigDecimal.ONE)),
                new PSymbolInstance(I_LOGOUT));


        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)

        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, longsuffix);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        LoggingOracle oracle = new LoggingOracle(new LoginExampleTreeOracle());

        Cell c = Cell.computeCell(oracle, prefix, symSuffix);

        logger.log(Level.FINE, "Memorable: {0}", Arrays.toString(c.getMemorable().toArray()));
        logger.log(Level.FINE, "Cell: {0}", c.toString());

        Assert.assertTrue(c.couldBeEquivalentTo(c));
        Assert.assertTrue(c.isEquivalentTo(c, new VarMapping()));

    }

}
