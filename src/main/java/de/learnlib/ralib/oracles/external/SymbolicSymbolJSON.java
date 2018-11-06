/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import java.util.Arrays;

/**
 *
 * @author falk
 */
public class SymbolicSymbolJSON {

    private final String symbol;
    private final DataValueSuffixJSON[] parameters;

    public SymbolicSymbolJSON(String symbol, DataValueSuffixJSON ... parameters) {
        this.symbol = symbol;
        this.parameters = parameters;
    }

    public String getSymbol() {
        return symbol;
    }

    public DataValueSuffixJSON[] getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "" + symbol + Arrays.toString(parameters);
    }    
}
