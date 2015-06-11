/*
 * Copyright (C) 2015 falk.
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
package de.learnlib.ralib.tools.theories;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author falk
 */
public class IntegerEqualityTheory extends EqualityTheory<Integer> implements TypedTheory<Integer> {

    private DataType type = null;

    public IntegerEqualityTheory() {
    }
    
    public IntegerEqualityTheory(DataType t) {
        this.type = t;
    }

    @Override
    public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
        int dv = -1;
        for (DataValue<Integer> d : vals) {
            dv = Math.max(dv, d.getId());
        }

        return new DataValue(type, dv + 1);
    }

    @Override
    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public void setUseSuffixOpt(boolean useit) {
        this.useNonFreeOptimization = useit;
    }

    @Override
    public void setCheckForFreshOutputs(boolean doit, IOOracle oracle) {
        super.setFreshValues(doit, oracle);
    }

    @Override
    public Collection<DataValue<Integer>> getAllNextValues(
            List<DataValue<Integer>> vals) {
        
        // TODO: add constants ...
        
        ArrayList<DataValue<Integer>> ret = new ArrayList<>(vals);
        ret.add(getFreshValue(vals));
        return ret;
    }

}
