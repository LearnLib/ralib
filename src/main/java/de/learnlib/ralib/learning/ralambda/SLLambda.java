package de.learnlib.ralib.learning.ralambda;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.ceanalysis.PrefixFinder;
import de.learnlib.ralib.ceanalysis.PrefixFinder.Result;
import de.learnlib.ralib.ct.CTAutomatonBuilder;
import de.learnlib.ralib.ct.CTHypothesis;
import de.learnlib.ralib.ct.ClassificationTree;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.RaLearningAlgorithm;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class SLLambda implements RaLearningAlgorithm {

    private final ClassificationTree ct;

    private final Constants consts;

    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples;

    private CTHypothesis hyp;

    private final TreeOracle sulOracle;

    private final OptimizedSymbolicSuffixBuilder suffixBuilder;
    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private final Map<DataType, Theory> teachers;

    private QueryStatistics queryStats;

    private final boolean ioMode;

    private final ConstraintSolver solver;

    public SLLambda(TreeOracle sulOracle, Map<DataType, Theory> teachers,
    		Constants consts, boolean ioMode, ConstraintSolver solver,
    		ParameterizedSymbol ... inputs) {
    	this.sulOracle = sulOracle;
    	this.teachers = teachers;
    	this.consts = consts;
    	this.ioMode = ioMode;
    	this.solver = solver;
    	restrictionBuilder = sulOracle.getRestrictionBuilder();
    	suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrictionBuilder);
        counterexamples = new ArrayDeque<>();
    	hyp = null;
    	ct = new ClassificationTree(sulOracle, solver, restrictionBuilder, suffixBuilder, consts, ioMode, inputs);
    	ct.initialize();
    }

	@Override
	public void learn() {
		if (hyp == null) {
			while (!checkClosedness());
			buildHypothesis();
		}

		while (analyzeCounterExample());

		if (queryStats != null) {
			queryStats.hypothesisConstructed();
		}
	}

	private boolean checkClosedness() {
		if (!ct.checkOutputClosed()) {
			return false;
		}
		if (!ct.checkLocationClosedness()) {
			return false;
		}
		if (!ct.checkTransitionClosedness()) {
			return false;
		}
		if (!ct.checkRegisterClosedness()) {
			return false;
		}
		return true;
	}

	private boolean checkConsistency() {
		if (!ct.checkLocationConsistency()) {
			return false;
		}
		if (!ct.checkTransitionConsistency()) {
			return false;
		}
		if (!ct.checkRegisterConsistency()) {
			return false;
		}
		return true;
	}

	private void buildHypothesis() {
		CTAutomatonBuilder ab = new CTAutomatonBuilder(ct, consts, ioMode, solver);
		hyp = ab.buildHypothesis();
	}

	private boolean analyzeCounterExample() {
		if (counterexamples.isEmpty()) {
			return false;
		}

		// check whether ce is still a ce
        DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.peek();
        Word<PSymbolInstance> ceWord = ce.getInput();
        boolean accHyp = hyp.accepts(ceWord);
        boolean accSUL = ce.getOutput();
        if (accHyp == accSUL) {
        	// not a ce, dequeue it
        	counterexamples.poll();
        	return true;
        }

        // analyze counterexample

        if (queryStats != null) {
        	queryStats.analyzingCounterExample();
        	queryStats.analyzeCE(ceWord);
        }

        PrefixFinder prefixFinder = new PrefixFinder(sulOracle,
        		hyp,
        		ct,
        		teachers,
        		restrictionBuilder,
        		solver,
        		consts);

        Result res = prefixFinder.analyzeCounterExample(ceWord);

        // process counterexample

        if (queryStats != null)
        	queryStats.processingCounterExample();

        switch (res.result()) {
        case TRANSITION:
        	assert !ct.getPrefixes().contains(res.prefix()) : "Duplicate prefix: " + res.prefix();
        	ct.sift(res.prefix());
        	break;
        case LOCATION:
        	assert !ct.getShortPrefixes().contains(res.prefix()) : "Prefix already short: " + res.prefix();
        	ct.expand(res.prefix());
        	break;
        }

        boolean closedAndConsistent = false;
        while (!closedAndConsistent) {
        	if (checkClosedness()) {
        		if (checkConsistency()) {
        			closedAndConsistent = true;
        		}
        	}
        }

        buildHypothesis();
        return true;
	}

	@Override
	public void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce) {
		counterexamples.add(ce);
	}

	@Override
	public Hypothesis getHypothesis() {
		return hyp;
	}

	@Override
	public void setStatisticCounter(QueryStatistics queryStats) {
		this.queryStats = queryStats;
	}

	@Override
	public QueryStatistics getQueryStatistics() {
		return queryStats;
	}

	@Override
	public RaLearningAlgorithmName getName() {
		return RaLearningAlgorithmName.RALAMBDA;
	}

	public ClassificationTree getCT() {
		return ct;
	}
}
