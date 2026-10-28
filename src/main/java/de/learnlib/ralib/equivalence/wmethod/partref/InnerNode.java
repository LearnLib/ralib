package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.ArrayList;
import java.util.List;

import net.automatalib.word.Word;

public class InnerNode extends Node {

    private final List<Node> children;

    private final Word<DSymbolInstance> label;

    public InnerNode(Block block, List<? extends Node> children, Word<DSymbolInstance> label) {
        super(block.getStates());
        this.children = new ArrayList<>(children);
        this.label = label;
    }

    public List<Node> getChildren() {
        return children;
    }

    public Word<DSymbolInstance> getLabel() {
        return label;
    }

    public void replaceChild(Node replace, Node by) {
        children.remove(replace);
        children.add(by);
    }

    @Override
    public int maxIndex() {
        int max = super.maxIndex();
        for (Node child : children) {
            max = Integer.max(child.maxIndex(), max);
        }
        return max;
    }
}
