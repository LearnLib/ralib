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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.Replacement;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;

/**
 *
 * @author Stealth
 */
public class SDTTrueGuard extends SDTGuard {

    public SDTTrueGuard(SymbolicDataValue.SuffixValue param) {
        super(param);
    }

    @Override
    public String toString() {
        return "TRUE: " + parameter.toString();
    }

    @Override
    public List<SDTGuard> unwrap() {
        List<SDTGuard> s = new ArrayList();
        s.add(this);
        return s;
    }

    @Override
    public GuardExpression toExpr() {
        return TrueGuardExpression.TRUE;
    }

    @Override
    public SDTGuard relabel(VarMapping relabelling) {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.getClass());

        return hash;
    }
    
    @Override
    public Set<SDTGuard> mergeWith(SDTGuard other, List<SymbolicDataValue> regPotential) {
        throw new IllegalStateException("trying to merge true guard");
    }

	@Override
	public SDTGuard replace(Replacement replacing) {
		return this;
	}

	@Override
	public Set<SymbolicDataValue> getAllRegs() {
		return Collections.emptySet();
	}

    
//    @Override
//    public SDTGuard mergeWith(Set<SDTGuard> _merged) {
//        return new SDTOrGuard(this.parameter, _merged.toArray(new SDTGuard[]{}));
//        
//    }
}
