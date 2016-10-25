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
 * @author falk
 * @param <T>
 */
public class DataValue<T> {
    
    protected final DataType type;

    protected final T id;
    
    public static DataValue<?> add(DataValue<?> rv, DataValue<?> lv) {
    	if (rv == null) return lv;
    	if (lv == null) return rv;
    	double sumValue = Double.valueOf(rv.getId().toString()) + Double.valueOf(lv.getId().toString());
    	return new DataValue(rv.getType(), cast(sumValue, rv.getType()));
    }
    
    public static DataValue<?> sub(DataValue<?> rv, DataValue<?> lv) {
    	if (rv == null) return sub(new DataValue(lv.getType(), 0.0), lv);
    	if (lv == null) return rv;
    	double subValue = Double.valueOf(rv.getId().toString()) - Double.valueOf(lv.getId().toString());
    	
    	return new DataValue(rv.getType(), cast(subValue, rv.getType()) );
    }
    
    protected static <NT extends Number> NT add(NT a, NT b) {
    	Number val;
    	if (a.getClass() == Integer.class) {
			val = ((Integer) a) - ((Integer) b);
		} else {
			if (a.getClass() == Double.class) {
				val = ((Integer) a) - ((Integer) b);
			} else 
				throw new RuntimeException("Unsupported type " + a.getClass());
		}
    	return null;
    }
    
    
    
    
    // can be made using reflection but this way is probably faster
    public static <T> T cast(Object numObject, DataType toType) {
    	if (toType.getBase() == numObject.getClass()) {
    		return (T) numObject;
    	}
    	Class baseType = toType.getBase();
    	String objString = numObject.toString();
    	if (Number.class.isAssignableFrom(baseType) && numObject instanceof Number) {
    		Number number = (Number)(numObject);
    		if (baseType == Integer.class) {
    			return (T) new Integer(number.intValue());
    		} else {
    			if (baseType == Double.class) {
    				return (T) new Double(number.doubleValue());
    			}
    		}
    	} else {
    		throw new RuntimeException("Cast not supported for " + toType + " on object " + numObject );
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
    
        
}
