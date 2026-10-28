package de.learnlib.ralib.equivalence.wmethod.partref.symbolgen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.learnlib.ralib.equivalence.wmethod.partref.Block;
import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.equivalence.wmethod.partref.Leaf;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class SplittingIterator implements Iterator<Map.Entry<Leaf, DSymbolInstance>> {

    private List<Leaf> blocks;
    private List<Iterator<DSymbolInstance>> normalGens;
    private List<Iterator<DSymbolInstance>> quantGens;

    private int currBlock;
    private int currQuantBlock;

    public SplittingIterator(int startingIndex, List<Leaf> blocks, RaModel model, ParameterizedSymbol ... inacts) {
        this.blocks = blocks;
        normalGens = new ArrayList<>();
        quantGens = new ArrayList<>();
        for (Block block : blocks) {
            InputSymbolGenerator isg = new InputSymbolGenerator(startingIndex, block, model, inacts);
            normalGens.add(isg);
            quantGens.add(new AllEqInputSymbolIterator(startingIndex, isg.guardedParams, inacts));
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
            Iterator<DSymbolInstance> gen = normalGens.get(currBlock);
            assert gen.hasNext();
            DSymbolInstance in = gen.next();
            Leaf block = blocks.get(currBlock);
            advance();
            return Map.entry(block, in);
        }
        assert currQuantBlock < blocks.size();
        Iterator<DSymbolInstance> gen = quantGens.get(currQuantBlock);
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
