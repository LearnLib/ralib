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

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;

/**
 *
 * @author falk
 */
public class PIVRemappingIteratorTest {

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
        for (VarMapping map : new PIVRemappingIterator(piv1, piv2)) {
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
        for (VarMapping map : new PIVRemappingIterator(piv1, piv2)) {
            System.out.println(map);
            count++;
        }

        Assert.assertEquals(count, 4);        
    }
}
