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
package de.learnlib.ralib.ceanalysis;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.smt.ConstraintSolverFactory;
import de.learnlib.ralib.smt.jconstraints.JConstraintsConstraintSolver;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.dt.DTHyp;
import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.ralambda.RaLambda;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class PrefixFinderTest extends RaLibTestSuite {

    @Test
    public void prefixFinderTest() {
        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        ConstraintSolver solver = ConstraintSolverFactory.createZ3ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        RaStar rastar = new RaStar(mto, hypFactory, slo,
                consts, I_LOGIN, I_LOGOUT, I_REGISTER);

        rastar.learn();
        final Hypothesis hyp = rastar.getHypothesis();
        // System.out.println(hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                        new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)),
                new PSymbolInstance(I_LOGIN,
                        new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)));

        PrefixFinder pf = new PrefixFinder(
                mto,
                hypFactory.createTreeOracle(hyp), hyp,
                slo,
                // rastar.getComponents(),
                consts
        );

        Word<PSymbolInstance> prefix = pf.analyzeCounterexample(ce).getPrefix();
	Assert.assertEquals(prefix.toString(), "register[0[T_uid], 0[T_pwd]]");
    }

    @Test
    public void prefixFinderMultipleAccessSequencesTest() {
	Constants consts = new Constants();
	RegisterAutomaton sul = de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
	DataWordOracle dwOracle = new SimulatorOracle(sul);

	final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = ConstraintSolverFactory.createZ3ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);
        RaLambda ralambda = new RaLambda(mto, hypFactory, slo,
        		consts, I_PUSH, I_POP);

        ralambda.learn();
        final DTHyp hyp = ralambda.getDTHyp();
        // System.out.println(hyp);

        Word<PSymbolInstance> shortPrefix = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)));
        DTLeaf leaf = ralambda.getDT().getLeaf(shortPrefix);
        leaf.elevatePrefix(ralambda.getDT(), shortPrefix, hyp, slo);

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ONE)));

        PrefixFinder pf = new PrefixFinder(
                mto,
                hypFactory.createTreeOracle(hyp), hyp,
                slo,
                // ralambda.getComponents(),
                consts
        );

        Word<PSymbolInstance> prefix = pf.analyzeCounterexample(ce).getPrefix();
	Assert.assertEquals(prefix.toString(), "push[0[T_int]] pop[0[T_int]]");
    }

}
