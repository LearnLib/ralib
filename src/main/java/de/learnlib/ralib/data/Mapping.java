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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author falk
 * @param <K>
 * @param <V>
 */
public class Mapping<K, V extends DataValue<?>> extends LinkedHashMap<K, V>
        implements Iterable<Map.Entry<K, V>> {

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

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return this.entrySet().iterator();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Mapping<?, ?> other = (Mapping<?, ?>) obj;
        return other.entrySet().equals(entrySet());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash * this.entrySet().hashCode();
    }

    @Override
    public V get(Object key) {
        V v = super.get(key);
        if (v == null) {
            throw new IllegalStateException();
        }
        return v;
    }
    
    
    public <V2 extends DataValue<?>> Mapping<V, V2> then(Mapping<V, V2> other) {
        throw new UnsupportedOperationException("Not implemented");
    }
    
    
    
    public K getKey(V value) {
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
