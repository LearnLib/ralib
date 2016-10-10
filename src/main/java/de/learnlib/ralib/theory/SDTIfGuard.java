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

import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;

public abstract class SDTIfGuard extends SDTGuard {

    protected final SymbolicDataExpression registerExpr;
    protected final Relation relation;

    public SymbolicDataValue getRegister() {
        return this.registerExpr.getSDV();
    }
    
    public SymbolicDataExpression getExpression() {
        return this.registerExpr;
    }

    public Relation getRelation() {
        return this.relation;
    }

    @Override
    public List<SDTGuard> unwrap() {
        List<SDTGuard> s = new ArrayList();
        s.add(this);
        return s;
    }
    
    public SDTIfGuard(SuffixValue param, SymbolicDataExpression regExpr, Relation rel) {
        super(param);
        this.relation = rel;
        this.registerExpr = regExpr;
    }

    public abstract SDTIfGuard toDeqGuard();

    @Override
    public abstract SDTIfGuard relabel(VarMapping relabelling);

    

}
