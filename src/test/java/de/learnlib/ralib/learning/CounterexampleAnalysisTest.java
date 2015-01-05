/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.learning;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.automata.guards.ElseGuard;
import de.learnlib.ralib.learning.ces.MockSDTLogicOracle;
import de.learnlib.ralib.learning.ces.MockTreeOracle;
import de.learnlib.ralib.theory.TreeOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.HashMap;
import java.util.Map;
import net.automatalib.words.Word;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
import de.learnlib.ralib.theory.Branching;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class CounterexampleAnalysisTest {
    
    public CounterexampleAnalysisTest() {
    }

    @Test
    public void testCounterexampleAnalysis1() {
    
        TreeOracle mockTreeOracle = new MockTreeOracle();
        MockSDTLogicOracle logicOracle = new MockSDTLogicOracle(false);
        
        final Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));  
        
        Hypothesis hyp = new Hypothesis(new Constants()) {
            
            @Override
            public Word<PSymbolInstance> transformAccessSequence(Word<PSymbolInstance> word) {
                return Word.epsilon();
            }

            @Override
            public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word) {
                return Word.fromSymbols(
                        new PSymbolInstance(I_LOGIN, 
                        new DataValue(T_UID, 1),
                        new DataValue(T_PWD, 1)));  
            }
                      
        };
        
        final Branching b = new Branching() {
            @Override
            public Map<Word<PSymbolInstance>, TransitionGuard> getBranches() {
                return new HashMap<>();
            }
        };
        
        Component c = new Component(Row.computeRow(
                mockTreeOracle, Word.<PSymbolInstance>epsilon(), new ArrayList<SymbolicSuffix>()), null) {

            @Override
            Branching getBranching(ParameterizedSymbol act) {
                return b;
            }                
        };
       
        Map<Word<PSymbolInstance>, Component> components = new HashMap<>();  
        components.put(Word.<PSymbolInstance>epsilon(), c);
        
        CounterexampleAnalysis ceAnalysis = new CounterexampleAnalysis(
                mockTreeOracle, mockTreeOracle, hyp, logicOracle, components); 
        
        CEAnalysisResult cer = ceAnalysis.analyzeCounterexample(ce);
        System.out.println(cer.getSuffix());
    }
    

    @Test
    public void testCounterexampleAnalysis2() {
    
        TreeOracle mockTreeOracle = new MockTreeOracle();
        MockSDTLogicOracle logicOracle = new MockSDTLogicOracle(true);
        
        final Word<PSymbolInstance> locId = Word.fromSymbols(
                        new PSymbolInstance(I_REGISTER, 
                            new DataValue(T_UID, 1),
                            new DataValue(T_PWD, 1)));
        
        final Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));  
        
        final Word<PSymbolInstance> transId = Word.fromSymbols(
                        new PSymbolInstance(I_REGISTER, 
                            new DataValue(T_UID, 1),
                            new DataValue(T_PWD, 1)),
                        new PSymbolInstance(I_LOGIN, 
                            new DataValue(T_UID, 2),
                            new DataValue(T_PWD, 2))); 
        
        Hypothesis hyp = new Hypothesis(new Constants()) {
            
            @Override
            public Word<PSymbolInstance> transformAccessSequence(Word<PSymbolInstance> word) {
                return locId;
            }

            @Override
            public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word) {
                return transId; 
            }
                      
        };
        
        final Branching b = new Branching() {
            @Override
            public Map<Word<PSymbolInstance>, TransitionGuard> getBranches() {
                Map<Word<PSymbolInstance>, TransitionGuard> map = new HashMap<>();
                map.put(transId, new ElseGuard());
                return map;
            }
        };
        
        Component c = new Component(Row.computeRow(
                mockTreeOracle, locId, new ArrayList<SymbolicSuffix>()), null) {

            @Override
            Branching getBranching(ParameterizedSymbol act) {
                return b;
            }                
        };
       
        Map<Word<PSymbolInstance>, Component> components = new HashMap<>();  
        components.put(locId, c);
        
        CounterexampleAnalysis ceAnalysis = new CounterexampleAnalysis(
                mockTreeOracle, mockTreeOracle, hyp, logicOracle, components); 
        
        CEAnalysisResult cer = ceAnalysis.analyzeCounterexample(ce);
        System.out.println(cer.getSuffix());
    }    
}
