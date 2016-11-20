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
package de.learnlib.ralib.theory.succ;

import static de.learnlib.ralib.example.succ.SimpleSuccAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.succ.SimpleSuccAutomatonExample.I_A;
import static de.learnlib.ralib.example.succ.SimpleSuccAutomatonExample.T_SEQ;
import static de.learnlib.ralib.theory.succ.SuccessorMinterms.IN_WINDOW;
import static de.learnlib.ralib.theory.succ.SuccessorMinterms.SUCC;

import java.util.Arrays;
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
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class TestSuccTheory extends RaLibTestSuite {
    
    public void testGetFreshValue() {
        Assert.fail("not implemented");
    }
    

	public void testSuccExample() {
        
        Map<DataType, Theory> theories = new LinkedHashMap<>();
        theories.put(T_SEQ, new SuccessorTheoryInt());
        
        MultiTheoryTreeOracle treeOracle = TestUtil.createMTO(AUTOMATON, theories, new Constants(), new SimpleConstraintSolver());
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(I_A, 
                    new DataValue(T_SEQ, 1)),
                new PSymbolInstance(I_A, 
                        new DataValue(T_SEQ, 1)));
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_A, 
                    new DataValue(T_SEQ, 2)),
                new PSymbolInstance(I_A, 
                        new DataValue(T_SEQ, 2)));
        
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)        
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(prefix, longsuffix, new Constants(), theories);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);        
        
        TreeQueryResult res = treeOracle.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        // to be completed
        String expectedTree = "[r2, r1]-+\n" +
"        []-(s1=r2)\n" +
"         |    []-(s2=r1)\n" +
"         |     |    []-(s3=r2)\n" +
"         |     |     |    []-(s4=r1)\n" +
"         |     |     |     |    [Leaf+]\n" +
"         |     |     |     +-(s4!=r1)\n" +
"         |     |     |          [Leaf-]\n" +
"         |     |     +-(s3!=r2)\n" +
"         |     |          []-TRUE: s4\n" +
"         |     |                [Leaf-]\n" +
"         |     +-(s2!=r1)\n" +
"         |          []-TRUE: s3\n" +
"         |                []-TRUE: s4\n" +
"         |                      [Leaf-]\n" +
"         +-(s1!=r2)\n" +
"              []-TRUE: s2\n" +
"                    []-TRUE: s3\n" +
"                          []-TRUE: s4\n" +
"                                [Leaf-]\n";
        
        String tree = sdt.toString();
        Assert.assertEquals(tree, expectedTree);
        logger.log(Level.FINE, "final SDT: \n{0}", tree);
        
        Parameter p1 = new Parameter(T_SEQ, 1);
        
        PIV testPiv =  new PIV();
        testPiv.put(p1, new Register(T_SEQ, 1));
        
        Branching b = treeOracle.getInitialBranching(prefix, I_A, testPiv, sdt);
        
        // to be completed
        Assert.assertEquals(b.getBranches().size(), 2);
        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }
    
    public void testTreeQuery() {
        Assert.fail("not implemented");    
    }
            
    public void testInstantiate() {
        Assert.fail("not implemented");    
    }

    @Test
    public void testEnumerateCases() {
        
        DataType type = new DataType("int", Integer.class);        
        SuccessorDataValue[] dvs = new SuccessorDataValue[4];
        dvs[0] = new SuccessorDataValue(type, 1, new SuccessorMinterms[] {});
        dvs[1] = new SuccessorDataValue(type, 2, new SuccessorMinterms[] {
            SUCC
        });
        dvs[2] = new SuccessorDataValue(type, 3, new SuccessorMinterms[] {
            IN_WINDOW, IN_WINDOW
        });
        
        ConstraintSolverFactory fact = new ConstraintSolverFactory();
        ConstraintSolver solver = fact.createSolver("z3");        
        WordUtil util = new WordUtil(solver);           
        
        int i=0;
        int j=0;
        for (SuccessorMinterms[] mt : SuccessorTheoryInt.generateMinterms(3)) {
            dvs[3] = new SuccessorDataValue(type, 4, mt);
            int[] result = util.instantiate(dvs);
            
            i++;
            if (result != null) {
                j++;
            }

            System.out.println(Arrays.toString(mt) + " : " + Arrays.toString(result));
                        
        }
        System.out.println(j + " : " + i);
    }
}
