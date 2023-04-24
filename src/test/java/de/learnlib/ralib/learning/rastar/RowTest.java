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

import de.learnlib.ralib.RaLibTestSuite;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.Arrays;

import net.automatalib.words.Word;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.example.sdts.LoginExampleTreeOracle;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.logging.Level;

/**
 *
 * @author falk
 */
public class RowTest extends RaLibTestSuite {

    @Test
    public void testRowEquivalence() {

        final Word<PSymbolInstance> prefix1 = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_REGISTER,
                    new DataValue(T_UID, 2),
                    new DataValue(T_PWD, 2)));

        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, 2),
                    new DataValue(T_PWD, 2)));

        final Word<PSymbolInstance> suffix1 = Word.epsilon();
        final Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));

        final SymbolicSuffix symSuffix1 = new SymbolicSuffix(prefix1, suffix1);
        final SymbolicSuffix symSuffix2 = new SymbolicSuffix(prefix1, suffix2);

        SymbolicSuffix[] suffixes = new SymbolicSuffix[] {symSuffix1, symSuffix2};
        logger.log(Level.FINE, "Suffixes: {0}", Arrays.toString(suffixes));

        LoggingOracle oracle = new LoggingOracle(new LoginExampleTreeOracle());

        Row r1 = Row.computeRow(oracle, prefix1, Arrays.asList(suffixes), false);
        Row r2 = Row.computeRow(oracle, prefix2, Arrays.asList(suffixes), false);

        VarMapping renaming = null;
        for (VarMapping map : new PIVRemappingIterator(r1.getParsInVars(), r2.getParsInVars())) {
            if (r1.isEquivalentTo(r2, map)) {
                renaming = map;
                break;
            }
        }

        Assert.assertNotNull(renaming);
        Assert.assertTrue(r1.couldBeEquivalentTo(r2));
        Assert.assertTrue(r1.isEquivalentTo(r2, renaming));

    }

}
