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
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SDTRelabeling;
import de.learnlib.ralib.data.util.RemappingIterator;
import de.learnlib.ralib.example.sdts.LoginExampleTreeOracle;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class RowTest extends RaLibTestSuite {

    @Test
    public void testRowEquivalence() {

        final Word<PSymbolInstance> prefix1 = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, BigDecimal.ONE),
                    new DataValue(T_PWD, BigDecimal.ONE)),
                new PSymbolInstance(I_REGISTER,
                    new DataValue(T_UID, new BigDecimal(2)),
                    new DataValue(T_PWD, new BigDecimal(2))));

        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                    new DataValue(T_UID, BigDecimal.ONE),
                    new DataValue(T_PWD, BigDecimal.ONE)),
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, new BigDecimal(2)),
                    new DataValue(T_PWD, new BigDecimal(2))));

        final Word<PSymbolInstance> suffix1 = Word.epsilon();
        final Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN,
                    new DataValue(T_UID, BigDecimal.ONE),
                    new DataValue(T_PWD, BigDecimal.ONE)));

        final SymbolicSuffix symSuffix1 = new SymbolicSuffix(prefix1, suffix1);
        final SymbolicSuffix symSuffix2 = new SymbolicSuffix(prefix1, suffix2);

        SymbolicSuffix[] suffixes = new SymbolicSuffix[] {symSuffix1, symSuffix2};
        logger.log(Level.FINE, "Suffixes: {0}", Arrays.toString(suffixes));

        LoggingOracle oracle = new LoggingOracle(new LoginExampleTreeOracle());

        Row r1 = Row.computeRow(oracle, prefix1, Arrays.asList(suffixes), false);
        Row r2 = Row.computeRow(oracle, prefix2, Arrays.asList(suffixes), false);

        Bijection<DataValue> renaming = null;
        for (Bijection<DataValue> map : new RemappingIterator<>(r1.memorableValues(), r2.memorableValues())) {
            if (r2.isEquivalentTo(r1, SDTRelabeling.fromBijection(map))) {
                renaming = map;
                break;
            }
        }

        Assert.assertNotNull(renaming);
        Assert.assertTrue(r1.couldBeEquivalentTo(r2));
        Assert.assertTrue(r2.isEquivalentTo(r1, SDTRelabeling.fromBijection(renaming)));

    }

}
