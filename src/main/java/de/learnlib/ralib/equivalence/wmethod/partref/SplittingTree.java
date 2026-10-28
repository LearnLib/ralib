package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
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
            if (leaf.getLocations().size() == 1) {
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

    private Optional<List<Set<SymbolicState>>> computeChildren(Leaf block, DSymbolInstance in) {
        List<Set<SymbolicState>> preconditions = new ArrayList<>();
        Set<DSymbolInstance> outs = block.getPossibleOutputs(in, in.maxIndex() + 1);
        for (DSymbolInstance out : outs) {
            preconditions.add(block.computeOutputPreconditions(in, out));
        }
        List<Set<SymbolicState>> children = computeChildren(block, in, preconditions);
        if (!children.isEmpty() &&
                canMeaningfullySplit(block, children)) {
            return Optional.of(children);
        }
        return Optional.empty();
    }

    private Optional<List<Set<SymbolicState>>> computeChildren(Leaf block, DSymbolInstance in, DSymbolInstance out, int offset, List<? extends Block> blocks) {
        List<Set<SymbolicState>> preconditions = new ArrayList<>();
        for (Block b : blocks) {
            preconditions.add(b.computeStatePreconditions(in, out, offset, model));
        }
        List<Set<SymbolicState>> children = computeChildren(block, in, preconditions);
        if (!children.isEmpty() &&
                canMeaningfullySplit(block, children) &&
                isValidSplit(block, children)) {
            return Optional.of(children);
        }
        return Optional.empty();
    }

    private List<Set<SymbolicState>> computeChildren(Block block, DSymbolInstance in, List<Set<SymbolicState>> preconditions) {
        List<Set<SymbolicState>> children = new ArrayList<>();

        if (!in.isQuantified()) {
            for (Set<SymbolicState> prec : preconditions) {
                Set<SymbolicState> intersection = block.intersect(prec);
                if (!intersection.isEmpty()) {
                    children.add(intersection);
                }
            }
            return children;
        }

        Map<Integer, Set<SymbolicState>> groups = new LinkedHashMap<>();
        for (SymbolicState s : block.getStates()) {
            int bitmask = 0;
            int bit = 1;
            for (Set<SymbolicState> prec : preconditions) {
                Set<SymbolicState> intersection = s.and(prec);
                if (!intersection.isEmpty()) {
                    bitmask = bitmask + bit;
                }
                bit = bit * 2;
            }
            Set<SymbolicState> group = groups.containsKey(bitmask) ? groups.get(bitmask) : new LinkedHashSet<>();
            group.add(s);
            groups.put(bitmask, group);
        }

        for (Set<SymbolicState> group : groups.values()) {
            for (Set<SymbolicState> prec : preconditions) {
                Set<SymbolicState> intersection = SymbolicState.intersect(group, prec);
                if (!intersection.isEmpty()) {
                    children.add(intersection);
                }
            }
        }

        return children;
    }

    public boolean splitByOutput(Leaf block, DSymbolInstance in) {
        assert getLeaves().contains(block) : "Not a leaf block";
        List<Leaf> leaves = new ArrayList<>();
        Optional<List<Set<SymbolicState>>> children = computeChildren(block, in);
        if (children.isPresent()) {
            assert isValidSplit(block, children.get()) : "States lost after split";
            for (Set<SymbolicState> states : children.get()) {
                leaves.add(new Leaf(states));
            }
            Word<DSymbolInstance> label = Word.fromLetter(in);
            expand(block, label, leaves);
            return true;
        }
        return false;
    }

    public boolean splitByState(Leaf block, DSymbolInstance in, DSymbolInstance out) {
        Map<Integer, Queue<InnerNode>> groups = orderBlocksByLabelLength();
        int k = Integer.max(in.maxIndex(), out.maxIndex());

        for (int i = 1; i <= groups.size(); i++) {
            assert groups.containsKey(i) : "Labels are not prefix-closed";
            for (InnerNode parent : groups.get(i)) {
                Optional<List<Set<SymbolicState>>> children = computeChildren(block, in, out, k, parent.getChildren());
                if (children.isPresent()) {
                    List<Leaf> leaves = new ArrayList<>();
                    for (Set<SymbolicState> states : children.get()) {
                        leaves.add(new Leaf(states));
                    }
                    Word<DSymbolInstance> parentLabel = DSymbolInstance.offset(parent.getLabel(), k);
                    Word<DSymbolInstance> inWord = Word.fromLetter(in);
                    Word<DSymbolInstance> label = DSymbolInstance.append(inWord, parentLabel);
                    expand(block, label, leaves);
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

    private boolean isValidSplit(Block parent, List<Set<SymbolicState>> children) {
        for (SymbolicState state : parent.getStates()) {
            boolean found = false;
            for (Set<SymbolicState> child : children) {
                for (SymbolicState s : child) {
                    if (s.getLocation().equals(state.getLocation()) &&
                            Constraint.implies(s.getConstraints(), state.getConstraints())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean canMeaningfullySplit(Block block, List<Set<SymbolicState>> children) {
        List<Delta> blockProgresses = new ArrayList<>();
        boolean revealsRegs = false;
        boolean separatesLocs = true;
        for (Set<SymbolicState> child : children) {
            Delta delta = new Delta(block, child);
            blockProgresses.add(delta);
            revealsRegs = revealsRegs || delta.revealsRegisters();
            separatesLocs = separatesLocs && delta.separatesLocations();
        }
        if (!revealsRegs && !separatesLocs) {
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
