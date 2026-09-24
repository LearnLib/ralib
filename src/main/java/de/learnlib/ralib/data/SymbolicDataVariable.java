package de.learnlib.ralib.data;

import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;

public sealed interface SymbolicDataVariable extends TypedValue permits SDV, QuantifiedSDV {

    public DataType getDataType();

    public int getId();
}
