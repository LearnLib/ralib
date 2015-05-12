/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import java.util.List;

/**
 *
 * @author falk
 */
public abstract class SDTGuard {

    //TODO: this should probably be a special sdtparameter
    protected final SuffixValue parameter;

    public abstract List<SDTGuard> unwrap();

    public SuffixValue getParameter() {
        return this.parameter;
    }

    public SDTGuard(SuffixValue param) {

        this.parameter = param;

    }

    public TransitionGuard toTG() {
        return new TransitionGuard(this.toExpr());
    }

    public abstract GuardExpression toExpr();

    public abstract SDTGuard relabel(VarMapping relabelling);

}
