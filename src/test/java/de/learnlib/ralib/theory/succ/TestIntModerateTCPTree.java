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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.example.succ.IntModerateFreshTCPSUL;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.mapper.ValueCanonizer;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.Slice;
import de.learnlib.ralib.oracles.mto.SliceBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixBuilder;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.sul.examples.IntAbstractTCPExample.Option;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCIntegerInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;


public class TestIntModerateTCPTree extends RaLibTestSuite {

  // @Test
    public void testModerateFreshTCPTree() {

        DataValueConstructor<Integer> b = new DataValueConstructor<>(IntModerateFreshTCPSUL.INT_TYPE);
    	  final Word<PSymbolInstance> prefix = //Word.epsilon(); 
          		Word.fromSymbols(
                  new PSymbolInstance(IntModerateFreshTCPSUL.ICONNECT),
                  new PSymbolInstance(IntModerateFreshTCPSUL.OCONNECT,
                  		b.fv(100000))
                  ,
                  new PSymbolInstance(IntModerateFreshTCPSUL.ISYN, 
                  		b.dv(100000),
                  		b.fv(200000)),
                  new PSymbolInstance(IntModerateFreshTCPSUL.OK) );
          
          
          final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                  new PSymbolInstance(IntModerateFreshTCPSUL.ISYNACK, 
                  		b.dv(100000),
                  		b.intv(100501, 100002, 101000)),
                  new PSymbolInstance(IntModerateFreshTCPSUL.NOK),
                  new PSymbolInstance(IntModerateFreshTCPSUL.ICONNECT),
                  new PSymbolInstance(IntModerateFreshTCPSUL.OCONNECT,
                  		b.fv(300000)),
                  new PSymbolInstance(IntModerateFreshTCPSUL.ISYN, 
                  		b.dv(300000),
                  		b.dv(300000)),
                  new PSymbolInstance(IntModerateFreshTCPSUL.NOK) );
        
