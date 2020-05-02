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
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.example.sumcineq.AbstractIntTCPExample;
import de.learnlib.ralib.example.sumcineq.TwoWayFreshTCPSUL;
import de.learnlib.ralib.example.sumcineq.TwoWayIntFreshTCPSUL;
import de.learnlib.ralib.example.sumcineq.TwoWayTCPSUL;
import de.learnlib.ralib.example.sumcineq.AbstractTCPExample.Option;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.SumCIntegerInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;


public class TestTwoWayTCPTreeOracle extends RaLibTestSuite {

  //  @Test
    public void testModerateTCPTree() {
    	
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
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, TwoWayTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Double> b = new DataValueConstructor<>(TwoWayTCPSUL.DOUBLE_TYPE);
                
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(TwoWayTCPSUL.ISYN, 
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 1.0),
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(TwoWayTCPSUL.OK),
                new PSymbolInstance(TwoWayTCPSUL.ISYNACK,
                        new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 3.0),
                        new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 4.0)),
                new PSymbolInstance(TwoWayTCPSUL.OK));
        
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(TwoWayTCPSUL.ICONNECT,
                        b.fv(1.0)),
                new PSymbolInstance(TwoWayTCPSUL.OK));
        
        final Word<PSymbolInstance> longsuffix2 = Word.fromSymbols(
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
                new PSymbolInstance(TwoWayTCPSUL.OK),
                new PSymbolInstance(TwoWayTCPSUL.ICONNECT,
                        new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 7.0)),
                new PSymbolInstance(TwoWayTCPSUL.NOK));
        
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
				new DataValue<Double>(TwoWayFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
				new DataValue<Double>(TwoWayFreshTCPSUL.DOUBLE_TYPE, win)};
        SumCDoubleInequalityTheory sumCTheory = new SumCDoubleInequalityTheory(TwoWayFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(TwoWayFreshTCPSUL.DOUBLE_TYPE, 
               sumCTheory);

        TwoWayFreshTCPSUL sul = new TwoWayFreshTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(sul, TwoWayFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Double> b = new DataValueConstructor<>(TwoWayFreshTCPSUL.DOUBLE_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(TwoWayFreshTCPSUL.ICONNECT),
                new PSymbolInstance(TwoWayFreshTCPSUL.OCONNECT,
                		b.fv(100001.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.ISYN, 
                		b.dv(100001.0),
                		b.fv(201002.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.OK)
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
                new PSymbolInstance(TwoWayFreshTCPSUL.ISYNACK, 
                		b.dv(201002.0),
                		b.intv(100501.5, 100002.0, 101001.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.NOK),
                new PSymbolInstance(TwoWayFreshTCPSUL.ICONNECT),
                new PSymbolInstance(TwoWayFreshTCPSUL.OCONNECT,
                		b.fv(302003.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.ISYN, 
                		b.dv(302003.0),
                		b.dv(302003.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.NOK));
        
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
				new DataValue<Double>(TwoWayFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
				new DataValue<Double>(TwoWayFreshTCPSUL.DOUBLE_TYPE, win)};
        SumCDoubleInequalityTheory sumCTheory = new SumCDoubleInequalityTheory(TwoWayFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(TwoWayFreshTCPSUL.DOUBLE_TYPE, 
               sumCTheory);

        TwoWayFreshTCPSUL sul = new TwoWayFreshTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, TwoWayFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Double> b = new DataValueConstructor<>(TwoWayFreshTCPSUL.DOUBLE_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(TwoWayFreshTCPSUL.ICONNECT),
                new PSymbolInstance(TwoWayFreshTCPSUL.OCONNECT,
                		b.fv(100001.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.ISYN, 
                		b.dv(100001.0),
                		b.fv(201002.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.OK),
                new PSymbolInstance(TwoWayFreshTCPSUL.ICONNECT),
                new PSymbolInstance(TwoWayFreshTCPSUL.OCONNECT,
                		b.fv(302003.0)));
        
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(TwoWayFreshTCPSUL.ISYNACK, 
                		b.dv(201002.0),
                		b.intv(100501.5, 100002.0, 101001.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.OK),
                new PSymbolInstance(TwoWayFreshTCPSUL.IACK, 
                		b.dv(302003.0),
                		b.dv(302003.0)),
                new PSymbolInstance(TwoWayFreshTCPSUL.OK));
        
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
				new DataValue<Integer>(TwoWayIntFreshTCPSUL.INT_TYPE, 1), // for successor
				new DataValue<Integer>(TwoWayIntFreshTCPSUL.INT_TYPE, win)};
        SumCIntegerInequalityTheory sumCTheory = new SumCIntegerInequalityTheory(TwoWayIntFreshTCPSUL.INT_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(TwoWayIntFreshTCPSUL.INT_TYPE, 
               sumCTheory);

        TwoWayIntFreshTCPSUL sul = new TwoWayIntFreshTCPSUL(win);
        sul.configure(AbstractIntTCPExample.Option.WIN_SYNRECEIVED_TO_CLOSED, AbstractIntTCPExample.Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, TwoWayIntFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Integer> b = new DataValueConstructor<>(TwoWayIntFreshTCPSUL.INT_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(TwoWayIntFreshTCPSUL.ICONNECT),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.OCONNECT,
                		b.fv(100001)),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.ISYN, 
                		b.dv(100001),
                		b.fv(201002)),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.OK)
        				);
        
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(TwoWayIntFreshTCPSUL.ISYNACK, 
                		b.dv(100001),
                		b.intv(100501, 100002, 101001)),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.NOK),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.ICONNECT),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.OCONNECT,
                		b.fv(302003)),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.ISYN, 
                		b.dv(302003),
                		b.dv(100001)),
                new PSymbolInstance(TwoWayIntFreshTCPSUL.NOK)
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
