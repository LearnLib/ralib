package de.learnlib.ralib.dt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.ralambda.DiscriminationTree;
import de.learnlib.ralib.learning.ralambda.RaLambda;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTLeaf;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 * Implementation of discrimination tree.
 *
 * @author fredrik
 */
public class DT implements DiscriminationTree {
    private final DTInnerNode root;

    private final ParameterizedSymbol[] inputs;
    private final TreeOracle oracle;
    private final boolean ioMode;
    private final Constants consts;
    private DTLeaf sink = null;
    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    public DT(TreeOracle oracle, boolean ioMode, Constants consts, ParameterizedSymbol... inputs) {
        this.oracle = oracle;
        this.ioMode = ioMode;
        this.inputs = inputs;
        this.consts = consts;
        this.restrictionBuilder = oracle.getRestrictionBuilder();

        Word<PSymbolInstance> epsilon = Word.epsilon();
        SymbolicSuffix suffEps = new SymbolicSuffix(epsilon, epsilon);

        root = new DTInnerNode(suffEps);
    }

    public DT(DTInnerNode root, TreeOracle oracle, boolean ioMode, Constants consts, ParameterizedSymbol... inputs) {
        this.root = root;
        this.oracle = oracle;
        this.ioMode = ioMode;
        this.inputs = inputs;
        this.consts = consts;
        this.restrictionBuilder = oracle.getRestrictionBuilder();
    }

    public DT(DT dt) {
        this.inputs = dt.inputs;
        this.oracle = dt.oracle;
        this.ioMode = dt.ioMode;
        this.consts = dt.consts;
        this.restrictionBuilder = dt.restrictionBuilder;

        root = new DTInnerNode(dt.root);
    }

    @Override
    public DTLeaf sift(Word<PSymbolInstance> prefix, boolean add) {
        //System.out.println("SIFT: " + prefix + ", add: " + add);
        DTLeaf leaf = getLeaf(prefix);
        if (leaf != null) {
            return leaf;
        }
        MappedPrefix mp = new MappedPrefix(prefix, new Bijection<>());
        DTLeaf result = sift(mp, root, add);
        return result;
    }

    public void initialize() {
        if (ioMode) {
            DTInnerNode parent = root;
            MappedPrefix epsilon = new MappedPrefix(RaLambda.EMPTY_PREFIX, new Bijection<>());
            for (ParameterizedSymbol symbol : inputs) {
                if (symbol instanceof OutputSymbol) {
                    DTInnerNode outputNode = new DTInnerNode(new SymbolicSuffix(symbol));
                    PathResult r = PathResult.computePathResult(oracle, epsilon, parent.getSuffixes(), ioMode);
                    DTBranch branch = new DTBranch(outputNode, r);
                    outputNode.setParent(parent);
                    parent.addBranch(branch);
                    parent = outputNode;
                }
            }
            sift(epsilon, root, true);

            for (DTBranch branch : root.getBranches()) {
                if (!branch.getUrap().isAccepting())
                    sink = (DTLeaf) branch.getChild();
            }
        } else {
            sift(RaLambda.EMPTY_PREFIX, true);
        }
    }

    private SDT makeRejectingSDT(OutputSymbol symbol, SuffixValueGenerator sgen, int paramIndex) {
        if (paramIndex == symbol.getArity()) {
            return SDTLeaf.REJECTING;
        } else {
            DataType param = symbol.getPtypes()[paramIndex];
            SuffixValue s = sgen.next(param);
            LinkedHashMap<SDTGuard, SDT> map = new LinkedHashMap<SDTGuard, SDT>();
            map.put(new SDTGuard.SDTTrueGuard(s), makeRejectingSDT(symbol, sgen, paramIndex + 1));
            return new SDT(map);
        }
    }

