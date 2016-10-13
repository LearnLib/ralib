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

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.example.fresh.SessionManagerSUL;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class TestEquWithFresh extends RaLibTestSuite {

    @Test
    public void testSessionExample() {
    	SessionManagerSUL sul = new SessionManagerSUL();
    		IntegerEqualityTheory theory = new IntegerEqualityTheory(SessionManagerSUL.INT_TYPE);
    				theory.setCheckForFreshOutputs(true, new SULOracle(sul, SessionManagerSUL.ERROR));
    				final Map<DataType, Theory> teachers = new LinkedHashMap<>();
           teachers.put(SessionManagerSUL.INT_TYPE, 
        		   theory);
           
           
           JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
           MultiTheoryTreeOracle mto = TestUtil.createMTO(
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
//        		   Word.fromSymbols(
//                   new PSymbolInstance(SessionManagerSUL.ISESSION,
//                           new DataValue(SessionManagerSUL.INT_TYPE, 1)));
           
           // create a symbolic suffix from the concrete suffix
           // symbolic data values: s1, s2 (userType, passType)
           final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, longsuffix);
           logger.log(Level.FINE, "Prefix: {0}", prefix);
           logger.log(Level.FINE, "Suffix: {0}", symSuffix);

           TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
           SymbolicDecisionTree sdt = res.getSdt();

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
           Assert.assertEquals(tree, expectedTree);
           logger.log(Level.FINE, "final SDT: \n{0}", tree);

           Parameter p1 = new Parameter(SessionManagerSUL.INT_TYPE, 1);
           Parameter p2 = new Parameter(SessionManagerSUL.INT_TYPE, 2);
           Parameter p3 = new Parameter(SessionManagerSUL.INT_TYPE, 3);

           PIV testPiv = new PIV();
           testPiv.put(p1, new Register(SessionManagerSUL.INT_TYPE, 1));
           testPiv.put(p2, new Register(SessionManagerSUL.INT_TYPE, 2));
           testPiv.put(p3, new Register(SessionManagerSUL.INT_TYPE, 3));

           Branching b = mto.getInitialBranching(
                   prefix, SessionManagerSUL.ISESSION, testPiv, sdt);

           Assert.assertEquals(b.getBranches().size(), 2);
           logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }
   
}
