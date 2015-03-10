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

import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Map;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * Analyzes Counterexamples in a binary search
 * as described in SEFM 2014.
 * 
 * @author falk
 */
public class CounterexampleAnalysis {

    private final TreeOracle sulOracle;
    
    private final TreeOracle hypOracle;
    
    private final Hypothesis hypothesis;

    private final SDTLogicOracle sdtOracle;

    private final Map<Word<PSymbolInstance>, Component> components;
    
    private static enum IndexResult {HAS_CE_AND_REFINES, HAS_CE_NO_REFINE, NO_CE};

    private static final LearnLogger log = LearnLogger.getLogger(CounterexampleAnalysis.class);
    
    CounterexampleAnalysis(TreeOracle sulOracle, TreeOracle hypOracle, 
            Hypothesis hypothesis, SDTLogicOracle sdtOracle, 
            Map<Word<PSymbolInstance>, Component> components) {
        
        this.sulOracle = sulOracle;
        this.hypOracle = hypOracle;
        this.hypothesis = hypothesis;
        this.sdtOracle = sdtOracle;
        this.components = components;
    }
    
    CEAnalysisResult analyzeCounterexample(Word<PSymbolInstance> ce) {
        
        int idx = binarySearch(ce);
        
        Word<PSymbolInstance> prefix = ce.prefix(idx);
        Word<PSymbolInstance> suffix = ce.suffix(ce.length() -idx);        
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);        
        
        return new CEAnalysisResult(prefix, symSuffix);        
    } 
    
    private IndexResult computeIndex(Word<PSymbolInstance> ce, int idx) {
                
        Word<PSymbolInstance> prefix = ce.prefix(idx);
        Word<PSymbolInstance> location = hypothesis.transformAccessSequence(prefix);
        Word<PSymbolInstance> transition = hypothesis.transformTransitionSequence(
            ce.prefix(idx+1));         
        
        Word<PSymbolInstance> suffix = ce.suffix(ce.length() -idx);        
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);
        
        TreeQueryResult resHyp = hypOracle.treeQuery(location, symSuffix);
        TreeQueryResult resSul = sulOracle.treeQuery(location, symSuffix);

        log.log(Level.FINEST,"------------------------------------------------------");
        log.log(Level.FINEST,"Computing index: " + idx);
        log.log(Level.FINEST,"Prefix: " + prefix);
        log.log(Level.FINEST,"SymSuffix: " + symSuffix);
        log.log(Level.FINEST,"Location: " + location);
        log.log(Level.FINEST,"Transition: " + transition);
        log.log(Level.FINEST,"PIV HYP: " + resHyp.getPiv());
        log.log(Level.FINEST,"SDT HYP: " + resHyp.getSdt());
        log.log(Level.FINEST,"PIV SYS: " + resSul.getPiv());
        log.log(Level.FINEST,"SDT SYS: " + resSul.getSdt());        
        log.log(Level.FINEST,"------------------------------------------------------");
        
        Component c = components.get(location);
        ParameterizedSymbol act = transition.lastSymbol().getBaseSymbol();
        TransitionGuard g = c.getBranching(act).getBranches().get(transition);
        
        boolean hasCE = sdtOracle.hasCounterexample(location, 
                resHyp.getSdt(), resHyp.getPiv(), //new PIV(location, resHyp.getParsInVars()), 
                resSul.getSdt(), resSul.getPiv(), //new PIV(location, resSul.getParsInVars()), 
                g, transition);
        
        if (!hasCE) {
            return IndexResult.NO_CE;
        }
        
        // PIV pivSul = new PIV(location, resSul.getParsInVars());
        PIV pivSul = resSul.getPiv();
        PIV pivHyp = c.getPrimeRow().getParsInVars();
        boolean sulHasMoreRegs = !pivHyp.keySet().containsAll(pivSul.keySet());                
        boolean hypRefinesTransition = 
                hypRefinesTransitions(location, act, resSul.getSdt(), pivSul);
        
        return (sulHasMoreRegs || !hypRefinesTransition) ? 
                IndexResult.HAS_CE_AND_REFINES : IndexResult.HAS_CE_NO_REFINE;        
    }
    
    private boolean hypRefinesTransitions(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol action, SymbolicDecisionTree sdtSUL, PIV pivSUL) {
        
        Branching branchSul = sulOracle.getInitialBranching(prefix, action, pivSUL, sdtSUL);
        Component c = components.get(prefix);
        Branching branchHyp = c.getBranching(action);
        
        for (TransitionGuard guardHyp : branchHyp.getBranches().values()) {
            boolean refines = false;
            for (TransitionGuard guardSul : branchSul.getBranches().values()) {
                if (sdtOracle.doesRefine(guardHyp, c.getPrimeRow().getParsInVars(), 
                        guardSul, pivSUL)) {
                    refines = true;
                    break;
                }
            }
            
            if (!refines) {
                return false;
            }
        }
        
        return true;
    }
    
    private int binarySearch(Word<PSymbolInstance> ce) {
        
        assert ce.length() > 1;
        
        IndexResult[] results = new IndexResult[ce.length()];
        results[0] = IndexResult.HAS_CE_NO_REFINE;
        results[ce.length()-1] = IndexResult.NO_CE;
        
        int min = 1;
        int max = ce.length() - 1;
        int mid = -1;
        
        while (max >= min) {
            //TODO: check that +1 does no harm
            mid = (max+min+1) / 2;
         
            IndexResult res = computeIndex(ce, mid);
            log.log(Level.FINEST, "" + res);
            
            results[mid] = res;
            if (res == IndexResult.NO_CE) {
                max = mid -1;
            } else {
                min = mid +1;
            }            
        }
        
        assert mid > 0;
        
        int idx = mid;
        // if in the last step there was no counterexample, 
        // we have to move one step to the left
        if (results[idx] == IndexResult.NO_CE) {
            idx--;
        }
        
        // if the current index has no refinement use the 
        // suffix of the next index
        if (results[idx] == IndexResult.HAS_CE_NO_REFINE) {
            idx++;
        }

        return idx;
    }
    
}
