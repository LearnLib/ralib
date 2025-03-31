package de.learnlib.ralib.ceanalysis;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.RemappingIterator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.*;
import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.smt.SMTUtil;
import de.learnlib.ralib.theory.Memorables;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class PrefixFinder {

    private final TreeOracle sulOracle;

    private TreeOracle hypOracle;

    private Hypothesis hypothesis;

    private final SDTLogicOracle sdtOracle;

    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private final Constants consts;

    private SymbolicWord[] candidates;

    private final Map<SymbolicWord, TreeQueryResult> candidateCEs = new LinkedHashMap<SymbolicWord, TreeQueryResult>();
    private final Map<SymbolicWord, TreeQueryResult> storedQueries = new LinkedHashMap<SymbolicWord, TreeQueryResult>();

    private static final Logger LOGGER = LoggerFactory.getLogger(PrefixFinder.class);

    public PrefixFinder(TreeOracle sulOracle, TreeOracle hypOracle,
            Hypothesis hypothesis, SDTLogicOracle sdtOracle,
            Constants consts) {

        this.sulOracle = sulOracle;
        this.hypOracle = hypOracle;
        this.hypothesis = hypothesis;
        this.sdtOracle = sdtOracle;
        this.consts = consts;
        this.restrictionBuilder = sulOracle.getRestrictionBuilder();
    }

    public CEAnalysisResult analyzeCounterexample(Word<PSymbolInstance> ce) {
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

        candidateCEs.put(candidates[idx], tqr);
        storeCandidateCEs(ce, idx);

        return result;
    }

	private int findIndex(Word<PSymbolInstance> ce) {
		candidates = new SymbolicWord[ce.length()];
		int max = ce.length() - 1;
		for (int idx=max; idx>=0; idx = idx-1) {

			Word<PSymbolInstance> prefix = ce.prefix(idx);
			Word<PSymbolInstance> nextPrefix = ce.prefix(idx+1);

			LOGGER.trace(Category.DATASTRUCTURE, "idx: {} ce:     {}", idx, ce);
			LOGGER.trace(Category.DATASTRUCTURE, "idx: {} prefix: {}", idx, prefix);
			LOGGER.trace(Category.DATASTRUCTURE, "idx: {} next:   {}", idx, nextPrefix);

			// check for location counterexample ...
			//
			Word<PSymbolInstance> suffix = ce.suffix(ce.length() - nextPrefix.length());
			SymbolicSuffix symSuffix = new SymbolicSuffix(nextPrefix, suffix, restrictionBuilder);
			LOC_CHECK: for (Word<PSymbolInstance> u : hypothesis.possibleAccessSequences(prefix)) {
				Word<PSymbolInstance> uAlpha = hypothesis.transformTransitionSequence(nextPrefix, u);
				TreeQueryResult uAlphaResult = sulOracle.treeQuery(uAlpha, symSuffix);
				storedQueries.put(new SymbolicWord(uAlpha, symSuffix), uAlphaResult);

				// check if the word is inequivalent to all access sequences
				//
				for (Word<PSymbolInstance> uPrime : hypothesis.possibleAccessSequences(nextPrefix)) {
					TreeQueryResult uPrimeResult = sulOracle.treeQuery(uPrime, symSuffix);
					storedQueries.put(new SymbolicWord(uPrime, symSuffix), uPrimeResult);

					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} u:  {}", idx, u);
					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} ua: {}", idx, uAlpha);
					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} u': {}", idx, uPrime);
					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} v:  {}", idx, symSuffix);

					// different piv sizes
					//
					if (!Memorables.typedSize(uPrimeResult.sdt().getDataValues()).equals(
							Memorables.typedSize(uAlphaResult.sdt().getDataValues()))) {
						continue;
					}

					// remapping
					//
					RemappingIterator<DataValue> iterator = new RemappingIterator<>(
							uPrimeResult.sdt().getDataValues(), uAlphaResult.sdt().getDataValues());

					for (Bijection<DataValue> m : iterator) {
						if (uAlphaResult.sdt().isEquivalent(uPrimeResult.sdt(), m)) {
							continue LOC_CHECK;
						}
					}

				}
				// found a counterexample!
				candidates[idx] = new SymbolicWord(uAlpha, symSuffix);
				LOGGER.trace(Category.COUNTEREXAMPLE, "Counterexample for location");
				return idx;
			}

			// check for transition counterexample ...
			//
			if (transitionHasCE(ce, idx-1)) {
				LOGGER.trace(Category.COUNTEREXAMPLE, "Counterexample for transition");
				return idx;
			}
		}
		throw new RuntimeException("should not reach here");
	}

