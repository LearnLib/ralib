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
public class DataValueSuffixJSON {
    
    public static final String TYPE_SYMBOLIC = "symbolic";
    public static final String TYPE_DONTCARE = "dontcare";
    
    private final String type;
    private final int id;

    public DataValueSuffixJSON(String type, int id) {
        this.type = type;
        this.id = id;
    }
    
    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    public int getId() {
        return id;
    }
    
    @Override
    public String toString() {
        switch (type) {
            case TYPE_SYMBOLIC:
                return "p" + id + "(s)";
            case TYPE_DONTCARE:
                return "p" + id + "(n)";
            default:
                throw new IllegalStateException("Illegal type.");
        }
    }    
}
