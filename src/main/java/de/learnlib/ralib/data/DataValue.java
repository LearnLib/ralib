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

import java.math.BigDecimal;
import java.util.Objects;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.types.BuiltinTypes;

/**
 * RaLib extension of SMT constant values that
 * retains user-annotated type information
 *
 * @author falk
 */
public class DataValue extends Constant<BigDecimal> implements
        TypedValue, SDTGuardElement, Comparable<DataValue> {

    protected final DataType type;

    public DataValue(DataType type, BigDecimal value) {
        super(BuiltinTypes.DECIMAL, value);
        this.type = type;
    }

    @Override
    public String toString() {
        return getValue().toString() + "[" + this.type.getName() + "]";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + Objects.hashCode(this.getValue());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DataValue other)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        return this.getValue().equals(other.getValue());
    }

    @Override
    public DataType getDataType() {
        return type;
    }

    @Override
    public BigDecimal getValue() {
        return super.getValue();
    }
    public static DataValue valueOf(String strVal, DataType type) {
    	return new DataValue(type, new BigDecimal(strVal));
    }

    @Override
    public Expression<BigDecimal> asExpression() {
        return this;
    }

    @Override
    public int compareTo(DataValue o) {
        int tc = this.type.compareTo(o.type);
        return tc != 0 ? tc : this.getValue().compareTo(o.getValue());
    }
}
