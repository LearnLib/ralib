/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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

import java.math.BigDecimal;
import java.util.Objects;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.types.BuiltinTypes;

/**
 * Symbolic Data Values (Parameters, registers, etc.).
 *
 * @author falk
 */
public sealed abstract class SymbolicDataValue extends Variable<BigDecimal> implements TypedValue permits
        SymbolicDataValue.Parameter, SymbolicDataValue.Constant, SymbolicDataValue.Register, SymbolicDataValue.SuffixValue {

    /**
     * a data parameter of an action
     */
    public static final class Parameter extends SymbolicDataValue {

        public Parameter(DataType dataType, int id) {
            super(dataType, id, "p" + id);
        }
    }

    /**
     * a register in a register automaton
     */
    public static final class Register extends SymbolicDataValue implements SDTGuardElement {

        public Register(DataType dataType, int id) {
            super(dataType, id, "r" + id);
        }

	@Override
	public Expression<BigDecimal> asExpression() {
	    return this;
	}
    }

    /**
     * a named constant in some theory
     */
    public static final class Constant extends SymbolicDataValue implements SDTGuardElement {

        public Constant(DataType dataType, int id) {
            super(dataType, id, "c" + id);
        }

        @Override
        public Expression<BigDecimal> asExpression() {
            return this;
        }
    }

    /**
     * a parameter in a suffix or SDT guard
     */
    // todo: we should replace those by v_i
    public static final class SuffixValue extends SymbolicDataValue implements SDTGuardElement {

        public SuffixValue(DataType dataType, int id) {
            super(dataType, id, "s" + id);
        }

        @Override
        public Expression<BigDecimal> asExpression() {
            return this;
        }
    }

    final DataType type;

    final int id;

    private SymbolicDataValue(DataType dataType, int id, String name) {
        super(BuiltinTypes.DECIMAL, name);
        this.type = dataType;
        this.id = id;
    }

    @Override
    public DataType getDataType() {
        return this.type;
    }

    public int getId() {
        return this.id;
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
        hash = 97 * hash + Integer.hashCode(this.id);
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
