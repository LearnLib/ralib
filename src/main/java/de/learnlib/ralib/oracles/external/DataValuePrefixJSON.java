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
public class DataValuePrefixJSON {
    
    public static final String TYPE_CONSTANT = "constant";
    public static final String TYPE_CONCRETE = "concrete";
    
    private final String type;    
    private final int id;

    public DataValuePrefixJSON(String type, int id) {
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
    public int getID() {
        return id;
    }

    @Override
    public String toString() {
        switch (type) {
            case TYPE_CONCRETE:
                return "d" + id;
            case TYPE_CONSTANT:
                return "c" + id;
            default:
                throw new IllegalStateException("Illegal type.");
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + Objects.hashCode(this.type);
        hash = 13 * hash + this.id;
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
        final DataValuePrefixJSON other = (DataValuePrefixJSON) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        return true;
    }

}
