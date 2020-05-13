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
package de.learnlib.ralib.learning;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class LearnLoginTest extends RaLibTestSuite {

	

	@Test
	public void learnLoginExample() {

		Constants consts = new Constants();
		RegisterAutomaton sul = AUTOMATON;

		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
		teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

		ConstraintSolver solver = new SimpleConstraintSolver();

		MultiTheoryTreeOracle mto = TestUtil.createSimulatorMTO(sul, teachers, new Constants(), solver);
		SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

		TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> TestUtil.createSimulatorMTO(hyp, teachers,
				new Constants(), solver);

		RaStar rastar = new RaStar(mto, hypFactory, slo, consts, teachers, solver, I_LOGIN, I_LOGOUT, I_REGISTER);

		rastar.learn();
		RegisterAutomaton hyp = rastar.getHypothesis();
		logger.log(Level.FINE, "HYP1: {0}", hyp);

		Word<PSymbolInstance> ce = Word.fromSymbols(
				new PSymbolInstance(I_REGISTER, new DataValue(T_UID, 1), new DataValue(T_PWD, 1)),
				new PSymbolInstance(I_LOGIN, new DataValue(T_UID, 1), new DataValue(T_PWD, 1)));

		rastar.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

		rastar.learn();
		hyp = rastar.getHypothesis();
		logger.log(Level.FINE, "HYP2: {0}", hyp);

		Assert.assertEquals(hyp.getStates().size(), 3);
		Assert.assertEquals(hyp.getTransitions().size(), 11);

	}
}
