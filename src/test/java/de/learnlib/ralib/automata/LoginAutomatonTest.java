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

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;
import net.automatalib.words.Word;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 *
 * @author falk
 */
public class LoginAutomatonTest {

    public LoginAutomatonTest() {
    }

    @Test
    public void testHasTrace() {
    
        RegisterAutomaton ra = AUTOMATON;
        System.out.println(ra);        
        
        Word<PSymbolInstance> test1 = Word.epsilon();        
        System.out.println("test1: " + test1);     
        Assert.assertFalse(ra.accepts(test1));

        Word<PSymbolInstance> test2 = Word.epsilon();        
        test2 = test2.append(new PSymbolInstance(I_REGISTER, new DataValue[] {
                new DataValue(T_UID, 1), new DataValue(T_PWD, 2)}));
        test2 = test2.append(new PSymbolInstance(I_LOGIN, new DataValue[] {
                new DataValue(T_UID, 1), new DataValue(T_PWD, 2)}));
        
        System.out.println("test2: " + test2);     
        Assert.assertTrue(ra.accepts(test2));        
        
        Word<PSymbolInstance> test3 = Word.epsilon();        
        test3 = test3.append(new PSymbolInstance(I_REGISTER, new DataValue[] {
                new DataValue(T_UID, 1), new DataValue(T_PWD, 2)}));
        test3 = test3.append(new PSymbolInstance(I_LOGIN, new DataValue[] {
                new DataValue(T_UID, 1), new DataValue(T_PWD, 3)}));
        
        System.out.println("test3: " + test3);     
        Assert.assertFalse(ra.accepts(test3));          
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
