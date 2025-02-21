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
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;

@Deprecated
public class SDTOrGuard extends SDTMultiGuard {

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.condis);
        hash = 59 * hash + Objects.hashCode(this.parameter);
        hash = 59 * hash + Objects.hashCode(this.guardSet);
        hash = 59 * hash + Objects.hashCode(this.getClass());

        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SDTOrGuard other = (SDTOrGuard) obj;

        if (!Objects.equals(this.guardSet, other.guardSet)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }

    public SDTOrGuard(SuffixValue param, SDTGuard... ifGuards) {
        super(param, ConDis.OR, ifGuards);
    }

    public SDTOrGuard(SDTOrGuard other) {
    	super(other);
    }

    private List<GuardExpression> toExprList() {
        List<GuardExpression> exprs = new ArrayList<>();
        for (SDTGuard guard : this.guards) {
            exprs.add(guard.toExpr());
        }
        return exprs;
    }

    @Override
    public GuardExpression toExpr() {
        List<GuardExpression> thisList = this.toExprList();
        if (thisList.isEmpty()) {
            return TrueGuardExpression.TRUE;
        }
        if (thisList.size() == 1) {
            return thisList.get(0);
        } else {
            return new Disjunction(thisList.toArray(new GuardExpression[]{}));
        }
    }

    @Override
    public SDTGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
        sv = (sv == null) ? getParameter() : sv;

        List<SDTGuard> gg = new ArrayList<>();
        for (SDTGuard g : this.guards) {
            gg.add(g.relabel(relabelling));
        }
        return new SDTOrGuard(sv, gg.toArray(new SDTGuard[]{}));
    }

    @Override
    public Set<SDTGuard> mergeWith(SDTGuard other, List<SymbolicDataValue> regPotential) {
        return other.mergeWith(this, regPotential);
    }

    @Override
    public SDTOrGuard copy() {
    	return new SDTOrGuard(this);
    }

    //@Override
    //public SDTGuard mergeWith(Set<SDTGuard> _merged) {
    //    return null;
    //}
//        Set<SDTGuard> merged = new LinkedHashSet<>();
//        merged.addAll(_merged);
//        for (SDTGuard x : this.getGuards()) {
//            if (x instanceof SDTIfGuard) {
//                SDTGuard newGuard = x.mergeWith(merged);
//            }
//        }
//        if (merged.isEmpty()) {
//            return new SDTTrueGuard(this.parameter);
//        } else {
//            SDTGuard[] mergedArr = merged.toArray(new SDTGuard[]{});
//            if (mergedArr.length == 1) {
//                return mergedArr[0];
//            }
//            else {
//                return new SDTOrGuard(this.parameter, mergedArr);
//            }
//
//        }
//    }
}
