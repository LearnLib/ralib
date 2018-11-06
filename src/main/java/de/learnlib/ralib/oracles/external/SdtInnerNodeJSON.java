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
public class SdtInnerNodeJSON extends SdtJSON {
 
    private final GuardedSubTreeJSON[] children;

    public SdtInnerNodeJSON(GuardedSubTreeJSON ... children) {
        super(TYPE_INNER);
        this.children = children;
    }

    public GuardedSubTreeJSON[] getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return Arrays.toString(children);
    }
   

}
