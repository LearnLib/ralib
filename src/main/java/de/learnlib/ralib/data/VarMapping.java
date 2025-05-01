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

/**
 * maps symbolic data values to symbolic data values.
 *
 *
 * @author falk
 * @param <K>
 * @param <V>
 */
public class VarMapping<K extends SymbolicDataValue, V extends SymbolicDataValue> extends Mapping<K, V> {

    public static <K extends SymbolicDataValue, V extends SymbolicDataValue> VarMapping<K,V> fromPair(K key, V value) {
        VarMapping<K,V> pairs = new VarMapping<>();
        pairs.put(key, value);
        return pairs;
    }

}
