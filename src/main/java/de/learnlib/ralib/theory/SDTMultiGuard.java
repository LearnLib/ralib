/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
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
            else if (g instanceof IntervalGuard) {
                allRegs.addAll(((IntervalGuard)g).getAllRegs());
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
