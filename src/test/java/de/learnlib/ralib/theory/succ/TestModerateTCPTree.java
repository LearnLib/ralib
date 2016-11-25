/*
 * Copyright (C) 2014 falk.
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
package de.learnlib.ralib.theory.succ;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.succ.AbstractTCPExample.Option;
import de.learnlib.ralib.example.succ.ModerateTCPSUL;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class TestModerateTCPTree extends RaLibTestSuite {

    @Test
    public void testModerateTCPTree() {
    	
    	Double win = 1000.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(ModerateTCPSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(ModerateTCPSUL.DOUBLE_TYPE,
                		Arrays.asList(
                				new DataValue<Double>(ModerateTCPSUL.DOUBLE_TYPE, 1.0), // for successor
                				new DataValue<Double>(ModerateTCPSUL.DOUBLE_TYPE, win)), // for window size
                		Collections.emptyList()));

        ModerateTCPSUL sul = new ModerateTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                sul, ModerateTCPSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Double> b = new DataValueConstructor<>(ModerateTCPSUL.DOUBLE_TYPE);
                
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(ModerateTCPSUL.ISYN, 
                		new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 1.0),
                		new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(ModerateTCPSUL.OK),
                new PSymbolInstance(ModerateTCPSUL.ISYNACK,
                        new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 3.0),
                        new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 4.0)),
                new PSymbolInstance(ModerateTCPSUL.OK));
        
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(ModerateTCPSUL.ICONNECT,
                        b.fv(1.0)),
                new PSymbolInstance(ModerateTCPSUL.OK));
        
        final Word<PSymbolInstance> longsuffix2 = Word.fromSymbols(
        		 new PSymbolInstance(ModerateTCPSUL.ICONNECT,
                         new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 1.0)),
                 new PSymbolInstance(ModerateTCPSUL.OK), 
                 new PSymbolInstance(ModerateTCPSUL.ISYN, 
                 		new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 3.0),
                 		new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 4.0)),
                 new PSymbolInstance(ModerateTCPSUL.OK),
                new PSymbolInstance(ModerateTCPSUL.ISYNACK, 
                		new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 5.0),
                		new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 6.0)),
                new PSymbolInstance(ModerateTCPSUL.OK),
                new PSymbolInstance(ModerateTCPSUL.ICONNECT,
                        new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 7.0)),
                new PSymbolInstance(ModerateTCPSUL.NOK));
        
        final Word<PSymbolInstance> prefix2 = Word.epsilon();
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, longsuffix, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        final GeneralizedSymbolicSuffix symSuffix2 = new GeneralizedSymbolicSuffix(prefix2, longsuffix2, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix2);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix2);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        System.out.println(sdt);
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        
        TreeQueryResult res2 = mto.treeQuery(prefix2, symSuffix2);
        SymbolicDecisionTree sdt2 = res2.getSdt();
        
        System.out.println(sdt2);
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());

        Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 7);
        Assert.assertEquals(((SDT)sdt2).getNumberOfLeaves() , 7);

    }


}
