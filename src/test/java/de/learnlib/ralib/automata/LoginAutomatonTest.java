/*
 * Copyright (C) 2014 falk.
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
package de.learnlib.ralib.automata;

import de.learnlib.ralib.data.DataValue;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
import de.learnlib.ralib.words.PSymbolInstance;
import junit.framework.Assert;
import net.automatalib.words.Word;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
        Assert.assertTrue(ra.accepts(test1));

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
        Assert.assertTrue(ra.accepts(test3));          
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
