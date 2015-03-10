/*
 * Copyright (C) 2014 falk.
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

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * Learning algorithm for register automata
 * 
 * @author falk
 */
public class RaStar {
    
    public static final Word<PSymbolInstance> EMPTY_PREFIX = Word.epsilon();
    
    public static final SymbolicSuffix EMPTY_SUFFIX = new SymbolicSuffix(
            Word.<PSymbolInstance>epsilon(), Word.<PSymbolInstance>epsilon());
    
    private final ObservationTable obs;
    
    private final Constants consts;

    private final Deque<Word<PSymbolInstance>> counterexamples = new LinkedList<>();
    
    private final Map<Word<PSymbolInstance>, Boolean> ceMap = new HashMap<>();
    
    private Hypothesis hyp = null;
    
    private final TreeOracle sulOracle;
    
    private final SDTLogicOracle sdtLogicOracle;
    
    private final TreeOracleFactory hypOracleFactory;
    
    private static final LearnLogger log = LearnLogger.getLogger(RaStar.class);
    
    public RaStar(TreeOracle oracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts, 
            ParameterizedSymbol ... inputs) {
        
        this.obs = new ObservationTable(oracle, inputs);
        this.consts = consts;
        
        this.obs.addPrefix(EMPTY_PREFIX);
        this.obs.addSuffix(EMPTY_SUFFIX);
        
        this.sulOracle = oracle;
        this.sdtLogicOracle = sdtLogicOracle;
        this.hypOracleFactory = hypOracleFactory;
    }
    
    
    public void learn() {
        if (hyp != null) {
            analyzeCounterExample();
        }
        
        do {
            
            log.logPhase("completing observation table");
            while(!(obs.complete())) {};        
            log.logPhase("completed observation table");

            AutomatonBuilder ab = new AutomatonBuilder(obs.getComponents(), consts);
            hyp = ab.toRegisterAutomaton();        
            log.logModel(hyp);
            
        } while (analyzeCounterExample());
    
        AutomatonBuilder ab = new AutomatonBuilder(obs.getComponents(), consts);
        hyp = ab.toRegisterAutomaton();        
    }
    
    
    public void addCounterexample(Word<PSymbolInstance> ce, boolean accepted) {
        log.logEvent("adding counterexample: " + ce + " - " + accepted);
        counterexamples.add(ce);
        ceMap.put(ce, accepted);
    }
    
    private boolean analyzeCounterExample() {
        if (counterexamples.isEmpty()) {
            return false;
        }
        
        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);
        
        CounterexampleAnalysis analysis = new CounterexampleAnalysis(
                sulOracle, hypOracle, hyp, sdtLogicOracle, obs.getComponents());
        
        Word<PSymbolInstance> ce = counterexamples.peek();    
        
        // check if ce still is a counterexample ...
        boolean hypce = hyp.accepts(ce);
        boolean sulce = ceMap.get(ce);        
        if (hypce == sulce) {
            log.logEvent("word is not a counterexample: " + ce + " - " + sulce);           
            counterexamples.poll();
            ceMap.remove(ce);
            return false;
        }
        
        CEAnalysisResult res = analysis.analyzeCounterexample(ce);        
        obs.addSuffix(res.getSuffix());       
        return true;
    }
            
    
    public RegisterAutomaton getHypothesis() {
        return hyp;
    }
    
}
