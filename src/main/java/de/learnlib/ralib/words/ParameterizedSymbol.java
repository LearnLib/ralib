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
package de.learnlib.ralib.words;

import de.learnlib.ralib.data.DataType;
import java.util.Arrays;

/**
 * A symbol with typed parameters 
 * (sometimes called an action). 
 * 
 * @author falk
 */
public class ParameterizedSymbol {

    /**
     * name of symbol
     */
    private final String name;
    
    /**
     * parameter types
     */
    private final DataType[] ptypes;

    public ParameterizedSymbol(String name, DataType[] ptypes) {
        this.name = name;
        this.ptypes = ptypes;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 71 * hash + Arrays.deepHashCode(this.ptypes);
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
        final ParameterizedSymbol other = (ParameterizedSymbol) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return Arrays.deepEquals(this.ptypes, other.ptypes);
    }

    @Override
    public String toString() {
        String[] tnames = new String[this.ptypes.length]; 
        for (int i = 0; i < this.ptypes.length; i++) {
            tnames[i] = ptypes[i].getName();
        }
        return this.name + Arrays.toString(tnames);
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the arity
     */
    public int getArity() {
        return this.ptypes.length;
    }

    public DataType[] getPtypes() {
        return ptypes;
    }
    
}
