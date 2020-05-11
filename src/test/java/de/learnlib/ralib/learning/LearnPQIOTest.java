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
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;

/**
 * TODO It would be nice if we could have an extended test run wherein each learning test is performed several times with different seeds. 
 */
public class LearnPQIOTest extends RaLibLearningTestSuite {

    @Test
    public void learnLoginExampleIO() {
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = 
                new DoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE);
        
        dit.setUseSuffixOpt(false);
        teachers.put(PriorityQueueSUL.DOUBLE_TYPE, dit);
        
        final Constants consts = new Constants();

        PriorityQueueSUL sul = new PriorityQueueSUL(3);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
        
        super.setHypValidator((hyp) -> {
              RegisterAutomatonImporter imp = TestUtil.getLoader(
                      "/de/learnlib/ralib/automata/xml/pq3.xml");

              IOEquivalenceTest checker = new IOEquivalenceTest(
                      imp.getRegisterAutomaton(), teachers, consts, true,
                      sul.getActionSymbols()
              );

              logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
              logger.log(Level.FINE, "Resets: " + sul.getResets());        
              Assert.assertNull(checker.findCounterExample(hyp, null));
        });
        
        super.runIOLearningExperiments(sul, teachers, consts, false, jsolv, sul.getActionSymbols(), PriorityQueueSUL.ERROR);
    }
}
