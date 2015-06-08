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

import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;
import net.automatalib.words.Word;

import org.testng.annotations.Test;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;


/**
 *
 * @author falk
 */
@Test
public class TestWords {
    
    
    
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
        
        System.out.println(prefix);
        System.out.println(suffix);
        
        SymbolicSuffix sym = new SymbolicSuffix(prefix, suffix);
        
        System.out.println(sym);
    }
    
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
        
        final SymbolicSuffix symSuffix1 = new SymbolicSuffix(prefix1, suffix);
        final SymbolicSuffix symSuffix2 = new SymbolicSuffix(prefix2, symSuffix1);
        
        System.out.println("Prefix 1: " + prefix1);
        System.out.println("Prefix 2: " + prefix2);
        System.out.println("Suffix: " + suffix);
        System.out.println("Sym. Suffix 1: " + symSuffix1);
        System.out.println("Sym. Suffix 2: " + symSuffix2);
        
        
    } 
}
