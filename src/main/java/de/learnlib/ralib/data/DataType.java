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

//import java.lang.reflect.Method;
import java.util.Objects;

/**
 * A user-defined type of data values. 
 * 
 * @author falk
 */
public abstract class DataType {

    /**
     * name of type (defining member)
     */
    protected final String name;
    
    /**
     * base type
     */
    protected final Class base;

    protected DataType(String name, Class base) {
        this.name = name;
        this.base = base;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataType other = (DataType) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }
        
    public String getName() {
        return name;
    }
    
    public Class getBase() {
        return base;
    }
       
}

