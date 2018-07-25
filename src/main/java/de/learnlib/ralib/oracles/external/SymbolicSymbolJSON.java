/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

/**
 *
 * @author falk
 */
public class SymbolicSymbolJSON {

    private final String symbol;
    private final int symbolicParameter;

    public SymbolicSymbolJSON(String symbol, int symbolicParameter) {
        this.symbol = symbol;
        this.symbolicParameter = symbolicParameter;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getSymbolicParameter() {
        return symbolicParameter;
    }
    
}
