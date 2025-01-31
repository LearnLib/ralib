package de.learnlib.ralib.learning.ralambda;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.ceanalysis.PrefixFinder;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.dt.DT;
import de.learnlib.ralib.dt.DTHyp;
import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.dt.MappedPrefix;
import de.learnlib.ralib.dt.ShortPrefix;
import de.learnlib.ralib.learning.AutomatonBuilder;
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
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

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
    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;
    private ConstraintSolver solver = null;

    private QueryStatistics queryStats = null;

    private final boolean ioMode;

    private static final Logger LOGGER = LoggerFactory.getLogger(RaLambda.class);

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
        this.consts = consts;
        this.sulOracle = oracle;
        this.sdtLogicOracle = sdtLogicOracle;
        this.hypOracleFactory = hypOracleFactory;
        this.useOldAnalyzer = useOldAnalyzer;
        if (oracle instanceof MultiTheoryTreeOracle) {
        	this.restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, ((MultiTheoryTreeOracle)oracle).getTeachers());
        } else {
        	this.restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts);
        }
        this.suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrictionBuilder);
        this.dt = new DT(oracle, ioMode, consts, inputs);
        this.dt.initialize();
    }

    public RaLambda(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            ParameterizedSymbol... inputs) {
        this(oracle, hypOracleFactory, sdtLogicOracle, consts, false, false, inputs);
    }

    @Override
    public void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce) {
        LOGGER.info(Category.EVENT, "adding counterexample: {}", ce);
        counterexamples.add(ce);
    }

    @Override
    public void learn() {

        if (hyp == null) {
            buildNewHypothesis();
        }

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
                //prefixFinder.setComponents(components);
        	prefixFinder.setHypothesisTreeOracle(hypOracleFactory.createTreeOracle(hyp));
        }
    }

    private boolean analyzeCounterExample() {
//        if (useOldAnalyzer)
//            return analyzeCounterExampleOld();
        LOGGER.info(Category.PHASE, "Analyzing Counterexample");

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
            //Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
            //components.putAll(dt.getComponents());
            prefixFinder = new PrefixFinder(sulOracle, hypOracle, hyp, sdtLogicOracle, consts);
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

        while (!dt.checkIOSuffixes());

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

        if (noShortPrefixes() && !dt.isMissingParameter()) {
        	buildNewHypothesis();
        }

        return true;
    }

    private boolean isGuardRefinement(Word<PSymbolInstance> word, CEAnalysisResult ceaResult) {
    	return dt.getLeaf(word) == null;
//    	if (dt.getLeaf(word) != null)
//    		return false;
//
//    	Word<PSymbolInstance> src_id = word.prefix(word.size() - 1);
//    	DTLeaf src_c = dt.getLeaf(src_id);
//        Branching hypBranching = null;
//        PIV piv = null;
//        if (src_c.getAccessSequence().equals(src_id)) {
//            hypBranching = src_c.getBranching(word.lastSymbol().getBaseSymbol());
//            piv = src_c.getPrimePrefix().getParsInVars();
//        } else {
//            ShortPrefix sp = (ShortPrefix) src_c.getShortPrefixes().get(src_id);
//            hypBranching = sp.getBranching(word.lastSymbol().getBaseSymbol());
//            piv = sp.getParsInVars();
//        }
//
//        if (hypBranching.getBranches().keySet().contains(word))
//        	return false;
//
//        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);
//        TreeQueryResult tqrHyp = hypOracle.treeQuery(word, ceaResult.getSuffix());
//        TreeQueryResult tqrSul = ceaResult.getTreeQueryResult();
//        if (tqrSul == null) {
//        	tqrSul = sulOracle.treeQuery(word, ceaResult.getSuffix());
//        }
//
//        if (tqrHyp.getSdt().isEquivalent(tqrSul.getSdt(), tqrSul.getPiv())) {
//        	guardPrefixes.put(word, false);
//        	return true;
//        }
//
//        TransitionGuard guard = AutomatonBuilder.findMatchingGuard(word, piv, hypBranching.getBranches(), consts);
//        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : hypBranching.getBranches().entrySet()) {
//        	boolean eq = sdtLogicOracle.areEquivalent(e.getValue(), piv, guard, piv, new Mapping<SymbolicDataValue, DataValue<?>>());
//        	if (eq && !e.getKey().equals(word)) {
//        		guardPrefixes.put(word, true);
//        		return true;
//        	}
//        }
//
//    	return false;
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
    			SymbolicSuffix suffix = null;
    			for (ParameterizedSymbol psi : dt.getInputs()) {
    				Branching access_b = l.getBranching(psi);
    				Branching prefix_b = sp.getBranching(psi);
    				for (Word<PSymbolInstance> ws : prefix_b.getBranches().keySet()) {
    					Word<PSymbolInstance> wa = DTLeaf.branchWithSameGuard(ws, prefix_b, sp.getParsInVars(), access_b, mp.getParsInVars(), sdtLogicOracle);

    					DTLeaf la = dt.getLeaf(wa);
    					DTLeaf ls = dt.getLeaf(ws);
    					if (la != ls) {
    						SymbolicSuffix v = distinguishingSuffix(wa, la, ws, ls);
    						if (suffix == null || suffix.length() > v.length()) {
    							suffix = v;
    						}
    					}
    				}
    			}
    			if (suffix != null) {
    				dt.split(sp.getPrefix(), suffix, l);
    				return false;
    			}
    		}
    	}
    	return true;
    }

    private boolean checkRegisterClosedness() {
    	return dt.checkVariableConsistency(suffixBuilder);
    }

    private boolean checkRegisterConsistency() {
    	return dt.checkRegisterConsistency(suffixBuilder);
    }

    private boolean checkGuardConsistency() {

    	for (DTLeaf dest_c : dt.getLeaves()) {
    		Collection<Word<PSymbolInstance>> words = new LinkedHashSet<>();
    		words.add(dest_c.getAccessSequence());
    		words.addAll(dest_c.getPrefixes().getWords());
    		words.addAll(dest_c.getShortPrefixes().getWords());
    		for (Word<PSymbolInstance> dest_id : words) {
    			if (dest_id.length() == 0) {
    				continue;
    			}
    			Word<PSymbolInstance> src_id = dest_id.prefix(dest_id.length() - 1);  // u
    			DTLeaf src_c = dt.getLeaf(src_id);

    			// find u.alpha(d_u^g)
    			Branching hypBranching = null;
    			if (src_c.getAccessSequence().equals(src_id)) {
    				hypBranching = src_c.getBranching(dest_id.lastSymbol().getBaseSymbol());
    			} else {
    				ShortPrefix sp = (ShortPrefix) src_c.getShortPrefixes().get(src_id);
    				assert sp != null;
    				hypBranching = sp.getBranching(dest_id.lastSymbol().getBaseSymbol());
    			}
    			Word<PSymbolInstance> hyp_id = branchWithSameGuard(dest_c.getPrefix(dest_id), hypBranching);  // u.alpha(d_u^g)

    			SymbolicSuffix suffix = null;

    			// transition consistency (a)
    			DTLeaf hyp_c = dt.getLeaf(hyp_id);
    			if (hyp_c != dest_c) {
    				suffix = distinguishingSuffix(hyp_id, hyp_c, dest_id, dest_c);
    			// transition consistency (b)
    			} else {
    				// find suffixes revealing transition inconsistency
    				List<SymbolicSuffix> suffixes = new ArrayList<>();
    				Map<SymbolicSuffix, TreeQueryResult> dest_tqrs = new LinkedHashMap<>();
    				Map<SymbolicSuffix, TreeQueryResult> hyp_tqrs = new LinkedHashMap<>();
    				for (Map.Entry<SymbolicSuffix, TreeQueryResult> entry : dest_c.getPrefix(dest_id).getTQRs().entrySet()) {
    					SymbolicSuffix s = entry.getKey();
    					TreeQueryResult dest_tqr = entry.getValue();
        				TreeQueryResult hyp_tqr = hyp_c.getPrefix(hyp_id).getTQRs().get(s);
    					assert hyp_tqr != null;

    					if (!dest_tqr.getSdt().isEquivalentUnderId(hyp_tqr.getSdt(), dest_tqr.getPiv(), hyp_tqr.getPiv())) {
    						suffixes.add(s);
    						dest_tqrs.put(s, dest_tqr);
    						hyp_tqrs.put(s, hyp_tqr);
    					}
    				}

    				if (suffixes.isEmpty()) {
    					continue;
    				}

    				// sort revealing suffixes by length
    				Collections.sort(suffixes, (sa, sb) -> {
    					SymbolicSuffix suffixA = (SymbolicSuffix) sa;
    					SymbolicSuffix suffixB = (SymbolicSuffix) sb;
    					return suffixA.length() > suffixB.length() ? 1 :
    						suffixA.length() < suffixB.length() ? -1 : 0;
    				});

    				// choose suffix v such that alpha.v also reveals inconsistency
    				for (SymbolicSuffix s : suffixes) {
    					SymbolicSuffix testSuffix;
    					TreeQueryResult hyp_tqr = hyp_tqrs.get(s);

    					// construct optimized suffix
    					if (suffixBuilder != null && hyp_tqr.getSdt() instanceof SDT) {
    						TreeQueryResult dest_tqr = dest_tqrs.get(s);
    						Register[] differentlyMapped = differentlyMappedRegisters(dest_tqr.getPiv(), hyp_tqr.getPiv());
    						testSuffix = suffixBuilder.extendSuffix(dest_id, (SDT)dest_tqr.getSdt(), dest_tqr.getPiv(), s, differentlyMapped);
    					} else {
    						testSuffix = new SymbolicSuffix(src_id, dest_id.suffix(1), restrictionBuilder);
    						testSuffix = testSuffix.concat(s);
    					}

    					// check that testSuffix reveals inconsistency
    					TreeQueryResult testTqr = sulOracle.treeQuery(src_id, testSuffix);
    					Branching testBranching = sulOracle.updateBranching(src_id, dest_id.lastSymbol().getBaseSymbol(), hypBranching, testTqr.getPiv(), testTqr.getSdt());
    					if (testBranching.getBranches().keySet().contains(dest_id)) {
    						suffix = testSuffix;
    						break;
    					}
    				}
    			}

    			if (suffix != null) {
    				dt.addSuffix(suffix, src_c);
    				return false;
    			}
    		}
    	}

    	return true;

//    	Map<Word<PSymbolInstance>, Boolean> toReuse = new LinkedHashMap<Word<PSymbolInstance>, Boolean>();
//    	for (Map.Entry<Word<PSymbolInstance>, Boolean> entry : guardPrefixes.entrySet()) {
//    		Word<PSymbolInstance> word = entry.getKey();					// u.alpha(d)
//    		Word<PSymbolInstance> src_id = word.prefix(word.size() - 1);	// u
//    		DTLeaf src_c = dt.getLeaf(src_id);
//    		DTLeaf dest_c = dt.getLeaf(word);
//    		Branching hypBranching = null;
//
//            if (src_c.getAccessSequence().equals(src_id)) {
//                hypBranching = src_c.getBranching(word.lastSymbol().getBaseSymbol());
//            } else {
//                ShortPrefix sp = (ShortPrefix) src_c.getShortPrefixes().get(src_id);
//                hypBranching = sp.getBranching(word.lastSymbol().getBaseSymbol());
//            }
//            if (hypBranching.getBranches().keySet().contains(word)) {
//            	continue;
//            }
//
//            // compute u.alpha(d_u^g)
//            Word<PSymbolInstance> branch = branchWithSameGuard(dest_c.getPrefix(word), hypBranching);
//            DTLeaf branchLeaf = dt.getLeaf(branch);
//
//            SymbolicSuffix suffix = null;
//
//            // transition consistency (a)
//            if (branchLeaf != dest_c) {
//            	suffix = distinguishingSuffix(branch, branchLeaf, word, dest_c);
//            } else {
//            	if (!entry.getValue()) {
//
//
//		            MappedPrefix mp = dest_c.getPrefix(word);
//		            Map<SymbolicSuffix, TreeQueryResult> branchTQRs = branchLeaf.getPrefix(branch).getTQRs();
//		            for (Map.Entry<SymbolicSuffix, TreeQueryResult> e : mp.getTQRs().entrySet()) {
//		            	TreeQueryResult tqr = e.getValue();
//		            	SymbolicSuffix s = e.getKey();
//
//		            	TreeQueryResult otherTQR = branchTQRs.get(s);
//                        //todo: not sure why this check was not here yet? It happens sometimes ...
//                        if (branchTQRs.get(s) == null) {
//                            continue;
//                        }
//
//		            	if (tqr.getSdt().isEquivalent(branchTQRs.get(s).getSdt(), tqr.getPiv())) {
//		            		if (!tqr.getPiv().equals(otherTQR.getPiv())) {
//			            		if (suffix == null || suffix.length() > s.length()+1) {
//			            			SymbolicSuffix testSuffix;
//			            			if (suffixBuilder != null && tqr.getSdt() instanceof SDT) {
//			            				Register[] differentlyMapped = differentlyMappedRegisters(tqr.getPiv(), otherTQR.getPiv());
//			            				testSuffix = suffixBuilder.extendSuffix(word, (SDT)tqr.getSdt(), tqr.getPiv(), s, differentlyMapped);
//			            			} else {
//			            				testSuffix = new SymbolicSuffix(word.prefix(word.length()-1), word.suffix(1), restrictionBuilder);
//			            				testSuffix = testSuffix.concat(s);
//			            			}
//			            			TreeQueryResult testTQR = sulOracle.treeQuery(src_id, testSuffix);
//			            			Branching testBranching = sulOracle.updateBranching(src_id, word.lastSymbol().getBaseSymbol(), hypBranching, testTQR.getPiv(), testTQR.getSdt());
//			            			if (testBranching.getBranches().keySet().contains(word)) {
//			            				suffix = testSuffix;
//			            			}
//			            		}
//		            		}
//		            	}
//		            }
//	            }
//            }
//
//            if (suffix == null) {
//            	toReuse.put(word, guardPrefixes.get(word));
//            	continue;
//            }
//
//            dt.addSuffix(suffix, src_c);
//            return false;
//    	}
//
//    	guardPrefixes.clear();
//    	guardPrefixes.putAll(toReuse);
//    	return true;
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

    	assert tqrA != null && tqrB != null;

        SymbolicDecisionTree sdtA = tqrA.getSdt();
        SymbolicDecisionTree sdtB = tqrB.getSdt();

        if (suffixBuilder != null && solver != null && sdtA instanceof SDT && sdtB instanceof SDT) {
//    		return suffixBuilder.extendDistinguishingSuffix(wa, (SDT)sdtA, tqrA.getPiv(), wb, (SDT)sdtB, tqrB.getPiv(), v);
          	SymbolicSuffix suffix = suffixBuilder.distinguishingSuffixFromSDTs(wa, (SDT) sdtA, tqrA.getPiv(), wb, (SDT) sdtB, tqrB.getPiv(), v.getActions(), solver);
           	return suffix;
        }

    	SymbolicSuffix alpha_a = new SymbolicSuffix(prefixA, sa, restrictionBuilder);
    	SymbolicSuffix alpha_b = new SymbolicSuffix(prefixB, sb, restrictionBuilder);
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

    private Word<PSymbolInstance> branchWithSameGuard(MappedPrefix mp, Branching branching) {
    	Word<PSymbolInstance> dw = mp.getPrefix();

    	return branching.transformPrefix(dw);
    }

    private Register[] differentlyMappedRegisters(PIV piv1, PIV piv2) {
    	Set<Register> differentlyMapped = new LinkedHashSet<>();
    	for (Map.Entry<Parameter, Register> e1 : piv1.entrySet()) {
    		Parameter p1 = e1.getKey();
    		Register r1 = e1.getValue();
    		for (Map.Entry<Parameter, Register> e2 : piv2.entrySet()) {
    			Parameter p2 = e2.getKey();
    			Register r2 = e2.getValue();
    			if (r1.equals(r2) && !p1.equals(p2)) {
    				differentlyMapped.add(r1);
    			}
    		}
    	}
    	Register[] ret = new Register[differentlyMapped.size()];
    	return differentlyMapped.toArray(ret);
    }

//    private boolean analyzeCounterExampleOld() {
//        log.logPhase("Analyzing Counterexample");
//        if (counterexamples.isEmpty()) {
//            return false;
//        }
//
//        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);
//
//        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
//        components.putAll(dt.getComponents());
//        CounterexampleAnalysis analysis = new CounterexampleAnalysis(sulOracle, hypOracle, hyp, sdtLogicOracle,
//                components, consts);
//
//        DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.peek();
//
//        // check if ce still is a counterexample ...
//        boolean hypce = hyp.accepts(ce.getInput());
//        boolean sulce = ce.getOutput();
//        if (hypce == sulce) {
//            log.logEvent("word is not a counterexample: " + ce + " - " + sulce);
//            counterexamples.poll();
//            return false;
//        }
//
//        if (queryStats != null)
//        	queryStats.analyzingCounterExample();
//
//        CEAnalysisResult res = analysis.analyzeCounterexample(ce.getInput());
//
//        if (queryStats != null) {
//        	queryStats.processingCounterExample();
//        	queryStats.analyzeCE(ce.getInput());
//        }
//
//        Word<PSymbolInstance> accSeq = hyp.transformAccessSequence(res.getPrefix());
//        DTLeaf leaf = dt.getLeaf(accSeq);
//        dt.addSuffix(res.getSuffix(), leaf);
//        while(!dt.checkVariableConsistency(suffixBuilder));
//        return true;
//    }

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

    public DTHyp getDTHyp() {
        return hyp;
    }

    public Map<Word<PSymbolInstance>, LocationComponent> getComponents() {
        return dt.getComponents();
    }

    @Override
    public void setStatisticCounter(QueryStatistics queryStats) {
    	this.queryStats = queryStats;
    }

    @Override
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
        return dt.toString();
    }
}
