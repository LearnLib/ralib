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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.automatalib.automata.DeterministicAutomaton;
import net.automatalib.words.Word;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public abstract class RegisterAutomaton 
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
        sb.append("Init:").append(initialRegisters).append("\n");
        return sb.toString();
    }

    /**
     * @return the initialRegisters
     */
    public VarValuation getInitialRegisters() {
        return initialRegisters;
    }
    
    public List<Transition> getTransitions() {
        List<Transition> tList = new ArrayList<>();
        for (RALocation loc : getStates()) {
            tList.addAll(loc.getOut());
        }
        return tList;
    }
    
    public List<Transition> getInputTransitions() {
        List<Transition> tList = new ArrayList<>();
        for (RALocation loc : getStates()) {
            for (Transition t : loc.getOut()) {
                if (!(t instanceof OutputTransition)) {
                    tList.add( t);
                }
            }
        }
        return tList;
    }    
    
    public Collection<RALocation> getInputStates() { 
        Set<RALocation> ret = new HashSet<>();
        for (Transition t : getInputTransitions()) {
            ret.add(t.getSource());
        }
        return ret;
    }
    
    public Collection<Register> getRegisters() {
        Set<Register> regs = new HashSet<>();
        for (Transition t : getTransitions()) {
            regs.addAll(t.getAssignment().getAssignment().keySet());
        }
        return regs;
    }
}
