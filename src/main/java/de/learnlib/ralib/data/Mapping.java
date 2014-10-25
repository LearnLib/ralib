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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 * @author falk
 * @param <K>
 * @param <V>
 */
public class Mapping<K, V extends DataValue<?>> extends LinkedHashMap<K, V> {

    public Mapping<K, V> createCopy() {
        Mapping val = new Mapping();
        val.putAll(this);
        return val;
    }    
    
    public <T> Collection<DataValue<T>> values(DataType type) {
        List<DataValue<T>> list = new ArrayList<>();
        for (DataValue<?> v : values()) {
            if (v.type.equals(type)) {
                list.add((DataValue<T>) v);
            }
        }
        return list;
    }
    
    public K getKey(DataValue value) {
        K retKey = null;
        for (K key : this.keySet()) {
            if (this.get(key).equals(value)){
                retKey = key;
                break;
            }
        }   
        return retKey;
    }
}
