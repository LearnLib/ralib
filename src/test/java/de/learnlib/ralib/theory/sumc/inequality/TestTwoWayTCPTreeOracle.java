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
package de.learnlib.ralib.theory.sumc.inequality;

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
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.example.sumc.inequality.TwoWayFreshTCPSUL;
import de.learnlib.ralib.example.sumc.inequality.TwoWayIntFreshTCPSUL;
import de.learnlib.ralib.example.sumc.inequality.TwoWayTCPSUL;
import de.learnlib.ralib.example.sumc.inequality.TCPExample.Option;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.SumCIntegerInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;


public class TestTwoWayTCPTreeOracle extends RaLibTestSuite {

    @Test
    public void testTwoWayTCPTree() {
    	
    	Double win = 1000.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Double>(TwoWayTCPSUL.DOUBLE_TYPE, 1.0), // for successor
				new DataValue<Double>(TwoWayTCPSUL.DOUBLE_TYPE, win)};
        teachers.put(TwoWayTCPSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(TwoWayTCPSUL.DOUBLE_TYPE,
                		Arrays.asList(sumConsts), // for window size
                		Collections.emptyList()));

        TwoWayTCPSUL sul = new TwoWayTCPSUL(win);
        sul.configure(Option.WIN_CONNECTING_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED, Option.WIN_SYNRECEIVED_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, TwoWayTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        
        final Word<PSymbolInstance> prefix = Word.epsilon();
        
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
        		 new PSymbolInstance(TwoWayTCPSUL.ICONNECT,
                         new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 1.0)),
                 new PSymbolInstance(TwoWayTCPSUL.OK), 
                 new PSymbolInstance(TwoWayTCPSUL.ISYN, 
                 		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 3.0),
                 		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 4.0)),
                 new PSymbolInstance(TwoWayTCPSUL.OK),
                new PSymbolInstance(TwoWayTCPSUL.ISYNACK, 
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 5.0),
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 6.0)),
                new PSymbolInstance(TwoWayTCPSUL.OK));
        
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix( prefix, suffix, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();
        
        String expectedTree = "[]-+\n" +
        		"  []-TRUE: s1\n" +
        		"        []-(s2=s1)\n" +
        		"         |    []-TRUE: s3\n" +
        		"         |          []-TRUE: s4\n" +
        		"         |                []-(s5=s1 + 1.0)\n" +
        		"         |                 |    [Leaf+]\n" +
        		"         |                 +-(s5!=s1 + 1.0)\n" +
        		"         |                      [Leaf-]\n" +
        		"         +-(s2!=s1)\n" +
        		"              []-TRUE: s3\n" +
        		"                    []-TRUE: s4\n" +
        		"                          []-TRUE: s5\n" +
        		"                                [Leaf-]\n";
        
        Assert.assertEquals(sdt.toString(), expectedTree);
        
        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(TwoWayTCPSUL.ICONNECT,
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 1.0)),
                new PSymbolInstance(TwoWayTCPSUL.OK));
        
        /*
         * Increase the length of suffix at own risk
         */
        final Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(TwoWayTCPSUL.ISYN, 
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 1.0),
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(TwoWayTCPSUL.NOK),
                new PSymbolInstance(TwoWayTCPSUL.ISYN,
                        new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 3.0),
                        new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 4.0)),
                new PSymbolInstance(TwoWayTCPSUL.OK)
        		);
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final GeneralizedSymbolicSuffix symSuffix2 = new GeneralizedSymbolicSuffix( prefix2, suffix2, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix2);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix2);
        

        TreeQueryResult res2 = mto.treeQuery(prefix2, symSuffix2);
        SymbolicDecisionTree sdt2 = res2.getSdt();
        
        String expectedTree2 = "[r1]-+\n" +
        		"    []-(s1=r1)\n" +
        		"     |    []-TRUE: s2\n" +
        		"     |          []-TRUE: s3\n" +
        		"     |                []-TRUE: s4\n" +
        		"     |                      [Leaf-]\n" +
        		"     +-(s1!=r1)\n" +
        		"          []-(s2<=r1 + 1.0)\n" +
        		"           |    []-TRUE: s3\n" +
        		"           |          []-TRUE: s4\n" +
        		"           |                [Leaf-]\n" +
        		"           +-(r1 + 1.0<s2<r1 + 1000.0)\n" +
        		"           |    []-(s3=r1)\n" +
        		"           |     |    []-TRUE: s4\n" +
        		"           |     |          [Leaf+]\n" +
        		"           |     +-(s3!=r1)\n" +
        		"           |          []-TRUE: s4\n" +
        		"           |                [Leaf-]\n" +
        		"           +-(s2>=r1 + 1000.0)\n" +
        		"                []-TRUE: s3\n" +
        		"                      []-TRUE: s4\n" +
        		"                            [Leaf-]\n";
        
        Assert.assertEquals(sdt2.toString(), expectedTree2);
        logger.log(Level.FINE, "inputs: {0} \n resets: {1}", new Object [] { sul.getInputs(), sul.getResets() });
        
        Parameter p1 = new Parameter(TwoWayTCPSUL.DOUBLE_TYPE, 1);

        PIV testPiv = new PIV();
        testPiv.put(p1, new Register(TwoWayTCPSUL.DOUBLE_TYPE, 1));

        Branching b = mto.getInitialBranching(
                prefix2, TwoWayTCPSUL.ISYN, testPiv, sdt2);

        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
        Assert.assertEquals(b.getBranches().size(), 4);
    }
    
    @Test
    public void testTwoWayFreshTCPTree() {
    	
    	Double win = 1000.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Double>(TwoWayFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
				new DataValue<Double>(TwoWayFreshTCPSUL.DOUBLE_TYPE, win)};
        
        SumCDoubleInequalityTheory theory = new SumCDoubleInequalityTheory(TwoWayFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        
        teachers.put(TwoWayFreshTCPSUL.DOUBLE_TYPE, theory);
        theory.setCheckForFreshOutputs(true);
        TwoWayFreshTCPSUL sul = new TwoWayFreshTCPSUL(win);
        sul.configure(Option.WIN_CONNECTING_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED, Option.WIN_SYNRECEIVED_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, TwoWayFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        
        final Word<PSymbolInstance> prefix = Word.epsilon();
        
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
        		 new PSymbolInstance(TwoWayFreshTCPSUL.ICONNECT),
                 new PSymbolInstance(TwoWayFreshTCPSUL.OCONNECT,
                         new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 1.0)
                		 ), 
                 new PSymbolInstance(TwoWayFreshTCPSUL.ISYN, 
                 		new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 3.0),
                 		new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 4.0)),
                 new PSymbolInstance(TwoWayFreshTCPSUL.OK),
                new PSymbolInstance(TwoWayFreshTCPSUL.ISYNACK, 
                		new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 5.0),
                		new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 6.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.OK));
        
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix( prefix, suffix, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();
        
        String expectedTree = "[]-+\n" +
        		"  []-TRUE: s1\n" +
        		"        []-(s2=s1)\n" +
        		"         |    []-TRUE: s3\n" +
        		"         |          []-TRUE: s4\n" +
        		"         |                []-(s5=s1 + 1.0)\n" +
        		"         |                 |    [Leaf+]\n" +
        		"         |                 +-(s5!=s1 + 1.0)\n" +
        		"         |                      [Leaf-]\n" +
        		"         +-(s2!=s1)\n" +
        		"              []-TRUE: s3\n" +
        		"                    []-TRUE: s4\n" +
        		"                          []-TRUE: s5\n" +
        		"                                [Leaf-]\n";
        
        Assert.assertEquals(sdt.toString(), expectedTree);
        
        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
        		new PSymbolInstance(TwoWayFreshTCPSUL.ICONNECT),
                new PSymbolInstance(TwoWayFreshTCPSUL.OCONNECT,
                        new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 1.0)));
        
        /*
         * Increase the length of suffix at own risk
         */
        final Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(TwoWayFreshTCPSUL.ISYN, 
                		new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 1.0),
                		new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.NOK),
                new PSymbolInstance(TwoWayFreshTCPSUL.ISYN,
                        new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 3.0),
                        new DataValue(TwoWayFreshTCPSUL.DOUBLE_TYPE, 4.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.OK)
        		);
        
        final GeneralizedSymbolicSuffix symSuffix2 = new GeneralizedSymbolicSuffix( prefix2, suffix2, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix2);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix2);
        
        TreeQueryResult res2 = mto.treeQuery(prefix2, symSuffix2);
        SymbolicDecisionTree sdt2 = res2.getSdt();
        
        String expectedTree2 = "[r1]-+\n" +
        		"    []-(s1=r1)\n" +
        		"     |    []-TRUE: s2\n" +
        		"     |          []-TRUE: s3\n" +
        		"     |                []-TRUE: s4\n" +
        		"     |                      [Leaf-]\n" +
        		"     +-(s1!=r1)\n" +
        		"          []-(s2<=r1 + 1.0)\n" +
        		"           |    []-TRUE: s3\n" +
        		"           |          []-TRUE: s4\n" +
        		"           |                [Leaf-]\n" +
        		"           +-(r1 + 1.0<s2<r1 + 1000.0)\n" +
        		"           |    []-(s3=r1)\n" +
        		"           |     |    []-TRUE: s4\n" +
        		"           |     |          [Leaf+]\n" +
        		"           |     +-(s3!=r1)\n" +
        		"           |          []-TRUE: s4\n" +
        		"           |                [Leaf-]\n" +
        		"           +-(s2>=r1 + 1000.0)\n" +
        		"                []-TRUE: s3\n" +
        		"                      []-TRUE: s4\n" +
        		"                            [Leaf-]\n";
        
        Assert.assertEquals(sdt2.toString(), expectedTree2);
        logger.log(Level.FINE, "inputs: {0} \n resets: {1}", new Object [] { sul.getInputs(), sul.getResets() });
        
        Parameter p1 = new Parameter(TwoWayFreshTCPSUL.DOUBLE_TYPE, 1);

        PIV testPiv = new PIV();
        testPiv.put(p1, new Register(TwoWayFreshTCPSUL.DOUBLE_TYPE, 1));

        Branching b = mto.getInitialBranching(
                prefix2, TwoWayFreshTCPSUL.ISYN, testPiv, sdt2);

        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
        Assert.assertEquals(b.getBranches().size(), 4);
    }
    
    @Test
    public void testTwoWayIntFreshTCPTree() {

    	Integer win = 1000;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Integer>(TwoWayIntFreshTCPSUL.INT_TYPE, 1), // for successor
				new DataValue<Integer>(TwoWayIntFreshTCPSUL.INT_TYPE, win)};
        
        SumCIntegerInequalityTheory theory = new SumCIntegerInequalityTheory(TwoWayIntFreshTCPSUL.INT_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        
        teachers.put(TwoWayIntFreshTCPSUL.INT_TYPE, theory);
        theory.setCheckForFreshOutputs(true);
        TwoWayIntFreshTCPSUL sul = new TwoWayIntFreshTCPSUL(win);
        sul.configure(Option.WIN_CONNECTING_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED, Option.WIN_SYNRECEIVED_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, TwoWayIntFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        
        final Word<PSymbolInstance> prefix = Word.epsilon();
        
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
        		 new PSymbolInstance(TwoWayIntFreshTCPSUL.ICONNECT),
                 new PSymbolInstance(TwoWayIntFreshTCPSUL.OCONNECT,
                         new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 1)
                		 ), 
                 new PSymbolInstance(TwoWayIntFreshTCPSUL.ISYN, 
                 		new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 3),
                 		new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 4)),
                 new PSymbolInstance(TwoWayIntFreshTCPSUL.OK),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.ISYNACK, 
                		new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 5),
                		new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 6)),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.OK));
        
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix( prefix, suffix, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();
        
        String expectedTree = "[]-+\n" +
        		"  []-TRUE: s1\n" +
        		"        []-(s2=s1)\n" +
        		"         |    []-TRUE: s3\n" +
        		"         |          []-TRUE: s4\n" +
        		"         |                []-(s5=s1 + 1)\n" +
        		"         |                 |    [Leaf+]\n" +
        		"         |                 +-(s5!=s1 + 1)\n" +
        		"         |                      [Leaf-]\n" +
        		"         +-(s2!=s1)\n" +
        		"              []-TRUE: s3\n" +
        		"                    []-TRUE: s4\n" +
        		"                          []-TRUE: s5\n" +
        		"                                [Leaf-]\n";
        
        Assert.assertEquals(sdt.toString(), expectedTree);
//        
        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
        		new PSymbolInstance(TwoWayIntFreshTCPSUL.ICONNECT),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.OCONNECT,
                        new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 1)));
        
        /*
         * Increase the length of suffix at own risk
         */
        final Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(TwoWayIntFreshTCPSUL.ISYN, 
                		new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 1),
                		new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 2)),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.NOK),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.ISYN,
                        new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 3),
                        new DataValue(TwoWayIntFreshTCPSUL.INT_TYPE, 4)),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.OK)
        		);
        
        final GeneralizedSymbolicSuffix symSuffix2 = new GeneralizedSymbolicSuffix( prefix2, suffix2, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix2);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix2);
        
        TreeQueryResult res2 = mto.treeQuery(prefix2, symSuffix2);
        SymbolicDecisionTree sdt2 = res2.getSdt();
        
        String expectedTree2 = "[r1]-+\n" +
        		"    []-(s1=r1)\n" +
        		"     |    []-TRUE: s2\n" +
        		"     |          []-TRUE: s3\n" +
        		"     |                []-TRUE: s4\n" +
        		"     |                      [Leaf-]\n" +
        		"     +-(s1!=r1)\n" +
        		"          []-(s2<=r1 + 1)\n" +
        		"           |    []-TRUE: s3\n" +
        		"           |          []-TRUE: s4\n" +
        		"           |                [Leaf-]\n" +
        		"           +-(r1 + 1<s2<r1 + 1000)\n" +
        		"           |    []-(s3=r1)\n" +
        		"           |     |    []-TRUE: s4\n" +
        		"           |     |          [Leaf+]\n" +
        		"           |     +-(s3!=r1)\n" +
        		"           |          []-TRUE: s4\n" +
        		"           |                [Leaf-]\n" +
        		"           +-(s2>=r1 + 1000)\n" +
        		"                []-TRUE: s3\n" +
        		"                      []-TRUE: s4\n" +
        		"                            [Leaf-]\n";
        
        Assert.assertEquals(sdt2.toString(), expectedTree2);
        logger.log(Level.FINE, "inputs: {0} \n resets: {1}", new Object [] { sul.getInputs(), sul.getResets() });
        
        Parameter p1 = new Parameter(TwoWayIntFreshTCPSUL.INT_TYPE, 1);

        PIV testPiv = new PIV();
        testPiv.put(p1, new Register(TwoWayIntFreshTCPSUL.INT_TYPE, 1));

        Branching b = mto.getInitialBranching(
                prefix2, TwoWayIntFreshTCPSUL.ISYN, testPiv, sdt2);

        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
        Assert.assertEquals(b.getBranches().size(), 4);
    }
}
