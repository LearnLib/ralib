/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import java.util.Objects;

/**
 *
 * @author falk
 */
public class SDTVariableJSON {
    
    public static final String TYPE_CONSTANT = "constant";
    public static final String TYPE_GUARD_PARAM = "parameter";
    public static final String TYPE_SDT_REGISTER = "register";
    
    private final String type;    
    private final int id;

    public SDTVariableJSON(String type, int id) {
        this.type = type;
        this.id = id;
    }
    
    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        switch (type) {
            case TYPE_CONSTANT:
                return "c" + id;
            case TYPE_GUARD_PARAM:
                return "p" + id;
            case TYPE_SDT_REGISTER:
                return "r" + id;                
            default:
                throw new IllegalStateException("Illegal type.");
        }
    }   

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.type);
        hash = 43 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SDTVariableJSON other = (SDTVariableJSON) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        return true;
    }
    

}
