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
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.example.sumc.inequality.TwoWayTCPSULMultitype;
import de.learnlib.ralib.example.sumc.inequality.TCPExample.Option;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleSumCInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class TestTwoWayTCPMultitypeTreeOracle extends RaLibTestSuite {
	
    @Test
    public void testModerateTCPTree() {
    	Double win = 100.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 
                new DoubleSumCInequalityTheory(TwoWayTCPSULMultitype.DOUBLE_TYPE1,
                		Arrays.asList(
                				new DataValue<Double>(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 1.0), // for successor
                				new DataValue<Double>(TwoWayTCPSULMultitype.DOUBLE_TYPE1, win)), // for window size
                		Collections.emptyList()));

        teachers.put(TwoWayTCPSULMultitype.DOUBLE_TYPE2, 
                new DoubleSumCInequalityTheory(TwoWayTCPSULMultitype.DOUBLE_TYPE2,
                		Arrays.asList(
                				new DataValue<Double>(TwoWayTCPSULMultitype.DOUBLE_TYPE2, 1.0), // for successor
                				new DataValue<Double>(TwoWayTCPSULMultitype.DOUBLE_TYPE2, win)), // for window size
                		Collections.emptyList()));
        
        TwoWayTCPSULMultitype sul = new TwoWayTCPSULMultitype(win);
        sul.configure(Option.WIN_CONNECTING_TO_CLOSED, Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, TwoWayTCPSULMultitype.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
        
        /*
         * Same prefix
         */
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
        		new PSymbolInstance(TwoWayTCPSULMultitype.ICONNECT,
                        new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 1.0)),
                new PSymbolInstance(TwoWayTCPSULMultitype.OK), 
                new PSymbolInstance(TwoWayTCPSULMultitype.ISYN, 
                		new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 1.0),
                		new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE2, 1.0)),
                new PSymbolInstance(TwoWayTCPSULMultitype.OK));
                
        final Word<PSymbolInstance> suffix1 = Word.fromSymbols(
                new PSymbolInstance(TwoWayTCPSULMultitype.ISYNACK,
                        new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE2, 1.0),
                        new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 2.0)),
                new PSymbolInstance(TwoWayTCPSULMultitype.OK),
                new PSymbolInstance(TwoWayTCPSULMultitype.IACK,
                        new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 1.0),
                        new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE2, 2.0)),
                new PSymbolInstance(TwoWayTCPSULMultitype.OK)
                
        		);
        
        

        final GeneralizedSymbolicSuffix symSuffix1 = new GeneralizedSymbolicSuffix(prefix, suffix1, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix1);
        
        TreeQueryResult res1 = mto.treeQuery(prefix, symSuffix1);
        logger.log(Level.FINE, "inputs: {0} resets: {1}", new Object[] {sul.getInputs(), sul.getResets()});
        long crtInputs = sul.getInputs(), crtResets = sul.getResets();
        SymbolicDecisionTree sdt1 = res1.getSdt();
        
        final String expectedTree1 = "[r1]-+\n" +
        		"    []-TRUE: s1\n" +
        		"          []-(s2=r1 + 1.0)\n" +
        		"           |    []-(s3=s2)\n" +
        		"           |     |    []-(s4=s1 + 1.0)\n" +
        		"           |     |     |    [Leaf+]\n" +
        		"           |     |     +-(s4!=s1 + 1.0)\n" +
        		"           |     |          [Leaf-]\n" +
        		"           |     +-(s3!=s2)\n" +
        		"           |          []-TRUE: s4\n" +
        		"           |                [Leaf-]\n" +
        		"           +-(s2!=r1 + 1.0)\n" +
        		"                []-TRUE: s3\n" +
        		"                      []-TRUE: s4\n" +
        		"                            [Leaf-]\n";
        
        String tree1 = sdt1.toString();
        Assert.assertEquals(tree1, expectedTree1);
        
        final Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(TwoWayTCPSULMultitype.ISYNACK,
                        new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE2, 1.0),
                        new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 1.0)),
                new PSymbolInstance(TwoWayTCPSULMultitype.NOK),
                new PSymbolInstance(TwoWayTCPSULMultitype.ISYNACK,
                		new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE2, 1.0),
                        new DataValue(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 1.0)),
                new PSymbolInstance(TwoWayTCPSULMultitype.OK));
//        
        final GeneralizedSymbolicSuffix symSuffix2 = new GeneralizedSymbolicSuffix(prefix, suffix2, new Constants(), teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix2);
        

        TreeQueryResult res2 = mto.treeQuery(prefix, symSuffix2);
        logger.log(Level.FINE, "inputs: {0} resets: {1}", 
        		new Object[] {sul.getInputs() - crtInputs, sul.getResets() - crtResets});
        
        SymbolicDecisionTree sdt2 = res2.getSdt();
        String expectedTree2 = "[r1]-+\n" +
        		"    []-TRUE: s1\n" +
        		"          []-(s2<=r1 + 1.0)\n" +
        		"           |    []-TRUE: s3\n" +
        		"           |          []-TRUE: s4\n" +
        		"           |                [Leaf-]\n" +
        		"           +-(r1 + 1.0<s2<r1 + 100.0)\n" +
        		"           |    []-TRUE: s3\n" +
        		"           |          []-(s4=r1 + 1.0)\n" +
        		"           |           |    [Leaf+]\n" +
        		"           |           +-(s4!=r1 + 1.0)\n" +
        		"           |                [Leaf-]\n" +
        		"           +-(s2>=r1 + 100.0)\n" +
        		"                []-TRUE: s3\n" +
        		"                      []-TRUE: s4\n" +
        		"                            [Leaf-]\n";
        
        
        String tree2 = sdt2.toString();
        Assert.assertEquals(tree2, expectedTree2);
        
        Parameter p1 = new Parameter(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 1);
        Parameter p2 = new Parameter(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 2);
        Parameter p3 = new Parameter(TwoWayTCPSULMultitype.DOUBLE_TYPE2, 3);

        PIV testPiv = new PIV();
        testPiv.put(p1, new Register(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 1));
        testPiv.put(p2, new Register(TwoWayTCPSULMultitype.DOUBLE_TYPE1, 2));
        testPiv.put(p3, new Register(TwoWayTCPSULMultitype.DOUBLE_TYPE2, 3));
        
        Branching b1 = mto.getInitialBranching(
                prefix, TwoWayTCPSULMultitype.ISYNACK, testPiv, sdt1);
        Assert.assertEquals(b1.getBranches().size(), 2);
        logger.log(Level.FINE, "initial branching for SDT1: \n{0}", b1.getBranches().toString());

        Branching b2 = mto.getInitialBranching(
                prefix, TwoWayTCPSULMultitype.ISYNACK, testPiv, sdt2);
        Assert.assertEquals(b2.getBranches().size(), 3);
        logger.log(Level.FINE, "initial branching for SDT2: \n{0}", b2.getBranches().toString());
    }


}
