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

import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import java.util.Objects;

/**
 * Symbolic Data Values (Parameters, registers, etc.).
 *
 * @author falk
 */
public abstract class SymbolicDataValue extends DataValue<Integer> {

    public static final class Parameter extends SymbolicDataValue {

        public Parameter(DataType dataType, int id) {
            super(dataType, id);
        }

        public boolean equals(Parameter other) {
            return (this.getType().equals(other.getType()) && this.getId().equals(other.getId()));

        }
    };

    public static final class Register extends SymbolicDataValue {

        public Register(DataType dataType, int id) {
            super(dataType, id);
        }
    };

    public static final class Constant extends SymbolicDataValue {

        public Constant(DataType dataType, int id) {
            super(dataType, id);
        }
    };

    public static final class SuffixValue extends SymbolicDataValue {

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
        } else if (this.isSuffixValue()) {
            s += "s";
        } else if (this.isConstant()) {
            s += "c";
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

    public Variable toVariable() {
        return new Variable(BuiltinTypes.DOUBLE, this.toString());
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

    public boolean isSuffixValue() {
        return this.getClass().equals(SuffixValue.class);
    }

}
