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
import java.util.Random;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.HypVerify;
import de.learnlib.ralib.equivalence.IOCounterExampleLoopRemover;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.CanonizingIOCacheOracle;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.CanonizingSULOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 * TODO It would be nice if we could have an extended test run wherein each learning test is performed several times with different seeds. 
 */
public class LearnPQIOTest extends RaLibTestSuite {

    @Test
    public void learnLoginExampleIO() {
        long seed = -4750580074638681518L; // buggy seed
        
        logger.log(Level.FINE, "SEED={0}", seed);
        final Random random = new Random(seed);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = 
                new DoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE);
        
        dit.setUseSuffixOpt(false);
        teachers.put(PriorityQueueSUL.DOUBLE_TYPE, dit);
                
        
        final Constants consts = new Constants();

        PriorityQueueSUL sul = new PriorityQueueSUL(3);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
        
        IOOracle ioOracle = new CanonizingSULOracle(sul, PriorityQueueSUL.ERROR, new SymbolicTraceCanonizer(teachers, consts));
        
        CanonizingIOCacheOracle ioCache = new CanonizingIOCacheOracle(ioOracle);
        
        IOFilter ioFilter = new IOFilter(ioCache, sul.getInputSymbols());
      
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, ioCache, teachers, consts, jsolv);

        MultiTheorySDTLogicOracle mlo
                = new MultiTheorySDTLogicOracle(consts, jsolv);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp)
                -> TestUtil.createSimulatorMTOWithFreshValueSupport(hyp, teachers, consts, jsolv);
        
        RaStar rastar = new RaStar(mto, hypFactory, mlo,
                consts, true, teachers, jsolv,
                sul.getActionSymbols());

        IORandomWalk iowalk = new IORandomWalk(random,
                sul,
                false, // do not draw symbols uniformly 
                0.1, // reset probability 
                0.5, // prob. of choosing a fresh data value
                1000, // 1000 runs 
                100, // max depth
                consts,
                false, // reset runs 
                teachers,
                sul.getInputSymbols());

        IOCounterExampleLoopRemover loops = new IOCounterExampleLoopRemover(ioOracle);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);
        DefaultQuery<PSymbolInstance, Boolean> ce = null;

        int check = 0;
        while (true && check < 100) {
            check++;	
            System.out.println("seed: " + seed);
            rastar.learn();        
            Hypothesis hyp = rastar.getHypothesis();
            if (ce != null) {
            	// the last CE should not be a CE for the current hypothesis
            	Assert.assertFalse(HypVerify.isCEForHyp(ce, hyp));
            }
  
            ce = iowalk.findCounterExample(hyp, null);
         
            //System.out.println("CE: " + ce);
            if (ce == null) {
                break;
            }
            
            ce = loops.optimizeCE(ce.getInput(), hyp);
            Assert.assertTrue(HypVerify.isCEForHyp(ce, hyp));
            ce = asrep.optimizeCE(ce.getInput(), hyp);
            Assert.assertTrue(HypVerify.isCEForHyp(ce, hyp));
            ce = pref.optimizeCE(ce.getInput(), hyp);
            Assert.assertTrue(HypVerify.isCEForHyp(ce, hyp));
            rastar.addCounterexample(ce);
            System.out.println("CE: " + ce);
            System.out.println("CE number: " + check);
        }

        RegisterAutomaton hyp = rastar.getHypothesis();
        RegisterAutomatonImporter imp = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/pq3.xml");

        IOEquivalenceTest checker = new IOEquivalenceTest(
                imp.getRegisterAutomaton(), teachers, consts, true,
                sul.getActionSymbols()
        );

        logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
        logger.log(Level.FINE, "Resets: " + sul.getResets());        
        Assert.assertNull(checker.findCounterExample(hyp, null));
    }
}