    private DTLeaf sift(MappedPrefix mp, DTInnerNode from, boolean add) {
        DTLeaf leaf = null;
        DTInnerNode inner = from;

        // traverse tree from root to leaf
        do {
            SymbolicSuffix suffix = inner.getSuffix();
            Pair<DTNode, PathResult> siftRes = inner.sift(mp, oracle, ioMode);

            if (siftRes == null) {
                // discovered new location
                leaf = new DTLeaf(oracle);
                //tqr = mp.computeTQR(suffix, oracle);
                PathResult r = PathResult.computePathResult(oracle, mp, inner.getSuffixes(), ioMode);
                assert !mp.getTQRs().containsKey(suffix);
                mp.addTQR(suffix, r.getSDTforSuffix(suffix));
                mp.updateRemapping(Bijection.identity(mp.memorableValues()));
                leaf.setAccessSequence(mp);
                DTBranch branch = new DTBranch(leaf, r);
                inner.addBranch(branch);
                leaf.setParent(inner);
                leaf.start(this, ioMode, inputs);
                leaf.updateBranching(this);
                return leaf;
            }
            SDT tqr = siftRes.getValue().getSDTforSuffix(suffix);
            mp.addTQR(suffix, tqr);
            mp.updateRemapping(siftRes.getValue().getRemapping());
            if (!siftRes.getKey().isLeaf()) {
                inner = (DTInnerNode) siftRes.getKey();
            } else {
                leaf = (DTLeaf) siftRes.getKey();
            }
        } while (leaf == null);

        if (add && !leaf.getAccessSequence().equals(mp.getPrefix())) {
            if (mp instanceof ShortPrefix) {
                leaf.addShortPrefix((ShortPrefix) mp);
            } else {
                leaf.addPrefix(mp);
            }
        }
        return leaf;
    }

    @Override
    public void split(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, DTLeaf leaf) {
        //System.out.println("SPLIT: " + prefix + ", " + suffix + ", " + leaf);
        // add new inner node
        DTBranch branch = leaf.getParentBranch();
        DTInnerNode node = new DTInnerNode(suffix);
        node.setParent(leaf.getParent());
        branch.setChild(node); // point branch to the new inner node

        // add the new leaf
        MappedPrefix mp = leaf.getMappedPrefix(prefix);
        //SDT tqr = mp.computeTQR(suffix, oracle);
        DTLeaf newLeaf = new DTLeaf(mp, oracle);
        newLeaf.setParent(node);
        PathResult r = PathResult.computePathResult(oracle, mp, node.getSuffixes(), ioMode);
        SDT tqr = r.getSDTforSuffix(suffix);
        assert !mp.getTQRs().containsKey(suffix);
        mp.addTQR(suffix, tqr);
        mp.updateRemapping(Bijection.identity(mp.memorableValues()));

        DTBranch newBranch = new DTBranch(newLeaf, r);
        node.addBranch(newBranch);
        ShortPrefix sp = (ShortPrefix) leaf.getShortPrefixes().get(prefix);

        // update old leaf
        boolean removed = leaf.removeShortPrefix(prefix);
        assert (removed); // must not split a prefix that isn't there

        //SDT tqr2 = leaf.getPrimePrefix().computeTQR(suffix, oracle);
        PathResult r2 = PathResult.computePathResult(oracle, leaf.getPrimePrefix(), node.getSuffixes(), ioMode);
        SDT tqr2 = r2.getSDTforSuffix(suffix);
        leaf.getPrimePrefix().addTQR(suffix, tqr2);
        leaf.getPrimePrefix().updateRemapping(Bijection.identity(leaf.getPrimePrefix().memorableValues()));
        //        assert !tqr.getSdt().isEquivalent(tqr2.getSdt(), new VarMapping<>());
        DTBranch b = new DTBranch(leaf, r2);
        leaf.setParent(node);
        node.addBranch(b);

        // resift all transitions targeting this location
        resift(leaf);

        newLeaf.start(this, sp.getBranching());
        newLeaf.updateBranching(this);

        if (removed) {
            leaf.updateBranching(this);
        }
    }

