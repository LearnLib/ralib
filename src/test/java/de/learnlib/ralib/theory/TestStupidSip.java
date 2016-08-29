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
package de.learnlib.ralib.theory;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.automatalib.words.Word;

import org.testng.annotations.Test;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 *
 * @author falk
 */
public class TestStupidSip {

    
    @Test
    public void testLoginExample1() {
        
                Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.ALL);
        }

    
        DataWordOracle oracle = new SimulatorOracle(AUTOMATON);
            
        Theory<Integer> uidTheory = new EqualityTheory<Integer>() {
            @Override
            public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
                DataValue v = vals.get(0);
                return new DataValue(v.getType(), vals.size() + 1);
            }

        };    
    
        Theory<Integer> pwdTheory = new EqualityTheory<Integer>() {
            @Override
            public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
                DataValue v = vals.get(0);
                return new DataValue(v.getType(), vals.size() + 1);
            }


        };  
    
        Map<DataType, Theory> theories = new LinkedHashMap();
        theories.put(T_UID, uidTheory);
        theories.put(T_PWD, pwdTheory);
        
        MultiTheoryTreeOracle treeOracle = new MultiTheoryTreeOracle(oracle, theories, 
                new Constants(), new SimpleConstraintSolver());
        
//        final Word<PSymbolInstance> prefix = Word.fromSymbols(
//                new PSymbolInstance(I_REGISTER, 
//                    new DataValue(T_UID, 1),
//                    new DataValue(T_PWD, 1)),
//                new PSymbolInstance(I_LOGIN, 
//                    new DataValue(T_UID, 2),
//                    new DataValue(T_PWD, 2)));           
//        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGOUT),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));
        
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, longsuffix);
        System.out.println("Prefix: " + prefix);
        System.out.println("Suffix: " + symSuffix);        
        
        TreeQueryResult res = treeOracle.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();
//        System.out.println(res.getSdt().isAccepting());
        System.out.println("final SDT: \n" + sdt.toString());
        
        Parameter p1 = new Parameter(T_UID, 1);
        Parameter p2 = new Parameter(T_PWD, 2);
        DataValue d1 = new DataValue(T_UID, 1);
        DataValue d2 = new DataValue(T_PWD, 1);
        
        PIV testPiv =  new PIV();
        testPiv.put(p1, new Register(T_UID, 1));
        testPiv.put(p2, new Register(T_PWD, 2));
        
        ParValuation testPval = new ParValuation();
        testPval.put(p1, d1);
        testPval.put(p2,d2);
    
        System.out.println("branching");
        System.out.println("initial branching: \n" + treeOracle.getInitialBranching(prefix, I_LOGIN, testPiv, testPval, sdt).getBranches().toString());
    }
//    
    
    
}
