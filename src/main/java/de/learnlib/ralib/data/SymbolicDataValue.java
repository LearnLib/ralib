/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.data;

import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.types.BuiltinTypes;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Symbolic Data Values (Parameters, registers, etc.).
 *
 * @author falk
 */
@Deprecated
public abstract class SymbolicDataValue extends Variable<BigDecimal> {

    public static final class Parameter extends SymbolicDataValue {

        public Parameter(DataType dataType, int id) {
            super(dataType, id, "p" + id);
        }
    }

    public static final class Register extends SymbolicDataValue {

        public Register(DataType dataType, int id) {
            super(dataType, id, "r" + id);
        }
    }

    public static final class Constant extends SymbolicDataValue {

        public Constant(DataType dataType, int id) {
            super(dataType, id, "c" + id);
        }
    }

    /*
     * a parameter in a suffix: we should replace those by v_i
     */
    public static final class SuffixValue extends SymbolicDataValue {

        public SuffixValue(DataType dataType, int id) {
            super(dataType, id, "s" + id);
        }
    }

    final DataType type;

    final int id;

    // TODO: id needed?
    private SymbolicDataValue(DataType dataType, int id, String name) {
        super(BuiltinTypes.DECIMAL, name);
        this.type = dataType;
        this.id = id;
    }

    public DataType getDataType() {
        return this.type;
    }

    public int getId() {
        return this.id;
    }

    public static <T extends SymbolicDataValue> T copy(T orig) {
        if (orig.isParameter()) {
            return (T) new Parameter(orig.type, orig.id);
        } else if (orig.isRegister()) {
            return (T) new Register(orig.type, orig.id);
        } else if (orig.isConstant()) {
            return (T) new Constant(orig.type, orig.id);
        } else if (orig.isSuffixValue()) {
            return (T) new SuffixValue(orig.type, orig.id);
        }
        throw new RuntimeException("should not be reachable.");
    }

    @Override
    public String toString() {
        return getName();
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
        return this.id == other.id;
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

    public boolean isSuffixValue() {
        return this.getClass().equals(SuffixValue.class);
    }
}
