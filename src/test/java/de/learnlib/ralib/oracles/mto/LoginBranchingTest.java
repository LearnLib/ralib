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


import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.automatalib.words.Word;

import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoaderTest;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public class LoginBranchingTest {
    
    public LoginBranchingTest() {
    }


    @Test
    public void testBranching() {
    
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.ALL);
        }

        final ParameterizedSymbol ERROR
                = new OutputSymbol("_io_err", new DataType[]{});

        RegisterAutomatonImporter loader = new RegisterAutomatonImporter(
                RegisterAutomatonLoaderTest.class.getResourceAsStream(
                        "/de/learnlib/ralib/automata/xml/login_typed.xml"));

        RegisterAutomaton model = loader.getRegisterAutomaton();
        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});
        
        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
        for (final DataType t : loader.getDataTypes()) {
            teachers.put(t, new IntegerEqualityTheory(t));
        }

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);

        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, 
                consts, new SimpleConstraintSolver());
        
        DataType uid = getType("uid", loader.getDataTypes());
        DataType pwd = getType("pwd", loader.getDataTypes());
        
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
        
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);
        
        System.out.println(prefix);
        System.out.println(suffix);
        System.out.println(symSuffix);
 
        System.out.println("MQ: " + ioOracle.trace(prefix.concat(suffix)));
        
        System.out.println("######################################################################");
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);        
        System.out.println(res.getSdt());
        
        System.out.println("######################################################################");
        // initial branching bug
        Branching bug1 = mto.getInitialBranching(prefix, log, res.getPiv(), res.getSdt());        
        System.out.println(Arrays.toString(bug1.getBranches().keySet().toArray()));
        System.out.println("Why does the last word in the set have a password val. of 2");

        System.out.println("######################################################################");
        
        // updated branching bug
        Branching bug2 = mto.getInitialBranching(prefix, log, new PIV());        
        bug2 = mto.updateBranching(prefix, log, bug2, res.getPiv(), res.getSdt());        
        System.out.println(Arrays.toString(bug2.getBranches().keySet().toArray()));
        System.out.println("This set has only one word, there should be three.");
    }

    private DataType getType(String name, Collection<DataType> dataTypes) {
        for (DataType t : dataTypes) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }
}
