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
public class GuardedSubTreeJSON {
 
    private final GuardJSON[] guards;
    private final SdtJSON tree;

    public GuardedSubTreeJSON(GuardJSON[] guards, SdtJSON tree) {
        this.guards = guards;
        this.tree = tree;
    }

    /**
     * @return the guards
     */
    public GuardJSON[] getGuards() {
        return guards;
    }

    /**
     * @return the tree
     */
    public SdtJSON getTree() {
        return tree;
    }

    @Override
    public String toString() {
        return "{" + Arrays.toString(guards) + ", " + tree + "}";
    }
    
    
}
