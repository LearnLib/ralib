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

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.rastar.RaStar;

import static de.learnlib.ralib.example.priority.PriorityQueueOracle.*;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import net.automatalib.words.Word;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class LearnPQTest extends RaLibTestSuite {

    @Test
    public void PQExample() {

        Constants consts = new Constants();
        DataWordOracle dwOracle = 
                new de.learnlib.ralib.example.priority.PriorityQueueOracle();
        
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(doubleType, new DoubleInequalityTheory(doubleType));

        
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();       
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), jsolv);
        
        SDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, jsolv);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> 
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, new Constants(), jsolv);

        RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, OFFER, POLL);
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
                //                new PSymbolInstance(OFFER,
                //                        new DataValue(doubleType, 1.0)),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, BigDecimal.ONE)));

        DefaultQuery<PSymbolInstance, Boolean> ceQuery = new DefaultQuery(ce);
        dwOracle.processQueries(Collections.singleton(ceQuery));

        rastar.addCounterexample(ceQuery);

        rastar.learn();
        hyp = rastar.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);
        
        Assert.assertEquals(hyp.getStates().size(), 7);
        Assert.assertEquals(hyp.getTransitions().size(), 27);
    }
}
