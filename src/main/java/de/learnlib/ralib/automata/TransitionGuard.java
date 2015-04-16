/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.automata;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.VarValuation;

/**
 * Transition Guard.
 * 
 * @author falk
 */
public class TransitionGuard {
    
    private final GuardExpression condition;

    public TransitionGuard() {
        this.condition = TrueGuardExpression.TRUE;
    }

    public TransitionGuard(GuardExpression condition) {
        assert condition != null;
        this.condition = condition;
    }
    
    /**
     * Checks if the guard is satisfied for the given assignments of 
     * registers, parameters, and named constants.
     * 
     * @param registers
     * @param parameters
     * @param consts
     * @return 
     */
    public boolean isSatisfied(VarValuation registers, ParValuation parameters, Constants consts) {
        VarMapping val = new VarMapping();
        val.putAll(registers);
        val.putAll(parameters);
        val.putAll(consts);
                
        return condition.isSatisfied(val);
    }

    @Override
    public String toString() {
        return condition.toString();
    }

    /**
     * @return the condition
     */
    public GuardExpression getCondition() {
        return condition;
    }
        
}
