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
package de.learnlib.ralib.learning.ralambda;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.api.query.DefaultQuery;
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
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public class LearnMixedIOTest extends RaLibTestSuite {

    @Test
    public void learnMixedIO() {

        long seed = -1386796323025681754L;
        logger.log(Level.FINE, "SEED={0}", seed);
        final Random random = new Random(seed);

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/mixed.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

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
        IOOracle ioOracle = new SULOracle(sul, ERROR);

        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                ioOracle, teachers, consts, jsolv, inputs);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, jsolv);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, jsolv);

        RaLambda rastar = new RaLambda(mto, hypFactory, mlo, consts, true, actions);
		rastar.setSolver(jsolv);

        IORandomWalk iowalk = new IORandomWalk(random,
                sul,
                false, // do not draw symbols uniformly
                0.1, // reset probability
                0.8, // prob. of choosing a fresh data value
                10000, // 1000 runs
                100, // max depth
                consts,
                false, // reset runs
                teachers,
                inputs);

        IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);

        int check = 0;
        while (true && check < 100) {

            check++;
            rastar.learn();
            Hypothesis hyp = rastar.getHypothesis();

            DefaultQuery<PSymbolInstance, Boolean> ce =
                    iowalk.findCounterExample(hyp, null);

            if (ce == null) {
                break;
            }

            ce = loops.optimizeCE(ce.getInput(), hyp);
            ce = asrep.optimizeCE(ce.getInput(), hyp);
            ce = pref.optimizeCE(ce.getInput(), hyp);

            Assert.assertTrue(model.accepts(ce.getInput()));
            Assert.assertTrue(!hyp.accepts(ce.getInput()));

            rastar.addCounterexample(ce);
        }

        RegisterAutomaton hyp = rastar.getHypothesis();
        IOEquivalenceTest checker = new IOEquivalenceTest(
                model, teachers, consts, true, actions);

        Assert.assertNull(checker.findCounterExample(hyp, null));

    }
}
