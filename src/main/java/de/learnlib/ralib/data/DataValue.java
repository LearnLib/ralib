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
 * @param <T>
 */
public class DataValue<T>{
    
    protected final DataType type;

    protected final T id;

    public DataValue(DataType type, T id) {
        this.type = type;
        this.id = id;
    }
    
//    @Override
//    public int compareTo(DataValue<T> other) {
//        Integer thint = (Integer) this.getId();
//        Integer oint = (Integer) other.getId();
//        return Integer.compare(thint,oint);
//    }
//       
    @Override
    public String toString() {
        return id.toString() + "[" + this.type.getName() + "]"; 
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + Objects.hashCode(this.id);
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
        final DataValue other = (DataValue) obj;
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

    public T getId() {
        return id;
    }

    public DataType getType() {
        return type;
    }
    
        
}
