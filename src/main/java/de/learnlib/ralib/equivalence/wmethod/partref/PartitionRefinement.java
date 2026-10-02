package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class PartitionRefinement implements Iterator<Partition> {

    private class SplittingIterator implements Iterator<Map.Entry<Leaf, DSymbolInstance>> {

        private List<Leaf> blocks;
        private List<UnquantifiedInputSymbolGenerator> normalGens;
        private List<QuantifiedInputSymbolGenerator> quantGens;

        private int currBlock;
        private int currQuantBlock;

        public SplittingIterator(int startingIndex, int quantStartingIndex, List<Leaf> blocks) {
            this.blocks = blocks;
            normalGens = new ArrayList<>();
            quantGens = new ArrayList<>();
            for (Block block : blocks) {
                normalGens.add(new UnquantifiedInputSymbolGenerator(startingIndex, inacts));
                quantGens.add(new QuantifiedInputSymbolGenerator(startingIndex, quantStartingIndex, block, model, inacts));
            }

            currBlock = 0;
            currQuantBlock = 0;
        }

        @Override
        public boolean hasNext() {
            for (int i = currBlock; i < blocks.size(); i++) {
                if (normalGens.get(i).hasNext()) {
                    return true;
                }
            }
            for (int i = currQuantBlock; i < blocks.size(); i++) {
                if (quantGens.get(i).hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Entry<Leaf, DSymbolInstance> next() {
            if (currBlock < blocks.size()) {
                UnquantifiedInputSymbolGenerator gen = normalGens.get(currBlock);
                assert gen.hasNext();
                DSymbolInstance in = gen.next();
                Leaf block = blocks.get(currBlock);
                advance();
                return Map.entry(block, in);
            }
            assert currQuantBlock < blocks.size();
            QuantifiedInputSymbolGenerator gen = quantGens.get(currQuantBlock);
            assert gen.hasNext();
            DSymbolInstance in = gen.next();
            Leaf block = blocks.get(currQuantBlock);
            advance();
            return Map.entry(block, in);
        }

        private void advance() {
            if (currBlock < blocks.size()) {
                if (!normalGens.get(currBlock).hasNext()) {
                    currBlock++;
                    if (currBlock >= blocks.size()) {
                        advance();
                        return;
                    }
                }
                if (!normalGens.get(currBlock).hasNext()) {
                    advance();
                    return;
                }
            } else if (currQuantBlock < blocks.size()) {
                if (!quantGens.get(currQuantBlock).hasNext()) {
                    currQuantBlock++;
                    if (currQuantBlock >= blocks.size()) {
                        advance();
                        return;
                    }
                }
                if (!quantGens.get(currQuantBlock).hasNext()) {
                    advance();
                    return;
                }
            }
        }
    }

    private final ParameterizedSymbol[] inacts;

    private final SplittingTree tree;

    private final RaModel model;

    private Partition currPart;

    private boolean isAcceptable;
    private boolean isStable;

    public PartitionRefinement(RegisterAutomaton model, ParameterizedSymbol ... inacts) {
        this.inacts = inacts;
        this.model = new RaModel(model);
        tree = new SplittingTree(this.model, inacts);
        isAcceptable = tree.isAcceptable();
        isStable = tree.isStable();
        currPart = new Partition(tree.getLeaves(), this.model.getLocations());
    }

    public List<Word<DSymbolInstance>> getSuffixes(RALocation raloc, Partition partition) {
        Location loc = model.getLocation(raloc);
        Block block = partition.getRepresentativeBlock(loc);
        return tree.getSeparatingSuffixes(block);
    }

    @Override
    public boolean hasNext() {
        return !isAcceptable || !isStable;
    }

    @Override
    public Partition next() {
        int maxIndex = tree.maxIndex();
        int maxQuantifiedSDVIndex = tree.maxQuantifiedSDVIndex();

        SplittingIterator split = new SplittingIterator(maxIndex + 1, maxQuantifiedSDVIndex + 1, currPart.getBlocks());

        if (isAcceptable) {
            // split by state
            while (split.hasNext()) {
                Map.Entry<Leaf, DSymbolInstance> pair = split.next();
                Leaf block = pair.getKey();
                DSymbolInstance in = pair.getValue();
                for (DSymbolInstance out : block.getPossibleOutputs(in)) {
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
        throw new IllegalStateException("Did not find a successful split");
    }

    private void checkTreeStatus() {
        isAcceptable = tree.isAcceptable();
        if (isAcceptable) {
            isStable = tree.isStable();
        }
    }
}
