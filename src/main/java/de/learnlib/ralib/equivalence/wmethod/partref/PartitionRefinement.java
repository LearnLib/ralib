package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.SplittingIterator;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class PartitionRefinement implements Iterator<Partition> {

    private final ParameterizedSymbol[] inacts;

    private final SplittingTree tree;

    private final RaModel model;

    private Partition currPart;

    private boolean isAcceptable;
    private boolean isStable;
    private boolean isInitial;

    public PartitionRefinement(RaModel model, boolean ioMode, ParameterizedSymbol ... inacts) {
        this.model = model;
        this.inacts = inacts;
        tree = new SplittingTree(model, inacts);
        isAcceptable = tree.isAcceptable();
        isStable = tree.isStable();
        currPart = null;
        isInitial = true;
    }

    public List<Word<DSymbolInstance>> getSuffixes(RALocation raloc, Partition partition) {
        Location loc = model.getLocation(raloc);
        Block block = partition.getRepresentativeBlock(loc);
        return tree.getSeparatingSuffixes(block);
    }

    @Override
    public boolean hasNext() {
        return isInitial || !isAcceptable || !isStable;
    }

    @Override
    public Partition next() {
        if (isInitial) {
            isInitial = false;
            currPart = new Partition(tree.getLeaves(), this.model.getLocations());
            return currPart;
        }
        int maxIndex = tree.maxIndex();

        SplittingIterator split = new SplittingIterator(maxIndex + 1, currPart.getBlocks(), model, inacts);

        if (isAcceptable) {
            // split by state
            while (split.hasNext()) {
                Map.Entry<Leaf, DSymbolInstance> pair = split.next();
                Leaf block = pair.getKey();
                DSymbolInstance in = pair.getValue();
                int k = Integer.max(in.maxIndex(), maxIndex) + 1;
                for (DSymbolInstance out : block.getPossibleOutputs(in, k)) {
                    if (tree.splitByState(block, in, out)) {
                        checkTreeStatus();
                        currPart = new Partition(tree.getLeaves(), model.getLocations());
                        return currPart;
                    }
                }
            }
        } else {
            // split by output
            while (split.hasNext()) {
                Map.Entry<Leaf, DSymbolInstance> pair = split.next();
                if (tree.splitByOutput(pair.getKey(), pair.getValue())) {
                    checkTreeStatus();
                    currPart = new Partition(tree.getLeaves(), model.getLocations());
                    return currPart;
                }
            }
        }
        System.out.println(tree.toString());
        System.out.println(isAcceptable);
        throw new IllegalStateException("Did not find a successful split");
    }

    private void checkTreeStatus() {
        isAcceptable = tree.isAcceptable();
        if (isAcceptable) {
            isStable = tree.isStable();
        }
    }
}
