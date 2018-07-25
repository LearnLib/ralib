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
public class SdtJSON {
 
    private final GuardedSubTreeJSON[] children;
    private final boolean accepting;

    public SdtJSON(GuardedSubTreeJSON[] children, boolean accepting) {
        this.children = children;
        this.accepting = accepting;
    }

    public GuardedSubTreeJSON[] getChildren() {
        return children;
    }

    public boolean isAccepting() {
        return accepting;
    }

    @Override
    public String toString() {
        return Arrays.toString(children) + ":" + accepting;
    }
   

}
