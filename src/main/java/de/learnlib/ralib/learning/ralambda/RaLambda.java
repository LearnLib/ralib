//package de.learnlib.ralib.learning.ralambda;
//
//import java.util.ArrayDeque;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Deque;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.LinkedHashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import de.learnlib.logging.Category;
//import de.learnlib.query.DefaultQuery;
//import de.learnlib.ralib.ceanalysis.PrefixFinder;
//import de.learnlib.ralib.data.*;
//import de.learnlib.ralib.dt.DT;
//import de.learnlib.ralib.dt.DTHyp;
//import de.learnlib.ralib.dt.DTLeaf;
//import de.learnlib.ralib.dt.MappedPrefix;
//import de.learnlib.ralib.dt.ShortPrefix;
//import de.learnlib.ralib.learning.AutomatonBuilder;
//import de.learnlib.ralib.learning.Hypothesis;
//import de.learnlib.ralib.learning.IOAutomatonBuilder;
//import de.learnlib.ralib.learning.LocationComponent;
//import de.learnlib.ralib.learning.QueryStatistics;
//import de.learnlib.ralib.learning.RaLearningAlgorithm;
//import de.learnlib.ralib.learning.RaLearningAlgorithmName;
//import de.learnlib.ralib.learning.SymbolicSuffix;
//import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
//import de.learnlib.ralib.oracles.Branching;
//import de.learnlib.ralib.oracles.SDTLogicOracle;
//import de.learnlib.ralib.oracles.TreeOracle;
//import de.learnlib.ralib.oracles.TreeOracleFactory;
//import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
//import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
//import de.learnlib.ralib.smt.ConstraintSolver;
//import de.learnlib.ralib.theory.SDT;
//import de.learnlib.ralib.words.PSymbolInstance;
//import de.learnlib.ralib.words.ParameterizedSymbol;
//import net.automatalib.word.Word;
//
//public class RaLambda implements RaLearningAlgorithm {
//
//    public static final Word<PSymbolInstance> EMPTY_PREFIX = Word.epsilon();
//
//    public static final SymbolicSuffix EMPTY_SUFFIX = new SymbolicSuffix(Word.epsilon(),
//            Word.epsilon());
//
//    private final DT dt;
//
//    private final Constants consts;
//
//    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples = new LinkedList<>();
//    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> candidateCEs = new LinkedList<>();
//
//    private DTHyp hyp = null;
//
//    private final TreeOracle sulOracle;
//
//    private final SDTLogicOracle sdtLogicOracle;
//
//    private final TreeOracleFactory hypOracleFactory;
//
//    private final OptimizedSymbolicSuffixBuilder suffixBuilder;
//    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;
//    private ConstraintSolver solver = null;
//
//    private QueryStatistics queryStats = null;
//
//    private final boolean ioMode;
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(RaLambda.class);
//
//    private boolean useOldAnalyzer;
//
//    private final Map<Word<PSymbolInstance>, Boolean> guardPrefixes = new LinkedHashMap<Word<PSymbolInstance>, Boolean>();
//
//    private PrefixFinder prefixFinder = null;
//
//    public RaLambda(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
//            boolean ioMode, ParameterizedSymbol... inputs) {
//
//        this(oracle, hypOracleFactory, sdtLogicOracle, consts, ioMode, false, inputs);
//    }
//
//    public RaLambda(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
//            boolean ioMode, boolean useOldAnalyzer, ParameterizedSymbol... inputs) {
//
//        this(oracle, hypOracleFactory, sdtLogicOracle, consts, ioMode, useOldAnalyzer, false, inputs);
//    }
//
//    public RaLambda(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
//            boolean ioMode, boolean useOldAnalyzer, boolean thoroughSearch, ParameterizedSymbol... inputs) {
//
//        this.ioMode = ioMode;
//        this.consts = consts;
//        this.sulOracle = oracle;
//        this.sdtLogicOracle = sdtLogicOracle;
//        this.hypOracleFactory = hypOracleFactory;
//        this.useOldAnalyzer = useOldAnalyzer;
//        this.restrictionBuilder = oracle.getRestrictionBuilder();
//        this.suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrictionBuilder);
//        this.dt = new DT(oracle, ioMode, consts, inputs);
//        this.dt.initialize();
//    }
//
//    public RaLambda(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
//            ParameterizedSymbol... inputs) {
//        this(oracle, hypOracleFactory, sdtLogicOracle, consts, false, false, inputs);
//    }
//
//    @Override
//    public void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce) {
//        LOGGER.info(Category.EVENT, "adding counterexample: {}", ce);
//        counterexamples.add(ce);
//    }
//
//    @Override
//    public void learn() {
//
//        if (hyp == null) {
//            buildNewHypothesis();
//        }
//
//        while (analyzeCounterExample());
//
//        if (queryStats != null)
//        	queryStats.hypothesisConstructed();
//
//    }
//
//    private void buildNewHypothesis() {
//
//        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
//        components.putAll(dt.getComponents());
//        AutomatonBuilder ab = new AutomatonBuilder(components, consts, dt);
//
//        hyp = (DTHyp) ab.toRegisterAutomaton();
//        if (prefixFinder != null) {
//        	prefixFinder.setHypothesis(hyp);
//                //prefixFinder.setComponents(components);
//        	prefixFinder.setHypothesisTreeOracle(hypOracleFactory.createTreeOracle(hyp));
//        }
//    }
//
//    private boolean analyzeCounterExample() {
////        if (useOldAnalyzer)
////            return analyzeCounterExampleOld();
//        LOGGER.info(Category.PHASE, "Analyzing Counterexample");
//
//        if (candidateCEs.isEmpty()) {
//        	prefixFinder = null;
//        	if (counterexamples.isEmpty()) {
//        		assert noShortPrefixes() || !dt.isMissingParameter();
//        		return false;
//        	}
//        	else {
//    			DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.poll();
//    			candidateCEs.push(ce);
//    		}
//        }
//
//        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);
//
//        if (prefixFinder == null) {
//            //Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
//            //components.putAll(dt.getComponents());
//            prefixFinder = new PrefixFinder(sulOracle, hypOracle, hyp, sdtLogicOracle, consts);
//        }
//
//        boolean foundce = false;
//        DefaultQuery<PSymbolInstance, Boolean> ce = null;
//        Deque<DefaultQuery<PSymbolInstance, Boolean>> ces = new ArrayDeque<DefaultQuery<PSymbolInstance, Boolean>>();
//        ces.addAll(candidateCEs);
//        while(!foundce && !ces.isEmpty()) {
//        	ce = ces.poll();
//        	boolean hypce = hyp.accepts(ce.getInput());
//        	boolean sulce = ce.getOutput();
//            //System.out.println("ce: " + ce + " - " + sulce + " vs. " + hypce);
//        	foundce = hypce != sulce;
//        }
//
//        if (!foundce) {
//        	candidateCEs.clear();
//        	return true;
//        }
//
//        if (queryStats != null) {
//        	queryStats.analyzingCounterExample();
//        	queryStats.analyzeCE(ce.getInput());
//        }
//        Word<PSymbolInstance> ceWord = ce.getInput();
//        CEAnalysisResult result = prefixFinder.analyzeCounterexample(ceWord);
//        Word<PSymbolInstance> transition = result.getPrefix();						// u alpha(d)
//        //System.out.println("new prefix: " + transition);
//
//        for (DefaultQuery<PSymbolInstance, Boolean> q : prefixFinder.getCounterExamples()) {
//        	if (!candidateCEs.contains(q))
//        		candidateCEs.addLast(q);
//        }
//
//        if (queryStats != null)
//        	queryStats.processingCounterExample();
//
//        if (isGuardRefinement(transition)) {
//        	addPrefix(transition);
//        }
//        else {
//        	expand(transition);
//        }
//
//        while (!dt.checkIOSuffixes());
//
//        boolean consistent = false;
//        while (!consistent) {
//
//            consistent = checkLocationConsistency();
//
//        	if (!checkRegisterClosedness()) {
//        		consistent = false;
//        	}
//
//        	if (!checkGuardConsistency()) {
//        		consistent = false;
//        	}
//
//        	if (!checkRegisterConsistency()) {
//        		consistent = false;
//        	}
//        }
//
//        if (noShortPrefixes() && !dt.isMissingParameter()) {
//        	buildNewHypothesis();
//        }
//        //System.out.println(hyp);
//        return true;
//    }
//
//    private boolean isGuardRefinement(Word<PSymbolInstance> word) {
//    	return dt.getLeaf(word) == null;
//    }
//
//    private void addPrefix(Word<PSymbolInstance> u) {
//    	dt.sift(u, true);
//    }
//
//    private void expand(Word<PSymbolInstance> u) {
//    	DTLeaf l = dt.getLeaf(u);
//    	assert l != null;
//    	l.elevatePrefix(dt, u, hyp, sdtLogicOracle);
//    }
//
//    private boolean checkLocationConsistency() {
//
//    	for (DTLeaf l : dt.getLeaves()) {
//    		MappedPrefix mp = l.getPrimePrefix();
//    		Iterator<MappedPrefix> it = l.getShortPrefixes().iterator();
//    		while (it.hasNext()) {
//    			ShortPrefix sp = (ShortPrefix)it.next();
//    			SymbolicSuffix suffix = null;
//    			for (ParameterizedSymbol psi : dt.getInputs()) {
//    				Branching access_b = l.getBranching(psi);
//    				Branching prefix_b = sp.getBranching(psi);
//    				for (Word<PSymbolInstance> ws : prefix_b.getBranches().keySet()) {
//    					Word<PSymbolInstance> wa = DTLeaf.branchWithSameGuard(ws, prefix_b, l.getRemapping(sp), access_b, sdtLogicOracle);
//                        //System.out.println("wa: " + wa + ", ws: " + ws);
//    					DTLeaf la = dt.getLeaf(wa);
//    					DTLeaf ls = dt.getLeaf(ws);
//    					if (la != ls) {
//    						SymbolicSuffix v = distinguishingSuffix(wa, la, ws, ls);
//    						if (suffix == null || suffix.length() > v.length()) {
//    							suffix = v;
//    						}
//                            assert suffix != null;
//    					}
//    				}
//    			}
//    			if (suffix != null) {
//    				dt.split(sp.getPrefix(), suffix, l);
//    				return false;
//    			}
//    		}
//    	}
//    	return true;
//    }
//
//    private boolean checkRegisterClosedness() {
//    	return dt.checkVariableConsistency(suffixBuilder);
//    }
//
//    private boolean checkRegisterConsistency() {
//    	return dt.checkRegisterConsistency(suffixBuilder);
//    }
//
//    private boolean checkGuardConsistency() {
//    	for (DTLeaf dest_c : dt.getLeaves()) {
//    		Collection<Word<PSymbolInstance>> words = new LinkedHashSet<>();
//    		words.add(dest_c.getAccessSequence());
//    		words.addAll(dest_c.getPrefixes().getWords());
//    		words.addAll(dest_c.getShortPrefixes().getWords());
//    		for (Word<PSymbolInstance> dest_id : words) {
//    			if (dest_id.length() == 0) {
//    				continue;
//    			}
//    			Word<PSymbolInstance> src_id = dest_id.prefix(dest_id.length() - 1);
//    			DTLeaf src_c = dt.getLeaf(src_id);
//
//    			Branching hypBranching = null;
//    			if (src_c.getAccessSequence().equals(src_id)) {
//    				hypBranching = src_c.getBranching(dest_id.lastSymbol().getBaseSymbol());
//    			} else {
//    				ShortPrefix sp = (ShortPrefix) src_c.getShortPrefixes().get(src_id);
//    				assert sp != null;
//    				hypBranching = sp.getBranching(dest_id.lastSymbol().getBaseSymbol());
//    			}
//    			if (hypBranching.getBranches().get(dest_id) != null) {
//    				// word already in branching, no guard refinement needed
//    				continue;
//    			}
//    			Word<PSymbolInstance> hyp_id = branchWithSameGuard(dest_c.getPrefix(dest_id), hypBranching);
//
//    			SymbolicSuffix suffix = null;
//
//    			DTLeaf hyp_c = dt.getLeaf(hyp_id);
//    			if (hyp_c != dest_c) {
//    				suffix = distinguishingSuffix(hyp_id, hyp_c, dest_id, dest_c);
//    			} else {
//    				List<SymbolicSuffix> suffixes = new LinkedList<>();
//    				Map<SymbolicSuffix, SDT> dest_sdts = new LinkedHashMap<>();
//    				Map<SymbolicSuffix, SDT> hyp_sdts = new LinkedHashMap<>();
//    				for (Map.Entry<SymbolicSuffix, SDT> e : dest_c.getPrefix(dest_id).getTQRs().entrySet()) {
//    					SymbolicSuffix s = e.getKey();
//    					SDT dest_sdt = e.getValue();
//    					SDT hyp_sdt = hyp_c.getPrefix(hyp_id).getTQRs().get(s);
//    					assert hyp_sdt != null;
//
//    					if (!SDT.equivalentUnderId(dest_sdt.toRegisterSDT(dest_id, consts), hyp_sdt.toRegisterSDT(hyp_id, consts))) {
//    						suffixes.add(s);
//    						dest_sdts.put(s, dest_sdt);
//    						hyp_sdts.put(s, hyp_sdt);
//    					}
//    				}
//
//    				if (suffixes.isEmpty()) {
//    					continue;
//    				}
//
//    				Collections.sort(suffixes, (sa, sb) -> sa.length() > sb.length() ? 1 :
//    						sa.length() < sb.length() ? -1 : 0);
//
//    				for (SymbolicSuffix s : suffixes) {
//    					SymbolicSuffix testSuffix;
//    					SDT hyp_sdt = hyp_sdts.get(s);
//
//    					if (suffixBuilder != null) {
//    						SDT dest_sdt = dest_sdts.get(s);
//    						DataValue[] regs = remappedRegisters(dest_sdt, hyp_sdt);
//    						testSuffix = suffixBuilder.extendSuffix(dest_id, dest_sdt, s, regs);
//    					} else {
//    						testSuffix = new SymbolicSuffix(src_id, dest_id.suffix(1), restrictionBuilder);
//    						testSuffix = testSuffix.concat(s);
//    					}
//
//    					SDT testSDT = sulOracle.treeQuery(src_id, testSuffix);
//    					Branching testBranching = sulOracle.updateBranching(src_id, dest_id.lastSymbol().getBaseSymbol(), hypBranching, testSDT);
//    					if (testBranching.getBranches().get(dest_id) != null) {
//    						suffix = testSuffix;
//    						break;
//    					}
//    				}
//    			}
//
//    			if (suffix != null) {
//    				dt.addSuffix(suffix, src_c);
//    				return false;
//    			}
//    		}
//    	}
//
//    	return true;
//    }
//
//    private SymbolicSuffix distinguishingSuffix(Word<PSymbolInstance> wa, DTLeaf ca, Word<PSymbolInstance> wb, DTLeaf cb) {
//    	Word<PSymbolInstance> sa = wa.suffix(1);
//    	Word<PSymbolInstance> sb = wb.suffix(1);
//
//    	assert sa.getSymbol(0).getBaseSymbol().equals(sb.getSymbol(0).getBaseSymbol());
//
//    	SymbolicSuffix v = dt.findLCA(ca, cb).getSuffix();
//
//    	Word<PSymbolInstance> prefixA = wa.prefix(wa.length() - 1);
//    	Word<PSymbolInstance> prefixB = wb.prefix(wb.length() - 1);
//
//    	SDT tqrA = ca.getTQR(wa, v);
//        SDT tqrB = cb.getTQR(wb, v);
//
//    	assert tqrA != null && tqrB != null;
//
//        SDT sdtA = tqrA;
//        SDT sdtB = tqrB;
//
//        if (suffixBuilder != null && solver != null) {
//    		//return suffixBuilder.extendDistinguishingSuffix(wa, sdtA, wb, sdtB, v);
//            SymbolicSuffix suffix = suffixBuilder.distinguishingSuffixFromSDTs(wa,  sdtA, wb,  sdtB, v.getActions(), solver);
//           	return suffix;
//        }
//
//    	SymbolicSuffix alpha_a = new SymbolicSuffix(prefixA, sa, restrictionBuilder);
//    	SymbolicSuffix alpha_b = new SymbolicSuffix(prefixB, sb, restrictionBuilder);
//    	return alpha_a.getFreeValues().size() > alpha_b.getFreeValues().size()
//    		   ? alpha_a.concat(v)
//    		   : alpha_b.concat(v);
//    }
//
//    private boolean noShortPrefixes() {
//    	for (DTLeaf l : dt.getLeaves()) {
//    		if (!l.getShortPrefixes().isEmpty()) {
//                return false;
//            }
//    	}
//    	return true;
//    }
//
//    public void setUseOldAnalyzer(boolean useOldAnalyzer) {
//        this.useOldAnalyzer = useOldAnalyzer;
//    }
//
//    private Word<PSymbolInstance> branchWithSameGuard(MappedPrefix mp, Branching branching) {
//    	Word<PSymbolInstance> dw = mp.getPrefix();
//
//    	return branching.transformPrefix(dw);
//    }
//
//    private DataValue[] remappedRegisters(SDT sdt1, SDT sdt2) {
//    	Bijection<DataValue> bijection = SDT.equivalentUnderBijection(sdt1, sdt2);
//    	assert bijection != null;
//    	List<DataValue> vals = new LinkedList<>();
//    	for (Map.Entry<DataValue, DataValue> e : bijection.entrySet()) {
//    		if (!e.getKey().equals(e.getValue())) {
//    			vals.add(e.getKey());
//    			vals.add(e.getValue());
//    		}
//    	}
//    	return vals.toArray(new DataValue[vals.size()]);
//    }
//
//    @Override
//    public Hypothesis getHypothesis() {
//        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
//        components.putAll(dt.getComponents());
//        AutomatonBuilder ab;
//        if (ioMode) {
//            ab = new IOAutomatonBuilder(components, consts);
//        } else {
//            ab = new AutomatonBuilder(components, consts);
//        }
//        return ab.toRegisterAutomaton();
//    }
//
//    public DT getDT() {
//        return dt;
//    }
//
//    public DTHyp getDTHyp() {
//        return hyp;
//    }
//
//    public Map<Word<PSymbolInstance>, LocationComponent> getComponents() {
//        return dt.getComponents();
//    }
//
//    @Override
//    public void setStatisticCounter(QueryStatistics queryStats) {
//    	this.queryStats = queryStats;
//    }
//
//    @Override
//    public QueryStatistics getQueryStatistics() {
//    	return queryStats;
//    }
//
//    public void setSolver(ConstraintSolver solver) {
//    	this.solver = solver;
//    }
//
//    @Override
//    public RaLearningAlgorithmName getName() {
//        return RaLearningAlgorithmName.RALAMBDA;
//    }
//
//    @Override
//    public String toString() {
//        return dt.toString();
//    }
//}
