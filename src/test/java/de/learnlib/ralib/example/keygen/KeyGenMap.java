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
package de.learnlib.ralib.example.keygen;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author falk
 */
public class KeyGenMap {

    private final Set<Integer> known = new TreeSet<>();

    private final Map<Integer,Integer> objects = new HashMap<>();

    public Integer put(Integer o) {
        known.add(o);
        if (objects.size() > 0) {
            throw new IllegalStateException();
        }
        Integer newKey = known.size();
        known.add(newKey);
        objects.put(newKey, o);
        return newKey;
    }

    public Integer get(Integer o) {
        known.add(o);
        return objects.get(o);
    }
}
