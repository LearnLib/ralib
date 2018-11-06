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
public class ConcreteSymbolJSON {
    
    private final String symbol;
    private final DataValuePrefixJSON[] dataValues;

    public ConcreteSymbolJSON(String symbol, DataValuePrefixJSON ... dataValues) {
        this.symbol = symbol;
        this.dataValues = dataValues;
    }

    public String getSymbol() {
        return symbol;
    }

    public DataValuePrefixJSON[] getDataValues() {
        return dataValues;
    }

    @Override
    public String toString() {
        return "" + symbol + Arrays.toString(dataValues);
    }


}
