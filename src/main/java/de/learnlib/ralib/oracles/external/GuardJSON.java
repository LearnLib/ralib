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
    
    private final int parameter;
    private final int register;
    private final boolean equality;

    public GuardJSON(int parameter, int register, boolean equality) {
        this.parameter = parameter;
        this.register = register;
        this.equality = equality;
    }

    /**
     * @return the parameter
     */
    public int getParameter() {
        return parameter;
    }

    /**
     * @return the register
     */
    public int getRegister() {
        return register;
    }

    /**
     * @return the equality
     */
    public boolean isEquality() {
        return equality;
    }

    @Override
    public String toString() {
        return "r" + register + " " + (equality ? "==" : "!=") + " p" + parameter;
    }
    
}
