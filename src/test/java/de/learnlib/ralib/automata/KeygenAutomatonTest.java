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
package de.learnlib.ralib.automata;

import de.learnlib.ralib.RaLibTestSuite;
import static de.learnlib.ralib.example.keygen.MapAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.keygen.MapAutomatonExample.I_GET;
import static de.learnlib.ralib.example.keygen.MapAutomatonExample.I_PUT;
import static de.learnlib.ralib.example.keygen.MapAutomatonExample.O_GET;
import static de.learnlib.ralib.example.keygen.MapAutomatonExample.O_NULL;
import static de.learnlib.ralib.example.keygen.MapAutomatonExample.O_PUT;
import static de.learnlib.ralib.example.keygen.MapAutomatonExample.T_KEY;
import static de.learnlib.ralib.example.keygen.MapAutomatonExample.T_VAL;
import net.automatalib.words.Word;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.logging.Level;

/**
 *
 * @author falk
 */
public class KeygenAutomatonTest extends RaLibTestSuite {

    public KeygenAutomatonTest() {
    }

    @Test
    public void testHasTrace() {

        RegisterAutomaton ra = AUTOMATON;

        //System.out.println(ra);

        Word<PSymbolInstance> test1 = Word.epsilon();
        logger.log(Level.FINE, "test1: {0}", test1);
        Assert.assertTrue(ra.accepts(test1));

        Word<PSymbolInstance> test2 = Word.epsilon();
        test2 = test2.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test2 = test2.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));

        logger.log(Level.FINE, "test2: {0}", test2);
        Assert.assertTrue(ra.accepts(test2));

        Word<PSymbolInstance> test3 = Word.epsilon();
        test3 = test3.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test3 = test3.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        test3 = test3.append(new PSymbolInstance(I_GET, new DataValue[] { new DataValue(T_KEY, 1)} ));
        test3 = test3.append(new PSymbolInstance(O_GET, new DataValue[] { new DataValue(T_VAL, 1) }));

        logger.log(Level.FINE, "test3: {0}", test3);
        Assert.assertTrue(ra.accepts(test3));

        Word<PSymbolInstance> test4 = Word.epsilon();
        test4 = test4.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test4 = test4.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        test4 = test4.append(new PSymbolInstance(I_GET, new DataValue[] { new DataValue(T_KEY, 2)} ));
        test4 = test4.append(new PSymbolInstance(O_NULL, new DataValue[] { }));

        logger.log(Level.FINE, "test4: {0}", test4);
        Assert.assertTrue(ra.accepts(test4));

        Word<PSymbolInstance> test5 = Word.epsilon();
        test5 = test5.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test5 = test5.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        test5 = test5.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 2)} ));
        test5 = test5.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 2) }));
        test5 = test5.append(new PSymbolInstance(I_GET, new DataValue[] { new DataValue(T_KEY, 1)} ));
        test5 = test5.append(new PSymbolInstance(O_GET, new DataValue[] { new DataValue(T_VAL, 1) }));
        test5 = test5.append(new PSymbolInstance(I_GET, new DataValue[] { new DataValue(T_KEY, 2)} ));
        test5 = test5.append(new PSymbolInstance(O_GET, new DataValue[] { new DataValue(T_VAL, 2) }));

        logger.log(Level.FINE, "test5: {0}", test5);
        Assert.assertTrue(ra.accepts(test5));

        Word<PSymbolInstance> test6 = Word.epsilon();
        test6 = test6.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test6 = test6.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        test6 = test6.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 2)} ));
        test6 = test6.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 2) }));
        test6 = test6.append(new PSymbolInstance(I_GET, new DataValue[] { new DataValue(T_KEY, 3)} ));

        logger.log(Level.FINE, "test6: {0}", test6);
        Assert.assertTrue(!ra.accepts(test6));

        Word<PSymbolInstance> test7 = Word.epsilon();
        test7 = test7.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test7 = test7.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        test7 = test7.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 2)} ));
        test7 = test7.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));

        logger.log(Level.FINE, "test7: {0}", test7);
        Assert.assertTrue(!ra.accepts(test7));
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }
}
