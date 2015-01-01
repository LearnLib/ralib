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
