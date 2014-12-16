/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.data;

import java.util.Objects;

/**
 *
 * @author falk
 */
public abstract class SymbolicDataValue extends DataValue<Integer> {

        
    public static final class Parameter extends SymbolicDataValue {
        @Deprecated()
        public Parameter(DataType dataType, int id) {
            super(dataType, id);
        }        
    }; 
            
    public static final class Register extends SymbolicDataValue {
        @Deprecated()
        public Register(DataType dataType, int id) {
            super(dataType, id);
        }        
    };         

    public static final class Constant extends SymbolicDataValue {
        @Deprecated()
        public Constant(DataType dataType, int id) {
            super(dataType, id);
        }        
    };   
    
    public static final class SuffixValue extends SymbolicDataValue {
        @Deprecated()
        public SuffixValue(DataType dataType, int id) {
            super(dataType, id);
        }        
    };     
    
    private SymbolicDataValue(DataType dataType, int id) {
        super(dataType, id);
    }

    public String toStringWithType() {
        return this.toString() + ":" + this.type.getName();
    }

    @Override
    public String toString() {
        String s = "";
        if (this.isParameter()) {
            s += "p"; 
        } else if (this.isRegister()) {
            s += "r";             
        }
        return s + this.id;
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SymbolicDataValue other = (SymbolicDataValue) obj;
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.getClass());
        return hash;
    }

    public boolean isRegister() {
        return this.getClass().equals(Register.class);
    }

    public boolean isParameter() {
        return this.getClass().equals(Parameter.class);
    }    

    public boolean isConstant() {
        return this.getClass().equals(Constant.class);
    }

}
