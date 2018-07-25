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
public class TreeQueryJSON {
    
    private final ConcreteSymbolJSON[] prefix;
    private final SymbolicSymbolJSON[] suffix;

    public TreeQueryJSON(ConcreteSymbolJSON[] prefix, SymbolicSymbolJSON[] suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public ConcreteSymbolJSON[] getPrefix() {
        return prefix;
    }

    public SymbolicSymbolJSON[] getSuffix() {
        return suffix;
    }
    
    
}
