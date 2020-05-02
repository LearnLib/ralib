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
package de.learnlib.ralib.theory.sumcineq;

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
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.example.sumcineq.AbstractTCPExample.Option;
import de.learnlib.ralib.example.sumcineq.OneWayFreshTCPSUL;
import de.learnlib.ralib.example.sumcineq.OneWayTCPSUL;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *	Tests the {@code SumCDoubleInequalityTheory} tree oracle on fresh and non-fresh versions 
 *of a system with inequalities over two sum constants.
 */
public class TestOneWayTCPTreeOracle extends RaLibTestSuite {

    @Test
    public void testOneWayTCPTreeOracle() {
    	
    	Double win = 100.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(OneWayTCPSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(OneWayTCPSUL.DOUBLE_TYPE,
                		Arrays.asList(
                				new DataValue<Double>(OneWayTCPSUL.DOUBLE_TYPE, 1.0), // for successor
                				new DataValue<Double>(OneWayTCPSUL.DOUBLE_TYPE, win)), // for window size
                		Collections.emptyList()));

        OneWayTCPSUL sul = new OneWayTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createBasicMTO(
                sul, OneWayTCPSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
                
        final Word<PSymbolInstance> prefix1 = Word.fromSymbols();
       
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
        		new PSymbolInstance(OneWayTCPSUL.ICONNECT,
                        new DataValue(OneWayTCPSUL.DOUBLE_TYPE, 1.0)),
        		new PSymbolInstance(OneWayTCPSUL.OK),
                new PSymbolInstance(OneWayTCPSUL.ISYN, 
                		new DataValue(OneWayTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(OneWayTCPSUL.OK),
                new PSymbolInstance(OneWayTCPSUL.ISYNACK,
                        new DataValue(OneWayTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(OneWayTCPSUL.OK));

        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(OneWayTCPSUL.ICONNECT,
                        new DataValue(OneWayTCPSUL.DOUBLE_TYPE, 1.0)),
                new PSymbolInstance(OneWayTCPSUL.OK));
        
        final Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(OneWayTCPSUL.ISYN, 
                		new DataValue(OneWayTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(OneWayTCPSUL.NOK)
                ,
                new PSymbolInstance(OneWayTCPSUL.ISYN,
                        new DataValue(OneWayTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(OneWayTCPSUL.OK)
                );

        // create a symbolic suffix from the concrete suffix
        final GeneralizedSymbolicSuffix symSuffix1 = new GeneralizedSymbolicSuffix(prefix1, suffix, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix1);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix1);
        
        
        final GeneralizedSymbolicSuffix symSuffix2 = new GeneralizedSymbolicSuffix(prefix2, suffix2, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix2);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix2);
        
        Assert.assertEquals("ISYN[s1] _not_ok[] ISYN[s2] _ok[]_P[[EQ_SUMC1], [EQ_SUMC1]]_S[[], [[EQ]]]", symSuffix2.toString());
        
        TreeQueryResult res1 = mto.treeQuery(prefix1, symSuffix1);
        SymbolicDecisionTree sdt1 = res1.getSdt();
        logger.log(Level.FINE, "Final SDT 1: {0}", sdt1.toString());
        
        final String expectedTree1 = "[]-+\n" +
        "  []-TRUE: s1\n" +
        "        []-(s2=s1)\n" +
        "         |    []-(s3=s1 + 1.0)\n" +
        "         |     |    [Leaf+]\n" +
        "         |     +-(s3!=s1 + 1.0)\n" +
        "         |          [Leaf-]\n" +
        "         +-(s2!=s1)\n" +
        "              []-TRUE: s3\n" +
        "                    [Leaf-]\n";
        
        Assert.assertEquals(expectedTree1, sdt1.toString());
        
        TreeQueryResult res2 = mto.treeQuery(prefix2, symSuffix2);
        SymbolicDecisionTree sdt2 = res2.getSdt();
        logger.log(Level.FINE, "Final SDT 2: {0}", sdt2.toString());
        
        final String expectedTree2 = "[r1]-+\n" +
        				"    []-(s1<=r1 + 1.0)\n" +
        				"     |    []-TRUE: s2\n" +
        				"     |          [Leaf-]\n" +
        				"     +-(r1 + 1.0<s1<r1 + 100.0)\n" +
        				"     |    []-(s2=r1)\n" +
        				"     |     |    [Leaf+]\n" +
        				"     |     +-(s2!=r1)\n" +
        				"     |          [Leaf-]\n" +
        				"     +-(s1>=r1 + 100.0)\n" +
        				"          []-TRUE: s2\n" +
        				"                [Leaf-]\n";
        
        Assert.assertEquals(expectedTree2, sdt2.toString());

        Parameter p1 = new Parameter(OneWayTCPSUL.DOUBLE_TYPE, 1);

        PIV testPiv = new PIV();
        testPiv.put(p1, new Register(OneWayTCPSUL.DOUBLE_TYPE, 1));

        Branching b = mto.getInitialBranching(
                prefix2, OneWayTCPSUL.ISYN, testPiv, sdt2);

        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
        Assert.assertEquals(b.getBranches().size(), 3);
    }

    @Test
    public void testOneWayFreshTCPTreeOracle() {
    	
    	Double win = 100.0;
    	SumCDoubleInequalityTheory sumcTheory = new SumCDoubleInequalityTheory(OneWayFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, win)), // for window size
        		Collections.emptyList());
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(OneWayFreshTCPSUL.DOUBLE_TYPE, 
                sumcTheory);
        sumcTheory.setCheckForFreshOutputs(true);

        OneWayFreshTCPSUL sul = new OneWayFreshTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, OneWayFreshTCPSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
                
        final Word<PSymbolInstance> prefix1 = Word.fromSymbols();
       
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
        		new PSymbolInstance(OneWayFreshTCPSUL.ICONNECT),
        		new PSymbolInstance(OneWayFreshTCPSUL.OCONNECT, 
        				new DataValue<>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0)),
                new PSymbolInstance(OneWayFreshTCPSUL.ISYN, 
                		new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(OneWayFreshTCPSUL.OK),
                new PSymbolInstance(OneWayFreshTCPSUL.ISYNACK,
                        new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(OneWayFreshTCPSUL.OK));

        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
        		new PSymbolInstance(OneWayFreshTCPSUL.ICONNECT),
        		new PSymbolInstance(OneWayFreshTCPSUL.OCONNECT, 
        				new DataValue<>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0)));
        
        final Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(OneWayFreshTCPSUL.ISYN, 
                		new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(OneWayFreshTCPSUL.NOK)
                ,
                new PSymbolInstance(OneWayFreshTCPSUL.ISYN,
                        new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(OneWayFreshTCPSUL.OK)
                );

        // create a symbolic suffix from the concrete suffix
        final GeneralizedSymbolicSuffix symSuffix1 = new GeneralizedSymbolicSuffix(prefix1, suffix, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix1);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix1);
        
        
        final GeneralizedSymbolicSuffix symSuffix2 = new GeneralizedSymbolicSuffix(prefix2, suffix2, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix2);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix2);
        
