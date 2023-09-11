package de.learnlib.ralib.learning.rattt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.ceanalysis.PrefixFinder;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.dt.DT;
import de.learnlib.ralib.dt.DTHyp;
import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.dt.MappedPrefix;
import de.learnlib.ralib.dt.ShortPrefix;
import de.learnlib.ralib.learning.AutomatonBuilder;
import de.learnlib.ralib.learning.CounterexampleAnalysis;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.IOAutomatonBuilder;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.RaLearningAlgorithm;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class RaLambda implements RaLearningAlgorithm {

    public static final Word<PSymbolInstance> EMPTY_PREFIX = Word.epsilon();

    public static final SymbolicSuffix EMPTY_SUFFIX = new SymbolicSuffix(Word.<PSymbolInstance>epsilon(),
            Word.<PSymbolInstance>epsilon());

    private final DT dt;

    private final Constants consts;

    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples = new LinkedList<>();
    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> candidateCEs = new LinkedList<>();

    private DTHyp hyp = null;

    private final TreeOracle sulOracle;

    private final SDTLogicOracle sdtLogicOracle;

    private final TreeOracleFactory hypOracleFactory;

    private final OptimizedSymbolicSuffixBuilder suffixBuilder;
    private ConstraintSolver solver = null;

    private QueryStatistics queryStats = null;

    private final boolean ioMode;

    private static final LearnLogger log = LearnLogger.getLogger(RaLambda.class);

    private boolean useOldAnalyzer;

    private final Map<Word<PSymbolInstance>, Boolean> guardPrefixes = new LinkedHashMap<Word<PSymbolInstance>, Boolean>();

    private PrefixFinder prefixFinder = null;

    public RaLambda(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            boolean ioMode, ParameterizedSymbol... inputs) {

        this(oracle, hypOracleFactory, sdtLogicOracle, consts, ioMode, false, inputs);
    }

    public RaLambda(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            boolean ioMode, boolean useOldAnalyzer, ParameterizedSymbol... inputs) {

        this(oracle, hypOracleFactory, sdtLogicOracle, consts, ioMode, useOldAnalyzer, false, inputs);
    }

    public RaLambda(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            boolean ioMode, boolean useOldAnalyzer, boolean thoroughSearch, ParameterizedSymbol... inputs) {

        this.ioMode = ioMode;
        this.dt = new DT(oracle, ioMode, consts, inputs);
        this.consts = consts;
        this.sulOracle = oracle;
        this.sdtLogicOracle = sdtLogicOracle;
        this.hypOracleFactory = hypOracleFactory;
        this.useOldAnalyzer = useOldAnalyzer;
        this.suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts);
        this.dt.initialize();
    }

    public RaLambda(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            ParameterizedSymbol... inputs) {
        this(oracle, hypOracleFactory, sdtLogicOracle, consts, false, false, inputs);
    }

    public void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce) {
        log.logEvent("adding counterexample: " + ce);
        counterexamples.add(ce);
    }

    public void learn() {

    	if (hyp == null)
    		buildNewHypothesis();

        while (analyzeCounterExample());

        if (queryStats != null)
        	queryStats.hypothesisConstructed();

    }

    private void buildNewHypothesis() {

        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        components.putAll(dt.getComponents());

        AutomatonBuilder ab = new AutomatonBuilder(components, consts, dt);
        hyp = (DTHyp) ab.toRegisterAutomaton();
        if (prefixFinder != null) {
        	prefixFinder.setHypothesis(hyp);
        	prefixFinder.setComponents(components);
        	prefixFinder.setHypothesisTreeOracle(hypOracleFactory.createTreeOracle(hyp));
        }
    }

    private boolean analyzeCounterExample() {
        if (useOldAnalyzer)
            return analyzeCounterExampleOld();
        log.logPhase("Analyzing Counterexample");

        if (candidateCEs.isEmpty()) {
        	prefixFinder = null;
        	if (counterexamples.isEmpty()) {
        		assert noShortPrefixes() && !dt.isMissingParameter();
        		return false;
        	}
        	else {
    			DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.poll();
    			candidateCEs.push(ce);
    		}
        }

        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);

        if (prefixFinder == null) {
        	Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        	components.putAll(dt.getComponents());
            prefixFinder = new PrefixFinder(sulOracle, hypOracle, hyp, sdtLogicOracle, components, consts);
        }

        boolean foundce = false;
        DefaultQuery<PSymbolInstance, Boolean> ce = null;
        Deque<DefaultQuery<PSymbolInstance, Boolean>> ces = new ArrayDeque<DefaultQuery<PSymbolInstance, Boolean>>();
        ces.addAll(candidateCEs);
        while(!foundce && !ces.isEmpty()) {
        	ce = ces.poll();
        	boolean hypce = hyp.accepts(ce.getInput());
        	boolean sulce = ce.getOutput();
        	foundce = hypce != sulce;
        }

        if (!foundce) {
        	candidateCEs.clear();
        	return true;
        }

        if (queryStats != null) {
        	queryStats.analyzingCounterExample();
        	queryStats.analyzeCE(ce.getInput());
        }
        Word<PSymbolInstance> ceWord = ce.getInput();
        CEAnalysisResult result = prefixFinder.analyzeCounterexample(ceWord);
        Word<PSymbolInstance> transition = result.getPrefix();						// u alpha(d)

        for (DefaultQuery<PSymbolInstance, Boolean> q : prefixFinder.getCounterExamples()) {
        	if (!candidateCEs.contains(q))
        		candidateCEs.addLast(q);
        }

        if (queryStats != null)
        	queryStats.processingCounterExample();

        if (isGuardRefinement(transition, result)) {
        	addPrefix(transition);
        }
        else {
        	expand(transition);
        }

        boolean consistent = false;
        while (!consistent) {
        	consistent = true;

        	if (!checkLocationConsistency()) {
        		consistent = false;
        	}

        	if (!checkRegisterClosedness()) {
        		consistent = false;
        	}

        	if (!checkGuardConsistency()) {
        		consistent = false;
        	}

        	if (!checkRegisterConsistency()) {
        		consistent = false;
        	}
        }

        if (noShortPrefixes() && !dt.isMissingParameter())
        	buildNewHypothesis();

        return true;
    }

    private boolean isGuardRefinement(Word<PSymbolInstance> word, CEAnalysisResult ceaResult) {
    	if (dt.getLeaf(word) != null)
    		return false;

    	Word<PSymbolInstance> src_id = word.prefix(word.size() - 1);
    	DTLeaf src_c = dt.getLeaf(src_id);
        Branching hypBranching = null;
        PIV piv = null;
        if (src_c.getAccessSequence().equals(src_id)) {
            hypBranching = src_c.getBranching(word.lastSymbol().getBaseSymbol());
            piv = src_c.getPrimePrefix().getParsInVars();
        } else {
            ShortPrefix sp = (ShortPrefix) src_c.getShortPrefixes().get(src_id);
            hypBranching = sp.getBranching(word.lastSymbol().getBaseSymbol());
            piv = sp.getParsInVars();
        }

        if (hypBranching.getBranches().keySet().contains(word))
        	return false;

        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);
        TreeQueryResult tqrHyp = hypOracle.treeQuery(word, ceaResult.getSuffix());
        TreeQueryResult tqrSul = ceaResult.getTreeQueryResult();
        if (tqrSul == null) {
        	tqrSul = sulOracle.treeQuery(word, ceaResult.getSuffix());
        }

        if (tqrHyp.getSdt().isEquivalent(tqrSul.getSdt(), tqrSul.getPiv())) {
        	guardPrefixes.put(word, false);
        	return true;
        }

        TransitionGuard guard = AutomatonBuilder.findMatchingGuard(word, piv, hypBranching.getBranches(), consts);
        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : hypBranching.getBranches().entrySet()) {
        	boolean eq = sdtLogicOracle.areEquivalent(e.getValue(), piv, guard, piv, new Mapping<SymbolicDataValue, DataValue<?>>());
        	if (eq && !e.getKey().equals(word)) {
        		guardPrefixes.put(word, true);
        		return true;
        	}
        }

    	return false;
    }

    private void addPrefix(Word<PSymbolInstance> u) {
    	dt.sift(u, true);
    }

    private void expand(Word<PSymbolInstance> u) {
    	DTLeaf l = dt.getLeaf(u);
    	assert l != null;
    	l.elevatePrefix(dt, u, hyp, sdtLogicOracle);
    }

    private boolean checkLocationConsistency() {

    	for (DTLeaf l : dt.getLeaves()) {
    		MappedPrefix mp = l.getPrimePrefix();
    		Iterator<MappedPrefix> it = l.getShortPrefixes().iterator();
    		while (it.hasNext()) {
    			ShortPrefix sp = (ShortPrefix)it.next();
    			for (ParameterizedSymbol psi : dt.getInputs()) {
    				Branching access_b = l.getBranching(psi);
    				Branching prefix_b = sp.getBranching(psi);
    				for (Word<PSymbolInstance> ws : prefix_b.getBranches().keySet()) {
    					Word<PSymbolInstance> wa = DTLeaf.branchWithSameGuard(ws, prefix_b, sp.getParsInVars(), access_b, mp.getParsInVars(), sdtLogicOracle);

    					DTLeaf la = dt.getLeaf(wa);
    					DTLeaf ls = dt.getLeaf(ws);
    					if (la != ls) {
    						SymbolicSuffix v = distinguishingSuffix(wa, la, ws, ls);
    						dt.split(sp.getPrefix(), v, l);
    						return false;
    					}
    				}
    			}
    		}
    	}
    	return true;
    }

    private boolean checkRegisterClosedness() {
    	return dt.checkVariableConsistency(suffixBuilder);
    }

    private boolean checkGuardConsistency() {
    	Map<Word<PSymbolInstance>, Boolean> toReuse = new LinkedHashMap<Word<PSymbolInstance>, Boolean>();
    	for (Word<PSymbolInstance> word : guardPrefixes.keySet()) {
    		Word<PSymbolInstance> src_id = word.prefix(word.size() - 1);
    		DTLeaf src_c = dt.getLeaf(src_id);
    		DTLeaf dest_c = dt.getLeaf(word);
            Branching hypBranching = null;
            if (src_c.getAccessSequence().equals(src_id)) {
                hypBranching = src_c.getBranching(word.lastSymbol().getBaseSymbol());
            } else {
                ShortPrefix sp = (ShortPrefix) src_c.getShortPrefixes().get(src_id);
                hypBranching = sp.getBranching(word.lastSymbol().getBaseSymbol());
            }
            if (hypBranching.getBranches().keySet().contains(word)) {
            	continue;
            }

            Word<PSymbolInstance> branch = branchWithSameGuard(dest_c.getPrefix(word), src_c.getPrefix(src_id), hypBranching);
            DTLeaf branchLeaf = dt.getLeaf(branch);

            SymbolicSuffix suffix = null;

            if (branchLeaf != dest_c) {
            	suffix = distinguishingSuffix(branch, branchLeaf, word, dest_c);
            }
            else {
            	if (!guardPrefixes.get(word)) {
		            MappedPrefix mp = dest_c.getPrefix(word);
		            Map<SymbolicSuffix, TreeQueryResult> branchTQRs = branchLeaf.getPrefix(branch).getTQRs();
		            for (Map.Entry<SymbolicSuffix, TreeQueryResult> e : mp.getTQRs().entrySet()) {
		            	TreeQueryResult tqr = e.getValue();
		            	SymbolicSuffix s = e.getKey();

		            	TreeQueryResult otherTQR = branchTQRs.get(s);
                        //todo: not sure why this check was not here yet? It happens sometimes ...
                        if (branchTQRs.get(s) == null) {
                            continue;
                        }

		            	if (tqr.getSdt().isEquivalent(branchTQRs.get(s).getSdt(), tqr.getPiv())) {
		            		if (!tqr.getPiv().equals(otherTQR.getPiv())) {
			            		if (suffix == null || suffix.length() > s.length()+1) {
			            			if (suffixBuilder != null && tqr.getSdt() instanceof SDT) {
			            				suffix = suffixBuilder.extendSuffix(word, (SDT)tqr.getSdt(), tqr.getPiv(), s);
			            			} else {
			            				suffix = new SymbolicSuffix(word.prefix(word.length()-1), word.suffix(1), consts);
			            				suffix = suffix.concat(s);
			            			}
			            		}
		            		}
		            	}
		            }
	            }
            }

            if (suffix == null) {
            	toReuse.put(word, guardPrefixes.get(word));
            	continue;
            }

            dt.addSuffix(suffix, src_c);
            return false;
    	}

    	guardPrefixes.clear();
    	guardPrefixes.putAll(toReuse);
    	return true;
    }

    private boolean checkRegisterConsistency() {
    	return dt.checkRegisterConsistency(suffixBuilder);
    }

    private SymbolicSuffix distinguishingSuffix(Word<PSymbolInstance> wa, DTLeaf ca, Word<PSymbolInstance> wb, DTLeaf cb) {
    	Word<PSymbolInstance> sa = wa.suffix(1);
    	Word<PSymbolInstance> sb = wb.suffix(1);

    	assert sa.getSymbol(0).getBaseSymbol().equals(sb.getSymbol(0).getBaseSymbol());

    	SymbolicSuffix v = dt.findLCA(ca, cb).getSuffix();

    	Word<PSymbolInstance> prefixA = wa.prefix(wa.length() - 1);
    	Word<PSymbolInstance> prefixB = wb.prefix(wb.length() - 1);

    	TreeQueryResult tqrA = ca.getTQR(wa, v);
    	TreeQueryResult tqrB = cb.getTQR(wb, v);

        // todo: i had to add this check. not sure why this did not happen before?
        if (tqrA != null && tqrB != null) {

            SymbolicDecisionTree sdtA = tqrA.getSdt();
            SymbolicDecisionTree sdtB = tqrB.getSdt();

            if (suffixBuilder != null && solver != null && sdtA instanceof SDT && sdtB instanceof SDT) {
//    		return suffixBuilder.extendDistinguishingSuffix(wa, (SDT)sdtA, tqrA.getPiv(), wb, (SDT)sdtB, tqrB.getPiv(), v);
                return suffixBuilder.distinguishingSuffixFromSDTs(wa, (SDT) sdtA, tqrA.getPiv(), wb, (SDT) sdtB, tqrB.getPiv(), v.getActions(), solver);
            }
        }

    	SymbolicSuffix alpha_a = new SymbolicSuffix(prefixA, sa, consts);
    	SymbolicSuffix alpha_b = new SymbolicSuffix(prefixB, sb, consts);
    	return alpha_a.getFreeValues().size() > alpha_b.getFreeValues().size()
    		   ? alpha_a.concat(v)
    		   : alpha_b.concat(v);
    }

    private boolean noShortPrefixes() {
    	for (DTLeaf l : dt.getLeaves()) {
    		if (!l.getShortPrefixes().isEmpty())
    			return false;
    	}
    	return true;
    }

    public void setUseOldAnalyzer(boolean useOldAnalyzer) {
        this.useOldAnalyzer = useOldAnalyzer;
    }

    private Word<PSymbolInstance> branchWithSameGuard(MappedPrefix mp, MappedPrefix src_id, Branching branching) {
    	Word<PSymbolInstance> dw = mp.getPrefix();

    	return branching.transformPrefix(dw);
    }

    private boolean analyzeCounterExampleOld() {
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
        while(!dt.checkVariableConsistency(suffixBuilder));
        return true;
    }

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

    public Map<Word<PSymbolInstance>, LocationComponent> getComponents() {
        return dt.getComponents();
    }

    public void setStatisticCounter(QueryStatistics queryStats) {
    	this.queryStats = queryStats;
    }

    public QueryStatistics getQueryStatistics() {
    	return queryStats;
    }

    public void setSolver(ConstraintSolver solver) {
    	this.solver = solver;
    }

    @Override
    public RaLearningAlgorithmName getName() {
        return RaLearningAlgorithmName.RALAMBDA;
    }

    @Override
    public String toString() {
        return this.dt.toString();
    }
}
