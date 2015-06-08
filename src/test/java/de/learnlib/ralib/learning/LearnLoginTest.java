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
package de.learnlib.ralib.learning;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.automatalib.words.Word;

import org.testng.annotations.Test;

import de.learnlib.logging.Category;
import de.learnlib.logging.filter.CategoryFilter;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 *
 * @author falk
 */
public class LearnLoginTest {
    
    public LearnLoginTest() {
    }

    @Test
    public void learnLoginExample() {
        
        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINEST);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.FINEST);
            h.setFilter(new CategoryFilter(EnumSet.of(
//                   Category.EVENT, Category.PHASE, Category.MODEL, Category.SYSTEM)));
                    Category.EVENT, Category.PHASE, Category.MODEL)));
        }
        
        Constants consts = new Constants();
        
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);
        System.out.println("SYS:------------------------------------------------");
        System.out.println(sul);
        System.out.println("----------------------------------------------------");

        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
        
        teachers.put(T_UID, new EqualityTheory() {
            @Override
            public DataValue getFreshValue(List vals) {
                return new DataValue(T_UID, vals.size());
            }
        });
        
        teachers.put(T_PWD, new EqualityTheory() {
            @Override
            public DataValue getFreshValue(List vals) {
                return new DataValue(T_PWD, vals.size());
            }
        });
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants());
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts);

        TreeOracleFactory hypFactory = new TreeOracleFactory() {

            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                return new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, new Constants());
            }
        };
        
        RaStar rastar = new RaStar(mto, hypFactory, slo, 
                consts, I_LOGIN, I_LOGOUT, I_REGISTER);
        
        rastar.learn();        
        RegisterAutomaton hyp = rastar.getHypothesis();        
        System.out.println("HYP:------------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                        new DataValue(T_UID, 1), new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN, 
                        new DataValue(T_UID, 1), new DataValue(T_PWD, 1)));
    
        rastar.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, sul.accepts(ce)));
    
        rastar.learn();        
        hyp = rastar.getHypothesis();        
        System.out.println("HYP:------------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");

    }
}
