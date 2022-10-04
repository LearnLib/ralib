package de.learnlib.ralib.ceanalysis;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.dt.ShortPrefix;
import de.learnlib.ralib.learning.*;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterators;

public class PrefixFinder {

    private final TreeOracle sulOracle;

    private TreeOracle hypOracle;

    private Hypothesis hypothesis;

    private final SDTLogicOracle sdtOracle;

    private Map<Word<PSymbolInstance>, LocationComponent> components;

    private final Constants consts;

    private Word<PSymbolInstance>[] candidates;
    private boolean[] isCE;
    
    private final Map<SymbolicWord, TreeQueryResult> candidateCEs = new LinkedHashMap<SymbolicWord, TreeQueryResult>();
    private final Map<SymbolicWord, TreeQueryResult> discardedCEs = new LinkedHashMap<SymbolicWord, TreeQueryResult>();
    private final Map<SymbolicWord, TreeQueryResult> usedCEs = new LinkedHashMap<SymbolicWord, TreeQueryResult>();
    private final Map<SymbolicWord, TreeQueryResult> storedQueries = new LinkedHashMap<SymbolicWord, TreeQueryResult>();
    
    private int low = 0;

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
    
    public Word<PSymbolInstance> analyzeCounterexample(Word<PSymbolInstance> ce, int ... indices) {
    	int idx = binarySearch(ce, indices);
    	return candidates[idx];
    }
    
    public Set<Word<PSymbolInstance>> getCandidatePrefixes() {
    	Set<Word<PSymbolInstance>> candidatePrefixes = new LinkedHashSet<Word<PSymbolInstance>>();
    	for (int i = 0; i < candidates.length; i++) {
    		if (isCE[i])
    			candidatePrefixes.add(candidates[i]);
    	}
    	return candidatePrefixes;
    }
    
    private boolean computeIndex(Word<PSymbolInstance> ce, int idx) {

        Word<PSymbolInstance> prefix = ce.prefix(idx);
//        System.out.println(idx + "  " + prefix);

        Word<PSymbolInstance> primeLocation = hypothesis.transformAccessSequence(prefix);
        Set<Word<PSymbolInstance>> locations = hypothesis.possibleAccessSequences(prefix);

        Word<PSymbolInstance> suffix = ce.suffix(ce.length() -idx);
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, consts);

