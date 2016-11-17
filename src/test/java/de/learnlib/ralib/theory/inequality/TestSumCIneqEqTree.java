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
package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;

import java.util.logging.Level;
import org.testng.Assert;

/**
 *
 * @author falk
 */
public class TestSumCIneqEqTree extends RaLibTestSuite {

 //   @Test
    public void testOnPQ() {

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(PriorityQueueSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE, 
                		Arrays.asList(new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 100.0)), 
                        		Collections.emptyList()));

        PriorityQueueSUL sul = new PriorityQueueSUL(3);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createBasicMTO(
                sul, PriorityQueueSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
                
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE, 4.0)),
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE, 5.0)),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE, 6.0)));
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE, 1.0)),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE, 3.0)),
                new PSymbolInstance(PriorityQueueSUL.OK));

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, longsuffix, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        final String expectedTree = "[r2, r1]-+\n" +
"                []-(s1=r2)\n" +
"                |    []-(s2<=r1)\n" +
"                |     |    []-(s3=s2)\n" +
"                |     |     |    [Leaf+]\n" +
"                |     |     +-(s3!=s2)\n" +
"                |     |          [Leaf-]\n" +
"                |     +-(s2>r1)\n" +
"                |          []-(s3=r1)\n" +
"                |           |    [Leaf+]\n" +
"                |           +-(s3!=r1)\n" +
"                |                [Leaf-]\n" +
"                +-(s1!=r2)\n" +
"                     []-TRUE: s2\n" +
"                           []-TRUE: s3\n" +
"                                 [Leaf-]\n";
        
        
        String tree = sdt.toString();
        Assert.assertEquals(tree.replaceAll("\\s", ""), expectedTree.replaceAll("\\s", ""));
        logger.log(Level.FINE, "final SDT: \n{0}", tree);

        Parameter p1 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 1);
        Parameter p2 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 2);
        Parameter p3 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 3);

        PIV testPiv = new PIV();
        testPiv.put(p1, new Register(PriorityQueueSUL.DOUBLE_TYPE, 1));
        testPiv.put(p2, new Register(PriorityQueueSUL.DOUBLE_TYPE, 2));
        testPiv.put(p3, new Register(PriorityQueueSUL.DOUBLE_TYPE, 3));

        Branching b = mto.getInitialBranching(
                prefix, PriorityQueueSUL.OFFER, testPiv, sdt);

        Assert.assertEquals(b.getBranches().size(), 2);
        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
        //((offer[s1] _ok[] poll[] _out[s2] poll[] _out[s1]))
    }
    
 

 //   @Test
    public void testOnPQ2() {

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(PriorityQueueSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE, 
                		Arrays.asList(new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 100.0)), 
                        		Collections.emptyList()));

        PriorityQueueSUL sul = new PriorityQueueSUL(3);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createBasicMTO(
                sul, PriorityQueueSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
                
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 5.0)),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 6.0)),
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 7.0)));
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 1.0)),
                new PSymbolInstance(PriorityQueueSUL.OK));

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, longsuffix, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        final String expectedTree = 
        		"[[r1]-+\n"+
        		"    []-(s1=r1)\n"+
        		"     |    []-(s2!=s1)\n"+
        		"     |     |    []-TRUE: s3\n"+
        		"     |     |          [Leaf-]\n"+
        		"     |     +-(s2=s1)\n"+
        		"     |          []-(s3!=r1)\n"+
        		"     |           |    [Leaf-]\n"+
        		"     |           +-(s3=r1)\n"+
        		"     |                [Leaf+]\n"+
        		"     +-(s1<r1)\n"+
        		"     |    []-(s2!=s1)\n"+
        		"     |     |    []-TRUE: s3\n"+
        		"     |     |          [Leaf-]\n"+
        		"     |     +-(s2=s1)\n"+
        		"     |          []-(s3!=r1)\n"+
        		"     |           |    [Leaf-]\n"+
        		"     |           +-(s3=r1)\n"+
        		"     |                [Leaf+]\n"+
        		"     +-(s1>r1)\n"+
        		"          []-(s2!=r1)\n"+
        		"           |    []-TRUE: s3\n"+
        		"           |          [Leaf-]\n"+
        		"           +-(s2=r1)\n"+
        		"                []-(s3!=s1)\n"+
        		"                 |    [Leaf-]\n"+
        		"                 +-(s3=s1)\n"+
        		"                      [Leaf+]\n"+
        		"]";
        
        String tree = sdt.toString();
        Assert.assertTrue(!tree.contains("00")); // assert that there is no sumc in the tree
