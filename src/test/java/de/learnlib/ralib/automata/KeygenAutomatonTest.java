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
import static de.learnlib.ralib.example.keygen.MapAutomatonExample.*;
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
public class KeygenAutomatonTest {

    public KeygenAutomatonTest() {
    }

    @Test
    public void testHasTrace() {
    
        RegisterAutomaton ra = AUTOMATON;
        
        System.out.println(ra);
        
        Word<PSymbolInstance> test1 = Word.epsilon();        
        System.out.println("test1: " + test1);     
        Assert.assertTrue(ra.accepts(test1));

        Word<PSymbolInstance> test2 = Word.epsilon();        
        test2 = test2.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test2 = test2.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        
        System.out.println("test2: " + test2);     
        Assert.assertTrue(ra.accepts(test2));        
        
        Word<PSymbolInstance> test3 = Word.epsilon();        
        test3 = test3.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test3 = test3.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        test3 = test3.append(new PSymbolInstance(I_GET, new DataValue[] { new DataValue(T_KEY, 1)} ));
        test3 = test3.append(new PSymbolInstance(O_GET, new DataValue[] { new DataValue(T_VAL, 1) }));
        
        System.out.println("test3: " + test3);     
        Assert.assertTrue(ra.accepts(test3));          

        Word<PSymbolInstance> test4 = Word.epsilon();        
        test4 = test4.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test4 = test4.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        test4 = test4.append(new PSymbolInstance(I_GET, new DataValue[] { new DataValue(T_KEY, 2)} ));
        test4 = test4.append(new PSymbolInstance(O_NULL, new DataValue[] { }));
        
        System.out.println("test4: " + test4);     
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
                
        System.out.println("test5: " + test5);     
        Assert.assertTrue(ra.accepts(test5));  
        
        Word<PSymbolInstance> test6 = Word.epsilon();        
        test6 = test6.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test6 = test6.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        test6 = test6.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 2)} ));
        test6 = test6.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 2) }));
        test6 = test6.append(new PSymbolInstance(I_GET, new DataValue[] { new DataValue(T_KEY, 3)} ));
                
        System.out.println("test6: " + test6);     
        Assert.assertTrue(!ra.accepts(test6));  

        Word<PSymbolInstance> test7 = Word.epsilon();        
        test7 = test7.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 1)} ));
        test7 = test7.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
        test7 = test7.append(new PSymbolInstance(I_PUT, new DataValue[] { new DataValue(T_VAL, 2)} ));
        test7 = test7.append(new PSymbolInstance(O_PUT, new DataValue[] { new DataValue(T_KEY, 1) }));
                
        System.out.println("test7: " + test7);     
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
