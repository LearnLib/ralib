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
package de.learnlib.ralib.data.util;

import java.util.Iterator;

/**
 *
 * @author falk
 */
public class PermutationIterator implements Iterable<int[]>, Iterator<int[]>{

    final int[] permutation;

    boolean init = false;

    public PermutationIterator(int N) {
        permutation = new int[N];
        for (int i = 0; i < N; i++) {
            permutation[i] = i;
        }
    }

    @Override
    public boolean hasNext() {
        if (permutation.length < 1) {
            return false;
        }

        if (!init) {
            return true;
        }

        for (int i = 1; i < permutation.length; i++) {
            if (permutation[i-1] <= permutation[i]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int[] next() {
        if (!init) {
            init = true;
            return permutation;
        }

        int i = permutation.length - 1;
        while (permutation[i - 1] >= permutation[i]) {
            i = i - 1;
        }

        int j = permutation.length;
        while (permutation[j - 1] <= permutation[i - 1]) {
            j = j - 1;
        }

        swap(i - 1, j - 1);

        i++;
        j = permutation.length;
        while (i < j) {
            swap(i - 1, j - 1);
            i++;
            j--;
        }
        return permutation;
    }

    private void swap(int i, int j) {
        int temp = permutation[i];
        permutation[i] = permutation[j];
        permutation[j] = temp;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Iterator<int[]> iterator() {
        return this;
    }

    public int[] current() {
        return permutation;
    }
}
