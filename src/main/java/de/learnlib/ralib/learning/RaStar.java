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

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.HypVerifier;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.solver.ConstraintSolver;
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
    
    public static final GeneralizedSymbolicSuffix EMPTY_SUFFIX = new GeneralizedSymbolicSuffix(
            Word.<PSymbolInstance>epsilon(), Word.<PSymbolInstance>epsilon(), new Constants(), null);
    
    private final ObservationTable obs;
    
    private final Constants consts;

    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples = 
            new LinkedList<>();
        
    private Hypothesis hyp = null;
    
    private final TreeOracle sulOracle;
    
    private final TreeOracle ceSulOracle;
    
    private final SDTLogicOracle sdtLogicOracle;
    
    private final TreeOracleFactory hypOracleFactory;
    
    private final boolean ioMode;

	private HypVerifier hypVerifier;
    
    private final Map<DataType, Theory> teachers;

    private final ConstraintSolver solver;
    
    private static final LearnLogger log = LearnLogger.getLogger(RaStar.class);

    public RaStar(TreeOracle oracle, TreeOracle ceSulOracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts, boolean ioMode,
            Map<DataType, Theory> teachers,
            HypVerifier hypVerifier,
            ConstraintSolver solver,
            ParameterizedSymbol ... inputs) {
        
        this.ioMode = ioMode;
        this.obs = new ObservationTable(oracle, ioMode, consts, teachers, solver, inputs);
        this.consts = consts;
        this.teachers = teachers;
        this.solver = solver;

        this.obs.addPrefix(EMPTY_PREFIX);
        this.obs.addSuffix(EMPTY_SUFFIX);
  
        //TODO: make this optional
        for (ParameterizedSymbol ps : inputs) {
            if (ps instanceof OutputSymbol) {
                this.obs.addSuffix(new GeneralizedSymbolicSuffix(ps, teachers));
            }
        }
        
        this.sulOracle = oracle;
        this.ceSulOracle = ceSulOracle;
        this.sdtLogicOracle = sdtLogicOracle;
        this.hypOracleFactory = hypOracleFactory;
        this.hypVerifier = hypVerifier;
    }   
    
    public RaStar(TreeOracle oracle,TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts, boolean ioMode,
            Map<DataType, Theory> teachers,
            HypVerifier hypVerifier, ConstraintSolver solver,
            ParameterizedSymbol ... inputs) {
    	this(oracle, oracle, hypOracleFactory, sdtLogicOracle, consts, ioMode, 
                teachers, hypVerifier, solver, inputs);
    	
    }   
    
    public RaStar(TreeOracle oracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts,  Map<DataType, Theory> teachers, 
            HypVerifier hypVerifier, ConstraintSolver solver,
            ParameterizedSymbol ... inputs) {
        
        this(oracle, hypOracleFactory, sdtLogicOracle, consts, false, 
                teachers, hypVerifier, solver, inputs);
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
            
            // if it's an IO system, then we make it output complete (for which is needed for CE analysis)
            AutomatonBuilder ab = this.ioMode? new IOAutomatonBuilder(obs.getComponents(), consts) : 
            	new AutomatonBuilder(obs.getComponents(), consts);            
            hyp = ab.toRegisterAutomaton();        
            
            //FIXME: the default logging appender cannot log models and data structures
            System.out.println("New Hyp: \n" + hyp.toString());
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
        
        CounterexampleAnalysis analysis = new CounterexampleAnalysis(
                ceSulOracle, hypOracle, hyp, sdtLogicOracle, obs.getComponents(), 
                        consts, teachers, solver);
        
        DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.peek();    
        
        // check if ce still is a counterexample ...
        if (!hypVerifier.isCEForHyp(ce.getInput(), hyp)) {
            log.logEvent("word is not a counterexample: " + ce);           
            counterexamples.poll();
            return false;
        }
        
        //System.out.println("CE ANALYSIS: " + ce + " ; S:" + sulce + " ; H:" + hypce);
        
        CEAnalysisResult res = analysis.analyzeCounterexample(ce.getInput());     
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
    
}
