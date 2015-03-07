/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoader;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoaderTest;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.annotations.Test;

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
        
         RegisterAutomatonLoader loader = new RegisterAutomatonLoader(
                RegisterAutomatonLoaderTest.class.getResourceAsStream(
                        "/de/learnlib/ralib/automata/xml/login.xml"));
         
         RegisterAutomaton model = loader.getRegisterAutomaton();         
         ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                 new ParameterizedSymbol[] {});
              
         Constants consts = loader.getConstants();
         
         Map<DataType, Theory> teachers = new HashMap<DataType, Theory>();
         for (final DataType t : loader.getDataTypes()) {
             teachers.put(t, new EqualityTheory() {
                 @Override
                 public DataValue getFreshValue(List vals) {
                     return new DataValue(t, vals.size());
                 }
             });
         }
         
         DataWordSUL sul = new SimulatorSUL(model, teachers, consts, inputs);
         
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
         
         iowalk.findCounterExample(model);
     }
}
