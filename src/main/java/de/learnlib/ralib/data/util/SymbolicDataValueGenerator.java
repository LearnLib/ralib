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

package de.learnlib.ralib.data.util;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;

/**
 * Generates symbolic data values with increasing ids starting from id=1.
 * 
 * @author falk
 */
public abstract class SymbolicDataValueGenerator {
    
    protected int id = 1;
    
    private SymbolicDataValueGenerator() {
    }
        
    public abstract SymbolicDataValue next(DataType type);
    
    public static final class ParameterGenerator extends SymbolicDataValueGenerator {
        @Override
        public SymbolicDataValue.Parameter next(DataType type) {
            return new SymbolicDataValue.Parameter(type, id++);
        }        
    };

    public static final class RegisterGenerator extends SymbolicDataValueGenerator {
        @Override
        public SymbolicDataValue.Register next(DataType type) {
            return new SymbolicDataValue.Register(type, id++);
        }        
    };   
    
    public static final class SuffixValueGenerator extends SymbolicDataValueGenerator {
        @Override
        public SymbolicDataValue.SuffixValue next(DataType type) {
            return new SymbolicDataValue.SuffixValue(type, id++);
        }        
    };  
    
    public static final class ConstantGenerator extends SymbolicDataValueGenerator {
        @Override
        public SymbolicDataValue.Constant next(DataType type) {
            return new SymbolicDataValue.Constant(type, id++);
        }        
    };     
}
