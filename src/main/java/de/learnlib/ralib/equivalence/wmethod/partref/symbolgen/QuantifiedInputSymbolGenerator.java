package de.learnlib.ralib.equivalence.wmethod.partref.symbolgen;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.Block;
import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class QuantifiedInputSymbolGenerator implements Iterator<DSymbolInstance> {

    private final ParameterizedSymbol[] inacts;
    private final Block block;
    private final RaModel model;
    private final int startIndex;

    private final Set<DSymbolInstance> done;
    private final Map<ParameterizedSymbol, List<Integer>> guardedParams;
    private final int maxQuant;

    private InputSymbolGenerator gen;
    private BooleanArrayIterator quantPositionIt;
    private DSymbolInstance currBase;
    private DSymbolInstance next;
    private int numberOfQuantified;
    private boolean hasNext;

    public QuantifiedInputSymbolGenerator(int startIndex, Block block, RaModel model, ParameterizedSymbol ... inacts) {
        this.inacts = inacts;
        this.startIndex = startIndex;
        this.block = block;
        this.model = model;

        done = new LinkedHashSet<>();
        guardedParams = InputSymbolGenerator.findGuardedParams(block, model, inacts);
        int max = 0;
        for (List<Integer> guardedParamList : guardedParams.values()) {
            max = Integer.max(max, guardedParamList.size());
        }
        maxQuant = max;
        gen = new InputSymbolGenerator(startIndex, block, model, inacts, guardedParams);
        numberOfQuantified = 1;
        hasNext = true;
        while (hasNext) {
            if (gen.hasNext()) {
                currBase = gen.next();
                List <Integer> params = gen.guardedParams.get(currBase.getBaseSymbol());
                if (!params.isEmpty()) {
                    quantPositionIt = new BooleanArrayIterator(params.size(), 1);
                    break;
                }
            } else {
                hasNext = false;
            }
        }
        assert !hasNext || quantPositionIt != null;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public DSymbolInstance next() {
        DSymbolInstance ret = next;
        if (ret != null) {
            done.add(ret);
        }

        while (hasNext) {
            next = getNext();
            hasNext = advance();
            if (ret != null && !done.contains(next)) {
                break;
            }
            if (ret == null) {
                ret = next;
                done.add(ret);
            }
        }

        return ret;
    }

    private DSymbolInstance getNext() {
        int arity = currBase.getBaseSymbol().getArity();
        DataType[] types = currBase.getBaseSymbol().getPtypes();
        SymbolicDataVariable[] baseVars = currBase.getSymbolicValues();

        boolean[] quantPos = quantPositionIt.next();
        List<Integer> guarded = guardedParams.get(currBase.getBaseSymbol());
        boolean[] isQuant = new boolean[arity];
        int index = 0;
        for (int i = 0; i < arity; i++) {
            if (guarded.contains(i)) {
                isQuant[i] = quantPos[index++];
            } else {
                isQuant[i] = false;
            }
        }

        SymbolicDataVariable[] vars = new SymbolicDataVariable[arity];
        for (int i = 0; i < arity; i++) {
            if (isQuant[i]) {
                vars[i] = new QuantifiedSDV(types[i], startIndex + i);
            } else {
                vars[i] = baseVars[i];
            }
        }
        return new DSymbolInstance(currBase.getBaseSymbol(), vars);
    }

    private boolean advance() {
        if (quantPositionIt == null) {
            int size = gen.guardedParams.get(currBase.getBaseSymbol()).size();
            if (size >= numberOfQuantified) {
                quantPositionIt = new BooleanArrayIterator(size, numberOfQuantified);
                return advance();
            }
        } else if (quantPositionIt.hasNext()) {
            return true;
        }

        if (gen.hasNext()) {
            currBase = gen.next();
            quantPositionIt = null;
            return advance();
        }

        if (numberOfQuantified < maxQuant) {
            numberOfQuantified++;
            gen = new InputSymbolGenerator(startIndex, block, model, inacts, guardedParams);
            return advance();
        }

        return false;
    }
}
