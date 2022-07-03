package de.learnlib.ralib.learning.rattt;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
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

    public RaTTT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts, boolean ioMode,
            ParameterizedSymbol ... inputs) {
    	
    	this.ioMode = ioMode;
    	this.dt = new DT(oracle, ioMode, consts, inputs);
    	this.consts = consts;
    	this.sulOracle = oracle;
    	this.sdtLogicOracle = sdtLogicOracle;
    	this.hypOracleFactory = hypOracleFactory;
    	
    	this.dt.initialize();
    }
    
    public RaTTT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, 
            SDTLogicOracle sdtLogicOracle, Constants consts,
            ParameterizedSymbol ... inputs) {
    	this(oracle, hypOracleFactory, sdtLogicOracle, consts, false, inputs);
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
            
//            log.logPhase("completing observation table");
//            while(!(obs.complete())) {};        
//            log.logPhase("completed observation table");

            //System.out.println(obs.toString());
            
        	dt.checkVariableConsistency();
        	
            Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
            components.putAll(dt.getComponents());
            AutomatonBuilder ab = new AutomatonBuilder(components, consts);            
            hyp = ab.toRegisterAutomaton();        
            
            //FIXME: the default logging appender cannot log models and data structures
            //System.out.println(hyp.toString());
            log.logModel(hyp);
            
        } while (analyzeCounterExample());
         
    }

    private boolean analyzeCounterExample() {
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
//        obs.addSuffix(res.getSuffix());
        Word<PSymbolInstance> prefix = res.getPrefix();
        DTLeaf leaf = dt.getLeaf(prefix);
        if (leaf == null) {
        	leaf = dt.sift(prefix, true);
        }
        Word<PSymbolInstance> word = ce.getInput();
        if (isGuardRefinement(leaf, prefix, ce.getInput().prefix(prefix.length()))) {
        	dt.addSuffix(res.getSuffix(), leaf);
        }
        else {
        	leaf.addShortPrefix(leaf.getPrefix(prefix));
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

