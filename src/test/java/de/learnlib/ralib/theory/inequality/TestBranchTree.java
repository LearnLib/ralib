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
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.example.ineq.BranchSUL;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class TestBranchTree extends RaLibTestSuite {

    @Test
    public void testOneWayTCPTree() {
    	
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(BranchSUL.DOUBLE_TYPE, 
                new DoubleInequalityTheory(BranchSUL.DOUBLE_TYPE));

        BranchSUL sul = new BranchSUL();
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, BranchSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
                
        

        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(BranchSUL.IINIT,
                        new DataValue(BranchSUL.DOUBLE_TYPE, 1.0),
                        new DataValue(BranchSUL.DOUBLE_TYPE, 4.0),
                        new DataValue(BranchSUL.DOUBLE_TYPE, 7.0)),
                new PSymbolInstance(BranchSUL.OK));
        

        final Word<PSymbolInstance> firstTwoSuffix = suffixNok(BranchSUL.ITEST_FIRST_TWO);
        final Word<PSymbolInstance> lastTwoSuffix = suffixNok(BranchSUL.ITEST_LAST_TWO);
        final Word<PSymbolInstance> eqDeqSuffix = suffixNok(BranchSUL.ITEST_EQU_DISEQ); 
        final Word<PSymbolInstance> eqDeqLastTwoSuffix = suffixNok(BranchSUL.ITEST_EQU_DISEQ, BranchSUL.ITEST_LAST_TWO);
        final Word<PSymbolInstance> trueLastTwoSuffix = suffixNok(BranchSUL.ITEST_EQU_DISEQ, BranchSUL.ITEST_LAST_TWO);
        

        this.test(teachers, mto, sul,  prefix, firstTwoSuffix, null);
        this.test(teachers, mto, sul,  prefix, lastTwoSuffix, null);
        this.test(teachers, mto, sul,  prefix, eqDeqSuffix, null);
        this.test(teachers, mto, sul,  prefix, eqDeqLastTwoSuffix, null);
        this.test(teachers, mto, sul,  prefix, trueLastTwoSuffix, null);
        this.test(teachers, mto, sul,  Word.epsilon(), eqDeqSuffix, null);
        
        SymbolicDecisionTree sdt1 = this.test(teachers, mto, sul,  prefix, suffix(BranchSUL.ITEST_LAST_TWO, BranchSUL.OK), null);
        SymbolicDecisionTree sdt2 = this.test(teachers, mto, sul,  prefix, suffix(BranchSUL.ITEST_LAST_TWO, BranchSUL.OK,
        		BranchSUL.ITEST_IS_MAX, BranchSUL.NOK), null);
        VarMapping relabel = new VarMapping();
        relabel.put(new SymbolicDataValue.Register(BranchSUL.DOUBLE_TYPE, 1), new SymbolicDataValue.Register(BranchSUL.DOUBLE_TYPE, 2));
        relabel.put(new SymbolicDataValue.Register(BranchSUL.DOUBLE_TYPE, 2), new SymbolicDataValue.Register(BranchSUL.DOUBLE_TYPE, 1));
        sdt2 = sdt2.relabel(relabel);
        PIV piv = new PIV();
        piv.put(new SymbolicDataValue.Parameter(BranchSUL.DOUBLE_TYPE, 1), new SymbolicDataValue.Register(BranchSUL.DOUBLE_TYPE, 1));
        piv.put(new SymbolicDataValue.Parameter(BranchSUL.DOUBLE_TYPE, 2), new SymbolicDataValue.Register(BranchSUL.DOUBLE_TYPE, 2));
        piv.put(new SymbolicDataValue.Parameter(BranchSUL.DOUBLE_TYPE, 3), new SymbolicDataValue.Register(BranchSUL.DOUBLE_TYPE, 3));
        Branching branching = mto.getInitialBranching(prefix, BranchSUL.ITEST_LAST_TWO, piv, sdt1);
        System.out.println("INITIAL: " + branching);
        System.out.println("UPDATED: " + mto.updateBranching(prefix,  BranchSUL.ITEST_LAST_TWO, branching, piv, sdt1, sdt2));
        
    }
    
    private Word<PSymbolInstance> suffixNok(ParameterizedSymbol ... testSymbols ) {
    	Word<PSymbolInstance> retSymbol = Word.epsilon();
    	for (ParameterizedSymbol testSymbol : testSymbols) {
    		retSymbol = retSymbol.concat(Word.fromSymbols(
                    new PSymbolInstance(testSymbol, 
                    		new DataValue(BranchSUL.DOUBLE_TYPE, 2.0)),
                    new PSymbolInstance(BranchSUL.NOK)));
    	}
    	
    	return retSymbol;
    }
    
    private Word<PSymbolInstance> suffix(ParameterizedSymbol ... testSymbols) {
    	boolean input = true;
    	Word<PSymbolInstance> retSymbol = Word.epsilon();
    	for (ParameterizedSymbol testSymbol : testSymbols) {
    		if (input) {
	    		retSymbol = retSymbol.concat(Word.fromSymbols(
	                    new PSymbolInstance(testSymbol, 
	                    		new DataValue(BranchSUL.DOUBLE_TYPE, 2.0))));
    		} else {
    			retSymbol = retSymbol.concat(Word.fromSymbols(
	                    new PSymbolInstance(testSymbol)));
    		}
    		input = !input;
    	}
    	
    	return retSymbol;
    } 
    
    private SymbolicDecisionTree test( Map<DataType, Theory> teachers, MultiTheoryTreeOracle mto, BranchSUL sul, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, String expectedTree) {
         // create a symbolic suffix from the concrete suffix
         // symbolic data values: s1, s2 (userType, passType)
    		final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, suffix, new Constants(), teachers);
         logger.log(Level.FINE, "Prefix: {0}", prefix);
         logger.log(Level.FINE, "Suffix: {0}", symSuffix);
         long inpBefore = sul.getInputs();
         long rstBefore = sul.getResets();
         
         // [p3>r3,p2>r2,p1>r1,]
         TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
         SymbolicDecisionTree sdt = res.getSdt();
         
         System.out.println(sdt);
        
         if (expectedTree != null) {
        	 Assert.assertEquals(sdt, expectedTree);
         }
         System.out.println("inputs: " + (sul.getInputs() - inpBefore) + " resets: " + (sul.getResets() - rstBefore));

         return sdt;
    }


}
