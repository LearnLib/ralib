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

import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.automata.DeterministicAutomaton;
import net.automatalib.automata.abstractimpl.AbstractDeterministicAutomaton;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public abstract class RegisterAutomaton 
        extends AbstractDeterministicAutomaton<RALocation, ParameterizedSymbol, Transition>
        implements DeterministicAutomaton<RALocation, ParameterizedSymbol, Transition> {
    
    private final VarValuation initialRegisters;

    public RegisterAutomaton(VarValuation initialRegisters) {
        this.initialRegisters = initialRegisters;
    }

    public RegisterAutomaton() {
        this(new VarValuation());
    }

    
    /**
     * Checks if a data word is accepted by an automaton.
     * 
     * @param dw a data word
     * @return true if dw is accepted
     */
    public abstract boolean accepts(Word<PSymbolInstance> dw);        

    public abstract RALocation getLocation(Word<PSymbolInstance> dw);
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (RALocation loc : getStates()) {
            sb.append(loc).append( loc.equals(getInitialState()) ? " (initial)" : "").append(":\n");            
            for (Transition t : loc.getOut()) {
                sb.append("  ").append(t).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * @return the initialRegisters
     */
    public VarValuation getInitialRegisters() {
        return initialRegisters;
    }
    
}
