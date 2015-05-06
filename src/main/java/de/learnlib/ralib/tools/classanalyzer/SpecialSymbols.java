/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.tools.classanalyzer;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.words.OutputSymbol;

/**
 *
 * @author falk
 */
public class SpecialSymbols {

    static final class ErrorSymbol extends OutputSymbol {
        
        private final Throwable error; 
                
        ErrorSymbol(Throwable error) {
            super("__ERR");
            this.error = error;
        }
        
        
        @Override
        public String toString() {
            return "E_" + this.error.getClass().getSimpleName();
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            return getClass() == obj.getClass();
        }        
    };
    
    public static final ErrorSymbol ERROR = new ErrorSymbol(new Exception("__dummy"));
    
    public static final OutputSymbol NULL = new OutputSymbol("NULL");
        
    public static final OutputSymbol VOID = new OutputSymbol("V");

    public static final OutputSymbol DEPTH = new OutputSymbol("MAXD");

    public static final OutputSymbol TRUE = new OutputSymbol("TRUE");

    public static final OutputSymbol FALSE = new OutputSymbol("FALSE");
    
    public static final DataType BOOLEAN_TYPE = new DataType("boolean", boolean.class);    
}
