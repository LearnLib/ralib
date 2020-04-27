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
 * DataValues comprise a type (for parameter typing) and a concrete value. 
 * The DataValue class is subclassed by specific DataValue classes, which add
 * information on how the DataValue was formed. This information is semi-symbolic,
 * and allows for the respective DataValue to be re-instantiated from a different set
 * of values.  
 * 
 * Equality for DataValues and all their subclassed instances,  is fully defined by 
 * the type and the concrete value. Consequently, a SumC value 11 (10+1) is equal to
 * a DataValue 11 of the same type.
 * @param <T>
 */
public class DataValue<T> {
    
    protected final DataType type;

    protected final T id;
    
    public static <P> DataValue<P> ONE(DataType type, Class<P> domainType) {
    	return new DataValue<P>(type, cast(1, domainType));
    }
    
    public static <P> DataValue<P> ZERO(DataType type, Class<P> domainType) {
    	return new DataValue<P>(type, cast(0, domainType));
    }
    
    public static <P> P CONST(int val, Class<P> type) {
    	return cast(val, type);
    }
    
    /**
     * FIXME All calls to this method must amended. Currently, the class used to deserialize the string is the base type. 
     */
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
    
    public static DataValue<?> add(DataValue<?> rv, DataValue<?> lv) {
    	if (rv == null) return lv;
    	if (lv == null) return rv;
    	Object sumValue = add(rv.getType().getBase(), rv.id, lv.getId());
    	return new DataValue(rv.getType(), cast(sumValue, rv.getIdType()));
    }
    
    public static DataValue<?> sub(DataValue<?> rv, DataValue<?> lv) {
    	if (rv == null) return sub(new DataValue(lv.getType(), 0.0), lv);
    	if (lv == null) return rv;
    	Object subValue = sub(rv.getType().getBase(), rv.id, lv.getId());
    	return new DataValue(rv.getType(), cast(subValue, rv.getIdType()) );
    }
    
    protected static <NT> NT add(Class<NT> cls, Object a, Object b) {
    	Number val;
    	if (a instanceof Integer) {
			val = Math.addExact(((Integer) a),((Integer) b));
		} else {
			if (a instanceof Double) 
				val = ((Double) a) + ((Double) b);
			else if (a instanceof Long) 
				val = ((Long) a) + ((Long) b);
			else
				throw new RuntimeException("Unsupported type " + a.getClass());
		}
    	return cls.cast(val);
    }
    
    protected static <NT> NT sub(Class<NT> cls, Object a, Object b) {
    	Number val;
    	if (a instanceof Integer) {
			val = Math.subtractExact(((Integer) a),((Integer) b));
		} else {
			if (a instanceof Double) {
				val = ((Double) a) - ((Double) b);
			} else if (a instanceof Long) 
				val = ((Long) a) - ((Long) b);
			else
				throw new RuntimeException("Unsupported type " + a.getClass());
		}
    	return cls.cast(val);
    }
    
    public static <T> T cast(Object numObject, Class<T> cls) {
    	if (cls == numObject.getClass()) {
    		return cls.cast(numObject);
    	}
    	if (Number.class.isAssignableFrom(cls) && numObject instanceof Number) {
    		Number number = (Number)(numObject);
    		if (cls == Integer.class) {
    			return cls.cast(new Integer(number.intValue()));
    		} else {
    			if (cls == Double.class) {
    				return cls.cast(new Double(number.doubleValue()));
    			} else if (cls == Long.class) {
    				return cls.cast(new Long(number.longValue()));
    			}
    		}
    	} else {
    		throw new RuntimeException("Cast not supported for " + cls + " on object " + numObject );
    	}
    	return null;
    }
    
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
    
    public Class<T> getIdType() {
    	return (Class<T>) id.getClass();
    }
}
