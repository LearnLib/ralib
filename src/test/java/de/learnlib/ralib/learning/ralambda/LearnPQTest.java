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

import static de.learnlib.ralib.example.priority.PriorityQueueOracle.OFFER;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.POLL;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.doubleType;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibLearningExperimentRunner;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class LearnPQTest extends RaLibTestSuite {

    @Test
    public void learnPQ() {

        Constants consts = new Constants();
        DataWordOracle dwOracle =
                new de.learnlib.ralib.example.priority.PriorityQueueOracle(2);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(doubleType, new DoubleInequalityTheory(doubleType));

        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), jsolv);

        SDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, jsolv);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, new Constants(), jsolv);

        RaLambda rastar = new RaLambda(mto, hypFactory, mlo, consts, OFFER, POLL);
//        rastar.setUseOldAnalyzer(true);
        rastar.learn();
        RegisterAutomaton hyp = rastar.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, BigDecimal.ONE)),
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, BigDecimal.ONE)),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, BigDecimal.ONE)),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, BigDecimal.ONE)));

        DefaultQuery<PSymbolInstance, Boolean> ceQuery = new DefaultQuery<>(ce, true);

        rastar.addCounterexample(ceQuery);

        rastar.learn();
        hyp = rastar.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        Assert.assertTrue(hyp.accepts(ceQuery.getInput()));

        Word<PSymbolInstance> ce2 = Word.fromSymbols(
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, BigDecimal.ONE)),
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, BigDecimal.valueOf(2.0))),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, BigDecimal.ONE)),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, BigDecimal.valueOf(2.0))));
        DefaultQuery<PSymbolInstance, Boolean> ce2Query = new DefaultQuery<>(ce2, true);
        rastar.addCounterexample(ce2Query);
        rastar.learn();
        hyp = rastar.getHypothesis();
        logger.log(Level.FINE, "HYP3: {0}", hyp);

        Assert.assertTrue(hyp.accepts(ce2Query.getInput()));
    }

    @Test
    public void learnPQRandom() {
        int SEEDS = 1;	// NB: results are hard-coded for one seed only
        Constants consts = new Constants();
        DataWordOracle dwOracle =
                new de.learnlib.ralib.example.priority.PriorityQueueOracle(2);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(doubleType, new DoubleInequalityTheory(doubleType));

        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        runner.setMaxDepth(4);
//        runner.setUseOldAnalyzer(true);

        Measurements[] ralambdaCount = new Measurements [SEEDS];
        Measurements[] rastarCount = new Measurements [SEEDS];
        for (int i = 0; i < SEEDS; i++) {
            runner.setSeed(i);
            runner.run(RaLearningAlgorithmName.RALAMBDA, dwOracle, teachers, consts, jsolv, new ParameterizedSymbol [] {OFFER, POLL});
            ralambdaCount[i] = runner.getMeasurements();
            runner.resetMeasurements();
            runner.run(RaLearningAlgorithmName.RASTAR, dwOracle, teachers, consts, jsolv, new ParameterizedSymbol [] {OFFER, POLL});
            rastarCount[i] = runner.getMeasurements();
            runner.resetMeasurements();
        }

	// hard-coded results from first seed
        Assert.assertEquals(Arrays.toString(ralambdaCount), "[{TQ: 82, Resets: 1989, Inputs: 0}]");
        Assert.assertEquals(Arrays.toString(rastarCount),   "[{TQ: 71, Resets: 8321, Inputs: 0}]");
    }
}
