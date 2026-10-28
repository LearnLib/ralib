package de.learnlib.ralib.equivalence.wmethod.partref.symbolgen;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class AllEqInputSymbolIterator implements Iterator<DSymbolInstance> {

    private final ParameterizedSymbol[] inacts;
    private final int startIndex;
    private Map<ParameterizedSymbol, List<Integer>> guardedParams;

    private int currAct;

    public AllEqInputSymbolIterator(int startIndex, Map<ParameterizedSymbol, List<Integer>> guardedParams, ParameterizedSymbol ... inacts) {
        this.inacts = inacts;
        this.startIndex = startIndex;
        this.guardedParams = guardedParams;
        currAct = 0;
    }

    @Override
    public boolean hasNext() {
        return currAct < inacts.length;
    }

    @Override
    public DSymbolInstance next() {
        ParameterizedSymbol inact = inacts[currAct];
        List<Integer> guarded = guardedParams.get(inact);
        DataType[] types = inact.getPtypes();
        SymbolicDataVariable[] vars = new SymbolicDataVariable[inact.getArity()];
        for (int i = 0; i < inact.getArity(); i++) {
            if (guarded.contains(i)) {
                vars[i] = new QuantifiedSDV(types[i], i + startIndex);
            } else {
                vars[i] = new SDV(types[i], i + startIndex);
            }
        }
        currAct++;
        return new DSymbolInstance(inact, vars);
    }
}
