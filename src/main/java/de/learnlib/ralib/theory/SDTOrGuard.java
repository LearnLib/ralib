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

import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.Replacement;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;

public class SDTOrGuard extends SDTMultiGuard {

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(condis);
        hash = 59 * hash + Objects.hashCode(parameter);
        hash = 59 * hash + Objects.hashCode(guardSet);
        hash = 59 * hash + Objects.hashCode(super.getClass());

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

        if (!Objects.equals(guardSet, other.guardSet)) {
            return false;
        }
        return Objects.equals(parameter, other.parameter);
    }

    public SDTOrGuard(SuffixValue param, SDTGuard... ifGuards) {
        super(param, ConDis.OR, ifGuards);
    }

    private List<GuardExpression> toExprList() {
        List<GuardExpression> exprs = new ArrayList<>();
        for (SDTGuard guard : guards) {
            exprs.add(guard.toExpr());
        }
        return exprs;
    }

    @Override
    public GuardExpression toExpr() {
        List<GuardExpression> thisList = toExprList();
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
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) relabelling.get(super.getParameter());
        sv = (sv == null) ? super.getParameter() : sv;

        List<SDTGuard> gg = new ArrayList<>();
        for (SDTGuard g : guards) {
            gg.add(g.relabel(relabelling));
        }
        return new SDTOrGuard(sv, gg.toArray(new SDTGuard[]{}));
    }
    
    public SDTGuard replace(Replacement replacing) {
    	List<SDTGuard> gg = new ArrayList<>();
        for (SDTGuard g : guards) {
            gg.add(g.replace(replacing));
        }
        return new SDTOrGuard(super.getParameter(), gg.toArray(new SDTGuard[]{}));
    }
}
