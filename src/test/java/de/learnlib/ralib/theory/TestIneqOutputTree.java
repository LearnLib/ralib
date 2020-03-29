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
package de.learnlib.ralib.theory;

import de.learnlib.api.SULException;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.LinkedHashMap;
import java.util.Map;
import net.automatalib.words.Word;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.utils.SDTBuilder;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import java.util.logging.Level;
import org.testng.Assert;

/**
 *
 * @author falk
 */
public class TestIneqOutputTree extends RaLibTestSuite {

    private final static DataType TYPE = PriorityQueueSUL.DOUBLE_TYPE;    
    private final static InputSymbol IN = new InputSymbol("in", TYPE);
    private final static OutputSymbol OUT = new OutputSymbol("out", TYPE);
    
    private static class BiggerSUL extends DataWordSUL {
         
        @Override
        public void pre() {
        }

        @Override
        public void post() {
        }

        @Override
        public PSymbolInstance step(PSymbolInstance i) throws SULException {
            return new PSymbolInstance(OUT, new DataValue(TYPE,
                    ((double)i.getParameterValues()[0].getId()) + 1.0));
        }
        
    }
    
    
    private static class SmallerSUL extends DataWordSUL {
        
        @Override
        public void pre() {
        }

        @Override
        public void post() {
        }

        @Override
        public PSymbolInstance step(PSymbolInstance i) throws SULException {
            return new PSymbolInstance(OUT, new DataValue(TYPE,
                    ((double)i.getParameterValues()[0].getId()) - 1.0));
        }
    }
    
    private static class MeanSUL extends DataWordSUL {
    	private Double x;
        
        @Override
        public void pre() {
        }

        @Override
        public void post() {
        }

        @Override
        public PSymbolInstance step(PSymbolInstance i) throws SULException {
        	if (x == null) {
        		x = (double)i.getParameterValues()[0].getId();
        		return new PSymbolInstance(OUT, new DataValue(TYPE,x));
        	} else {
        		double aux = x;
        		x = (double)i.getParameterValues()[0].getId();
        		return new PSymbolInstance(OUT, new DataValue(TYPE, (aux + x) / 2.0));
        	}
        }
    }
    
    private void genericIneqTest(DataWordSUL sul, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, SymbolicDecisionTree expectedSdt) {

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(TYPE, new DoubleInequalityTheory(TYPE));

        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, PriorityQueueSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                IN);
        
        GeneralizedSymbolicSuffix symSuffix = 
                new GeneralizedSymbolicSuffix(prefix, suffix,
                        new Constants(), teachers);
        

        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();
        
        String tree = sdt.toString();
        Assert.assertEquals(tree, expectedSdt.toString());
        logger.log(Level.FINE, "final SDT: \n{0}", tree);
    }
    
    
    @Test
    public void testBiggerIneqEqTree() {

    	BiggerSUL sul = new BiggerSUL();
                
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(IN, new DataValue(TYPE, 1.0)));
                
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(OUT, new DataValue(TYPE, 1.0)));

        SymbolicDecisionTree expectedSdt = new SDTBuilder(TYPE)
        		.lsrEq("r1").reject().up()
        		.grt("r1").accept().up()
        		.build();
        		
        genericIneqTest(sul, prefix, suffix, expectedSdt);
    }


    @Test
    public void testSmallerIneqEqTree() {
    	SmallerSUL sul = new SmallerSUL();
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(IN, new DataValue(TYPE, 1.0)));
                
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(OUT, new DataValue(TYPE, 1.0)));

        SymbolicDecisionTree expectedSdt = new SDTBuilder(TYPE)
        		.lsr("r1").accept().up()
        		.grtEq("r1").reject().up()
        		.build();
        		
        genericIneqTest(sul, prefix, suffix, expectedSdt);
    }
    
    @Test
    public void testMeanIneqEqTree() {
    	MeanSUL sul = new MeanSUL();
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(IN, new DataValue(TYPE, 1.0)),
                new PSymbolInstance(OUT, new DataValue(TYPE, 1.0))
        		);
                
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
        		new PSymbolInstance(IN, new DataValue(TYPE, 1.0)),
                new PSymbolInstance(OUT, new DataValue(TYPE, 1.0)));

        SymbolicDecisionTree expectedSdt = new SDTBuilder(TYPE)
        		.lsr("r1").accept().up()
        		.grtEq("r1").reject().up()
        		.build();
        		
        genericIneqTest(sul, prefix, suffix, expectedSdt);

    }
}
