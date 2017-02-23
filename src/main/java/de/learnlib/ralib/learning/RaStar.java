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
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
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

    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples = 
            new LinkedList<>();
        
    private Hypothesis hyp = null;
    
    private final TreeOracle sulOracle;
    
    private TreeOracle sulOracleCE;
    
    private TreeOracle sulOracleStats;
    
    private final SDTLogicOracle sdtLogicOracle;
    
    private final TreeOracleFactory hypOracleFactory;

    private final Map<DataType, Theory> teachers;
    
    private final boolean ioMode;
    
    private static final LearnLogger log = LearnLogger.getLogger(RaStar.class);

    public RaStar(Map<DataType, Theory> teachers, 
            TreeOracle oracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts, boolean ioMode,
            ParameterizedSymbol ... inputs) {
        
        this.teachers = teachers;
        
        this.ioMode = ioMode;
        this.obs = new ObservationTable(oracle, ioMode, consts, inputs);
        this.consts = consts;
        
        this.obs.addPrefix(EMPTY_PREFIX);
        this.obs.addSuffix(EMPTY_SUFFIX);
  
        //TODO: make this optional
        for (ParameterizedSymbol ps : inputs) {
            if (ps instanceof OutputSymbol) {
                this.obs.addSuffix(new SymbolicSuffix(ps));
            }
        }
        
        this.sulOracle = oracle;
        this.sdtLogicOracle = sdtLogicOracle;
        this.hypOracleFactory = hypOracleFactory;        
        
        this.sulOracleCE = oracle;
        this.sulOracleStats = oracle;
    }   
    
    public RaStar(Map<DataType, Theory> teachers,
            TreeOracle oracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts, 
            ParameterizedSymbol ... inputs) {
        
        this(teachers, oracle, hypOracleFactory, sdtLogicOracle, consts, false, inputs);
    }
        
    public void learn() {
        if (hyp != null) {
            analyzeCounterExample();
        }
        
        do {
            
            log.logPhase("completing observation table");
            while(!(obs.complete())) {};        
            log.logPhase("completed observation table");

            //System.out.println(obs.toString());
            
            AutomatonBuilder ab = new AutomatonBuilder(obs.getComponents(), consts);            
            hyp = ab.toRegisterAutomaton();        
            
            //FIXME: the default logging appender cannot log models and data structures
            System.out.println(hyp.toString());
            log.logModel(hyp);
            
        } while (analyzeCounterExample());
         
    }
    
    
    public void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce) {
        log.logEvent("adding counterexample: " + ce);
        counterexamples.add(ce);
    }
    
    private boolean analyzeCounterExample() {
        log.logPhase("Analyzing Counterexample");        
        if (counterexamples.isEmpty()) {
            return false;
        }
        
        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);
        
        CounterexampleStatistics stats = new CounterexampleStatistics(
                teachers, sulOracleStats, hypOracle, hyp, sdtLogicOracle, obs.getComponents(), consts);
                
        CounterexampleAnalysis analysis = new CounterexampleAnalysis(
                sulOracleCE, hypOracle, hyp, sdtLogicOracle, obs.getComponents(), consts);
        
        DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.peek();    
        
        // check if ce still is a counterexample ...
        boolean hypce = hyp.accepts(ce.getInput());
        boolean sulce = ce.getOutput();
        if (hypce == sulce) {
            log.logEvent("word is not a counterexample: " + ce + " - " + sulce);           
            counterexamples.poll();
            return false;
        }
        
        System.out.println("CE ANALYSIS: " + ce + " ; S:" + sulce + " ; H:" + hypce);
        
        Word<PSymbolInstance> nce = stats.analyzeCounterexample(ce.getInput());        
        
        CEAnalysisResult res = analysis.analyzeCounterexample(nce);        
        obs.addSuffix(res.getSuffix());       
        return true;
    }
            
    
    public Hypothesis getHypothesis() {
        AutomatonBuilder ab = new AutomatonBuilder(obs.getComponents(), consts);
        if (ioMode) {
            ab = new IOAutomatonBuilder(obs.getComponents(), consts);
        }
        return ab.toRegisterAutomaton();   
    }

    /**
     * @param sulOracleCE the sulOracleCE to set
     */
    public void setSulOracleCE(TreeOracle sulOracleCE) {
        this.sulOracleCE = sulOracleCE;
    }

    /**
     * @param sulOracleStats the sulOracleStats to set
     */
    public void setSulOracleStats(TreeOracle sulOracleStats) {
        this.sulOracleStats = sulOracleStats;
    }
    
}
