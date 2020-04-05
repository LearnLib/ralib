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
import de.learnlib.ralib.example.succ.ModerateTCPSULMultitype;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class TestModerateTCPMultitypeTree extends RaLibTestSuite {

    @Test
    public void testModerateTCPTree() {
    	
    	Double win = 100.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(ModerateTCPSULMultitype.DOUBLE_TYPE1, 
                new SumCDoubleInequalityTheory(ModerateTCPSULMultitype.DOUBLE_TYPE1,
                		Arrays.asList(
                				new DataValue<Double>(ModerateTCPSULMultitype.DOUBLE_TYPE1, 1.0), // for successor
                				new DataValue<Double>(ModerateTCPSULMultitype.DOUBLE_TYPE1, win)), // for window size
                		Collections.emptyList()));

        teachers.put(ModerateTCPSULMultitype.DOUBLE_TYPE2, 
                new SumCDoubleInequalityTheory(ModerateTCPSULMultitype.DOUBLE_TYPE2,
                		Arrays.asList(
                				new DataValue<Double>(ModerateTCPSULMultitype.DOUBLE_TYPE2, 1.0), // for successor
                				new DataValue<Double>(ModerateTCPSULMultitype.DOUBLE_TYPE2, win)), // for window size
                		Collections.emptyList()));
        
        ModerateTCPSULMultitype sul = new ModerateTCPSULMultitype(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, ModerateTCPSULMultitype.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
                
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(ModerateTCPSULMultitype.ISYN, 
                		new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE1, 1.0),
                		new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE2, 2.0)),
                new PSymbolInstance(ModerateTCPSULMultitype.OK),
                new PSymbolInstance(ModerateTCPSULMultitype.ISYNACK,
                        new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE2, 1.0),
                        new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE1, 2.0)),
                new PSymbolInstance(ModerateTCPSULMultitype.OK));
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(ModerateTCPSULMultitype.ICONNECT,
                        new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE1, 1.0),
                        new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE2, 102.0)),
                new PSymbolInstance(ModerateTCPSULMultitype.OK));
        
        
        final Word<PSymbolInstance> longsuffix2 = Word.fromSymbols(
                new PSymbolInstance(ModerateTCPSULMultitype.ISYNACK, 
                		new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE2, 1.0),
                		new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE1, 2.0)),
                new PSymbolInstance(ModerateTCPSULMultitype.NOK),
                new PSymbolInstance(ModerateTCPSULMultitype.ICONNECT,
                        new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE1, 1.0),
                        new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE2, 2.0)),
                new PSymbolInstance(ModerateTCPSULMultitype.NOK));
        
        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(ModerateTCPSULMultitype.ICONNECT,
                        new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE1, 1.0),
                        new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE2, 102.0)),
                new PSymbolInstance(ModerateTCPSULMultitype.OK), 
                new PSymbolInstance(ModerateTCPSULMultitype.ISYN, 
                		new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE1, 1.0),
                		new DataValue(ModerateTCPSULMultitype.DOUBLE_TYPE2, 2.0)),
                new PSymbolInstance(ModerateTCPSULMultitype.OK));

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, longsuffix, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        final GeneralizedSymbolicSuffix symSuffix2 = new GeneralizedSymbolicSuffix(prefix, longsuffix, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix2);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix2);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();
        
        TreeQueryResult res2 = mto.treeQuery(prefix2, symSuffix2);
        SymbolicDecisionTree sdt2 = res2.getSdt();
        
        System.out.println(sdt);
        System.out.println(sdt2);

        final String expectedTree = "[r2, r1]-+\n" +
"        []-(s1!=r2)\n" +
"         |    []-TRUE: s2\n" +
"         |          []-TRUE: s3\n" +
"         |                [Leaf-]\n" +
"         +-(s1=r2)\n" +
"              []-(s2=r1)\n" +
"               |    []-(s3!=s2)\n" +
"               |     |    [Leaf-]\n" +
"               |     +-(s3=s2)\n" +
"               |          [Leaf+]\n" +
"               +-(s2<r1)\n" +
"               |    []-(s3!=s2)\n" +
"               |     |    [Leaf-]\n" +
"               |     +-(s3=s2)\n" +
"               |          [Leaf+]\n" +
"               +-(s2>r1)\n" +
"                    []-(s3!=r1)\n" +
"                     |    [Leaf-]\n" +
"                     +-(s3=r1)\n" +
"                          [Leaf+]\n";
        
        String tree = sdt.toString();
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        Assert.assertEquals(tree, expectedTree);
//        logger.log(Level.FINE, "final SDT: \n{0}", tree);
//
//        Parameter p1 = new Parameter(ModerateTCPSUL.DOUBLE_TYPE, 1);
//        Parameter p2 = new Parameter(ModerateTCPSUL.DOUBLE_TYPE, 2);
//        Parameter p3 = new Parameter(ModerateTCPSUL.DOUBLE_TYPE, 3);
//
//        PIV testPiv = new PIV();
//        testPiv.put(p1, new Register(ModerateTCPSUL.DOUBLE_TYPE, 1));
//        testPiv.put(p2, new Register(ModerateTCPSUL.DOUBLE_TYPE, 2));
//        testPiv.put(p3, new Register(ModerateTCPSUL.DOUBLE_TYPE, 3));
//
//        Branching b = mto.getInitialBranching(
//                prefix, ModerateTCPSUL.OFFER, testPiv, sdt);
//
//        Assert.assertEquals(b.getBranches().size(), 2);
//        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }


}
