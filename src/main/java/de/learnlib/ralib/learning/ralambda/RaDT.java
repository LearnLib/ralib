package de.learnlib.ralib.learning.ralambda;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.dt.DT;
import de.learnlib.ralib.dt.DTHyp;
import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.learning.AutomatonBuilder;
import de.learnlib.ralib.learning.CounterexampleAnalysis;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.IOAutomatonBuilder;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.RaLearningAlgorithm;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class RaDT implements RaLearningAlgorithm {

    private final DT dt;

    private final Constants consts;

    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples = new LinkedList<>();

    private DTHyp hyp = null;

    private final TreeOracle sulOracle;

    private final SDTLogicOracle sdtLogicOracle;

    private final TreeOracleFactory hypOracleFactory;

    private final OptimizedSymbolicSuffixBuilder suffixBuilder;
    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private QueryStatistics queryStats = null;

    private final boolean ioMode;

    private static final Logger LOGGER = LoggerFactory.getLogger(RaDT.class);

    public RaDT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            boolean ioMode, ParameterizedSymbol... inputs) {
    	this.sulOracle = oracle;
    	this.hypOracleFactory = hypOracleFactory;
    	this.sdtLogicOracle = sdtLogicOracle;
    	this.consts = consts;
    	this.ioMode = ioMode;
    	if (oracle instanceof MultiTheoryTreeOracle) {
    		this.restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, ((MultiTheoryTreeOracle)oracle).getTeachers());
    	} else {
    		this.restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts);
    	}
        this.suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrictionBuilder);
        this.dt = new DT(oracle, ioMode, consts, inputs);
        this.dt.initialize();
    }

    private void buildHypothesis() {
        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        components.putAll(dt.getComponents());

        AutomatonBuilder ab = new AutomatonBuilder(components, consts, dt);
        hyp = (DTHyp) ab.toRegisterAutomaton();
    }

	@Override
	public void learn() {
        if (hyp == null) {
            buildHypothesis();
        }

        while (analyzeCounterExample());

        if (queryStats != null) {
        	queryStats.hypothesisConstructed();
        }
	}

    private boolean analyzeCounterExample() {
        LOGGER.info(Category.PHASE, "Analyzing Counterexample");
        if (counterexamples.isEmpty()) {
            return false;
        }

        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);

        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        components.putAll(dt.getComponents());
        CounterexampleAnalysis analysis = new CounterexampleAnalysis(sulOracle, hypOracle, hyp, sdtLogicOracle,
                components, consts);

        DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.peek();

        // check if ce still is a counterexample ...
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
        DTLeaf leaf = dt.getLeaf(accSeq);
        dt.addSuffix(res.getSuffix(), leaf);
        while(!dt.checkIOSuffixes());
        while(!dt.checkVariableConsistency(suffixBuilder));
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
        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        components.putAll(dt.getComponents());
        AutomatonBuilder ab;
        if (ioMode) {
            ab = new IOAutomatonBuilder(components, consts);
        } else {
            ab = new AutomatonBuilder(components, consts);
        }
        return ab.toRegisterAutomaton();
	}

    public DT getDT() {
        return dt;
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

}
