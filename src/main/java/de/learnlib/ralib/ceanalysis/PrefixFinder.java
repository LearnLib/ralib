package de.learnlib.ralib.ceanalysis;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.learning.*;
import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class PrefixFinder {

    private final TreeOracle sulOracle;

    private TreeOracle hypOracle;

    private Hypothesis hypothesis;

    private final SDTLogicOracle sdtOracle;

    private Map<Word<PSymbolInstance>, LocationComponent> components;

    private final Constants consts;

    private SymbolicWord[] candidates;
    private boolean[] isCE;

    private final Map<SymbolicWord, TreeQueryResult> candidateCEs = new LinkedHashMap<SymbolicWord, TreeQueryResult>();
    private final Map<SymbolicWord, TreeQueryResult> discardedCEs = new LinkedHashMap<SymbolicWord, TreeQueryResult>();
    private final Map<SymbolicWord, TreeQueryResult> usedCEs = new LinkedHashMap<SymbolicWord, TreeQueryResult>();
    private final Map<SymbolicWord, TreeQueryResult> storedQueries = new LinkedHashMap<SymbolicWord, TreeQueryResult>();

    private SymbolicSuffix[] candidateSuffixes;
    private int candidateIdx = -1;

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


    public CEAnalysisResult analyzeCounterexample(Word<PSymbolInstance> ce) {
        //int idx = binarySearch(ce);
		int idx = findIndex(ce);
        SymbolicWord sw = new SymbolicWord(candidates[idx].getPrefix(), candidates[idx].getSuffix());
        TreeQueryResult tqr = null; //storedQueries.get(sw);
        if (tqr == null) {
        	// THIS CAN (possibly) BE DONE WITHOUT A NEW TREE QUERY
        	tqr = sulOracle.treeQuery(sw.getPrefix(), sw.getSuffix());
        }
        CEAnalysisResult result = new CEAnalysisResult(candidates[idx].getPrefix(),
        		                                       candidates[idx].getSuffix(),
        		                                       tqr);
        //candidateCEs.put(candidates[idx], tqr);
        //storeCandidateCEs(ce, idx);
        return result;
    }

    public CEAnalysisResult analyzeCounterexample(Word<PSymbolInstance> ce, int ... indices) {
    	int idx = binarySearch(ce, indices);
    	SymbolicWord sw = new SymbolicWord(candidates[idx].getPrefix(), candidates[idx].getSuffix());
        CEAnalysisResult result = new CEAnalysisResult(candidates[idx].getPrefix(),
                candidates[idx].getSuffix(),
                storedQueries.get(sw));
        candidateCEs.put(candidates[idx], result.getTreeQueryResult());
        storeCandidateCEs(ce, idx);
        return result;
    }

    public Set<Word<PSymbolInstance>> getCandidatePrefixes() {
    	Set<Word<PSymbolInstance>> candidatePrefixes = new LinkedHashSet<Word<PSymbolInstance>>();
    	for (int i = 0; i < candidates.length; i++) {
    		if (isCE[i])
    			candidatePrefixes.add(candidates[i].getPrefix());
    	}
    	return candidatePrefixes;
    }

    public SymbolicSuffix getCandidateSuffix() {
    	if (candidateIdx < 0)
    		return null;
    	return candidateSuffixes[candidateIdx];
    }

	private int findIndex(Word<PSymbolInstance> ce) {
		candidates = new SymbolicWord[ce.length()];
		int max = ce.length() - 1;
		for (int idx=max; idx>=0; idx = idx-1) {

			Word<PSymbolInstance> prefix = ce.prefix(idx);
			Word<PSymbolInstance> nextPrefix = ce.prefix(idx+1);

			System.out.println("ce:     " + ce);
			System.out.println("prefix: " + prefix);
			System.out.println("next:   " + nextPrefix);

			// check for location counterexample ...
			//
			Word<PSymbolInstance> suffix = ce.suffix(ce.length() - nextPrefix.length());
			SymbolicSuffix symSuffix = new SymbolicSuffix(nextPrefix, suffix, consts);
			LOC_CHECK: for (Word<PSymbolInstance> u : hypothesis.possibleAccessSequences(prefix)) {
				Word<PSymbolInstance> uAlpha = hypothesis.transformTransitionSequence(nextPrefix, u);
				TreeQueryResult uAlphaResult = sulOracle.treeQuery(uAlpha, symSuffix);
				storedQueries.put(new SymbolicWord(uAlpha, symSuffix), uAlphaResult);

				// check if the word is inequivalent to all access sequences
				//
				for (Word<PSymbolInstance> uPrime : hypothesis.possibleAccessSequences(nextPrefix)) {
					TreeQueryResult uPrimeResult = sulOracle.treeQuery(uPrime, symSuffix);
					storedQueries.put(new SymbolicWord(uPrime, symSuffix), uPrimeResult);

					System.out.println("--------------------");
					System.out.println("u:  " + u);
					System.out.println("ua: " + uAlpha);
					System.out.println("u': " + uPrime);
					System.out.println("v:  " + symSuffix);

					// different piv sizes
					//
					if (!uPrimeResult.getPiv().typedSize().equals(uAlphaResult.getPiv().typedSize())) {
						continue;
					}

					// remapping
					//
					PIVRemappingIterator iterator = new PIVRemappingIterator(
							uAlphaResult.getPiv(), uPrimeResult.getPiv());

					for (VarMapping m : iterator) {
						if (uAlphaResult.getSdt().isEquivalent(uPrimeResult.getSdt(), m)) {
							continue LOC_CHECK;
						}
					}

				}
				// found a counterexample!
				candidates[idx] = new SymbolicWord(uAlpha, symSuffix);
				System.out.println("Counterexample for location");
				return idx;
			}

			// check for transition counterexample ...
			//
			if (transitionHasCE(ce, idx-1)) {
				System.out.println("Counterexample for transition");
				return idx;
			}
		}
		throw new RuntimeException("should not reach here");
	}

    private boolean computeIndex(Word<PSymbolInstance> ce, int idx) {
        Word<PSymbolInstance> prefix = ce.prefix(idx);

        Word<PSymbolInstance> primeLocation = hypothesis.transformAccessSequence(prefix);
        Set<Word<PSymbolInstance>> locations = hypothesis.possibleAccessSequences(prefix);

        Word<PSymbolInstance> suffix = ce.suffix(ce.length() -(idx+1));
        SymbolicSuffix symSuffix = new SymbolicSuffix(ce.prefix(idx+1), suffix, consts);
        SymbolicSuffix extendedSuffix = new SymbolicSuffix(prefix, ce.suffix(ce.length() - idx), consts);

        for(Word<PSymbolInstance> location : locations) {
	        Word<PSymbolInstance> transition = hypothesis.transformTransitionSequence(
	                ce.prefix(idx+1), location);

	        boolean candidateFound = true;
	        Pair<TreeQueryResult, TreeQueryResult> results = checkForCE(transition, symSuffix, transition);

	        if (results == null) {
	        	results = checkForCE(location, extendedSuffix, transition);
	        	candidateFound = false;
	        }

	        if (results != null) {
	        	TreeQueryResult resHyp = results.getLeft();
	        	TreeQueryResult resSul = results.getRight();
	        	if (candidateFound)
	        		candidates[idx] = new SymbolicWord(transition, symSuffix);
	        	else {
			        LocationComponent c = components.get(primeLocation);
			        ParameterizedSymbol act = extendedSuffix.length() > 0 ? extendedSuffix.getActions().firstSymbol() : null;
	        		candidates[idx] = candidate(location, act, extendedSuffix, resSul.getSdt(), resSul.getPiv(), resHyp.getSdt(), resHyp.getPiv(), c, ce);
	        	}
	        	if (!transitionHasCE(ce, idx)) {
	        		isCE[idx] = true;
	        	}
	            return true;
	        }
        }
        return false;
    }

    private Pair<TreeQueryResult, TreeQueryResult> checkForCE(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, Word<PSymbolInstance> transition) {
    	SymbolicWord symWord = new SymbolicWord(prefix, suffix);
    	TreeQueryResult resHyp = hypOracle.treeQuery(prefix, suffix);
    	TreeQueryResult resSul;
    	if (storedQueries.containsKey(symWord))
    		resSul = storedQueries.get(symWord);
    	else {
    		resSul = sulOracle.treeQuery(prefix, suffix);
    		storedQueries.put(symWord, resSul);
    	}

        boolean hasCE = sdtOracle.hasCounterexample(prefix,
                resHyp.getSdt(), resHyp.getPiv(),
                resSul.getSdt(), resSul.getPiv(),
                new TransitionGuard(), transition);

        return hasCE ? new ImmutablePair<TreeQueryResult, TreeQueryResult>(resHyp, resSul) : null;
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

    		if (hasCE) {
				Word<PSymbolInstance> primeLocation = hypothesis.transformAccessSequence(location);
				SymbolicWord sw = candidate(location,transition.lastSymbol().getBaseSymbol(),symSuffix, resSul.getSdt(), resSul.getPiv(), resHyp.getSdt(), resHyp.getPiv(), components.get(primeLocation), ce);

				// new by falk
				candidates[idx+1] = sw;
				return true;
			}
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



    private SymbolicWord candidate(Word<PSymbolInstance> prefix,
            ParameterizedSymbol action, SymbolicSuffix symSuffix, SymbolicDecisionTree sdtSul, PIV pivSul,
            SymbolicDecisionTree sdtHyp, PIV pivHyp, LocationComponent c, Word<PSymbolInstance> ce) {
    	Word<PSymbolInstance> candidate = null;

    	GuardExpression expr = sdtOracle.getCEGuard(prefix, sdtSul, pivSul, sdtHyp, pivHyp);

        Map<Word<PSymbolInstance>, Boolean> sulPaths = sulOracle.instantiate(prefix, symSuffix, sdtSul, pivSul);
        for (Word<PSymbolInstance> path : sulPaths.keySet()) {
        	ParameterGenerator parGen = new ParameterGenerator();
        	for (PSymbolInstance psi : prefix) {
        		for (DataType dt : psi.getBaseSymbol().getPtypes())
        			parGen.next(dt);
        	}

        	VarMapping<SuffixValue, Parameter> renaming = new VarMapping<>();
        	SuffixValueGenerator svGen = new SuffixValueGenerator();
        	for (ParameterizedSymbol ps : symSuffix.getActions()) {
        		for (DataType dt : ps.getPtypes()) {
        			Parameter p = parGen.next(dt);
        			SuffixValue sv = svGen.next(dt);
        			renaming.put(sv, p);
        		}
        	}
        	GuardExpression exprR = expr.relabel(renaming);

        	ParValuation pars = new ParValuation(path);
        	Mapping<SymbolicDataValue, DataValue<?>> vals = new Mapping<>();
        	vals.putAll(DataWords.computeVarValuation(pars, pivSul));
        	vals.putAll(pars);
        	vals.putAll(consts);

        	if (exprR.isSatisfied(vals)) {
        		candidate = path.prefix(prefix.length() + 1);
        		SymbolicSuffix suffix = new SymbolicSuffix(candidate, ce.suffix(symSuffix.length() - 1), consts);
        		return new SymbolicWord(candidate, suffix);
        	}
        }
        throw new IllegalStateException("No CE transition found");
    }

    private int binarySearch(Word<PSymbolInstance> ce, int ... indices) {

        assert ce.length() > 1;

        boolean[] results = new boolean[ce.length()];
        candidates = new SymbolicWord[ce.length()];
        isCE = new boolean[ce.length()];
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

    public Set<DefaultQuery<PSymbolInstance, Boolean>> getCounterExamples() {
    	Set<DefaultQuery<PSymbolInstance, Boolean>> ces = new LinkedHashSet<DefaultQuery<PSymbolInstance, Boolean>>();
    	for (Map.Entry<SymbolicWord, TreeQueryResult> e : candidateCEs.entrySet()) {
    		SymbolicWord sw = e.getKey();
    		TreeQueryResult tqr = e.getValue();
    		Map<Word<PSymbolInstance>, Boolean> cemaps = sulOracle.instantiate(sw.getPrefix(), sw.getSuffix(), tqr.getSdt(), tqr.getPiv());
    		for (Map.Entry<Word<PSymbolInstance>, Boolean> c : cemaps.entrySet()) {
    			ces.add(new DefaultQuery<PSymbolInstance, Boolean>(c.getKey(), c.getValue()));
    		}
    	}

    	return ces;
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
