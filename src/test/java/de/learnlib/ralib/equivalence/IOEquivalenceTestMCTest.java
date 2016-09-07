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
package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Collection;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class IOEquivalenceTestMCTest extends RaLibTestSuite {
    
    @Test
    public void testIOEquivalenceMCTest() {
        
        RegisterAutomatonImporter importer1 = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/login.xml");

        RegisterAutomatonImporter importer2 = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/login_error.xml");
        
        RegisterAutomaton r1 = importer1.getRegisterAutomaton();
        RegisterAutomaton r2 = importer2.getRegisterAutomaton();
        
        Collection<ParameterizedSymbol> inputs = importer1.getActions();
        
        IOEquivalenceMC mcTest = new IOEquivalenceMC(r1, inputs);        
        mcTest.findCounterExample(r2, null);
        
    }
}
