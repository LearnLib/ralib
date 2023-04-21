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

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.dt.DTHyp;
import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.dt.ShortPrefix;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.learning.rattt.RaTTT;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;

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
        
        ConstraintSolver solver = new SimpleConstraintSolver();
        
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
        logger.log(Level.FINE, "HYP1: {0}", hyp);
        System.out.println(hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                        new DataValue(T_UID, 1), new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN, 
                        new DataValue(T_UID, 1), new DataValue(T_PWD, 1)));

        PrefixFinder pf = new PrefixFinder(
                mto,
                hypFactory.createTreeOracle(hyp), hyp,
                slo,
                rastar.getComponents(),
                consts
        );

        Word<PSymbolInstance> prefix = pf.analyzeCounterexample(ce).getPrefix();
        System.out.println(prefix);
    }
    
	@Test
	public void prefixFinderMultipleAccessSequencesTest() {
		Constants consts = new Constants();
		RegisterAutomaton sul = de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
		DataWordOracle dwOracle = new SimulatorOracle(sul);
		
		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		teachers.put(T_INT, new IntegerEqualityTheory(T_INT));
		
        ConstraintSolver solver = new SimpleConstraintSolver();
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> 
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, 
                        new Constants(), solver);
        RaTTT rattt = new RaTTT(mto, hypFactory, slo,
        		consts, I_PUSH, I_POP);
        
        rattt.learn();
        final DTHyp hyp = (DTHyp)rattt.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);
        System.out.println(hyp);
        
        Word<PSymbolInstance> shortPrefix = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)));
        DTLeaf leaf = rattt.getDT().getLeaf(shortPrefix);
        leaf.elevatePrefix(rattt.getDT(), shortPrefix, hyp, slo);
        
        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)));

        PrefixFinder pf = new PrefixFinder(
                mto,
                hypFactory.createTreeOracle(hyp), hyp,
                slo,
                rattt.getComponents(),
                consts
        );

        Word<PSymbolInstance> prefix = pf.analyzeCounterexample(ce).getPrefix();
        System.out.println(prefix);

	}

}
