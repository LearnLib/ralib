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
public class PIVElementJSON {

    private final SDTVariableJSON key;
    private final DataValuePrefixJSON value;     

    public PIVElementJSON(SDTVariableJSON key, DataValuePrefixJSON value) {
        this.key = key;
        this.value = value;
    }

    public SDTVariableJSON getKey() {
        return key;
    }

    public DataValuePrefixJSON getValue() {
        return value;
    }
    
    
}
