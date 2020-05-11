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

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.Slice;
import de.learnlib.ralib.oracles.mto.SliceBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixBuilder;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
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
    
    private final Constants consts;
    
    private final Map<DataType, Theory> teachers;

    private final ConstraintSolver solver;
    
    private static enum IndexStatus {HAS_CE_AND_REFINES, HAS_CE_NO_REFINE, NO_CE};

    private static class IndexResult {
        
        private final int idx;
        private final IndexStatus status;
        private final Slice slice;
        private GeneralizedSymbolicSuffix suffix;

        private IndexResult(int idx, IndexStatus status, Slice slice) {
            this.idx = idx;
            this.status = status;
            this.slice = slice;
        }
               
        /**
         * If the symbolic suffix is not null, it should be used. 
         */
        public GeneralizedSymbolicSuffix getSuffix() {
        	return this.suffix;
        }
        
        public void setSuffix(GeneralizedSymbolicSuffix suffix) {
        	this.suffix = suffix;
        }
    }
    
    private static final LearnLogger log = LearnLogger.getLogger(CounterexampleAnalysis.class);
    
    CounterexampleAnalysis(TreeOracle sulOracle, TreeOracle hypOracle, 
            Hypothesis hypothesis, SDTLogicOracle sdtOracle, 
            Map<Word<PSymbolInstance>, Component> components, 
            Constants consts, Map<DataType, Theory> teachers,
            ConstraintSolver solver) {
        
        this.sulOracle = sulOracle;
        this.hypOracle = hypOracle;
        this.hypothesis = hypothesis;
        this.sdtOracle = sdtOracle;
        this.components = components;
        this.consts = consts;
        this.teachers = teachers;
        this.solver = solver;
    }
    
    CEAnalysisResult analyzeCounterexample(Word<PSymbolInstance> ce) {
        IndexResult result  = linearBackWardsSearch(ce);
        
        Word<PSymbolInstance> prefix = ce.prefix(result.idx);
        Word<PSymbolInstance> suffix = ce.suffix(ce.length() - result.idx);
        if (result.getSuffix() != null) {
        	return new CEAnalysisResult(prefix, result.getSuffix());
        }
        
        GeneralizedSymbolicSuffix symSuffix = 
                SymbolicSuffixBuilder.suffixFromSlice(
                        DataWords.actsOf(suffix), result.slice);

        assert ce.length() - result.idx == symSuffix.getActions().length();     
        
        return new CEAnalysisResult(prefix, symSuffix);
    } 

    
    private IndexResult computeIndex(Word<PSymbolInstance> ce, int idx) {
        
        Word<PSymbolInstance> prefix = ce.prefix(idx);
        System.out.println(idx + "  " + prefix);        
        Word<PSymbolInstance> location = hypothesis.transformAccessSequence(prefix);
        Word<PSymbolInstance> transition = hypothesis.transformTransitionSequence(
            ce.prefix(idx+1)); 
        if (transition == null)
        	return  new IndexResult(idx, IndexStatus.NO_CE, null);
        
        Word<PSymbolInstance> suffix = ce.suffix(ce.length() -idx);        
        
        SliceBuilder sb = new SliceBuilder(teachers, consts, solver);
        
        Slice slice = sb.sliceFromWord(prefix, suffix);
        System.out.println("Slice from word: " + slice);
        
        GeneralizedSymbolicSuffix symSuffix = SymbolicSuffixBuilder.suffixFromSlice(DataWords.actsOf(suffix), slice);
        
        System.out.println("exhaustive suffix: " + symSuffix);
        System.out.println("location: " + location);
        TreeQueryResult resHyp = hypOracle.treeQuery(location, symSuffix);
        TreeQueryResult resSul = sulOracle.treeQuery(location, symSuffix);
        
        System.out.println("HYP (initial suffix): " + resHyp);
        System.out.println("SUL (initial suffix): " + resSul);
                
        log.log(Level.FINE,"------------------------------------------------------");
        log.log(Level.FINE,"Computing index: " + idx);
        log.log(Level.FINE,"Prefix: " + prefix);
        log.log(Level.FINE,"SymSuffix: " + symSuffix);
        log.log(Level.FINE,"Location: " + location);
        log.log(Level.FINE,"Transition: " + transition);
        log.log(Level.FINE,"PIV HYP: " + resHyp.getPiv());
        log.log(Level.FINE,"SDT HYP: " + resHyp.getSdt());
        log.log(Level.FINE,"PIV SYS: " + resSul.getPiv());
        log.log(Level.FINE,"SDT SYS: " + resSul.getSdt());        
        log.log(Level.FINE,"------------------------------------------------------");
        
        Component c = components.get(location);
        ParameterizedSymbol act = transition.lastSymbol().getBaseSymbol();
        TransitionGuard g = c.getBranching(act).getBranches().get(transition);
        
        boolean hasCE = sdtOracle.hasCounterexample(location, 
                resHyp.getSdt(), resHyp.getPiv(), //new PIV(location, resHyp.getParsInVars()), 
                resSul.getSdt(), resSul.getPiv(), //new PIV(location, resSul.getParsInVars()), 
                g, transition);
                
        if (!hasCE) {
            return new IndexResult(idx, IndexStatus.NO_CE, null);
        }
        
        PIV pivSul = resSul.getPiv();
        PIV pivHyp = c.getPrimeRow().getParsInVars();
        boolean sulHasMoreRegs = !pivHyp.keySet().containsAll(pivSul.keySet());         
        boolean hypRefinesTransition = 
                hypRefinesTransitions(location, act, resSul.getSdt(), pivSul);
        

        IndexResult indx = new IndexResult(idx, (sulHasMoreRegs || !hypRefinesTransition) ? 
                IndexStatus.HAS_CE_AND_REFINES : IndexStatus.HAS_CE_NO_REFINE,
                slice);
        indx.setSuffix(symSuffix);
        return indx;
    }
    
    private boolean hypRefinesTransitions(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol action, SymbolicDecisionTree sdtSUL, PIV pivSUL) {
        
        Branching branchSul = sulOracle.getInitialBranching(prefix, action, pivSUL, sdtSUL);
        Component c = components.get(prefix);
        Branching branchHyp = c.getBranching(action);
        
        System.out.println("Hyp Branching:");
        System.out.println(Arrays.toString(branchHyp.getBranches().values().toArray()));
        System.out.println("Sul Branching:");
        System.out.println(Arrays.toString(branchSul.getBranches().values().toArray()));
        
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
    
    private IndexResult linearBackWardsSearch(Word<PSymbolInstance> ce) {
        
        assert ce.length() > 1;
        
        IndexResult[] results = new IndexResult[ce.length()];
        results[ce.length()-1] = 
                new IndexResult(ce.length()-1, IndexStatus.NO_CE, null);

        int idx = ce.length()-2;
        
        while (idx >= 0) {
            IndexResult res = computeIndex(ce, idx);
            results[idx] = res;
            if (res.status != IndexStatus.NO_CE) {
                break;
            }
            idx--;
        }
        
        assert (idx >= 0);
        
        // if in the last step there was no counterexample, 
        // we have to move one step to the left
        if (results[idx].status == IndexStatus.NO_CE) {
            assert idx > 0;
            idx--;
        }
                
        // if the current index has no refinement use the 
        // suffix of the next index
        if (results[idx].status == IndexStatus.HAS_CE_NO_REFINE) {
        	if (results[idx+1].status == IndexStatus.NO_CE) {
                int arity = ce.getSymbol(idx).getBaseSymbol().getArity();
                Slice s = results[idx].slice.suffix(arity);
                System.out.println("Suffix slice: " + s);
                IndexResult old = results[idx+1];
                results[idx+1] = new IndexResult(old.idx, old.status, s);
            }
            idx++;
        }

        return results[idx];        
    }
    
    private IndexResult binarySearch(Word<PSymbolInstance> ce) {
        
        assert ce.length() > 1;
        
        IndexResult[] results = new IndexResult[ce.length()];
        //results[0] = IndexResult.HAS_CE_NO_REFINE;
        //results[ce.length()-1] = IndexResult.NO_CE;
        
        int min = 0;
        int max = ce.length() - 1;
        int mid = -1;
        
        while (max >= min) {

            mid = (max+min+1) / 2;
         
            IndexResult res = computeIndex(ce, mid);
            log.log(Level.FINEST, "" + res);
            
            results[mid] = res;
            if (res.status == IndexStatus.NO_CE) {
                max = mid -1;
            } else {
                min = mid +1;
            }            
        }
        
        assert mid >= 0;
        
        int idx = mid;
        
        //System.out.println(Arrays.toString(results));
        //System.out.println(idx + " : " + results[idx]);
        
        // if in the last step there was no counterexample, 
        // we have to move one step to the left
        if (results[idx].status == IndexStatus.NO_CE) {
            assert idx > 0;
            idx--;
        }      

        // if the current index has no refinement use the 
        // suffix of the next index
        if (results[idx].status == IndexStatus.HAS_CE_NO_REFINE) {
            if (results[idx+1].status == IndexStatus.NO_CE) {
                int arity = ce.getSymbol(idx).getBaseSymbol().getArity();
                Slice s = results[idx].slice.suffix(arity);
                System.out.println("Suffix slice: " + s);                
                IndexResult old = results[idx+1];
                results[idx+1] = new IndexResult(old.idx, old.status, s);
            }
            idx++;
        }

        //System.out.println("IDX: " + idx);        
        return results[idx];
    }
    
}