        testTreeQuery( prefix, longsuffix, (sdt) -> 
        Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 3) );
    }
    
    
	public void testModerateFreshTCPTree2() {
        DataValueConstructor<Integer> b = new DataValueConstructor<>(IntModerateFreshTCPSUL.INT_TYPE);
		 final Word<PSymbolInstance> prefix = //Word.epsilon(); 
	        		Word.fromSymbols(
	                new PSymbolInstance(IntModerateFreshTCPSUL.ICONNECT),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OCONNECT,
	                		b.fv(100000))
	                ,
	                new PSymbolInstance(IntModerateFreshTCPSUL.ISYN, 
	                		b.dv(100000),
	                		b.fv(200000)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OK),
	                new PSymbolInstance(IntModerateFreshTCPSUL.ISYNACK, 
	                		b.fv(300000),
	                		b.sumcv(100000,1)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OK),
	                new PSymbolInstance(IntModerateFreshTCPSUL.IACK, 
	                		b.sumcv(100000,1),
	                		b.sumcv(300000,1)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OK));
	        
	        
	        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
	                new PSymbolInstance(IntModerateFreshTCPSUL.IFINACK, 
	                		b.dv(100001),
	                		b.dv(300001)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OK));
	        
	        testTreeQuery(prefix, longsuffix); 
	        		//sdt -> Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6));
	  //      Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6);
	    }
    
	public void testModerateFreshTCPTree3() {
        DataValueConstructor<Integer> b = new DataValueConstructor<>(IntModerateFreshTCPSUL.INT_TYPE);
		 final Word<PSymbolInstance> prefix = //Word.epsilon(); 
	        		Word.fromSymbols(
	                new PSymbolInstance(IntModerateFreshTCPSUL.ICONNECT),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OCONNECT,
	                		b.fv(100000))
	                ,
	                new PSymbolInstance(IntModerateFreshTCPSUL.ISYN, 
	                		b.dv(100000),
	                		b.fv(200000)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OK),
	                new PSymbolInstance(IntModerateFreshTCPSUL.ISYNACK, 
	                		b.fv(300000),
	                		b.sumcv(100000,1)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OK),
	                new PSymbolInstance(IntModerateFreshTCPSUL.IACK, 
	                		b.sumcv(100000,1),
	                		b.sumcv(300000,1)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OK));
	        
	        
	        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
	                new PSymbolInstance(IntModerateFreshTCPSUL.IFINACK, 
	                		b.sumcv(100000, 1),
	                		b.sumcv(300000, 1, 1)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OK));
	        
	        testTreeQuery(prefix, longsuffix, 
	        		sdt -> Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 7)
	        		); 
	        		//sdt -> Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6));
	  //      Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6);
	    }
	
	public void testModerateFreshTCPTree4() {
        DataValueConstructor<Integer> b = new DataValueConstructor<>(IntModerateFreshTCPSUL.INT_TYPE);
		 final Word<PSymbolInstance> prefix = //Word.epsilon(); 
	        		Word.fromSymbols(
	                new PSymbolInstance(IntModerateFreshTCPSUL.ICONNECT),
	                new PSymbolInstance(IntModerateFreshTCPSUL.OCONNECT,
	                		b.fv(100000))
	                ,
	                new PSymbolInstance(IntModerateFreshTCPSUL.ISYNACK, 
	                		b.sumcv(100000, 1),
	                		b.sumcv(100000, 1)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.NOK));
	        
	        
	        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
	                new PSymbolInstance(IntModerateFreshTCPSUL.ISYNACK, 
	                		b.sumcv(100000, 1),
	                		b.sumcv(300000, 1, 1)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.NOK),
	                new PSymbolInstance(IntModerateFreshTCPSUL.ISYN, 
	                		b.sumcv(100000, 1),
	                		b.sumcv(300000, 1, 1)),
	                new PSymbolInstance(IntModerateFreshTCPSUL.NOK));
	       
	        
	        testTreeQuery(prefix, (teach, cons) ->  GeneralizedSymbolicSuffix.fullSuffix(longsuffix, cons, teach)
	        		, sdt -> Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 7) ); 
	        		//sdt -> Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6));
	  //      Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6);
	    }
	
	@SafeVarargs
	public final void testTreeQuery(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Consumer<SDT>... assertions) {
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
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(teachers, consts), sul), IntModerateFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        SliceBuilder sb = new SliceBuilder(teachers, consts, jsolv);
        // create a symbolic suffix from the concrete suffix
        Slice slice = sb.sliceFromWord(prefix, suffix);
        System.out.println("Slice from word: " + slice);
        GeneralizedSymbolicSuffix symSuffix = SymbolicSuffixBuilder.suffixFromSlice(DataWords.actsOf(suffix), slice);
//        GeneralizedSymbolicSuffix symSuffix = 
//                SymbolicSuffixBuilder.suffixFromSlice(DataWords.actsOf(suffix), slice);
        
        //symSuffix = symSuffix.getSuffixWithExtendedRelationsOnFirstAction();
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

       // System.out.println(((SDT)sdt).getVariables());
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        System.out.println("Result: " + sdt.toString());
        
        for (Consumer<SDT> assertion : assertions) {
        	assertion.accept((SDT)sdt);
        }
	}
	

	
	@SafeVarargs
	public final void testTreeQuery(Word<PSymbolInstance> prefix, BiFunction<Map<DataType, Theory>, Constants, GeneralizedSymbolicSuffix> symSuffProvider, Consumer<SDT>... assertions) {
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
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(teachers, consts), sul), IntModerateFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        GeneralizedSymbolicSuffix symSuffix = symSuffProvider.apply(teachers, consts);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        System.out.println(symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

       // System.out.println(((SDT)sdt).getVariables());
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        System.out.println("Result: " + sdt.toString());
        
        for (Consumer<SDT> assertion : assertions) {
        	assertion.accept((SDT)sdt);
        }
	}
	
    public static void main(String args []) {
    	TestIntModerateTCPTree testSuite = new TestIntModerateTCPTree();
//    	testSuite.testModerateFreshTCPTree();
//    	testSuite.testModerateFreshTCPTree2();
//    	testSuite.testModerateFreshTCPTree3();
    	testSuite.testModerateFreshTCPTree4();
    }

}
