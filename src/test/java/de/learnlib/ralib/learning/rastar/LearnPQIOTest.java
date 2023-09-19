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
package de.learnlib.ralib.learning.rastar;

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
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 *
 * @author falk
 */
public class LearnPQIOTest extends RaLibTestSuite {

    @Test
    public void learnLoginExampleIO() {

        long seed = -4750580074638681533L;
        logger.log(Level.FINE, "SEED={0}", seed);
        final Random random = new Random(seed);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(PriorityQueueSUL.DOUBLE_TYPE,
                new DoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE));

        final Constants consts = new Constants();

        PriorityQueueSUL sul = new PriorityQueueSUL();
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
        IOOracle ioOracle = new SULOracle(sul, PriorityQueueSUL.ERROR);

        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                ioOracle, teachers, consts, jsolv, sul.getInputSymbols());

        MultiTheorySDTLogicOracle mlo
                = new MultiTheorySDTLogicOracle(consts, jsolv);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp)
                -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, jsolv);

        RaStar rastar = new RaStar(mto, hypFactory, mlo,
                consts, true, sul.getActionSymbols());

        IORandomWalk iowalk = new IORandomWalk(random,
                sul,
                false, // do not draw symbols uniformly
                0.1, // reset probability
                0.8, // prob. of choosing a fresh data value
                10000, // 1000 runs
                100, // max depth
                consts,
                false, // reset runs
                false,
                teachers,
                sul.getInputSymbols());

        IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);

        int check = 0;
        while (true && check < 100) {
            check++;
            rastar.learn();
            Hypothesis hyp = rastar.getHypothesis();

            DefaultQuery<PSymbolInstance, Boolean> ce
                    = iowalk.findCounterExample(hyp, null);

            //System.out.println("CE: " + ce);
            if (ce == null) {
                break;
            }

            ce = loops.optimizeCE(ce.getInput(), hyp);
            ce = asrep.optimizeCE(ce.getInput(), hyp);
            ce = pref.optimizeCE(ce.getInput(), hyp);
            rastar.addCounterexample(ce);
        }

        RegisterAutomaton hyp = rastar.getHypothesis();
        RegisterAutomatonImporter imp = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/pq3.xml");

        IOEquivalenceTest checker = new IOEquivalenceTest(
                imp.getRegisterAutomaton(), teachers, consts, true,
                sul.getActionSymbols()
        );

        Assert.assertNull(checker.findCounterExample(hyp, null));

    }
}
