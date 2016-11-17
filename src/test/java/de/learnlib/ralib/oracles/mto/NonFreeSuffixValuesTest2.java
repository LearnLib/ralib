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
package de.learnlib.ralib.oracles.mto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import net.automatalib.words.Word;

import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import org.testng.Assert;


/**
 *
 * @author falk
 */
public class NonFreeSuffixValuesTest2 extends RaLibTestSuite {
        
    @Test
    public void testModelswithOutput() {
 
        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/palindrome.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();
        logger.log(Level.FINE, "SYS: {0}", model);
        
        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            teachers.put(t, new IntegerEqualityTheory(t));
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        MultiTheoryTreeOracle mto = TestUtil.createMTO(sul, ERROR, 
                teachers, consts, new SimpleConstraintSolver(), inputs);
    
        DataType intType = TestUtil.getType("int", loader.getDataTypes());
      
        ParameterizedSymbol i4 = new InputSymbol(
                "IPalindrome4", new DataType[] {intType, intType, intType, intType});

         ParameterizedSymbol oyes = new OutputSymbol(
                "OYes", new DataType[] {}); 
                  
         DataValue d0 = new DataValue(intType, 0);
         DataValue d1 = new DataValue(intType, 1);
         DataValue d2 = new DataValue(intType, 2);
         DataValue d3 = new DataValue(intType, 3);
         DataValue d4 = new DataValue(intType, 4);
         DataValue d5 = new DataValue(intType, 5);
         DataValue d6 = new DataValue(intType, 6);
         DataValue d7 = new DataValue(intType, 7);

        //****** 
        Word<PSymbolInstance> prefix1 = Word.fromSymbols();
        
        Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(i4, d0, d1, d2, d3),
                new PSymbolInstance(oyes));
        
        //**** []((IPalindrome4[s1, s2, s2, s1] OYes[]))
        Word<PSymbolInstance> suffix =  Word.fromSymbols(
                new PSymbolInstance(i4, d4, d5, d6, d7),
                new PSymbolInstance(oyes));
        
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                prefix2, suffix, consts, teachers);
        
        logger.log(Level.FINE, "Prefix: {0}", prefix1);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult tqr = mto.treeQuery(prefix1, symSuffix);       
        String tree = tqr.getSdt().toString();
                
        logger.log(Level.FINE, "PIV: {0}", tqr.getPiv());        
        logger.log(Level.FINE, "SDT: {0}",tree);
        
        String expectedTree = "[]-+\n" +
"  []-TRUE: s1\n" +
"        []-TRUE: s2\n" +
"              []-(s3=s2)\n" +
"               |    []-(s4=s1)\n" +
"               |     |    [Leaf+]\n" +
"               |     +-(s4!=s1)\n" +
"               |          [Leaf-]\n" +
"               +-(s3!=s2)\n" +
"                    []-TRUE: s4\n" +
"                          [Leaf-]\n";
        
        Assert.assertEquals(tree, expectedTree);        
    }
 
}
