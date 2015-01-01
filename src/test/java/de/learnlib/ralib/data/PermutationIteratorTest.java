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

import java.util.Arrays;
import org.testng.Assert;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class PermutationIteratorTest {
    
    public PermutationIteratorTest() {
    }

    @Test
    public void testIterator() {
    
        int expected = 0;
        for (int i=0; i<10; i++) {
            int count = 0;
            PermutationIterator iter = new PermutationIterator(i);
            for (int[] xx : iter) {
                count++;
            }         
            Assert.assertEquals(expected, count);
            expected = (expected == 0) ? 1 : expected * (i+1);
        }
    }
}
