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

import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.automatalib.automata.DeterministicAutomaton;
import net.automatalib.automata.abstractimpl.AbstractDeterministicAutomaton;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class RegisterAutomaton 
        extends AbstractDeterministicAutomaton<RALocation, ParameterizedSymbol, Transition>
        implements DeterministicAutomaton<RALocation, ParameterizedSymbol, Transition> {
    
    
    private final RALocation initial;
    
    private final List<RALocation> locations = new ArrayList<>();

    public RegisterAutomaton() {
        this.initial = new RALocation();
        locations.add(initial);
    }
    
    @Override
    public RALocation getSuccessor(Transition t) {
        return t.getDestination();
    }

    @Override
    public Transition getTransition(RALocation s, ParameterizedSymbol i) {
        throw new UnsupportedOperationException(
                "There may be more than one transition per symbol in an RA."); 
    }

    @Override
    public Collection<Transition> getTransitions(RALocation s, ParameterizedSymbol i) {
        return s.getOut(i);
    }
    
    @Override
    public RALocation getInitialState() {
        return initial;
    }

    @Override
    public Collection<RALocation> getStates() {
        return locations;
    }

    public boolean hasTrace(Word<PSymbolInstance> dw) {        
        VarValuation vars = new VarValuation();
        RALocation current = initial;
        for (PSymbolInstance psi : dw) {
            
            ParValuation pars = new ParValuation(psi);
            
            Collection<Transition> candidates = 
                    current.getOut(psi.getBaseSymbol());
            
            for (Transition t : candidates) {
                if (t.isEnabled(vars, pars)) {
                    vars = t.execute(vars, pars);
                }
            }
        }
        return true;
    }
    
}