    public void addSuffix(SymbolicSuffix suffix, DTLeaf leaf) {

        DTBranch branch = leaf.getParentBranch();
        DTInnerNode node = new DTInnerNode(suffix);
        node.setParent(leaf.getParent());
        branch.setChild(node);
        leaf.setParent(node);

        //SDT tqr  = leaf.getPrimePrefix().computeTQR(suffix, oracle);
        PathResult r = PathResult.computePathResult(oracle, leaf.getPrimePrefix(), node.getSuffixes(), ioMode);
        SDT tqr = r.getSDTforSuffix(suffix);
        assert !leaf.getPrimePrefix().getTQRs().containsKey(suffix);
        leaf.getPrimePrefix().addTQR(suffix, tqr);
        leaf.getPrimePrefix().updateRemapping(Bijection.identity(leaf.getPrimePrefix().memorableValues()));

        DTBranch newBranch = new DTBranch(leaf, r);
        node.addBranch(newBranch);

        Set<MappedPrefix> prefixes = new LinkedHashSet<MappedPrefix>();
        leaf.getMappedExtendedPrefixes(prefixes);
        for (MappedPrefix prefix : prefixes) {
            leaf.removePrefix(prefix.getPrefix());
            DTLeaf l = sift(prefix, node, true);
            //System.out.println("SIFTED: " + prefix + " to " + l);
        }

        leaf.updateBranching(this);
    }

    public boolean addLocation(Word<PSymbolInstance> target, DTLeaf src_c, DTLeaf dest_c, DTLeaf target_c) {

        Word<PSymbolInstance> prefix = target.prefix(target.length() - 1);
        SymbolicSuffix suff1 = new SymbolicSuffix(prefix, target.suffix(1), restrictionBuilder);
        SymbolicSuffix suff2 = findLCA(dest_c, target_c).getSuffix();
        SymbolicSuffix suffix = suff1.concat(suff2);

        DTInnerNode parent = src_c.getParent();
        while (parent != null) {
        	if (parent.getSuffix().equals(suffix))
        		return false;
        	parent = parent.getParent();
        }

        split(prefix, suffix, src_c);
        return true;
    }

    /**
     * resift all prefixes of a leaf, in order to add them to the correct leaf
     *
     * @param leaf
     */
    private void resift(DTLeaf leaf) {
        // Potential optimization:
        // can keep TQRs up to the parent, as they should still be the same

        Set<MappedPrefix> prefixes = new LinkedHashSet<MappedPrefix>();
        leaf.getMappedExtendedPrefixes(prefixes);
        DTInnerNode parent = leaf.getParent();
        for (MappedPrefix prefix : prefixes) {
            leaf.removePrefix(prefix.getPrefix());
            sift(prefix, parent, true);
        }
    }

    public boolean checkIOSuffixes() {
    	if (ioMode) {
    		for (DTBranch b : root.getBranches()) {
    			if (b.getUrap().getSDTforSuffix(root.getSuffix()).isAccepting())
    				return checkIOSuffixes(b.getChild());
    		}
    		throw new java.lang.RuntimeException("No accepting child of root");
    	}
    	else
    		return true;
    }

    private boolean checkIOSuffixes(DTNode node) {
    	if (node.isLeaf()) {
    		boolean missingSuffix = false;
    		DTLeaf leaf = (DTLeaf) node;
    		MappedPrefix accessMP = leaf.getPrimePrefix();
    		if (accessMP.getPrefix().length() == 0
    				|| accessMP.getPrefix().lastSymbol().getBaseSymbol() instanceof OutputSymbol) {
    			return true;
    		}
    		Set<ParameterizedSymbol> ioSuffixes = accessMP.getTQRs()
    				.keySet()
    				.stream()
    				.filter(s -> s.length() == 1)
    				.map(s -> s.getActions().firstSymbol())
    				.filter(ps -> ps instanceof OutputSymbol)
    				.collect(Collectors.toSet());
    		for (ParameterizedSymbol ps : inputs) {
    			if (ps instanceof OutputSymbol
    					&& !ioSuffixes.contains(ps)) {
    				SymbolicSuffix suffix = new SymbolicSuffix(ps);
    				addSuffix(suffix, leaf);
    				missingSuffix = true;
    			}
    		}
    		return !missingSuffix;
    	}
    	boolean ret = true;
    	DTInnerNode inner = (DTInnerNode) node;
    	for (DTBranch b : Collections.unmodifiableCollection(new LinkedHashSet<DTBranch>(inner.getBranches()))) {
    		ret = ret && checkIOSuffixes(b.getChild());
    	}
    	return ret;
    }

