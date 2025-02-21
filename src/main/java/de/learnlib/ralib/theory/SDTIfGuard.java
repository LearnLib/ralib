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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;

@Deprecated
public abstract class SDTIfGuard extends SDTGuard {

    protected final SymbolicDataValue register;

    public SymbolicDataValue getRegister() {
        return this.register;
    }

    @Override
    public List<SDTGuard> unwrap() {
        List<SDTGuard> s = new ArrayList<SDTGuard>();
        s.add(this);
        return s;
    }

    protected SDTIfGuard(SuffixValue param, SymbolicDataValue reg) {
        super(param);
        this.register = reg;
    }

    public SDTIfGuard(SDTIfGuard other) {
    	super(other);
    	register = SymbolicDataValue.copy(other.register);
    }

    public abstract SDTIfGuard toDeqGuard();

    @Override
    public Set<SymbolicDataValue> getComparands(SymbolicDataValue dv) {
    	Set<SymbolicDataValue> comparands = new LinkedHashSet<>();
    	if (this.parameter.equals(dv))
    		comparands.add(register);
    	else if (register.equals(dv))
    		comparands.add(parameter);
    	return comparands;
    }

    @Override
    public abstract SDTIfGuard relabel(VarMapping relabelling);



}
