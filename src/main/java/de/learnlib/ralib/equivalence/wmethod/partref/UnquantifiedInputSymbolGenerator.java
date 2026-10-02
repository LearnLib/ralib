package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.Iterator;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class UnquantifiedInputSymbolGenerator implements Iterator<DSymbolInstance> {

    private final ParameterizedSymbol[] inacts;
    private final int startIndex;
    private int currAct;

    public UnquantifiedInputSymbolGenerator(int startIndex, ParameterizedSymbol[] inacts) {
        this.inacts = inacts;
        this.startIndex = startIndex;
        currAct = 0;
    }

    @Override
    public boolean hasNext() {
        return currAct < inacts.length;
    }

    @Override
    public DSymbolInstance next() {
        ParameterizedSymbol inact = inacts[currAct];
        SymbolicDataVariable[] vars = new SymbolicDataVariable[inact.getArity()];
        DataType[] types = inact.getPtypes();
        for (int i = 0; i < inacts[currAct].getArity(); i++) {
            vars[i] = new SDV(types[i], startIndex + i);
        }
        currAct++;
        return new DSymbolInstance(inact, vars);
    }
}
