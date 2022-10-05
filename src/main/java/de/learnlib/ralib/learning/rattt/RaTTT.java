package de.learnlib.ralib.learning.rattt;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.ceanalysis.PrefixFinder;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.dt.DT;
import de.learnlib.ralib.dt.DTHyp;
import de.learnlib.ralib.dt.DTInnerNode;
import de.learnlib.ralib.dt.DTLeaf;
import de.learnlib.ralib.dt.MappedPrefix;
import de.learnlib.ralib.dt.ShortPrefix;
import de.learnlib.ralib.learning.AutomatonBuilder;
import de.learnlib.ralib.learning.CounterexampleAnalysis;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.IOAutomatonBuilder;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.RaLearningAlgorithm;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class RaTTT implements RaLearningAlgorithm {

    public static final Word<PSymbolInstance> EMPTY_PREFIX = Word.epsilon();

    public static final SymbolicSuffix EMPTY_SUFFIX = new SymbolicSuffix(Word.<PSymbolInstance>epsilon(),
            Word.<PSymbolInstance>epsilon());

    private final DT dt;

    private final Constants consts;

    private final Deque<DefaultQuery<PSymbolInstance, Boolean>> counterexamples = new LinkedList<>();

    private DTHyp hyp = null;

    private final TreeOracle sulOracle;

    private final SDTLogicOracle sdtLogicOracle;

    private final TreeOracleFactory hypOracleFactory;

    private final boolean ioMode;

    private static final LearnLogger log = LearnLogger.getLogger(RaTTT.class);

    private boolean useOldAnalyzer;
    private boolean thoroughSearch;
    private int[] indices = new int[0];

    private final Deque<Word<PSymbolInstance>> shortPrefixes = new ArrayDeque<Word<PSymbolInstance>>();

    private PrefixFinder prefixFinder = null;

    public RaTTT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            boolean ioMode, ParameterizedSymbol... inputs) {

        this(oracle, hypOracleFactory, sdtLogicOracle, consts, ioMode, false, inputs);
    }

    public RaTTT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            boolean ioMode, boolean useOldAnalyzer, ParameterizedSymbol... inputs) {

        this(oracle, hypOracleFactory, sdtLogicOracle, consts, ioMode, useOldAnalyzer, false, inputs);
    }

    public RaTTT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            boolean ioMode, boolean useOldAnalyzer, boolean thoroughSearch, ParameterizedSymbol... inputs) {

        this.ioMode = ioMode;
        this.dt = new DT(oracle, ioMode, consts, inputs);
        this.consts = consts;
        this.sulOracle = oracle;
        this.sdtLogicOracle = sdtLogicOracle;
        this.hypOracleFactory = hypOracleFactory;
        this.useOldAnalyzer = useOldAnalyzer;
        this.thoroughSearch = thoroughSearch;

        this.dt.initialize();
    }

    public RaTTT(TreeOracle oracle, TreeOracleFactory hypOracleFactory, SDTLogicOracle sdtLogicOracle, Constants consts,
            ParameterizedSymbol... inputs) {
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

            while (!dt.checkVariableConsistency())
                ;
//            if (ioMode)
//                while(!dt.checkIOConsistency(hyp));

            Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
            components.putAll(dt.getComponents());

            AutomatonBuilder ab;
            if (ioMode)
                ab = new IOAutomatonBuilder(components, consts, dt);
            else
                ab = new AutomatonBuilder(components, consts, dt);
            hyp = (DTHyp) ab.toRegisterAutomaton();

            prefixFinder = null;

            // FIXME: the default logging appender cannot log models and data structures
            // System.out.println(hyp.toString());
            log.logModel(hyp);

        } while (analyzeCounterExample());

    }

    /*
     * Update the prefix finder with a new temporary hypothesis taking into account
     * the discovered locations. The hypothesis is temporary because it contains
     * locations representing dangling short prefixes.
     */
    private void updatePrefixFinder() {
        DT dt = new DT(this.dt);
        dt.checkVariableConsistency();

        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        Map<Word<PSymbolInstance>, LocationComponent> shortComponents = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        components.putAll(dt.getComponents());
        for (Map.Entry<Word<PSymbolInstance>, LocationComponent> e : components.entrySet()) {
            DTLeaf l = (DTLeaf) e.getValue();
            for (MappedPrefix mp : l.getShortPrefixes().get())
                shortComponents.put(mp.getPrefix(), e.getValue());
        }
        components.putAll(shortComponents);

        AutomatonBuilder ab;
        if (useOldAnalyzer)
            ab = new AutomatonBuilder(components, consts);
        else
            ab = new AutomatonBuilder(components, consts, dt);

        Hypothesis h = ab.toRegisterAutomaton();
        prefixFinder.setHypothesis(h);
        prefixFinder.setComponents(components);
        prefixFinder.setHypothesisTreeOracle(hypOracleFactory.createTreeOracle(h));
    }

    private boolean analyzeCounterExample() {
        if (useOldAnalyzer)
            return analyzeCounterExampleOld();
        log.logPhase("Analyzing Counterexample");
        if (counterexamples.isEmpty()) {
            prefixFinder = null;
            return false;
        }

        TreeOracle hypOracle = hypOracleFactory.createTreeOracle(hyp);

        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        components.putAll(dt.getComponents());
        if (prefixFinder == null) {
            prefixFinder = new PrefixFinder(sulOracle, hypOracle, hyp, sdtLogicOracle, components, consts);
        }

        DefaultQuery<PSymbolInstance, Boolean> ce = counterexamples.peek();

        // check if ce still is a counterexample ...
        boolean hypce = hyp.accepts(ce.getInput());
        boolean sulce = ce.getOutput();
        if (hypce == sulce) {
            log.logEvent("word is not a counterexample: " + ce + " - " + sulce);
            counterexamples.poll();
            prefixFinder = null;
            return true;
        }

        // find short prefixes until getting dt refinement
        Word<PSymbolInstance> ceWord = ce.getInput();
        Set<Word<PSymbolInstance>> prefixes;
        while (true) {
            System.out.println(dt);
            boolean refinement = false;
            do {
                Word<PSymbolInstance> prefix = prefixFinder.analyzeCounterexample(ceWord, indices);
                if (this.thoroughSearch)
                    prefixes = prefixFinder.getCandidatePrefixes();
                else {
                    prefixes = new HashSet<Word<PSymbolInstance>>();
                    prefixes.add(prefix);
                }

                for (Word<PSymbolInstance> p : prefixes) {
                    refinement = refinement | addPrefix(p);
                }
            } while (!refinement && !prefixes.isEmpty());

            processShortPrefixes();
            // if dangling short prefixes present, examine candidate counterexample
            if (!shortPrefixes.isEmpty()) {

                updatePrefixFinder();
                ceWord = prefixFinder.getCounterExample().getInput();

                clearIndices();

                // if there's still a dangling prefix but no more CEs, we have a problem
                assert ceWord != null;
            } else
                return true;
        }
    }

    public void setUseOldAnalyzer(boolean useOldAnalyzer) {
        this.useOldAnalyzer = useOldAnalyzer;
    }

    private boolean addPrefix(Word<PSymbolInstance> word) {
        DTLeaf dest_c = dt.sift(word, true);
        Word<PSymbolInstance> src_id = word.prefix(word.length() - 1);
        DTLeaf src_c = dt.getLeaf(src_id);
        Branching hypBranching = null;
        if (src_c.getAccessSequence().equals(src_id)) {
            hypBranching = src_c.getBranching(word.lastSymbol().getBaseSymbol());
        } else {
            ShortPrefix sp = (ShortPrefix) src_c.getShortPrefixes().get(src_id);
            hypBranching = sp.getBranching(word.lastSymbol().getBaseSymbol());
        }
        Word<PSymbolInstance> branch = hyp.branchWithSameGuard(word, hypBranching); 
                //hyp.branchWithSameGuard(word, src_c.getBranching(word.lastSymbol().getBaseSymbol()));
        DTLeaf branchLeaf = dt.getLeaf(branch);
        boolean isDTRefinement = (branchLeaf != dest_c);

        // does this guard lead to a dt refinement?
        if (isDTRefinement) {
            assert word.length() > 0;
            assert src_c != null;

            SymbolicSuffix suff1 = new SymbolicSuffix(src_id, word.suffix(1));
            SymbolicSuffix suff2 = smallestDiscriminator(word, dest_c, src_c);
            SymbolicSuffix suffix = suff1.concat(suff2);

            if (!shortPrefixes.isEmpty()) {
                // if we have guard refinement, and stack is not empty,
                // we then the source location must be in the list of short prefixes

                assert shortPrefixes.contains(src_id);
                shortPrefixes.remove(src_id);

                assert src_c.getShortPrefixes().contains(src_id); // source should be a short prefix

                // sub is a short prefix, so it must be a new location
                dt.split(src_id, suffix, src_c);
            } else {
                dt.addSuffix(suffix, src_c);
            }
            return true;
        }
        if (!dt.checkVariableConsistency()) {
            while(!dt.checkVariableConsistency());
            return true;
        }

        // no refinement, so must be a new location
        boolean refinement = addNewLocation(word, dest_c);
        return refinement;
    }
    
    private boolean addNewLocation(Word<PSymbolInstance> prefix, DTLeaf src_c) {
        Pair<Word<PSymbolInstance>, Word<PSymbolInstance>> divergance = src_c.elevatePrefix(getDT(), prefix,
                (DTHyp) hyp);
        if (divergance == null) {
            shortPrefixes.push(prefix); // no refinement of dt
        } else {
            // elevating and expanding prefix lead to refinement of dt
            Word<PSymbolInstance> refinedTarget = divergance.getKey();
            Word<PSymbolInstance> target = divergance.getValue();

            dt.addLocation(refinedTarget, src_c, dt.getLeaf(target), dt.getLeaf(refinedTarget));
            return true;
        }
        return false;
    }

    private void processShortPrefixes() {
        boolean progress = true;
        
        dt.getLeaves().forEach(l -> shortPrefixes.remove(l.getAccessSequence()));
        
        while (!shortPrefixes.isEmpty() && progress) {
            progress = false;
            for (Word<PSymbolInstance> sp : Collections.unmodifiableCollection(shortPrefixes)) {
                boolean spProgress = checkAddNewLocation(sp);
                if (spProgress) {
                    shortPrefixes.remove(sp);
                    dt.getLeaves().forEach(l -> shortPrefixes.remove(l.getAccessSequence()));
                }
                progress |= spProgress;
            }
        }
    }

    private boolean checkAddNewLocation(Word<PSymbolInstance> shortPrefix) {
        DTLeaf leaf = dt.getLeaf(shortPrefix);
        for (ParameterizedSymbol ps : dt.getInputs()) {
            Collection<Word<PSymbolInstance>> extensions = dt.getOneSymbolExtensions(shortPrefix, ps);
            for (Word<PSymbolInstance> word : extensions) {
                DTLeaf ext_c = dt.getLeaf(word);
                Word<PSymbolInstance> branch = hyp.branchWithSameGuard(word, leaf.getBranching(ps));
                DTLeaf branchLeaf = dt.getLeaf(branch);
                if (ext_c != branchLeaf) {
                    DTInnerNode lca = dt.findLCA(branchLeaf, ext_c);
                    SymbolicSuffix suffix = new SymbolicSuffix(ps).concat(lca.getSuffix());
                    dt.split(shortPrefix, suffix, leaf);
                    return true;
              }
            }
        }
        
        return false;
    }

    private SymbolicSuffix smallestDiscriminator(Word<PSymbolInstance> word, DTLeaf word_c, DTLeaf src_c) {

        ParameterizedSymbol ps = word.lastSymbol().getBaseSymbol();
        int min = Integer.MAX_VALUE;
        SymbolicSuffix suffix = null;

        for (Word<PSymbolInstance> p : src_c.getBranching(ps).getBranches().keySet()) {
            DTLeaf dest_c = dt.getLeaf(p);
            SymbolicSuffix disc = dt.findLCA(dest_c, word_c).getSuffix();
            int len = disc.getActions().length();
            if (len < min) {
                suffix = disc;
                min = len;
            }
        }
        return suffix;
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

        // System.out.println("CE ANALYSIS: " + ce + " ; S:" + sulce + " ; H:" + hypce);

        CEAnalysisResult res = analysis.analyzeCounterexample(ce.getInput());
        Word<PSymbolInstance> accSeq = hyp.transformAccessSequence(res.getPrefix());
        DTLeaf leaf = dt.getLeaf(accSeq);
        dt.addSuffix(res.getSuffix(), leaf);
        return true;
    }
    
    private boolean isGuardRefinement(DTLeaf leaf, Word<PSymbolInstance> prefix, Word<PSymbolInstance> extendedPrefix) {
        ParameterizedSymbol ps = extendedPrefix.getSymbol(extendedPrefix.length() - 1).getBaseSymbol();
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

    public void setIndicesToSearch(int... indices) {
        this.indices = indices;
    }

    private void clearIndices() {
        setIndicesToSearch();
    }

    @Override
    public RaLearningAlgorithmName getName() {
        return RaLearningAlgorithmName.RATTT;
    }

    public void doThoroughCESearch(boolean thoroughSearch) {
        this.thoroughSearch = thoroughSearch;
    }
}
