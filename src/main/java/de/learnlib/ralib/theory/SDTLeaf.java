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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.SDTGuardElement;
import de.learnlib.ralib.data.SDTRelabeling;
import de.learnlib.ralib.data.SymbolicDataValue;

/**
 * Leaf implementation of an SDT.
 *
 * @author falk
 */
public class SDTLeaf extends SDT {

    public static final SDTLeaf ACCEPTING = new SDTLeaf(true);

    public static final SDTLeaf REJECTING = new SDTLeaf(false);

    private final boolean accepting;

    protected SDTLeaf(boolean accepting) {
        super(null);
        this.accepting = accepting;
    }

    @Override
    public boolean isEquivalent(
            SDT other, SDTRelabeling renaming) {
        return (getClass() == other.getClass() &&
                isAccepting() == other.isAccepting());
    }

    @Override
    public boolean canUse(SDT other) {
        if (!(other instanceof SDTLeaf)) {
            return false;
        }
        else {
            return this.isEquivalent(other, new SDTRelabeling());
        }
    }

    @Override
    public String toString() {
        return this.isAccepting() ? "+" : "-";
    }

    @Override
    public SDT relabel(SDTRelabeling relabeling) {
        return this;
    }

    public SDT relabelUnderEq(SDTGuard.EqualityGuard e) {
        return this;
    }

    @Override
    public boolean isAccepting() {
        return accepting;
    }

    @Override
    void toString(StringBuilder sb, String indentation) {
        sb.append(indentation).append("[Leaf").
                append(isAccepting() ? "+" : "-").append("]").append("\n");
    }

    @Override
    List<List<SDTGuard>> getPaths(List<SDTGuard> path) {
        List<List<SDTGuard>> ret = new ArrayList<>();
        if (this.isAccepting()) {
            ret.add(path);
        }
        return ret;
    }

    @Override
    public Map<List<SDTGuard>, Boolean> getAllPaths(List<SDTGuard> path) {
    	Map<List<SDTGuard>, Boolean> ret = new LinkedHashMap<>();
    	ret.put(path, this.isAccepting());
    	return ret;
    }

    @Override
    public Set<SymbolicDataValue.Register> getRegisters() {
        return new LinkedHashSet<>();
    }

    @Override
    public Set<SDTGuardElement> getVariables() {
        return new LinkedHashSet<>();
    }
}
