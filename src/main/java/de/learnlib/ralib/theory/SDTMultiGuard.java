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

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class SDTMultiGuard extends SDTGuard {

    protected enum ConDis {

        AND, OR
    }

    protected final List<SDTGuard> guards;
    protected final Set<SDTGuard> guardSet;
    protected final ConDis condis;

    public List<SDTGuard> getGuards() {
        return guards;
    }

    @Override
    public List<SDTGuard> unwrap() {
        List<SDTGuard> unwrapped = new ArrayList();
        if (isEmpty()) {
            unwrapped.add(asTrueGuard());
        } else if (isSingle()) {
            unwrapped.add(getSingle());
        } else {
            unwrapped.addAll(guards);
        }
        return unwrapped;
    }

    public boolean isSingle() {
        return guards.size() == 1;
    }

    public boolean isEmpty() {
        return guards.isEmpty();
    }

    public SDTTrueGuard asTrueGuard() {
        return new SDTTrueGuard(parameter);
    }

    public SDTGuard getSingle() {
        assert isSingle();
        return guards.get(0);
    }

    public Set<SymbolicDataValue> getAllRegs() {
        Set<SymbolicDataValue> allRegs = new LinkedHashSet<SymbolicDataValue>();
        for (SDTGuard g : guards) {
            if (g instanceof SDTIfGuard) {
                allRegs.add(((SDTIfGuard)g).getRegister());
            }
            else if (g instanceof SDTMultiGuard) {
                allRegs.addAll(((SDTMultiGuard)g).getAllRegs());
            }
        }
        return allRegs;
    }

    public SDTMultiGuard(SuffixValue param, ConDis condis, SDTGuard... ifGuards) {
        super(param);
        this.condis = condis;
        this.guards = new ArrayList<>();
        this.guards.addAll(Arrays.asList(ifGuards));
        this.guardSet = new LinkedHashSet<>(guards);
    }

    public SDTMultiGuard(SDTMultiGuard other) {
    	super(other);
    	this.condis = other.condis;
    	this.guards = new ArrayList<>();
    	for (SDTGuard g : other.guards)
    		guards.add(g.copy());
    	this.guardSet = new LinkedHashSet<>(guards);
    	for (SDTGuard g : other.guards)
    		guardSet.add(g.copy());
    }

    @Override
    public abstract GuardExpression toExpr();

    @Override
    public String toString() {
        String p = this.condis.toString() + "COMPOUND: " + parameter.toString();
        if (this.guards.isEmpty()) {
            return p + "empty";
        }
        return p + this.guards.toString();
    }

}
