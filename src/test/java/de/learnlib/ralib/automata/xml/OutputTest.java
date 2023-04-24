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
package de.learnlib.ralib.automata.xml;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import net.automatalib.words.Word;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class OutputTest extends RaLibTestSuite {

    @Test
    public void testModelswithOutput() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/sip.xml");

        de.learnlib.ralib.automata.RegisterAutomaton model =
                loader.getRegisterAutomaton();

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            teachers.put(t, new IntegerEqualityTheory(t));
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        IOOracle ioOracle = new SULOracle(sul, ERROR);


        DataType intType = TestUtil.getType("int", loader.getDataTypes());

        ParameterizedSymbol inv = new InputSymbol(
                "IINVITE", new DataType[] {intType});

        ParameterizedSymbol o100 = new OutputSymbol(
                "O100", new DataType[] {intType});

        DataValue d0 = new DataValue(intType, 0);
        DataValue d1 = new DataValue(intType, 1);

        Word<PSymbolInstance> test1 = Word.fromSymbols(
                new PSymbolInstance(inv, new DataValue[] {d0}),
                new PSymbolInstance(o100, new DataValue[] {d0}));

        Word<PSymbolInstance> test2 = Word.fromSymbols(
                new PSymbolInstance(inv, new DataValue[] {d0}),
                new PSymbolInstance(o100, new DataValue[] {d1}));

        logger.log(Level.FINE, "Test 1: {0}", test1);
        logger.log(Level.FINE, "Test 2: {0}", test2);

        boolean acc1 = model.accepts(test1);
        boolean acc2 = model.accepts(test2);

        logger.log(Level.FINE, "SYS: {0} - {1}", new Object[]{test1, acc1});
        logger.log(Level.FINE, "SYS: {0} - {1}", new Object[]{test2, acc2});

        Word<PSymbolInstance> trace1 = ioOracle.trace(test1);
        Word<PSymbolInstance> trace2 = ioOracle.trace(test2);

        logger.log(Level.FINE, "SUL: {0} - {1}", new Object[]{test1, trace1});
        logger.log(Level.FINE, "SUL: {0} - {1}", new Object[]{test2, trace2});

        Assert.assertTrue(acc1);
        Assert.assertFalse(acc2);

        Assert.assertEquals(test1, trace1);
        Assert.assertNotEquals(test2, trace2);

    }

}
