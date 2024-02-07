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

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.util.PermutationIterator;

/**
 *
 * @author falk
 */
public class PermutationIteratorTest extends RaLibTestSuite {

    @Test
    public void testIterator() {

        int expected = 0;
        for (int i = 0; i < 10; i++) {
            int count = 0;
            PermutationIterator iter = new PermutationIterator(i);
            for (int[] unused : iter) {
                count++;
            }
            Assert.assertEquals(expected, count);
            expected = (expected == 0) ? 1 : expected * (i+1);
        }
    }
}
