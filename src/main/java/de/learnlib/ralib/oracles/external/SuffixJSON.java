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
public class SuffixJSON {
  
    private final String action;
    private final int parameters;

    public SuffixJSON(String action, int parameters) {
        this.action = action;
        this.parameters = parameters;
    }

    public int getParameters() {
        return parameters;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return action + ":" + parameters;
    }
    
}
