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
public class ConcreteSymbolJSON {
    
    private final String symbol;
    private final int[] concreteParameter;

    public ConcreteSymbolJSON(String symbol, int ... concreteParameter) {
        this.symbol = symbol;
        this.concreteParameter = concreteParameter;
    }

    public String getSymbol() {
        return symbol;
    }

    public int[] getConcreteParameter() {
        return concreteParameter;
    }
    
}