        for(Word<PSymbolInstance> location : locations) {
	        Word<PSymbolInstance> transition = hypothesis.transformTransitionSequence(
	                ce.prefix(idx+1), location);
		        
	        SymbolicWord symWord = new SymbolicWord(location, symSuffix);
	        TreeQueryResult resHyp = hypOracle.treeQuery(location, symSuffix);
	        TreeQueryResult resSul;
	        if (storedQueries.containsKey(symWord))
	        	resSul = storedQueries.get(symWord);
	        else {
	        	resSul = sulOracle.treeQuery(location, symSuffix);
	        	storedQueries.put(new SymbolicWord(location, symSuffix), resSul);
	        }
	
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
	
//	        System.out.println("------------------------------------------------------");
//	        System.out.println("Computing index: " + idx);
//	        System.out.println("Prefix: " + prefix);
//	        System.out.println("SymSuffix: " + symSuffix);
//	        System.out.println("Location: " + location);
//	        System.out.println("Transition: " + transition);
//	        System.out.println("PIV HYP: " + resHyp.getPiv());
//	        System.out.println("SDT HYP: " + resHyp.getSdt());
//	        System.out.println("PIV SYS: " + resSul.getPiv());
//	        System.out.println("SDT SYS: " + resSul.getSdt());
//	        System.out.println("------------------------------------------------------");
	
	        LocationComponent c = components.get(primeLocation);
	        ParameterizedSymbol act = transition.lastSymbol().getBaseSymbol();
	        TransitionGuard g = c.getBranching(act).getBranches().get(transition);
	
	        boolean hasCE = sdtOracle.hasCounterexample(location,
	                resHyp.getSdt(), resHyp.getPiv(),
	                resSul.getSdt(), resSul.getPiv(),
	                new TransitionGuard(), transition);
	
//	        System.out.println("CE: " + hasCE);
	
	        if (hasCE) {
	        	if (!transitionHasCE(ce, idx)) {
	        		candidateCEs.put(new SymbolicWord(location, symSuffix), resSul);
	        		storeCandidateCEs(ce, idx);
	        		isCE[idx] = true;
	        	}
//	        	candidates[idx] = candidate2(location, act, resSul.getSdt(), resSul.getPiv(), transition, c);
                candidates[idx] = candidate(location, act, symSuffix, resSul.getSdt(), resSul.getPiv(), resHyp.getSdt(), resHyp.getPiv(), c);
//	            System.out.println("candidate [" + idx + "]: " + candidates[idx]);
	            return true;
	        }
        }
        return false;
    }

    private boolean transitionHasCE(Word<PSymbolInstance> ce, int idx) {
    	if (idx+1 >= ce.length())
    		return false;
    	
    	Word<PSymbolInstance> prefix = ce.prefix(idx+1);
    	
    	Word<PSymbolInstance> suffix = ce.suffix(ce.length() - (idx+1));
    	SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, consts);
    	
    	Set<Word<PSymbolInstance>> locations = hypothesis.possibleAccessSequences(prefix);
    	for (Word<PSymbolInstance> location : locations) {
	        Word<PSymbolInstance> transition = hypothesis.transformTransitionSequence(
	                ce.prefix(idx+2), location);
	        
    		TreeQueryResult resHyp = hypOracle.treeQuery(location, symSuffix);
    		TreeQueryResult resSul;
    		SymbolicWord symWord = new SymbolicWord(location, symSuffix);
    		if (storedQueries.containsKey(symWord))
    			resSul = storedQueries.get(symWord);
    		else {
    			resSul = sulOracle.treeQuery(location, symSuffix);
    			storedQueries.put(symWord, resSul);
    		}
    		
    		boolean hasCE = sdtOracle.hasCounterexample(location,
	                resHyp.getSdt(), resHyp.getPiv(),
	                resSul.getSdt(), resSul.getPiv(),
	                new TransitionGuard(), transition);
    		if (hasCE)
    			return true;
    	}
    	return false;
    }

    private void storeCandidateCEs(Word<PSymbolInstance> ce, int idx) {
    	if (idx+1 >= ce.length())
    		return;
    	Word<PSymbolInstance> prefix = ce.prefix(idx+1);
    	
    	Word<PSymbolInstance> suffix = ce.suffix(ce.length() - (idx+1));
    	SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, consts);
    	
    	Set<Word<PSymbolInstance>> locations = hypothesis.possibleAccessSequences(prefix);
    	for (Word<PSymbolInstance> location : locations) {
    		SymbolicWord symWord = new SymbolicWord(location, symSuffix);
    		TreeQueryResult tqr = storedQueries.get(symWord);
    		
    		assert tqr != null;
    		
    		candidateCEs.put(symWord, tqr);
    	}
    }
    


    private Word<PSymbolInstance> findRefiningPrefix(Word<PSymbolInstance> prefix,
            ParameterizedSymbol action, SymbolicDecisionTree sdtSUL, PIV pivSUL, LocationComponent c) {

        Branching branchSul = sulOracle.getInitialBranching(prefix, action, pivSUL, sdtSUL);
        Branching branchHyp = c.getBranching(action);

        Branching updated = sulOracle.updateBranching(prefix, action, branchHyp, pivSUL, sdtSUL);

//        System.out.println("Branching Hyp:");
//        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : branchHyp.getBranches().entrySet()) {
//            System.out.println(e.getKey() + " -> " + e.getValue());
//        }
//        System.out.println("Branching Sys:");
//        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : branchSul.getBranches().entrySet()) {
//            System.out.println(e.getKey() + " -> " + e.getValue());
//        }
//        System.out.println("Branching Updated:");
//        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : updated.getBranches().entrySet()) {
//            System.out.println(e.getKey() + " -> " + e.getValue());
//        }

//        if (updated.getBranches().size() == branchHyp.getBranches().size()) {
//            return transition;
//        }

        for (Word<PSymbolInstance> cand : updated.getBranches().keySet()) {
            if (!branchHyp.getBranches().containsKey(cand)) {
                return cand;
            }
        }
        
        return null;

//        throw new IllegalStateException("cannot be reached!");
    }

    private Word<PSymbolInstance> candidate(Word<PSymbolInstance> prefix,
            ParameterizedSymbol action, SymbolicSuffix symSuffix, SymbolicDecisionTree sdtSul, PIV pivSul,
            SymbolicDecisionTree sdtHyp, PIV pivHyp, LocationComponent c) {
        Word<PSymbolInstance> candidate = findRefiningPrefix(prefix, action, sdtSul, pivSul, c);
        if (candidate != null) {
            return candidate;   
        }
        
        Map<Word<PSymbolInstance>, Boolean> sulPaths = sulOracle.instantiate(prefix, symSuffix, sdtSul, pivSul);
        Map<Word<PSymbolInstance>, Boolean> hypPaths = sulOracle.instantiate(prefix, symSuffix, sdtHyp, pivHyp);
        Set<Word<PSymbolInstance>> allPaths = new LinkedHashSet<>();
        Word<PSymbolInstance> cePath = null;
        allPaths.addAll(sulPaths.keySet());
        allPaths.addAll(hypPaths.keySet());
        for (Word<PSymbolInstance> path : allPaths) {
            boolean hypAcc = sdtOracle.accepts(path, prefix, sdtHyp, pivHyp);
            boolean sulAcc = sdtOracle.accepts(path, prefix, sdtSul, pivSul);
            if (hypAcc != sulAcc) {
                cePath = path;
                break;
            }
        }

        assert cePath != null : "There should be a CE path";
        candidate = cePath.prefix(prefix.length() + 1);

        return candidate;
    }

    private int binarySearch(Word<PSymbolInstance> ce, int ... indices) {

        assert ce.length() > 1;

        boolean[] results = new boolean[ce.length()];
        //results = new boolean[ce.length()];
        candidates = new Word[ce.length()];
        isCE = new boolean[ce.length()];
        //results[0] = IndexResult.HAS_CE_NO_REFINE;
        //results[ce.length()-1] = IndexResult.NO_CE;
        for (int i = 0; i < indices.length; i++) {
        	int idx = indices[i];
        	results[idx] = computeIndex(ce, idx);
        }

        int min = low;
        int max = ce.length() - 1;
        int mid = -1;

        int idx = 0;
        while(max >= 0) {
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
	
	        idx = mid;
	        	        
//	        System.out.println(Arrays.toString(results));
//	        System.out.println(idx + " : " + results[idx]);

	        // if in the last step there was no counterexample,
	        // we have to move one step to the left
	        if (!results[idx]) {
	        	assert idx > 0;
	        	idx--;
	        }

	        if (!isCE[idx]) {
	        	max = idx - 1;
	        	min = 0;
	        }
	        else
	        	break;
        }
        return idx;
    }

    public void reset() {
    	low = 0;
    }

    public DefaultQuery<PSymbolInstance, Boolean> getCounterExample() {
    	Map<SymbolicWord, TreeQueryResult> cces = new LinkedHashMap<SymbolicWord, TreeQueryResult>(candidateCEs);
    	for (Map.Entry<SymbolicWord, TreeQueryResult> e : cces.entrySet()) {
    		SymbolicWord sw = e.getKey();
    		TreeQueryResult tqr = e.getValue();
    		Map<Word<PSymbolInstance>, Boolean> ces = sulOracle.instantiate(sw.getPrefix(), sw.getSuffix(), tqr.getSdt(), tqr.getPiv());
    		for (Word<PSymbolInstance> ce : ces.keySet()) {
    			boolean hypce = hypothesis.accepts(ce);
    			boolean sulce = ces.get(ce);
    			if (hypce != sulce) {
    				DefaultQuery<PSymbolInstance, Boolean> query = new DefaultQuery<PSymbolInstance, Boolean>(ce, sulce);
    				usedCEs.put(sw, tqr);
    				candidateCEs.remove(sw);
    				return query;
    			}
    		}
			discardedCEs.put(sw, tqr);
			candidateCEs.remove(sw);
    	}
    	
    	candidateCEs.putAll(discardedCEs);
    	candidateCEs.putAll(usedCEs);
    	if (usedCEs.isEmpty())
    		return null;
    	return getCounterExample();
    }
    
    public void setHypothesisTreeOracle(TreeOracle hypOracle) {
        this.hypOracle = hypOracle;
    }
    
    public void setHypothesis(Hypothesis hyp) {
    	this.hypothesis = hyp;
    }
    
    public void setComponents(Map<Word<PSymbolInstance>, LocationComponent> components) {
    	this.components = components;
    }
}
