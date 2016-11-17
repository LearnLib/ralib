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
package de.learnlib.ralib.words;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import net.automatalib.words.Word;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 *
 * @author falk
 */
public class TestWords extends RaLibTestSuite {
    
   @Test 
    public void testSymbolicSuffix1() {
        
        DataType intType = new DataType("int", int.class);
                
        ParameterizedSymbol a = new InputSymbol("a", new DataType[]{intType});
        
        DataValue<Integer> i1 = new DataValue(intType, 1);
        DataValue<Integer> i2 = new DataValue(intType, 2);
        DataValue<Integer> i3 = new DataValue(intType, 3);
        
        PSymbolInstance[] prefixSymbols = new PSymbolInstance[] {
            new PSymbolInstance(a, i1),
            new PSymbolInstance(a, i3)
        };
        
        PSymbolInstance[] suffixSymbols = new PSymbolInstance[] {
            new PSymbolInstance(a, i1),
            new PSymbolInstance(a, i2),
            new PSymbolInstance(a, i2),
            new PSymbolInstance(a, i1)
        };
        
        Word<PSymbolInstance> prefix = Word.fromSymbols(prefixSymbols);
        Word<PSymbolInstance> suffix = Word.fromSymbols(suffixSymbols);
        
       logger.log(Level.FINE, "Prefix: {0}", prefix);
       logger.log(Level.FINE, "Suffix: {0}", suffix);
        
       Map<DataType, Theory> teachers = new HashMap<>();
       teachers.put(intType, new IntegerEqualityTheory(intType));
       
        GeneralizedSymbolicSuffix sym = new GeneralizedSymbolicSuffix(
                prefix, suffix, new Constants(), teachers);
        
       logger.log(Level.FINE, "Symbolic Suffix: {0}", sym);
       // String expString = "[s1, s3]((a[s1] a[s2] a[s2] a[s3]))";
       String expString = "a[s1] a[s2] a[s3] a[s4]_"
               + "P[[DEFAULT, EQ], [DEFAULT], [DEFAULT], [DEFAULT, EQ]]_"
               + "S[[], [[DEFAULT]], [[DEFAULT], [EQ]], [[EQ], [DEFAULT], [DEFAULT]]]";
        Assert.assertEquals(sym.toString(), expString);
    }
    
    @Test
    public void testSymbolicSuffix2() {   

        final Word<PSymbolInstance> prefix1 = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));    
        
        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGOUT));          
           
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));

       Map<DataType, Theory> teachers = new HashMap<>();
       teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
       teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));
       
        final GeneralizedSymbolicSuffix symSuffix1 = 
                new GeneralizedSymbolicSuffix(prefix1, suffix,
                        new Constants(), teachers);
        final GeneralizedSymbolicSuffix symSuffix2 = 
                new GeneralizedSymbolicSuffix(prefix2, symSuffix1,
                        new Constants(), teachers);
        
        logger.log(Level.FINE, "Prefix 1: {0}", prefix1);
        logger.log(Level.FINE, "Prefix 2: {0}", prefix2);
        logger.log(Level.FINE, "Suffix: {0}", suffix);
        logger.log(Level.FINE, "Sym. Suffix 1: {0}", symSuffix1);
        logger.log(Level.FINE, "Sym. Suffix 2: {0}", symSuffix2);
        
        String expected1 = "login[s1, s2]_P[[DEFAULT, EQ], [DEFAULT, EQ]]_S[[], []]";
        String expected2 = "logout[] login[s1, s2]_P[[DEFAULT, EQ], [DEFAULT, EQ]]_S[[], []]";
        
        Assert.assertEquals(symSuffix1.toString(), expected1);
        Assert.assertEquals(symSuffix2.toString(), expected2);
    } 
}
