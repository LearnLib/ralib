package de.learnlib.ralib.learning.rattt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.ceanalysis.PrefixFinder;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.dt.DT;
import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.learning.AutomatonBuilder;
import de.learnlib.ralib.learning.CounterexampleAnalysis;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.IOAutomatonBuilder;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class RaTTT {

    public static final Word<PSymbolInstance> EMPTY_PREFIX = Word.epsilon();
    
    public static final SymbolicSuffix EMPTY_SUFFIX = new SymbolicSuffix(
            Word.<PSymbolInstance>epsilon(), Word.<PSymbolInstance>epsilon());
    
    private final DT dt;
    
    private final Constants consts;

    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples = 
            new LinkedList<>();
        
    private Hypothesis hyp = null;
    
    private final TreeOracle sulOracle;
    
    private final SDTLogicOracle sdtLogicOracle;
    
    private final TreeOracleFactory hypOracleFactory;
    
    private final boolean ioMode;
    
    private static final LearnLogger log = LearnLogger.getLogger(RaTTT.class);
    
    private final boolean useOldAnalyzer;
    
    private final Deque<Word<PSymbolInstance>> shortPrefixes = new ArrayDeque<Word<PSymbolInstance>>();

    public RaTTT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts, boolean ioMode,
            ParameterizedSymbol ... inputs) {
    	
    	this.ioMode = ioMode;
    	this.dt = new DT(oracle, ioMode, consts, inputs);
    	this.consts = consts;
    	this.sulOracle = oracle;
    	this.sdtLogicOracle = sdtLogicOracle;
    	this.hypOracleFactory = hypOracleFactory;
    	this.useOldAnalyzer = false;
    	
    	this.dt.initialize();
    }
    
    public RaTTT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts, boolean ioMode, boolean useOldAnalyzer,
            ParameterizedSymbol ... inputs) {
    	
    	this.ioMode = ioMode;
    	this.dt = new DT(oracle, ioMode, consts, inputs);
    	this.consts = consts;
    	this.sulOracle = oracle;
    	this.sdtLogicOracle = sdtLogicOracle;
    	this.hypOracleFactory = hypOracleFactory;
    	this.useOldAnalyzer = useOldAnalyzer;
    	
    	this.dt.initialize();
    }
    
    public RaTTT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts,
            ParameterizedSymbol ... inputs) {
    	this(oracle, hypOracleFactory, sdtLogicOracle, consts, false, false, inputs);
    }
    
    public void addCounterexample(DefaultQuery<PSymbolInstance, Boolean> ce) {
        log.logEvent("adding counterexample: " + ce);
        counterexamples.add(ce);
    }
    
    public void learn() {
        if (hyp != null) {
            analyzeCounterExample();
        }
        
        do {
            
        	dt.checkVariableConsistency();
        	
            Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
            components.putAll(dt.getComponents());
            AutomatonBuilder ab;
            if (useOldAnalyzer)
            	ab = new AutomatonBuilder(components, consts);
            else
            	ab = new AutomatonBuilder(components, consts, dt);
            hyp = ab.toRegisterAutomaton();        
            
            //FIXME: the default logging appender cannot log models and data structures
            //System.out.println(hyp.toString());
            log.logModel(hyp);
            
        } while (analyzeCounterExample());
         
    }

    private boolean analyzeCounterExample() {
    	if (useOldAnalyzer)
    		return analyzeCounterExampleOld();
        log.logPhase("Analyzing Counterexample");        
        if (counterexamples.isEmpty()) {
            return false;
        }
        
        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);
        
        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        components.putAll(dt.getComponents());
        PrefixFinder pf = new PrefixFinder(
                sulOracle, hypOracle, hyp, sdtLogicOracle, components, consts);
        
        DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.peek();    
        
        // check if ce still is a counterexample ...
        boolean hypce = hyp.accepts(ce.getInput());
        boolean sulce = ce.getOutput();
        if (hypce == sulce) {
            log.logEvent("word is not a counterexample: " + ce + " - " + sulce);           
            counterexamples.poll();
            return false;
        }
        
        // find short prefixes until getting dt refinement
        boolean refinement = false;
        do {
	        Word<PSymbolInstance> prefix = pf.analyzeCounterexample(ce.getInput());
	        DTLeaf leaf = dt.getLeaf(prefix);
	        
	        if (leaf == null)
	        	refinement = addGuardRefinement(prefix);
	        else
	        	refinement = addNewLocation(prefix, leaf);
        } while(!refinement);
        
    	return true;
    }
    
    private boolean addGuardRefinement(Word<PSymbolInstance> word) {

    	DTLeaf leaf = dt.sift(word, false);
    	
    	// does this guard lead to a dt refinement?
    	if (dt.isRefinement(word)) {
        	assert word.length() > 0;
        	Word<PSymbolInstance> sub = word.prefix(word.length()-1);
        	DTLeaf subLeaf = dt.getLeaf(sub);
        	assert subLeaf != null;

    		SymbolicSuffix suff1 = new SymbolicSuffix(sub, word.suffix(1));
    		SymbolicSuffix suff2 = dt.findLCA(leaf, subLeaf).getSuffix();
    		SymbolicSuffix suffix = suff1.concat(suff2);
			
        	if (!shortPrefixes.isEmpty()) {
        		// if we have guard refinement, and stack is not empty, 
        		// subword of prefix must be latest elevated prefix
        		assert shortPrefixes.peek().equals(sub);
        		shortPrefixes.pop();
        		
        		assert subLeaf.getShortPrefixes().contains(sub);	// sub should be a short prefix
        		
        		// sub is a short prefix, so it must be a new location
        		dt.split(sub, suffix, subLeaf);
        	}
        	else {
        		dt.addSuffix(suffix, subLeaf); 
        	}
        	processShortPrefixes(sub, suffix);
        	return true;
    	}
    	
    	// no refinement, so must be a new location
    	shortPrefixes.push(word);
    	return false;

    }
    
    private boolean addNewLocation(Word<PSymbolInstance> prefix, DTLeaf leaf) {

    	Pair<Word<PSymbolInstance>, Word<PSymbolInstance>> divergance = 
    			leaf.elevatePrefix(getDT(), prefix, sulOracle);
        if (divergance == null) {
        	shortPrefixes.push(prefix);	// no refinement of dt
        }
        else {
        	// elevating and expanding prefix lead to refinement of dt
        	Word<PSymbolInstance> refinedTarget = divergance.getKey();
        	Word<PSymbolInstance> target = divergance.getValue();
        	DTLeaf targetLeaf = dt.getLeaf(refinedTarget);
        	
        	SymbolicSuffix suff1 = dt.findLCA(dt.getLeaf(target), targetLeaf).getSuffix();
        	SymbolicSuffix suff2 = new SymbolicSuffix(
        			refinedTarget.prefix(refinedTarget.length()-1),
        			refinedTarget.suffix(1));
        	SymbolicSuffix suffix = suff1.concat(suff2);
        		
        	dt.split(prefix, suffix, leaf);
        		
        	processShortPrefixes(prefix, suffix);
        	return true;
        }
        return false;
    }
    
    private void processShortPrefixes(Word<PSymbolInstance> prevPrefix, SymbolicSuffix prevSuffix) {
    	while(!shortPrefixes.isEmpty()) {
    		Word<PSymbolInstance> prefix = shortPrefixes.poll();
    		
    		DTLeaf leaf = dt.getLeaf(prefix);
    		assert leaf != null;
    		assert leaf.getShortPrefixes().contains(prefix);

    		Word<PSymbolInstance> discriminator = prevPrefix.suffix(1);
    		assert prevPrefix.equals(prefix.append(discriminator.firstSymbol()));
    		
    		SymbolicSuffix suff1 = new SymbolicSuffix(prefix, discriminator);
    		SymbolicSuffix suffix = suff1.concat(prevSuffix);
    		
    		dt.split(prefix, suffix, leaf);
    		prevPrefix = prefix;
    		prevSuffix = suffix;
    	}
    }
    
    private boolean analyzeCounterExampleOld() {
        log.logPhase("Analyzing Counterexample");        
        if (counterexamples.isEmpty()) {
            return false;
        }
        
        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);
        
        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        components.putAll(dt.getComponents());
        CounterexampleAnalysis analysis = new CounterexampleAnalysis(
                sulOracle, hypOracle, hyp, sdtLogicOracle, components, consts);
        
        DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.peek();    
        
        // check if ce still is a counterexample ...
        boolean hypce = hyp.accepts(ce.getInput());
        boolean sulce = ce.getOutput();
        if (hypce == sulce) {
            log.logEvent("word is not a counterexample: " + ce + " - " + sulce);           
            counterexamples.poll();
            return false;
        }
        
        //System.out.println("CE ANALYSIS: " + ce + " ; S:" + sulce + " ; H:" + hypce);
        
        CEAnalysisResult res = analysis.analyzeCounterexample(ce.getInput());
        Word<PSymbolInstance> prefix = res.getPrefix();
        DTLeaf leaf = dt.getLeaf(prefix);
        if (leaf != null && isGuardRefinement(leaf, prefix, ce.getInput().prefix(prefix.length())))
        	dt.addSuffix(res.getSuffix(), leaf);
        else {
        	if (leaf == null)
        		leaf = dt.sift(prefix, true);
        	leaf.elevatePrefix(dt, prefix, sulOracle);
        	dt.split(prefix, res.getSuffix(), leaf);
        }
        return true;
    }

    private boolean isGuardRefinement(DTLeaf leaf, Word<PSymbolInstance> prefix, Word<PSymbolInstance> extendedPrefix) {
    	ParameterizedSymbol ps = extendedPrefix.getSymbol(extendedPrefix.length()-1).getBaseSymbol();
    	Branching b = leaf.getBranching(ps);
    	for (Word<PSymbolInstance> p : b.getBranches().keySet()) {
    		if (p.equals(extendedPrefix))
    			return false;
    	}
    	return true;
    }
    
    public Hypothesis getHypothesis() {
    	Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
    	components.putAll(dt.getComponents());
        AutomatonBuilder ab = new AutomatonBuilder(components, consts, this.dt);
        if (ioMode) {
            ab = new IOAutomatonBuilder(components, consts);
        }
        return ab.toRegisterAutomaton();   
    }
    
    public DT getDT() {
    	return this.dt;
    }
    
    public Map<Word<PSymbolInstance>, LocationComponent> getComponents() {
    	return dt.getComponents();
    }
}

