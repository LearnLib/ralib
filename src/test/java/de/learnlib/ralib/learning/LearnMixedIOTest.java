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
package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibLearningTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public class LearnMixedIOTest extends RaLibLearningTestSuite {

    @Test
    public void learnLoginExampleIO() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/mixed.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();

        
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            TypedTheory th;
            if (t.getName().equals("int")) {
                th = new IntegerEqualityTheory();
            }
            else {
                th = new DoubleInequalityTheory();
            }
            th.setType(t);
            th.setUseSuffixOpt(true);
            teachers.put(t, th);
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);

        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        IOOracle ioOracle = new BasicSULOracle(sul, ERROR);
        
        super.setHypValidator((hyp)
        		-> {
        			IOEquivalenceTest checker = new IOEquivalenceTest(
        	                model, teachers, consts, true, actions);
        	        
        	        logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
        	        logger.log(Level.FINE,"Resets: " + sul.getResets());  
        	        Assert.assertNull(checker.findCounterExample(hyp, null));
        		});
        super.runIOLearningExperiments(sul, teachers, consts, false, jsolv, actions, ERROR);  
    }
}
