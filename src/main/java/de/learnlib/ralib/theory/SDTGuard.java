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
package de.learnlib.ralib.theory;

import java.util.List;
import java.util.Set;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import gov.nasa.jpf.constraints.api.Expression;

/**
 *
 * @author falk
 */
@Deprecated
public abstract class SDTGuard {

    //TODO: this should probably be a special sdtparameter
    protected final SuffixValue parameter;

    public abstract List<SDTGuard> unwrap();

    public SuffixValue getParameter() {
        return this.parameter;
    }

    public SDTGuard(SuffixValue param) {

        this.parameter = param;

    }

    public abstract Set<SymbolicDataValue> getComparands(SymbolicDataValue dv);

    public SDTGuard(SDTGuard other) {
    	this.parameter = SymbolicDataValue.copy(other.parameter);
    }

    public abstract Expression<Boolean> toExpr();

    public abstract SDTGuard relabel(VarMapping relabelling);

    public abstract Set<SDTGuard> mergeWith(SDTGuard other, List<SymbolicDataValue> regPotential);

    public abstract SDTGuard copy();

}
