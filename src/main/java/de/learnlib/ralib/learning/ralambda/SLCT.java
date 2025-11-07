package de.learnlib.ralib.learning.ralambda;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.ct.CTAutomatonBuilder;
import de.learnlib.ralib.ct.CTLeaf;
import de.learnlib.ralib.ct.ClassificationTree;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.learning.CounterexampleAnalysis;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.RaLearningAlgorithm;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class SLCT implements RaLearningAlgorithm {

	private final ClassificationTree ct;

	private final Constants consts;

    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples;

    private Hypothesis hyp;

    private final TreeOracle sulOracle;

    private final SDTLogicOracle sdtLogicOracle;

    private final TreeOracleFactory hypOracleFactory;

    private final OptimizedSymbolicSuffixBuilder suffixBuilder;
    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private QueryStatistics queryStats;

    private final boolean ioMode;

    private final ConstraintSolver solver;

    private static final Logger LOGGER = LoggerFactory.getLogger(SLCT.class);

    public SLCT(TreeOracle sulOracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle,
    		Constants consts, boolean ioMode, ConstraintSolver solver, ParameterizedSymbol ... inputs) {
    	this.consts = consts;
    	this.sulOracle = sulOracle;
    	this.sdtLogicOracle = sdtLogicOracle;
    	this.hypOracleFactory = hypOracleFactory;
    	this.solver = solver;
    	this.ioMode = ioMode;
    	restrictionBuilder = sulOracle.getRestrictionBuilder();
    	suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrictionBuilder);
    	counterexamples = new LinkedList<>();
    	hyp = null;
    	ct = new ClassificationTree(sulOracle, solver, restrictionBuilder, suffixBuilder, consts, ioMode, inputs);
    	ct.sift(RaStar.EMPTY_PREFIX);
    }

	@Override
	public void learn() {
		if (hyp == null) {
			while(!checkClosedness());
			buildHypothesis();
		}

		while(analyzeCounterExample());

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

	private void buildHypothesis() {
		CTAutomatonBuilder ab = new CTAutomatonBuilder(ct, consts, ioMode, solver);
		hyp = ab.buildHypothesis();
	}

	private boolean analyzeCounterExample() {
        LOGGER.info(Category.PHASE, "Analyzing Counterexample");
        if (counterexamples.isEmpty()) {
            return false;
        }

        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);

        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<>();
        for (CTLeaf leaf : ct.getLeaves()) {
        	for (Word<PSymbolInstance> u : leaf.getPrefixes()) {
        		components.put(u, leaf);
        	}
        }
        CounterexampleAnalysis analysis = new CounterexampleAnalysis(sulOracle, hypOracle, hyp, sdtLogicOracle, components, consts);

        DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.peek();

        boolean hypce = hyp.accepts(ce.getInput());
        boolean sulce = ce.getOutput();
        if (hypce == sulce) {
            LOGGER.info(Category.EVENT, "word is not a counterexample: " + ce + " - " + sulce);
            counterexamples.poll();
            return false;
        }

        if (queryStats != null) {
        	queryStats.analyzingCounterExample();
        }

        CEAnalysisResult res = analysis.analyzeCounterexample(ce.getInput());

        if (queryStats != null) {
        	queryStats.processingCounterExample();
        	queryStats.analyzeCE(ce.getInput());
        }

        Word<PSymbolInstance> accSeq = hyp.transformAccessSequence(res.getPrefix());
        CTLeaf leaf = ct.getLeaf(accSeq);
        assert leaf != null : "Prefix not in classification tree: " + accSeq;
        ct.refine(leaf, res.getSuffix());

        while(!checkClosedness());

        buildHypothesis();
        return true;
	}

	@Override
	public void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce) {
        LOGGER.info(Category.EVENT, "adding counterexample: " + ce);
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
		return RaLearningAlgorithmName.RADT;
	}

	public ClassificationTree getCT() {
		return ct;
	}
}