//        Assert.assertEquals(tree.replaceAll("\\s", ""), expectedTree.replaceAll("\\s", ""),
//        		"Expected " + expectedTree + "\n got " + tree);
        logger.log(Level.FINE, "final SDT: \n{0}", tree);
    }

    
    
 //   @Test
    public void testOnPQ3() {

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(PriorityQueueSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE, 
                		Arrays.asList(new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 100.0)), 
                        		Collections.emptyList()));

        PriorityQueueSUL sul = new PriorityQueueSUL(3);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createBasicMTO(
                sul, PriorityQueueSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
                
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 5.0)),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 5.0)),
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 6.0)));
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 10001.0)),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 20102.0)),
                new PSymbolInstance(PriorityQueueSUL.OK));

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, longsuffix, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        final String expectedTree = 
        		"[[r1]-+\n"+
        		"    []-(s1=r1)\n"+
        		"     |    []-(s2!=s1)\n"+
        		"     |     |    []-TRUE: s3\n"+
        		"     |     |          [Leaf-]\n"+
        		"     |     +-(s2=s1)\n"+
        		"     |          []-(s3!=r1)\n"+
        		"     |           |    [Leaf-]\n"+
        		"     |           +-(s3=r1)\n"+
        		"     |                [Leaf+]\n"+
        		"     +-(s1<r1)\n"+
        		"     |    []-(s2!=s1)\n"+
        		"     |     |    []-TRUE: s3\n"+
        		"     |     |          [Leaf-]\n"+
        		"     |     +-(s2=s1)\n"+
        		"     |          []-(s3!=r1)\n"+
        		"     |           |    [Leaf-]\n"+
        		"     |           +-(s3=r1)\n"+
        		"     |                [Leaf+]\n"+
        		"     +-(s1>r1)\n"+
        		"          []-(s2!=r1)\n"+
        		"           |    []-TRUE: s3\n"+
        		"           |          [Leaf-]\n"+
        		"           +-(s2=r1)\n"+
        		"                []-(s3!=s1)\n"+
        		"                 |    [Leaf-]\n"+
        		"                 +-(s3=s1)\n"+
        		"                      [Leaf+]\n"+
        		"]";
        
        String tree = sdt.toString();

        Assert.assertTrue(!tree.contains("00")); // assert that there is no sumc in the tree
//        Assert.assertEquals(tree.replaceAll("\\s", ""), expectedTree.replaceAll("\\s", ""),
//        		"Expected " + expectedTree + "\n got " + tree);
        logger.log(Level.FINE, "final SDT: \n{0}", tree);

//        Parameter p1 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 1);
//        Parameter p2 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 2);
//        Parameter p3 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 3);
//
//        PIV testPiv = new PIV();
//        testPiv.put(p1, new Register(PriorityQueueSUL.DOUBLE_TYPE, 1));
//        testPiv.put(p2, new Register(PriorityQueueSUL.DOUBLE_TYPE, 2));
//        testPiv.put(p3, new Register(PriorityQueueSUL.DOUBLE_TYPE, 3));
//
//        Branching b = mto.getInitialBranching(
//                prefix, PriorityQueueSUL.OFFER, testPiv, sdt);
//
//        Assert.assertEquals(b.getBranches().size(), 2);
//        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }
    
    
    @Test
    public void testOnPQ4() {

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(PriorityQueueSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE, 
                		Arrays.asList(new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 100.0)), 
                        		Collections.emptyList()));

        PriorityQueueSUL sul = new PriorityQueueSUL(3);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createBasicMTO(
                sul, PriorityQueueSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
                
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 5.0)),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 5.0)),
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 6.0)));
        
        final Word<PSymbolInstance> prefix = //Word.epsilon();
        		Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 10001.0)),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                		new DataValue<Double>(PriorityQueueSUL.DOUBLE_TYPE, 201002.0)),
                new PSymbolInstance(PriorityQueueSUL.OK));

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, longsuffix, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        final String expectedTree = 
        		"[[r1]-+\n"+
        		"    []-(s1=r1)\n"+
        		"     |    []-(s2!=s1)\n"+
        		"     |     |    []-TRUE: s3\n"+
        		"     |     |          [Leaf-]\n"+
        		"     |     +-(s2=s1)\n"+
        		"     |          []-(s3!=r1)\n"+
        		"     |           |    [Leaf-]\n"+
        		"     |           +-(s3=r1)\n"+
        		"     |                [Leaf+]\n"+
        		"     +-(s1<r1)\n"+
        		"     |    []-(s2!=s1)\n"+
        		"     |     |    []-TRUE: s3\n"+
        		"     |     |          [Leaf-]\n"+
        		"     |     +-(s2=s1)\n"+
        		"     |          []-(s3!=r1)\n"+
        		"     |           |    [Leaf-]\n"+
        		"     |           +-(s3=r1)\n"+
        		"     |                [Leaf+]\n"+
        		"     +-(s1>r1)\n"+
        		"          []-(s2!=r1)\n"+
        		"           |    []-TRUE: s3\n"+
        		"           |          [Leaf-]\n"+
        		"           +-(s2=r1)\n"+
        		"                []-(s3!=s1)\n"+
        		"                 |    [Leaf-]\n"+
        		"                 +-(s3=s1)\n"+
        		"                      [Leaf+]\n"+
        		"]";
        
        String tree = sdt.toString();
        System.out.println(tree);

        Assert.assertTrue(!tree.contains("00")); // assert that there is no sumc in the tree
//        Assert.assertEquals(tree.replaceAll("\\s", ""), expectedTree.replaceAll("\\s", ""),
//        		"Expected " + expectedTree + "\n got " + tree);
        logger.log(Level.FINE, "final SDT: \n{0}", tree);

//        Parameter p1 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 1);
//        Parameter p2 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 2);
//        Parameter p3 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 3);
//
//        PIV testPiv = new PIV();
//        testPiv.put(p1, new Register(PriorityQueueSUL.DOUBLE_TYPE, 1));
//        testPiv.put(p2, new Register(PriorityQueueSUL.DOUBLE_TYPE, 2));
//        testPiv.put(p3, new Register(PriorityQueueSUL.DOUBLE_TYPE, 3));
//
//        Branching b = mto.getInitialBranching(
//                prefix, PriorityQueueSUL.OFFER, testPiv, sdt);
//
//        Assert.assertEquals(b.getBranches().size(), 2);
//        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }

}
