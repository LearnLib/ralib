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

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import net.automatalib.words.Word;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 *
 * @author falk
 */
public class SecondSDTBranchingTest extends RaLibTestSuite {
    
    @Test
    public void testModelswithOutput() {


        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/sip.xml");

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
  
        ParameterizedSymbol ipr = new InputSymbol(
                "IPRACK", new DataType[] {intType});

        ParameterizedSymbol inv = new InputSymbol(
                "IINVITE", new DataType[] {intType});

        ParameterizedSymbol o100 = new OutputSymbol(
                "O100", new DataType[] {intType});    

        ParameterizedSymbol o200 = new OutputSymbol(
                "O200", new DataType[] {intType});    

        ParameterizedSymbol o481 = new OutputSymbol(
                "O481", new DataType[] {intType});         

        DataValue d0 = new DataValue(intType, 0);
        DataValue d1 = new DataValue(intType, 1);

        //****** ROW:  IINVITE[0[int]] O100[0[int]] IPRACK[1[int]]
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(inv, d0),
                new PSymbolInstance(o100, d0),
                new PSymbolInstance(ipr, d1));
        
        //**** [s1]((O481[s1]))
        Word<PSymbolInstance> suffix1 =  Word.fromSymbols(
                new PSymbolInstance(o481, d0));
        GeneralizedSymbolicSuffix symSuffix1 = new GeneralizedSymbolicSuffix(
                prefix, suffix1, new Constants(), teachers);
        
        //[s1, s2, s3]((O481[s1] IPRACK[s2] O200[s3]))
        Word<PSymbolInstance> suffix2 =  Word.fromSymbols(
                new PSymbolInstance(o481, d0),
                new PSymbolInstance(ipr, d0),
                new PSymbolInstance(o200, d0));
        GeneralizedSymbolicSuffix symSuffix2 = new GeneralizedSymbolicSuffix(
                prefix, suffix2, new Constants(), teachers);
        
        logger.log(Level.FINE, "{0}", prefix);
        logger.log(Level.FINE, "{0}", symSuffix1);
        logger.log(Level.FINE, "{0}", symSuffix2);
        
        TreeQueryResult tqr1 = mto.treeQuery(prefix, symSuffix1);
        TreeQueryResult tqr2 = mto.treeQuery(prefix, symSuffix2);

        RegisterGenerator rgen = new RegisterGenerator();
        Register r1 = rgen.next(intType);
        Register r2 = rgen.next(intType);
        VarMapping remap = new VarMapping();
        remap.put(r1, r2);
        remap.put(r2, r1);
        
        PIV piv = tqr2.getPiv();
        SymbolicDecisionTree sdt1 = tqr1.getSdt().relabel(remap);
        SymbolicDecisionTree sdt2 = tqr2.getSdt();
       
        logger.log(Level.FINE, "PIV: {0}", piv);
        logger.log(Level.FINE, "SDT1: {0}", sdt1);
        logger.log(Level.FINE, "SDT2: {0}", sdt2);
     
        // combine branching 1+2
        Branching b = mto.getInitialBranching(prefix, o100, piv, sdt1);        
        b = mto.updateBranching(prefix, o100, b, piv, sdt1, sdt2);
   
        String guards = Arrays.toString(b.getBranches().values().toArray());
        logger.log(Level.FINE, "Guards: {0}", guards);
        
        final String expected = "[(r2==p1), (r2!=p1)]";
        Assert.assertEquals(guards, expected);
    }
              
}
