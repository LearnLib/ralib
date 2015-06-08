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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoaderTest;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public class IORandomWalkTest {
    
     @Test
     public void testRandomWalk() {
        
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.ALL);
        }
        
         RegisterAutomatonImporter loader = new RegisterAutomatonImporter(
                RegisterAutomatonLoaderTest.class.getResourceAsStream(
                        "/de/learnlib/ralib/automata/xml/login.xml"));
         
         RegisterAutomaton model = loader.getRegisterAutomaton();         
         ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                 new ParameterizedSymbol[] {});
              
         Constants consts = loader.getConstants();
         
         Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
         for (final DataType t : loader.getDataTypes()) {
             teachers.put(t, new EqualityTheory() {
                 @Override
                 public DataValue getFreshValue(List vals) {
                     return new DataValue(t, vals.size());
                 }
             });
         }
         
         DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
         
         IORandomWalk iowalk = new IORandomWalk(new Random(0), 
                 sul, 
                 false, // do not draw symbols uniformly 
                 0.1,   // reset probability 
                 0.5,   // prob. of choosing a fresh data value
                 1000,  // 1000 runs 
                 20,    // max depth
                 consts, 
                 true,  // reset runs 
                 teachers, 
                 inputs);
         
         iowalk.findCounterExample(model, null);
     }
}
