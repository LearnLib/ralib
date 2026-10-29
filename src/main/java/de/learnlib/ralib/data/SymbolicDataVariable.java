package de.learnlib.ralib.data;

import de.learnlib.ralib.data.SymbolicDataValue.FreshSDV;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;

public sealed interface SymbolicDataVariable extends TypedValue permits SDV, QuantifiedSDV, FreshSDV {

    public int getId();

    public SymbolicDataVariable offset(int k);
}
