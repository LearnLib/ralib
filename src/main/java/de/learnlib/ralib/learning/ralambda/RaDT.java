package de.learnlib.ralib.learning.ralambda;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.query.DefaultQuery;
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
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class RaDT implements RaLearningAlgorithm {

    private final DT dt;

    private final Constants consts;

    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples = new LinkedList<>();

    private DTHyp hyp = null;

    private final TreeOracle sulOracle;

    private final SDTLogicOracle sdtLogicOracle;

    private final TreeOracleFactory hypOracleFactory;

    private QueryStatistics queryStats = null;

    private final boolean ioMode;

    private static final LearnLogger log = LearnLogger.getLogger(RaDT.class);

    public RaDT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            boolean ioMode, ParameterizedSymbol... inputs) {
    	this.sulOracle = oracle;
    	this.hypOracleFactory = hypOracleFactory;
    	this.sdtLogicOracle = sdtLogicOracle;
    	this.consts = consts;
    	this.ioMode = ioMode;
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
    	if (hyp == null)
    		buildHypothesis();

        while (analyzeCounterExample());

        if (queryStats != null)
        	queryStats.hypothesisConstructed();
	}

    private boolean analyzeCounterExample() {
        log.logPhase("Analyzing Counterexample");
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
            log.logEvent("word is not a counterexample: " + ce + " - " + sulce);
            counterexamples.poll();
            return false;
        }

        if (queryStats != null)
        	queryStats.analyzingCounterExample();

        CEAnalysisResult res = analysis.analyzeCounterexample(ce.getInput());

        if (queryStats != null) {
        	queryStats.processingCounterExample();
        	queryStats.analyzeCE(ce.getInput());
        }

        Word<PSymbolInstance> accSeq = hyp.transformAccessSequence(res.getPrefix());
        DTLeaf leaf = dt.getLeaf(accSeq);
        dt.addSuffix(res.getSuffix(), leaf);
        while(!dt.checkVariableConsistency(null));
        return true;
    }

	@Override
	public void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce) {
        log.logEvent("adding counterexample: " + ce);
        counterexamples.add(ce);
    }

	@Override
	public Hypothesis getHypothesis() {
        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        components.putAll(dt.getComponents());
        AutomatonBuilder ab;
        if (ioMode)
            ab = new IOAutomatonBuilder(components, consts, dt);
        else
            ab = new AutomatonBuilder(components, consts, this.dt);
        return ab.toRegisterAutomaton();
	}

    public DT getDT() {
        return this.dt;
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