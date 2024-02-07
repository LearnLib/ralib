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
import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;

/**
 *
 * @author falk
 */
public class PIVRemappingIteratorTest extends RaLibTestSuite {

    public PIVRemappingIteratorTest() {
    }

    private PIV generatePIV(PIV piv, DataType t, int size) {

        ParameterGenerator gen_p = new ParameterGenerator();
        RegisterGenerator gen_r = new RegisterGenerator();

        for (int i = 0; i < size; i++) {
            piv.put(gen_p.next(t), gen_r.next(t));
        }

        return piv;
    }

    @Test
    public void testOneType() {

        DataType type1 = new DataType("type1", Integer.class);

        PIV piv1 = generatePIV(new PIV(), type1, 4);
        PIV piv2 = generatePIV(new PIV(), type1, 4);

        int count = 0;
        for (VarMapping unused : new PIVRemappingIterator(piv1, piv2)) {
            count++;
        }

        Assert.assertEquals(count, 24);
    }

    @Test
    public void testTwoTypes() {

        DataType type1 = new DataType("type1", Integer.class);
        DataType type2 = new DataType("type2", Integer.class);

        PIV piv1 = generatePIV(new PIV(), type1, 2);
        PIV piv2 = generatePIV(new PIV(), type1, 2);

        piv1 = generatePIV(piv1, type2, 2);
        piv2 = generatePIV(piv2, type2, 2);

        int count = 0;
        for (VarMapping unused : new PIVRemappingIterator(piv1, piv2)) {
            count++;
        }

        Assert.assertEquals(count, 4);
    }
}
