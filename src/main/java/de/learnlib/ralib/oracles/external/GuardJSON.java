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
public class GuardJSON {
    
    private final SDTVariableJSON parameter;
    private final SDTVariableJSON other;    
    private final String comparator;

    public GuardJSON(SDTVariableJSON parameter, SDTVariableJSON other, String comparator) {
        this.parameter = parameter;
        this.other = other;
        this.comparator = comparator;
    }

    /**
     * @return the parameter
     */
    public SDTVariableJSON getParameter() {
        return parameter;
    }

    /**
     * @return the other
     */
    public SDTVariableJSON getOther() {
        return other;
    }

    /**
     * @return the comparator
     */
    public String getComparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return "" + other + " " + comparator + " " + parameter;
    }
}
