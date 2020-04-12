/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.theory.equality;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.example.fresh.SessionManagerSUL;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.mapper.MultiTheoryDeterminizer;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.DeterminizerDataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.utils.SDTAssert;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class TestEquWithFresh extends RaLibTestSuite {
	
    @Test
    public void testDeterminizer() {
    	SessionManagerSUL sul = new SessionManagerSUL();

    	IntegerEqualityTheory theory = new IntegerEqualityTheory(SessionManagerSUL.INT_TYPE);

				final Map<DataType, Theory> teachers = new LinkedHashMap<>();
       teachers.put(SessionManagerSUL.INT_TYPE, 
    		   theory);
		BasicSULOracle sulOracle = new BasicSULOracle(new DeterminizerDataWordSUL(teachers, new Constants(), sul), SessionManagerSUL.ERROR);
		
		theory.setCheckForFreshOutputs(true);
       Word<PSymbolInstance> testWord = Word.fromSymbols(
    		   new PSymbolInstance(SessionManagerSUL.ISESSION,
                       new DataValue(SessionManagerSUL.INT_TYPE, 1)),
               new PSymbolInstance(SessionManagerSUL.OSESSION,
                       new DataValue(SessionManagerSUL.INT_TYPE, 0)),
               new PSymbolInstance(SessionManagerSUL.ILOGIN,
            		   new DataValue(SessionManagerSUL.INT_TYPE, 1),
            		   new DataValue(SessionManagerSUL.INT_TYPE, 0))
               , new PSymbolInstance(SessionManagerSUL.OK)
               );
       Word<PSymbolInstance> testInputWord = Word.fromSymbols(
    		   new PSymbolInstance(SessionManagerSUL.ISESSION,
                       new DataValue(SessionManagerSUL.INT_TYPE, 0)),
    		   new PSymbolInstance(SessionManagerSUL.ISESSION,
                       new DataValue(SessionManagerSUL.INT_TYPE, 0)),
    		   new PSymbolInstance(SessionManagerSUL.ILOGIN,
            		   new DataValue(SessionManagerSUL.INT_TYPE, 1),
            		   new DataValue(SessionManagerSUL.INT_TYPE, 3)),
    		   new PSymbolInstance(SessionManagerSUL.ILOGIN,
            		   new DataValue(SessionManagerSUL.INT_TYPE, 0),
            		   new DataValue(SessionManagerSUL.INT_TYPE, 1))
               );
       
       final DeterminizerDataWordSUL sulDet = new DeterminizerDataWordSUL(teachers, new Constants(), sul);
		theory.setCheckForFreshOutputs(true);
       Word<PSymbolInstance> actualTrace = Word.epsilon();
       MultiTheoryDeterminizer canonizer = new MultiTheoryDeterminizer(teachers, new Constants());
       sulDet.pre();
       for (PSymbolInstance symbol : testInputWord) {
    	   symbol = canonizer.canonize(symbol, false);
    	   actualTrace = actualTrace.append(symbol);
    	   PSymbolInstance out = sulDet.step(symbol);
    	   out = canonizer.canonize(out, false);
    	   actualTrace = actualTrace.append(out);
       }
       
       Word<PSymbolInstance> expectedTrace = Word.fromSymbols(
    		   new PSymbolInstance(SessionManagerSUL.ISESSION,
                       new DataValue(SessionManagerSUL.INT_TYPE, 0)),
    		   new PSymbolInstance(SessionManagerSUL.OSESSION,
                       new DataValue(SessionManagerSUL.INT_TYPE, 1)),
    		   new PSymbolInstance(SessionManagerSUL.ISESSION,
                       new DataValue(SessionManagerSUL.INT_TYPE, 0)),
    		   new PSymbolInstance(SessionManagerSUL.OSESSION,
                       new DataValue(SessionManagerSUL.INT_TYPE, 1)),
    		   new PSymbolInstance(SessionManagerSUL.ILOGIN,
            		   new DataValue(SessionManagerSUL.INT_TYPE, 1),
            		   new DataValue(SessionManagerSUL.INT_TYPE, 2)),
    		   new PSymbolInstance(SessionManagerSUL.NOK),
    		   new PSymbolInstance(SessionManagerSUL.ILOGIN,
            		   new DataValue(SessionManagerSUL.INT_TYPE, 0),
            		   new DataValue(SessionManagerSUL.INT_TYPE, 1)),
               new PSymbolInstance(SessionManagerSUL.OK));
       
       Assert.assertEquals(actualTrace, expectedTrace);
       long numberOfFreshValues = actualTrace.stream().flatMap(sym -> Stream.of(sym.getParameterValues())).filter(dv -> dv instanceof FreshValue).count();
       Assert.assertEquals(numberOfFreshValues, 3);
    }

    @Test
    public void testSessionExample() {
    	SessionManagerSUL sul = new SessionManagerSUL();
    		IntegerEqualityTheory theory = new IntegerEqualityTheory(SessionManagerSUL.INT_TYPE);
    				theory.setCheckForFreshOutputs(true);
    				final Map<DataType, Theory> teachers = new LinkedHashMap<>();
           teachers.put(SessionManagerSUL.INT_TYPE, 
        		   theory);
           
           JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
           MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                   sul, SessionManagerSUL.ERROR, teachers, 
                   new Constants(), jsolv, 
                   sul.getInputSymbols());

           final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
        		   new PSymbolInstance(SessionManagerSUL.ISESSION,
                           new DataValue(SessionManagerSUL.INT_TYPE, 1)),
                   new PSymbolInstance(SessionManagerSUL.OSESSION,
                           new DataValue(SessionManagerSUL.INT_TYPE, 5)),
                   new PSymbolInstance(SessionManagerSUL.ILOGIN,
                		   new DataValue(SessionManagerSUL.INT_TYPE, 5),
                		   new DataValue(SessionManagerSUL.INT_TYPE, 5)),
                   new PSymbolInstance(SessionManagerSUL.OK));
           
           final Word<PSymbolInstance> prefix = Word.epsilon(); 
           
           // create a symbolic suffix from the concrete suffix
           // symbolic data values: s1, s2 (userType, passType)
           final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, longsuffix, new Constants(), teachers);
           logger.log(Level.FINE, "Prefix: {0}", prefix);
           logger.log(Level.FINE, "Suffix: {0}", symSuffix);

           TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
           SymbolicDecisionTree sdt = res.getSdt();
           
           final String expectedTree = "[]-+\n"+
        		   "  []-TRUE: s1\n"+
        		   "        []-TRUE: s2\n"+
        		   "              []-(s3=s1)\n"+
        		   "               |    []-(s4=s2)\n"+
        		   "               |     |    [Leaf+]\n"+
        		   "               |     +-(s4!=s2)\n"+
        		   "               |          [Leaf-]\n"+
        		   "               +-(s3!=s1)\n"+
        		   "                    []-TRUE: s4\n"+
        		   "                          [Leaf-]\n";
        		   
           
           String tree = sdt.toString();
           SDTAssert.assertEquals(tree, expectedTree);
           logger.log(Level.FINE, "final SDT: \n{0}", tree);

           PIV testPiv = new PIV();

           Branching b = mto.getInitialBranching(
                   prefix, SessionManagerSUL.ISESSION, testPiv, sdt);

           Assert.assertEquals(b.getBranches().size(), 1);
           logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }
   
}