    public boolean checkVariableConsistency(OptimizedSymbolicSuffixBuilder suffixBuilder) {
        return checkConsistency(this.root, suffixBuilder);
    }

    private boolean checkConsistency(DTNode node, OptimizedSymbolicSuffixBuilder suffixBuilder) {
        if (node.isLeaf()) {
            DTLeaf leaf = (DTLeaf) node;
            return leaf.checkVariableConsistency(this, this.consts, suffixBuilder);
        }
        boolean ret = true;
        DTInnerNode inner = (DTInnerNode) node;
        for (DTBranch b : Collections.unmodifiableCollection(new LinkedHashSet<DTBranch>(inner.getBranches()))) {
            ret = ret && checkConsistency(b.getChild(), suffixBuilder);
        }
        return ret;
    }

    public boolean checkRegisterConsistency(OptimizedSymbolicSuffixBuilder suffixBuilder) {
    	return checkRegisterConsistency(root, suffixBuilder);
    }

    private boolean checkRegisterConsistency(DTNode node, OptimizedSymbolicSuffixBuilder suffixBuilder) {
        if (node.isLeaf()) {
            DTLeaf leaf = (DTLeaf) node;
            return leaf.checkRegisterConsistency(this, this.consts, suffixBuilder);
        }
        boolean ret = true;
        DTInnerNode inner = (DTInnerNode) node;
        for (DTBranch b : Collections.unmodifiableCollection(new LinkedHashSet<DTBranch>(inner.getBranches()))) {
            ret = ret && checkRegisterConsistency(b.getChild(), suffixBuilder);
        }
        return ret;
    }

    /**
     * check whether sifting a word into the dt leads to a refinement of the dt, i.e
     * whether the location corresponding to word is already in the branching of the
     * source location of word
     *
     * @param word
     * @return true if sifting word into dt leads to refinement
     */
    public boolean isRefinement(Word<PSymbolInstance> word) {
        Word<PSymbolInstance> prefix = word.prefix(word.length() - 1);
        DTLeaf prefixLeaf = getLeaf(prefix);
        assert prefixLeaf != null;

        return prefixLeaf.isRefinemement(this, word);
    }

    /**
     * get leaf containing prefix as
     *
     * @param as
     * @return leaf containing as, or null
     */
    public DTLeaf getLeaf(Word<PSymbolInstance> as) {
        return getLeaf(as, root);
    }

    DTLeaf getLeaf(Word<PSymbolInstance> as, DTNode node) {
        if (node.isLeaf()) {
            DTLeaf leaf = (DTLeaf) node;
            if (leaf.getPrimePrefix().getPrefix().equals(as) || leaf.getShortPrefixes().contains(as)
                    || leaf.getPrefixes().contains(as))
                return leaf;
        } else {
            DTInnerNode in = (DTInnerNode) node;
            for (DTBranch b : in.getBranches()) {
                DTLeaf l = getLeaf(as, b.getChild());
                if (l != null)
                    return l;
            }
        }
        return null;
    }

    public ParameterizedSymbol[] getInputs() {
        return inputs;
    }

    boolean getIoMode() {
        return ioMode;
    }

    TreeOracle getOracle() {
        return oracle;
    }

    public Collection<DTLeaf> getLeaves() {
        Collection<DTLeaf> leaves = new ArrayList<DTLeaf>();
        getLeaves(root, leaves);
        return leaves;
    }

    private void getLeaves(DTNode node, Collection<DTLeaf> leaves) {
        if (node.isLeaf())
            leaves.add((DTLeaf) node);
        else {
            DTInnerNode inner = (DTInnerNode) node;
            for (DTBranch b : inner.getBranches())
                getLeaves(b.getChild(), leaves);
        }
    }

