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

import java.util.Objects;

/**
 *
 * @author falk
 * @param <T>
 */
public class DataValue<T> {

    protected final DataType type;

    protected final T id;

    public DataValue(DataType type, T id) {
        this.type = type;
        this.id = id;
    }


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
        if (!(obj instanceof DataValue)) {
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

    public static <P> DataValue<P> valueOf(String strVal, DataType type) {
    	return new DataValue(type, valueOf(strVal, type.getBase()));
    }

    public static <P> P valueOf(String strVal, Class<P> cls) {
    	P realValue = null;
    	if (Number.class.isAssignableFrom(cls)) {
    		Object objVal;
    		try {
    			objVal = cls.getMethod("valueOf", String.class).invoke(cls, strVal);

    			realValue = cls.cast(objVal);
    		} catch (Exception e) {
    			throw new RuntimeException(e);
    		}
    	} else {
    		if (cls.isPrimitive()) {
    			if (cls.equals(int.class))
    				return (P) Integer.valueOf(strVal);
    			else if (cls.equals(double.class))
    				return (P) Double.valueOf(strVal);
    			else if (cls.equals(long.class))
    				return (P) Long.valueOf(strVal);
    		}
    		throw new RuntimeException("Cannot deserialize values of the class " + cls);
    	}
    	return realValue;
    }

}
