/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class SDTIfGuard extends SDTGuard {

    protected final SymbolicDataValue register;
    protected final Relation relation;

    public SymbolicDataValue getRegister() {
        return this.register;
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

    public SDTIfGuard(SuffixValue param, SymbolicDataValue reg, Relation rel) {
        super(param);
        this.relation = rel;
        this.register = reg;
    }

    public abstract SDTIfGuard toDeqGuard();

    @Override
    public abstract SDTIfGuard relabel(VarMapping relabelling);

    

}
