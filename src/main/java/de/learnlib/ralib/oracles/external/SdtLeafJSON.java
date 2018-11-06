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
public class SdtLeafJSON extends SdtJSON {
 
    private final boolean accepting;

    public SdtLeafJSON(boolean accepting) {
        super(TYPE_LEAF);
        this.accepting = accepting;
    }


    public boolean isAccepting() {
        return accepting;
    }

    @Override
    public String toString() {
        return "" + accepting;
    }
   

}