    private void getSuffixes(DTNode node, Collection<SymbolicSuffix> suffixes) {
        if (!node.isLeaf()) {
            DTInnerNode inner = (DTInnerNode) node;
            suffixes.add(inner.getSuffix());
            for (DTBranch b : inner.getBranches())
                getSuffixes(b.getChild(), suffixes);
        }
    }

    public Collection<SymbolicSuffix> getSuffixes() {
        Collection<SymbolicSuffix> suffixes = new LinkedHashSet<>();
        getSuffixes(root, suffixes);
        return suffixes;
    }

    private void getAllPrefixes(Collection<Word<PSymbolInstance>> prefs, DTNode node) {
        if (node.isLeaf()) {
            DTLeaf leaf = (DTLeaf) node;
            prefs.addAll(leaf.getAllPrefixes());
        } else {
            DTInnerNode inner = (DTInnerNode) node;
            for (DTBranch b : inner.getBranches())
                getAllPrefixes(prefs, b.getChild());
        }
    }

    public boolean isMissingParameter() {
    	return isMissingParameter(root);
    }

    private boolean isMissingParameter(DTNode node) {
    	if (node.isLeaf())
    		return ((DTLeaf)node).isMissingVariable();
    	else {
    		for (DTBranch b : ((DTInnerNode)node).getBranches()) {
    			if (isMissingParameter(b.getChild()))
    				return true;
    		}
    	}
    	return false;
    }

    @Override
    public Map<Word<PSymbolInstance>, LocationComponent> getComponents() {
        Map<Word<PSymbolInstance>, LocationComponent> components = new LinkedHashMap<Word<PSymbolInstance>, LocationComponent>();
        collectComponents(components, root);
        return components;
    }

    private void collectComponents(Map<Word<PSymbolInstance>, LocationComponent> comp, DTNode node) {
        if (node.isLeaf()) {
            DTLeaf leaf = (DTLeaf) node;
            comp.put(leaf.getAccessSequence(), leaf);
        } else {
            DTInnerNode inner = (DTInnerNode) node;
            for (DTBranch b : inner.getBranches()) {
                collectComponents(comp, b.getChild());
            }
        }
    }

    /**
     * find the lowest common ancestor of two leaves
     *
     * @param l1
     * @param l2
     * @return the lowest common ancestor of l1 and l2
     */
    public DTInnerNode findLCA(DTLeaf l1, DTLeaf l2) {
        Deque<DTInnerNode> path1 = new ArrayDeque<DTInnerNode>();
        Deque<DTInnerNode> path2 = new ArrayDeque<DTInnerNode>();

        if (l1.getParent() == l2.getParent())
            return l1.getParent();

        DTInnerNode parent = l1.getParent();
        while (parent != null) {
            path1.add(parent);
            parent = parent.getParent();
        }
        parent = l2.getParent();
        while (parent != null) {
            path2.add(parent);
            parent = parent.getParent();
        }

        while (!path1.isEmpty()) {
        	DTInnerNode node = path1.poll();
        	if (path2.contains(node))
        		return node;
        }
        return null;
    }

    public DTLeaf getSink() {
        return sink;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DT: {");
        buildTreeString(builder, root, "", "   ", " -- ");
        builder.append("}");
        return builder.toString();
    }

    private void buildTreeString(StringBuilder builder, DTNode node, String currentIndentation, String indentation,
            String sep) {
        if (node.isLeaf()) {
            builder.append("\n").append(currentIndentation).append("Leaf: ").append(node);
        } else {
            DTInnerNode inner = (DTInnerNode) node;
            builder.append("\n").append(currentIndentation).append("Inner: ").append(inner.getSuffix());
            if (!inner.getBranches().isEmpty()) {
                Iterator<DTBranch> iter = inner.getBranches().iterator();
                while (iter.hasNext()) {
                    builder.append("\n").append(currentIndentation);
                    DTBranch branch = iter.next();
                    builder.append("Branch: ").append(branch.getUrap());
                    buildTreeString(builder, branch.getChild(), indentation + currentIndentation, indentation, sep);
                }
            }
            //else {
            //    builder.append("(").append(inner.getSuffix()).append(",").append("∅").append(")");
            //}
        }
    }
}
