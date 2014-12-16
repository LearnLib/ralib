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
public class SymbolicDataValue extends DataValue<Integer> {
    
    public static enum ValueClass {PARAMETER, SUFFIX, REGISTER, TEMP}
    
    private final ValueClass valueClass;
    
    
    
    public SymbolicDataValue(ValueClass valueType, DataType dataType, int id) {
        super(dataType, id);
        this.valueClass = valueType;
    }

    public String toStringWithType() {
        return this.toString() + ":" + this.type.getName();
    }

    @Override
    public String toString() {
        String s = "";
        switch (this.valueClass) {
            case PARAMETER: s += "p"; break;
            case SUFFIX:    s += "s"; break;
            case REGISTER:  s += "r"; break;
            case TEMP:      s += "t"; break;   
        }
        return s + this.id;
    }
    
    public ValueClass getVC() {
        return this.valueClass;
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
        if (this.valueClass != other.valueClass) {
            return false;
        }
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
        hash = 67 * hash + Objects.hashCode(this.valueClass);
        return hash;
    }

    
    
    public static SymbolicDataValue suffix(DataType t, int id) {
        return new SymbolicDataValue(ValueClass.SUFFIX, t, id);
    }

    public static SymbolicDataValue register(DataType t, int id) {
        return new SymbolicDataValue(ValueClass.REGISTER, t, id);
    }

    public static SymbolicDataValue temp(DataType t, int id) {
        return new SymbolicDataValue(ValueClass.TEMP, t, id);
    }
    
    public static SymbolicDataValue parameter(DataType t, int id) {
        return new SymbolicDataValue(ValueClass.PARAMETER, t, id);
    }
    
}
