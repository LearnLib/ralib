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

import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SDTAndGuard extends SDTMultiGuard {

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
        final SDTAndGuard other = (SDTAndGuard) obj;

        if (!Objects.equals(this.guardSet, other.guardSet)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }

    public SDTAndGuard(SuffixValue param, SDTGuard... ifGuards) {
        super(param, ConDis.AND, ifGuards);
    }

    public SDTAndGuard(SDTAndGuard other) {
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
            return new Conjunction(thisList.toArray(new GuardExpression[]{}));
        }
    }

    @Override
    public SDTGuard relabel(VarMapping relabelling) {
        SymbolicDataValue.SuffixValue sv
                = (SymbolicDataValue.SuffixValue) relabelling.get(getParameter());
        sv = (sv == null) ? getParameter() : sv;

        List<SDTGuard> gg = new ArrayList<>();
        for (SDTGuard g : this.guards) {
            gg.add(g.relabel(relabelling));
        }
        return new SDTAndGuard(sv, gg.toArray(new SDTIfGuard[]{}));
    }

    @Override
    public Set<SDTGuard> mergeWith(SDTGuard other, List<SymbolicDataValue> regPotential) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SDTAndGuard copy() {
    	return new SDTAndGuard(this);
    }

}