//    private Pair<TreeQueryResult, TreeQueryResult> checkForCE(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, Word<PSymbolInstance> transition) {
//    	SymbolicWord symWord = new SymbolicWord(prefix, suffix);
//    	TreeQueryResult resHyp = hypOracle.treeQuery(prefix, suffix);
//    	TreeQueryResult resSul;
//    	if (storedQueries.containsKey(symWord))
//    		resSul = storedQueries.get(symWord);
//    	else {
//    		resSul = sulOracle.treeQuery(prefix, suffix);
//    		storedQueries.put(symWord, resSul);
//    	}
//
//        boolean hasCE = sdtOracle.hasCounterexample(prefix,
//                resHyp.getSdt(), resHyp.getPiv(),
//                resSul.getSdt(), resSul.getPiv(),
//                new TransitionGuard(), transition);
//
//        return hasCE ? new ImmutablePair<TreeQueryResult, TreeQueryResult>(resHyp, resSul) : null;
//    }

    private boolean transitionHasCE(Word<PSymbolInstance> ce, int idx) {
    	if (idx+1 >= ce.length())
    		return false;

    	Word<PSymbolInstance> prefix = ce.prefix(idx+1);

    	Word<PSymbolInstance> suffix = ce.suffix(ce.length() - (idx+1));
    	SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, restrictionBuilder);

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
	                resHyp.sdt(),
	                resSul.sdt(),
					ExpressionUtil.TRUE, transition);

    		if (hasCE) {
				SymbolicWord sw = candidate(location, symSuffix, resSul.sdt(), resHyp.sdt(), ce);
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
    	SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, restrictionBuilder);

    	Set<Word<PSymbolInstance>> locations = hypothesis.possibleAccessSequences(prefix);
    	for (Word<PSymbolInstance> location : locations) {
    		SymbolicWord symWord = new SymbolicWord(location, symSuffix);
    		TreeQueryResult tqr = storedQueries.get(symWord);

    		assert tqr != null;

    		candidateCEs.put(symWord, tqr);
    	}
    }

    private SymbolicWord candidate(Word<PSymbolInstance> prefix,
                                   SymbolicSuffix symSuffix, SDT sdtSul,
                                   SDT sdtHyp, Word<PSymbolInstance> ce) {
    	Word<PSymbolInstance> candidate = null;

    	Expression<Boolean> expr = sdtOracle.getCEGuard(prefix, sdtSul, sdtHyp);

        Map<Word<PSymbolInstance>, Boolean> sulPaths = sulOracle.instantiate(prefix, symSuffix, sdtSul);
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
        	Expression<Boolean> exprR = SMTUtil.renameVars(expr, renaming);

        	ParameterValuation pars = ParameterValuation.fromPSymbolWord(path);
        	Mapping<SymbolicDataValue, DataValue> vals = new Mapping<>();
        	//vals.putAll(DataWords.computeRegisterValuation(pars, pivSul));
        	vals.putAll(pars);
        	vals.putAll(consts);

        	if (exprR.evaluateSMT(SMTUtil.compose(vals))) {
        		candidate = path.prefix(prefix.length() + 1);
        		SymbolicSuffix suffix = new SymbolicSuffix(candidate, ce.suffix(symSuffix.length() - 1), restrictionBuilder);
        		return new SymbolicWord(candidate, suffix);
        	}
        }
        throw new IllegalStateException("No CE transition found");
    }

    public Set<DefaultQuery<PSymbolInstance, Boolean>> getCounterExamples() {
    	Set<DefaultQuery<PSymbolInstance, Boolean>> ces = new LinkedHashSet<DefaultQuery<PSymbolInstance, Boolean>>();
    	for (Map.Entry<SymbolicWord, TreeQueryResult> e : candidateCEs.entrySet()) {
    		SymbolicWord sw = e.getKey();
    		TreeQueryResult tqr = e.getValue();
    		Map<Word<PSymbolInstance>, Boolean> cemaps = sulOracle.instantiate(sw.getPrefix(), sw.getSuffix(), tqr.sdt());
    		for (Map.Entry<Word<PSymbolInstance>, Boolean> c : cemaps.entrySet()) {
    			ces.add(new DefaultQuery<PSymbolInstance, Boolean>(c.getKey(), c.getValue()));
    		}
    	}

    	return ces;
    }

    public void setHypothesisTreeOracle(TreeOracle hypOracle) {
        this.hypOracle = hypOracle;
    }

    public void setHypothesis(Hypothesis hyp) {
    	this.hypothesis = hyp;
    }

    //public void setComponents(Map<Word<PSymbolInstance>, LocationComponent> components) {
    //    this.components = components;
    //}
}
