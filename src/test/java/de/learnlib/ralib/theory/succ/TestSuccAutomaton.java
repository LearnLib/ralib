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
package de.learnlib.ralib.theory.succ;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoaderTest;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class TestSuccAutomaton {
 
    @Test    
    public void testSuccAutomaton() {
        
        RegisterAutomatonImporter loader = new RegisterAutomatonImporter(
                RegisterAutomatonLoaderTest.class.getResourceAsStream(
                        "/de/learnlib/ralib/automata/xml/seqnr.xml"));

        RegisterAutomaton model = loader.getRegisterAutomaton();
        System.out.println("SYS:------------------------------------------------");
        System.out.println(model);
        System.out.println("----------------------------------------------------");

        ParameterizedSymbol[] inputs = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol inext = inputs[0];
        ParameterizedSymbol ook = inputs[1];
        ParameterizedSymbol onok = inputs[2];
        
        System.out.println(inext + " : " + ook + " : " + onok);
        
        DataType __int = inext.getPtypes()[0];
        
        Word<PSymbolInstance> test1 = Word.fromSymbols(
                new PSymbolInstance(inext, new DataValue(__int, 1)),
                new PSymbolInstance(ook),
                new PSymbolInstance(inext, new DataValue(__int, 1)),
                new PSymbolInstance(onok)
        );

        Word<PSymbolInstance> test2 = Word.fromSymbols(
                new PSymbolInstance(inext, new DataValue(__int, 1)),
                new PSymbolInstance(ook),
                new PSymbolInstance(inext, new DataValue(__int, 2)),
                new PSymbolInstance(ook)
        );
        
        System.out.println(test1);
        Assert.assertTrue(model.accepts(test1));

        System.out.println(test2);
        Assert.assertTrue(model.accepts(test2));
    }
    
}
