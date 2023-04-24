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
public class IntegerEqualityTheory  extends EqualityTheory<Integer> implements TypedTheory<Integer> {


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
