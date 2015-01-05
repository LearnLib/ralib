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

package de.learnlib.ralib.automata.guards;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.VarValuation;
import java.util.ArrayList;
import java.util.Collection;

/**
 * else-case to a bunch of if-guards.
 * 
 * @author falk
 */
public class ElseGuard implements TransitionGuard {

    private final Collection<IfGuard> ifs;
    
    public ElseGuard(Collection<IfGuard> ifs) {
        this.ifs = ifs;
    }  

    public ElseGuard() {
        this(new ArrayList<IfGuard>());
    }
    
    @Override
    public boolean isSatisfied(VarValuation registers, ParValuation parameters, Constants consts) {
        for (IfGuard g : ifs) {
            if (g.isSatisfied(registers, parameters, consts)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return (ifs.isEmpty()) ? "true" : "else";
    }
    

    
}
