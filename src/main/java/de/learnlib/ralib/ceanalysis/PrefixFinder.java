package de.learnlib.ralib.ceanalysis;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.*;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;

public class PrefixFinder {

    private final TreeOracle sulOracle;

    private final TreeOracle hypOracle;

    private final Hypothesis hypothesis;

    private final SDTLogicOracle sdtOracle;

    private final Map<Word<PSymbolInstance>, LocationComponent> components;

    private final Constants consts;

    private Word<PSymbolInstance>[] candidates;

    private static final LearnLogger log = LearnLogger.getLogger(PrefixFinder.class);

    public PrefixFinder(TreeOracle sulOracle, TreeOracle hypOracle,
            Hypothesis hypothesis, SDTLogicOracle sdtOracle,
            Map<Word<PSymbolInstance>, LocationComponent> components,
            Constants consts) {

        this.sulOracle = sulOracle;
        this.hypOracle = hypOracle;
        this.hypothesis = hypothesis;
        this.sdtOracle = sdtOracle;
        this.components = components;
        this.consts = consts;
    }


    public Word<PSymbolInstance> analyzeCounterexample(Word<PSymbolInstance> ce) {
        int idx = binarySearch(ce);
        return candidates[idx];
    }

    private boolean computeIndex(Word<PSymbolInstance> ce, int idx) {

        Word<PSymbolInstance> prefix = ce.prefix(idx);
        System.out.println(idx + "  " + prefix);

        //TODO: this can be multiple!
        Word<PSymbolInstance> location = hypothesis.transformAccessSequence(prefix);
        Word<PSymbolInstance> transition = hypothesis.transformTransitionSequence(
                ce.prefix(idx+1));

        Word<PSymbolInstance> suffix = ce.suffix(ce.length() -idx);
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, consts);

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

        System.out.println("------------------------------------------------------");
        System.out.println("Computing index: " + idx);
        System.out.println("Prefix: " + prefix);
        System.out.println("SymSuffix: " + symSuffix);
        System.out.println("Location: " + location);
        System.out.println("Transition: " + transition);
        System.out.println("PIV HYP: " + resHyp.getPiv());
        System.out.println("SDT HYP: " + resHyp.getSdt());
        System.out.println("PIV SYS: " + resSul.getPiv());
        System.out.println("SDT SYS: " + resSul.getSdt());
        System.out.println("------------------------------------------------------");

        LocationComponent c = components.get(location);
        ParameterizedSymbol act = transition.lastSymbol().getBaseSymbol();
        TransitionGuard g = c.getBranching(act).getBranches().get(transition);

        boolean hasCE = sdtOracle.hasCounterexample(location,
                resHyp.getSdt(), resHyp.getPiv(),
                resSul.getSdt(), resSul.getPiv(),
                new TransitionGuard(), transition);

        System.out.println("CE: " + hasCE);

        if (hasCE) {
            candidates[idx] = candidate(location, act, resSul.getSdt(), resSul.getPiv(), transition);
            System.out.println("candidate [" + idx + "]: " + candidates[idx]);
        }

        return hasCE;
    }

    private Word<PSymbolInstance> candidate(Word<PSymbolInstance> prefix,
            ParameterizedSymbol action, SymbolicDecisionTree sdtSUL, PIV pivSUL,
            Word<PSymbolInstance> transition) {

        Branching branchSul = sulOracle.getInitialBranching(prefix, action, pivSUL, sdtSUL);
        LocationComponent c = components.get(prefix);
        Branching branchHyp = c.getBranching(action);

        Branching updated = sulOracle.updateBranching(prefix, action, branchHyp, pivSUL, sdtSUL);

        System.out.println("Branching Hyp:");
        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : branchHyp.getBranches().entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }
        System.out.println("Branching Sys:");
        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : branchSul.getBranches().entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }
        System.out.println("Branching Updated:");
        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : updated.getBranches().entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }

        if (updated.getBranches().size() == branchHyp.getBranches().size()) {
            return transition;
        }

        for (Word<PSymbolInstance> cand : updated.getBranches().keySet()) {
            if (!branchHyp.getBranches().containsKey(cand)) {
                return cand;
            }
        }

        throw new IllegalStateException("cannot be reached!");
    }


    private int binarySearch(Word<PSymbolInstance> ce) {

        assert ce.length() > 1;

        boolean[] results = new boolean[ce.length()];
        candidates = new Word[ce.length()];
        //results[0] = IndexResult.HAS_CE_NO_REFINE;
        //results[ce.length()-1] = IndexResult.NO_CE;

        int min = 0;
        int max = ce.length() - 1;
        int mid = -1;

        while (max >= min) {

            mid = (max+min+1) / 2;

            boolean hasCe = computeIndex(ce, mid);
            log.log(Level.FINEST, "" + hasCe);

            results[mid] = hasCe;
            if (!hasCe) {
                max = mid -1;
            } else {
                min = mid +1;
            }
        }

        assert mid >= 0;

        int idx = mid;

        System.out.println(Arrays.toString(results));
        System.out.println(idx + " : " + results[idx]);

        // if in the last step there was no counterexample,
        // we have to move one step to the left
        if (!results[idx]) {
            assert idx > 0;
            idx--;
        }

        return idx;
    }

}
