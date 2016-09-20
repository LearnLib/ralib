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

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.testng.Assert;
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
        
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        importer1.getDataTypes().stream().forEach((t) -> {
            IntegerEqualityTheory theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);
            teachers.put(t, theory);
        });

        DataWordSUL sul = new SimulatorSUL(r1, teachers, importer1.getConstants());
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        
        IOEquivalenceMC mcTest = new IOEquivalenceMC(r1, inputs, ioOracle);        
        DefaultQuery<PSymbolInstance, Boolean> ce = 
                mcTest.findCounterExample(r2, null);
        
        Assert.assertNotNull(ce);
    }
    
    @Test
    public void testLearningWithMCTest() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/sip.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();
        
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            IntegerEqualityTheory theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);
            teachers.put(t, theory);
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);

        ConstraintSolver solver = new SimpleConstraintSolver();
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo = 
                new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> 
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, actions);
        IOEquivalenceMC mcTest = new IOEquivalenceMC(
                model, loader.getActions(), ioOracle);     
                                         
        int check = 0;
        while (true && check < 100) {
            
            check++;
            rastar.learn();
            Hypothesis hyp = rastar.getHypothesis();
              
            System.out.println(hyp);
            
            DefaultQuery<PSymbolInstance, Boolean> ce = 
                    mcTest.findCounterExample(hyp, null);

            //System.out.println(ce);
            
            if (ce == null) {
                break;
            }

            Assert.assertTrue(model.accepts(ce.getInput()) ^ hyp.accepts(ce.getInput()));
            rastar.addCounterexample(ce);
        }

        RegisterAutomaton hyp = rastar.getHypothesis();
        logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
        
        Assert.assertTrue(check <  10);
    }
}
