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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import net.automatalib.words.Word;

import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
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
import org.testng.Assert;

/**
 *
 * @author falk
 */
public class LoginBranchingTest extends RaLibTestSuite {
        
    @Test
    public void testBranching() {
    
        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/login_typed.xml");

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
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(sul, ERROR,
                teachers, consts, new SimpleConstraintSolver(), inputs);

        DataType uid = TestUtil.getType("uid", loader.getDataTypes());
        DataType pwd = TestUtil.getType("pwd", loader.getDataTypes());
        
        ParameterizedSymbol reg = new InputSymbol(
                "IRegister", new DataType[] {uid, pwd});

        ParameterizedSymbol log = new InputSymbol(
                "ILogin", new DataType[] {uid, pwd});    
    
        ParameterizedSymbol ok = new OutputSymbol(
                "OOK", new DataType[] {});    

        DataValue u = new DataValue(uid, 0);
        DataValue p = new DataValue(pwd, 0);
        
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(reg, new DataValue[] {u, p}),
                new PSymbolInstance(ok, new DataValue[] {}));

        Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(log, new DataValue[] {u, p}),
                new PSymbolInstance(ok, new DataValue[] {}));        
        
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                prefix, suffix, new Constants(), teachers);
        
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Conc. Suffix: {0}", suffix);
        logger.log(Level.FINE, "Sym. Suffix: {0}", symSuffix);
 
        
        TreeQueryResult tqr = mto.treeQuery(prefix, symSuffix);       
        logger.log(Level.FINE, "PIV: {0}", tqr.getPiv());        
        logger.log(Level.FINE, "SDT: {0}", tqr.getSdt());
        
        // initial branching bug
        // Regression: Why does the last word in the set have a password val. of 2
        
        Branching bug1 = mto.getInitialBranching(prefix, log, tqr.getPiv(), tqr.getSdt());
        final String expectedKeyset = 
                  "[IRegister[0[uid], 0[pwd]] OOK[] ILogin[0[uid], 0[pwd]],"
                + " IRegister[0[uid], 0[pwd]] OOK[] ILogin[0[uid], 1[pwd]],"
                + " IRegister[0[uid], 0[pwd]] OOK[] ILogin[1[uid], 1[pwd]]]";
        
        String keyset = Arrays.toString(bug1.getBranches().keySet().toArray());
        Assert.assertEquals(keyset, expectedKeyset);
                
        // updated branching bug
        // Regression: This keyset has only one word, there should be three.
        
        Branching bug2 = mto.getInitialBranching(prefix, log, new PIV());        
        bug2 = mto.updateBranching(prefix, log, bug2, tqr.getPiv(), tqr.getSdt());        
        String keyset2 = Arrays.toString(bug2.getBranches().keySet().toArray());
        Assert.assertEquals(keyset2, expectedKeyset);
        
    }

}
