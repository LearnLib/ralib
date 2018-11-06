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
public class SdtJSON {
 
    public final static String TYPE_INNER = "inner";
    public final static String TYPE_LEAF = "leaf";
        
    private final String type;

    public SdtJSON(String type) {
        this.type = type;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }
   
}
