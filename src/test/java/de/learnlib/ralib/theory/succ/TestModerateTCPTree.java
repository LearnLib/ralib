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
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.example.succ.AbstractTCPExample.Option;
import de.learnlib.ralib.example.succ.IntAbstractTCPExample;
import de.learnlib.ralib.example.succ.IntModerateFreshTCPSUL;
import de.learnlib.ralib.example.succ.ModerateFreshTCPSUL;
import de.learnlib.ralib.example.succ.ModerateTCPSUL;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.DeterminizerDataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.SumCIntegerInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;


public class TestModerateTCPTree extends RaLibTestSuite {

  //  @Test
    public void testModerateTCPTree() {
    	
    	Double win = 1000.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Double>(ModerateTCPSUL.DOUBLE_TYPE, 1.0), // for successor
				new DataValue<Double>(ModerateTCPSUL.DOUBLE_TYPE, win)};
        teachers.put(ModerateTCPSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(ModerateTCPSUL.DOUBLE_TYPE,
                		Arrays.asList(sumConsts), // for window size
                		Collections.emptyList()));

        ModerateTCPSUL sul = new ModerateTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, ModerateTCPSUL.ERROR, teachers, 
                consts, jsolv, 
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
        final GeneralizedSymbolicSuffix symSuffix = GeneralizedSymbolicSuffix.fullSuffix(longsuffix, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        final GeneralizedSymbolicSuffix symSuffix2 = GeneralizedSymbolicSuffix.fullSuffix(longsuffix2, consts, teachers);
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

        Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 3);
        Assert.assertEquals(((SDT)sdt2).getNumberOfLeaves() , 3);

    }
    
 //   @Test
    public void testModerateFreshTCPTree() {
    	
    	Double win = 1000.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Double>(ModerateFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
				new DataValue<Double>(ModerateFreshTCPSUL.DOUBLE_TYPE, win)};
        SumCDoubleInequalityTheory sumCTheory = new SumCDoubleInequalityTheory(ModerateFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(ModerateFreshTCPSUL.DOUBLE_TYPE, 
               sumCTheory);

        ModerateFreshTCPSUL sul = new ModerateFreshTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(sul, ModerateFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Double> b = new DataValueConstructor<>(ModerateFreshTCPSUL.DOUBLE_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(ModerateFreshTCPSUL.ICONNECT),
                new PSymbolInstance(ModerateFreshTCPSUL.OCONNECT,
                		b.fv(100001.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYN, 
                		b.dv(100001.0),
                		b.fv(201002.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.OK)
                );
        
        
        // 2 million inputs
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
//        		new PSymbolInstance(ModerateFreshTCPSUL.ICONNECT),
//                new PSymbolInstance(ModerateFreshTCPSUL.OCONNECT,
//                		b.fv(100001.0)),
//                new PSymbolInstance(ModerateFreshTCPSUL.ISYN, 
//                		b.dv(100001.0),
//                		b.fv(201002.0)),
//                new PSymbolInstance(ModerateFreshTCPSUL.OK),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYNACK, 
                		b.dv(201002.0),
                		b.intv(100501.5, 100002.0, 101001.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.NOK),
                new PSymbolInstance(ModerateFreshTCPSUL.ICONNECT),
                new PSymbolInstance(ModerateFreshTCPSUL.OCONNECT,
                		b.fv(302003.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYN, 
                		b.dv(302003.0),
                		b.dv(302003.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.NOK));
        
        // create a symbolic suffix from the concrete suffix
        final GeneralizedSymbolicSuffix symSuffix = GeneralizedSymbolicSuffix.fullSuffix(longsuffix, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        System.out.println(symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        System.out.println(sdt);
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        
        Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6);
    }

//    @Test
    public void testModerateFreshTCPTreeMerge() {
    	
    	Double win = 1000.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Double>(ModerateFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
				new DataValue<Double>(ModerateFreshTCPSUL.DOUBLE_TYPE, win)};
        SumCDoubleInequalityTheory sumCTheory = new SumCDoubleInequalityTheory(ModerateFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(ModerateFreshTCPSUL.DOUBLE_TYPE, 
               sumCTheory);

        ModerateFreshTCPSUL sul = new ModerateFreshTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, ModerateFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Double> b = new DataValueConstructor<>(ModerateFreshTCPSUL.DOUBLE_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(ModerateFreshTCPSUL.ICONNECT),
                new PSymbolInstance(ModerateFreshTCPSUL.OCONNECT,
                		b.fv(100001.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYN, 
                		b.dv(100001.0),
                		b.fv(201002.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.OK),
                new PSymbolInstance(ModerateFreshTCPSUL.ICONNECT),
                new PSymbolInstance(ModerateFreshTCPSUL.OCONNECT,
                		b.fv(302003.0)));
        
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(ModerateFreshTCPSUL.ISYNACK, 
                		b.dv(201002.0),
                		b.intv(100501.5, 100002.0, 101001.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.OK),
                new PSymbolInstance(ModerateFreshTCPSUL.IACK, 
                		b.dv(302003.0),
                		b.dv(302003.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.OK));
        
        // create a symbolic suffix from the concrete suffix
        final GeneralizedSymbolicSuffix symSuffix = GeneralizedSymbolicSuffix.fullSuffix(longsuffix, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        System.out.println(symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        System.out.println(sdt);
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        
        Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 4);
    }
    
    
    @Test
    public void testIntModerateFreshTCPTreeMerge() {
    	
    	Integer win = 1000;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Integer>(IntModerateFreshTCPSUL.INT_TYPE, 1), // for successor
				new DataValue<Integer>(IntModerateFreshTCPSUL.INT_TYPE, win)};
        SumCIntegerInequalityTheory sumCTheory = new SumCIntegerInequalityTheory(IntModerateFreshTCPSUL.INT_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(IntModerateFreshTCPSUL.INT_TYPE, 
               sumCTheory);

        IntModerateFreshTCPSUL sul = new IntModerateFreshTCPSUL(win);
        sul.configure(IntAbstractTCPExample.Option.WIN_SYNRECEIVED_TO_CLOSED, IntAbstractTCPExample.Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, IntModerateFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Integer> b = new DataValueConstructor<>(IntModerateFreshTCPSUL.INT_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(IntModerateFreshTCPSUL.ICONNECT),
                new PSymbolInstance(IntModerateFreshTCPSUL.OCONNECT,
                		b.fv(100001)),
                new PSymbolInstance(IntModerateFreshTCPSUL.ISYN, 
                		b.dv(100001),
                		b.fv(201002)),
                new PSymbolInstance(IntModerateFreshTCPSUL.OK)
        				);
        
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(IntModerateFreshTCPSUL.ISYNACK, 
                		b.dv(100001),
                		b.intv(100501, 100002, 101001)),
                new PSymbolInstance(IntModerateFreshTCPSUL.NOK),
                new PSymbolInstance(IntModerateFreshTCPSUL.ICONNECT),
                new PSymbolInstance(IntModerateFreshTCPSUL.OCONNECT,
                		b.fv(302003)),
                new PSymbolInstance(IntModerateFreshTCPSUL.ISYN, 
                		b.dv(302003),
                		b.dv(100001)),
                new PSymbolInstance(IntModerateFreshTCPSUL.NOK)
                );
        
        // create a symbolic suffix from the concrete suffix
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, longsuffix, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        System.out.println(symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        System.out.println(sdt);
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        
        Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6);
    }
}
