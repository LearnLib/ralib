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

import java.util.Map;

import de.learnlib.api.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
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

    private final Map<Word<PSymbolInstance>, LocationComponent> components;

    private final Constants consts;

    private static enum IndexResult {HAS_CE_AND_REFINES, HAS_CE_NO_REFINE, NO_CE};

    private static final LearnLogger log = LearnLogger.getLogger(CounterexampleAnalysis.class);

    public CounterexampleAnalysis(TreeOracle sulOracle, TreeOracle hypOracle,
            Hypothesis hypothesis, SDTLogicOracle sdtOracle,
            Map<Word<PSymbolInstance>, LocationComponent> components, Constants consts) {

        this.sulOracle = sulOracle;
        this.hypOracle = hypOracle;
        this.hypothesis = hypothesis;
        this.sdtOracle = sdtOracle;
        this.components = components;
        this.consts = consts;
    }

    public CEAnalysisResult analyzeCounterexample(Word<PSymbolInstance> ce) {

        int idx = binarySearch(ce);

        Word<PSymbolInstance> prefix = ce.prefix(idx);
        Word<PSymbolInstance> suffix = ce.suffix(ce.length() -idx);
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, consts);

        return new CEAnalysisResult(prefix, symSuffix);
    }

    private IndexResult computeIndex(Word<PSymbolInstance> ce, int idx) {

        Word<PSymbolInstance> prefix = ce.prefix(idx);
        //System.out.println(idx + "  " + prefix);
        Word<PSymbolInstance> location = hypothesis.transformAccessSequence(prefix);
        Word<PSymbolInstance> transition = hypothesis.transformTransitionSequence(
            ce.prefix(idx+1));

        Word<PSymbolInstance> suffix = ce.suffix(ce.length() -idx);
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, consts);

        TreeQueryResult resHyp = hypOracle.treeQuery(location, symSuffix);
        TreeQueryResult resSul = sulOracle.treeQuery(location, symSuffix);

        log.trace("------------------------------------------------------");
        log.trace("Computing index: " + idx);
        log.trace("Prefix: " + prefix);
        log.trace("SymSuffix: " + symSuffix);
        log.trace("Location: " + location);
        log.trace("Transition: " + transition);
        log.trace("PIV HYP: " + resHyp.getPiv());
        log.trace("SDT HYP: " + resHyp.getSdt());
        log.trace("PIV SYS: " + resSul.getPiv());
        log.trace("SDT SYS: " + resSul.getSdt());
        log.trace("------------------------------------------------------");

//        System.out.println("------------------------------------------------------");
//        System.out.println("Computing index: " + idx);
//        System.out.println("Prefix: " + prefix);
//        System.out.println("SymSuffix: " + symSuffix);
//        System.out.println("Location: " + location);
//        System.out.println("Transition: " + transition);
//        System.out.println("PIV HYP: " + resHyp.getPiv());
//        System.out.println("SDT HYP: " + resHyp.getSdt());
//        System.out.println("PIV SYS: " + resSul.getPiv());
//        System.out.println("SDT SYS: " + resSul.getSdt());
//        System.out.println("------------------------------------------------------");

        LocationComponent c = components.get(location);
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
        PIV pivHyp = c.getPrimePrefix().getParsInVars();
        boolean sulHasMoreRegs = !pivHyp.keySet().containsAll(pivSul.keySet());
        boolean hypRefinesTransition =
                hypRefinesTransitions(location, act, resSul.getSdt(), pivSul);

//        System.out.println("sulHasMoreRegs: " + sulHasMoreRegs);
//        System.out.println("hypRefinesTransition: " + hypRefinesTransition);

        return (sulHasMoreRegs || !hypRefinesTransition) ?
                IndexResult.HAS_CE_AND_REFINES : IndexResult.HAS_CE_NO_REFINE;
    }

    private boolean hypRefinesTransitions(Word<PSymbolInstance> prefix,
            ParameterizedSymbol action, SymbolicDecisionTree sdtSUL, PIV pivSUL) {

        Branching branchSul = sulOracle.getInitialBranching(prefix, action, pivSUL, sdtSUL);
        LocationComponent c = components.get(prefix);
        Branching branchHyp = c.getBranching(action);

//        System.out.println("Branching Hyp:");
//        for (Entry<Word<PSymbolInstance>, TransitionGuard> e : branchHyp.getBranches().entrySet()) {
//            System.out.println(e.getKey() + " -> " + e.getValue());
//        }
//        System.out.println("Branching Sys:");
//        for (Entry<Word<PSymbolInstance>, TransitionGuard> e : branchSul.getBranches().entrySet()) {
//            System.out.println(e.getKey() + " -> " + e.getValue());
//        }

        for (TransitionGuard guardHyp : branchHyp.getBranches().values()) {
            boolean refines = false;
            for (TransitionGuard guardSul : branchSul.getBranches().values()) {
                if (sdtOracle.doesRefine(guardHyp, c.getPrimePrefix().getParsInVars(),
                        guardSul, pivSUL, new Mapping<>())) {
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
        //results[0] = IndexResult.HAS_CE_NO_REFINE;
        //results[ce.length()-1] = IndexResult.NO_CE;

        int min = 0;
        int max = ce.length() - 1;
        int mid = -1;

        while (max >= min) {

            mid = (max+min+1) / 2;

            IndexResult res = computeIndex(ce, mid);
            log.trace("" + res);

            results[mid] = res;
            if (res == IndexResult.NO_CE) {
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
        if (results[idx] == IndexResult.NO_CE) {
            assert idx > 0;
            idx--;
        }

        // if the current index has no refinement use the
        // suffix of the next index
        if (results[idx] == IndexResult.HAS_CE_NO_REFINE) {
            idx++;
        }

        //System.out.println("IDX: " + idx);

        return idx;
    }

}
