package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class SplittingTree {

    private Node root;

    private final Map<Node, InnerNode> parents;

    private final RaModel model;

    private final ParameterizedSymbol[] inacts;

    public SplittingTree(RaModel model, ParameterizedSymbol ... inacts) {
        this.model = model;
        this.inacts = inacts;
        this.parents = new LinkedHashMap<>();
        Set<SymbolicState> states = new LinkedHashSet<>();
        for (Location loc : model.getLocations()) {
            states.add(new SymbolicState(loc));
        }
        root = new Leaf(states);
    }

    public int maxIndex() {
        return root.maxIndex();
    }

    public int maxQuantifiedSDVIndex() {
        return root.maxQuantifiedSDVIndex();
    }

    public List<Leaf> getLeaves() {
        return getLeaves(root);
    }

    private List<Leaf> getLeaves(Node node) {
        if (node instanceof InnerNode inner) {
            List<Leaf> leaves = new ArrayList<>();
            for (Node child : inner.getChildren()) {
                leaves.addAll(getLeaves(child));
            }
            return leaves;
        }
        return List.of((Leaf) node);
    }

    public boolean isAcceptable() {
        for (Leaf leaf : getLeaves()) {
            if (!leaf.isOutputConsistent(inacts)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStable() {
        Set<Location> locs = model.getLocations();
        Set<Location> identified = new LinkedHashSet<>();
        for (Leaf leaf : getLeaves()) {
            Set<SymbolicState> states = leaf.getStates();
            if (states.size() == 1) {
                SymbolicState s = states.iterator().next();
                if (s.areAllRegistersIdentified()) {
                    identified.add(s.getLocation());
                    if (identified.containsAll(locs)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean splitByOutput(Leaf block, DSymbolInstance in) {
        assert getLeaves().contains(block) : "Not a leaf block";
        Set<DSymbolInstance> outs = block.getPossibleOutputs(in);
        List<Leaf> leaves = new ArrayList<>();
        List<Set<SymbolicState>> intersections = new ArrayList<>();
        for (DSymbolInstance out : outs) {
            Set<SymbolicState> precs = block.computeOutputPreconditions(in, out);
            Set<SymbolicState> intersection = block.intersect(precs);
            intersections.add(intersection);
            Leaf leaf = new Leaf(intersection);
            leaves.add(leaf);
        }
        if (!leaves.isEmpty() &&
                canMeaningfullySplit(block, intersections)) {
            Word<DSymbolInstance> label = Word.fromLetter(in);
            expand(block, label, leaves);
            return true;
        }
        return false;
    }

    public boolean splitByState(Leaf leaf, DSymbolInstance in, DSymbolInstance out) {
        assert leaf.isOutputConsistent() : "Attempting to split by state a leaf that is not output consistent";
        int k = Integer.max(in.maxIndex(), out.maxIndex());

        Map<Integer, Queue<InnerNode>> groups = orderBlocksByLabelLength();

        for (int i = 1; i <= groups.size(); i++) {
            assert groups.containsKey(i) : "Labels are not prefix-closed";
            for (InnerNode parent : groups.get(i)) {
                List<Set<SymbolicState>> intersections = new ArrayList<>();
                for (Block block : parent.getChildren()) {
                    Set<SymbolicState> precond = block.computeStatePreconditions(in, out, k, model);
                    Set<SymbolicState> intersection = leaf.intersect(precond);
                    if (!intersection.isEmpty()) {
                        intersections.add(intersection);
                    }
                }
                if (!intersections.isEmpty() &&
                        canMeaningfullySplit(leaf, intersections)) {
                    Word<DSymbolInstance> parentLabel = DSymbolInstance.offset(parent.getLabel(), k);
                    Word<DSymbolInstance> inWord = Word.fromLetter(in);
                    Word<DSymbolInstance> label = DSymbolInstance.append(inWord, parentLabel);
                    List<Leaf> leaves = new ArrayList<>();
                    for (Set<SymbolicState> prec : intersections) {
                        Set<SymbolicState> intersection = leaf.intersect(prec);
                        leaves.add(new Leaf(intersection));
                    }
                    expand(leaf, label, leaves);
                    return true;
                }
            }
        }

        return false;
    }

    public List<Word<DSymbolInstance>> getSeparatingSuffixes(Block block) {
        List<Word<DSymbolInstance>> suffixes = new ArrayList<>();
        InnerNode parent = parents.get(block);
        while (parent != null) {
            suffixes.add(parent.getLabel());
            parent = parents.get(parent);
        }
        return suffixes;
    }

    private void expand(Leaf leaf, Word<DSymbolInstance> label, List<Leaf> children) {
        InnerNode inner = new InnerNode(leaf, children, label);
        InnerNode parent = parents.get(leaf);
        if (parent == null) {
            root = inner;
        } else {
            parent.replaceChild(leaf, inner);
        }
        for (Leaf child : children) {
            parents.put(child, inner);
        }
    }

    private boolean canMeaningfullySplit(Block block, List<Set<SymbolicState>> children) {
//        if (ancestor == null) {
//            return true;
//        }
        List<Delta> blockProgresses = new ArrayList<>();
        boolean makesProgress = false;
        for (Set<SymbolicState> child : children) {
            Delta delta = new Delta(block, child);
            blockProgresses.add(delta);
            makesProgress = makesProgress || delta.makesProgress();
        }
        if (!makesProgress) {
            return false;
        }

        Node ancestor = parents.get(block);
        while (ancestor != null) {
            assert ancestor instanceof InnerNode;
            List<Delta> ancestorProgresses = new ArrayList<>();
            for (Node child : ((InnerNode) ancestor).getChildren()) {
                ancestorProgresses.add(new Delta(ancestor, child.getStates()));
            }
            boolean foundEquivSplit = true;
            for (Delta blockProgress : blockProgresses) {
                boolean foundEquivChild = false;
                for (int i = 0; i < ancestorProgresses.size(); i++) {
                    if (blockProgress.isSubsetOf(ancestorProgresses.get(i))) {
                        foundEquivChild = true;
                        ancestorProgresses.remove(i);
                        break;
                    }
                }
                if (!foundEquivChild) {
                    foundEquivSplit = false;
                    break;
                }
            }
            if (foundEquivSplit) {
                return false;
            }
            ancestor = parents.get(ancestor);
        }
        return true;
    }

    private Map<Integer, Queue<InnerNode>> orderBlocksByLabelLength() {
        Map<Integer, Queue<InnerNode>> groups = new LinkedHashMap<>();
        addBlockToGroup(root, groups);
        return groups;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ST: {");
        buildTreeString(builder, root, "", "   ", " -- ");
        builder.append("}");
        return builder.toString();
    }

    private void buildTreeString(StringBuilder builder, Node node, String currentIndentation, String indentation, String sep) {
        if (node instanceof Leaf) {
            builder.append("\n").append(currentIndentation).append("Leaf: ").append(node);
        } else {
            InnerNode inner = (InnerNode) node;
            builder.append("\n").append(currentIndentation).append("Inner: ").append(inner.getStates());
            builder.append("\n").append(currentIndentation).append("Label: ").append(inner.getLabel());
            if (!inner.getChildren().isEmpty()) {
                Iterator<Node> iter = inner.getChildren().iterator();
                while (iter.hasNext()) {
                    builder.append(currentIndentation);
                    Node child = iter.next();
                    buildTreeString(builder, child, indentation + currentIndentation, indentation, sep);
                }
            }
        }
    }

    private static void addBlockToGroup(Node node, Map<Integer, Queue<InnerNode>> groups) {
        if (node instanceof InnerNode inner) {
            int n = inner.getLabel().length();
            Queue<InnerNode> group = groups.containsKey(n) ? groups.get(n) : new ArrayDeque<>();
            group.add(inner);
            groups.put(n, group);
            for (Node child : inner.getChildren()) {
                addBlockToGroup(child, groups);
            }
        }
    }
}
