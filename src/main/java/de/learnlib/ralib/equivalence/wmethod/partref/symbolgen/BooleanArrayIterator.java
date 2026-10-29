package de.learnlib.ralib.equivalence.wmethod.partref.symbolgen;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator for the possible permutations of a boolean array of
 * size {@code n} with {@code m} elements true and {@code n-m}
 * elements false.
 */
public class BooleanArrayIterator implements Iterator<boolean[]> {

    private final int n;
    private boolean[] current;
    private boolean hasNext;

    public BooleanArrayIterator(int n, int m) {
        if (n < 0 || m < 0 || m > n) {
            throw new IllegalArgumentException(
                "Require 0 <= m <= n"
            );
        }

        this.n = n;
        this.current = new boolean[n];

        // First combination: m true values, then n-m false values
        for (int i = 0; i < m; i++) {
            current[i] = true;
        }

        this.hasNext = true;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public boolean[] next() {
        if (!hasNext) {
            throw new NoSuchElementException();
        }

        // Return a copy so callers cannot modify the iterator's state
        boolean[] result = current.clone();

        advance();
        return result;
    }

    private void advance() {
        // Find the rightmost true value that can be moved right
        int i = n - 2;

        while (i >= 0 && (!current[i] || current[i + 1])) {
            i--;
        }

        if (i < 0) {
            hasNext = false;
            return;
        }

        // Move the true value one position to the right
        current[i] = false;
        current[i + 1] = true;

        // Count true values to the left of the moved position
        int count = 0;
        for (int j = 0; j < i; j++) {
            if (current[j]) {
                count++;
            }
            current[j] = false;
        }

        // Pack those true values into the leftmost positions
        for (int j = 0; j < count; j++) {
            current[j] = true;
        }
    }
}
