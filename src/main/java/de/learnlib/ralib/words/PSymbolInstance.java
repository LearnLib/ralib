package de.learnlib.ralib.words;

import de.learnlib.ralib.data.DataValue;
import java.util.Arrays;

/**
 * A concrete data symbol.
 * 
 * @author falk
 */
public class PSymbolInstance {

    /**
     * action
     */
    private final ParameterizedSymbol baseSymbol;
    
    /**
     * concrete parameter values
     */
    private final DataValue[] parameterValues;

    public PSymbolInstance(ParameterizedSymbol baseSymbol, 
            DataValue ... parameterValues) {
        this.baseSymbol = baseSymbol;
        this.parameterValues = parameterValues;
    }
    
    public ParameterizedSymbol getBaseSymbol() {
        return baseSymbol;
    }

    public DataValue[] getParameterValues() {
        return parameterValues;
    }

    @Override
    public String toString() {
        return this.baseSymbol.getName() + Arrays.toString(parameterValues);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PSymbolInstance other = (PSymbolInstance) obj;
        if (this.baseSymbol != other.baseSymbol && (this.baseSymbol == null || !this.baseSymbol.equals(other.baseSymbol))) {
            return false;
        }
        return Arrays.deepEquals(this.parameterValues, other.parameterValues);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 11 * hash + (this.baseSymbol != null ? this.baseSymbol.hashCode() : 0);
        hash = 11 * hash + Arrays.deepHashCode(this.parameterValues);
        return hash;
    }
    
    
}
