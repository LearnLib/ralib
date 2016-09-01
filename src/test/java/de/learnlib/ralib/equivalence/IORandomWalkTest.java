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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.ParameterizedSymbol;
import org.testng.Assert;

/**
 *
 * @author falk
 */
public class IORandomWalkTest extends RaLibTestSuite {

    @Test
    public void testRandomWalk() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/login.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();
        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        Constants consts = loader.getConstants();

        Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            teachers.put(t, new IntegerEqualityTheory(t));
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);

        IORandomWalk iowalk = new IORandomWalk(new Random(0),
                sul,
                false, // do not draw symbols uniformly 
                0.1, // reset probability 
                0.5, // prob. of choosing a fresh data value
                1000, // 1000 runs 
                20, // max depth
                consts,
                true, // reset runs 
                teachers,
                inputs);

        Object o = iowalk.findCounterExample(model, null);        
        Assert.assertNull(o);
    }
}