        TreeQueryResult res1 = mto.treeQuery(prefix1, symSuffix1);
        SymbolicDecisionTree sdt1 = res1.getSdt();
        logger.log(Level.FINE, "Final SDT 1: {0}", sdt1.toString());
        
        final String expectedTree1 = "[]-+\n" +
        "  []-TRUE: s1\n" +
        "        []-(s2=s1)\n" +
        "         |    []-(s3=s1 + 1.0)\n" +
        "         |     |    [Leaf+]\n" +
        "         |     +-(s3!=s1 + 1.0)\n" +
        "         |          [Leaf-]\n" +
        "         +-(s2!=s1)\n" +
        "              []-TRUE: s3\n" +
        "                    [Leaf-]\n";
        
        Assert.assertEquals(expectedTree1, sdt1.toString());
        
        TreeQueryResult res2 = mto.treeQuery(prefix2, symSuffix2);
        SymbolicDecisionTree sdt2 = res2.getSdt();
        logger.log(Level.FINE, "Final SDT 2: {0}", sdt2.toString());
        
        final String expectedTree2 = "[r1]-+\n" +
        				"    []-(s1<=r1 + 1.0)\n" +
        				"     |    []-TRUE: s2\n" +
        				"     |          [Leaf-]\n" +
        				"     +-(r1 + 1.0<s1<r1 + 100.0)\n" +
        				"     |    []-(s2=r1)\n" +
        				"     |     |    [Leaf+]\n" +
        				"     |     +-(s2!=r1)\n" +
        				"     |          [Leaf-]\n" +
        				"     +-(s1>=r1 + 100.0)\n" +
        				"          []-TRUE: s2\n" +
        				"                [Leaf-]\n";
        
        Assert.assertEquals(expectedTree2, sdt2.toString());

        Parameter p1 = new Parameter(OneWayFreshTCPSUL.DOUBLE_TYPE, 1);

        PIV testPiv = new PIV();
        testPiv.put(p1, new Register(OneWayFreshTCPSUL.DOUBLE_TYPE, 1));

        Branching b = mto.getInitialBranching(
                prefix2, OneWayFreshTCPSUL.ISYN, testPiv, sdt2);

        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
        Assert.assertEquals(b.getBranches().size(), 3);
    }
}
