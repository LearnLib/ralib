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
package de.learnlib.ralib.example.priority;

/**
 * This wrapper limits the size of a priority queue to 3.
 */
public class PQWrapper<T> extends java.util.PriorityQueue<T> {

    public static final int CAPACITY = 3;
    private int capacity;

    public PQWrapper() {
        this.capacity = CAPACITY;
    }

    public PQWrapper(int capacity) {
        this.capacity = capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean offer(T e) {
        return this.size() < capacity && super.offer(e);
    }

}
