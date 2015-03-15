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

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.automatalib.automata.MutableDeterministic;
import net.automatalib.words.Word;

/**
 * Mutable Register Automaton.
 * 
 * @author falk
 */
public class MutableRegisterAutomaton extends RegisterAutomaton
        implements MutableDeterministic<RALocation, ParameterizedSymbol, Transition, Boolean, Void> {
    
    private final Constants constants;
    
    private int ids = 0;
    
    private RALocation initial;
    
    private final Set<RALocation> locations = new LinkedHashSet<>();
    
    public MutableRegisterAutomaton(Constants consts, VarValuation initialRegisters) {
        super(initialRegisters);
        this.constants = consts;
    }
    
    public MutableRegisterAutomaton(Constants consts) {
        this.constants = consts;
    }
    
    public MutableRegisterAutomaton() {
        this(new Constants());
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
    
    protected List<Transition> getTransitions(Word<PSymbolInstance> dw) {
        VarValuation vars = new VarValuation(initialRegisters);
        RALocation current = initial;
        List<Transition> tseq = new ArrayList<>();
        for (PSymbolInstance psi : dw) {
            
            ParValuation pars = new ParValuation(psi);
            
            Collection<Transition> candidates = 
                    current.getOut(psi.getBaseSymbol());
                        
            if (candidates == null) {
                return null;
            }
            
            boolean found = false;
            for (Transition t : candidates) {
                if (t.isEnabled(vars, pars, this.constants)) {
                    vars = t.execute(vars, pars, this.constants);
                    current = t.getDestination();
                    tseq.add(t);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                return null;
            }
        }
        return tseq;        
    }
    
    @Override
    public RALocation getLocation(Word<PSymbolInstance> dw) {
        List<Transition> tseq = getTransitions(dw);
        if (tseq == null) {
            return null;
        }
        if (tseq.isEmpty()) {
            return initial;
        } else {
            Transition last = tseq.get(tseq.size() -1);
            return last.getDestination();
        }
    }

    @Override
    public boolean accepts(Word<PSymbolInstance> dw) {        
        RALocation dest = getLocation(dw);
        return (dest != null && dest.isAccepting());
    }

    @Override
    public void setInitialState(RALocation s) {
        this.initial = s;
    }

    @Override
    public void setTransition(RALocation s, ParameterizedSymbol i, Transition t) {
        s.addOut(t);
    }

    @Override
    public void setTransition(RALocation s, ParameterizedSymbol i, RALocation s1, Void tp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Boolean getStateProperty(RALocation s) {
        return s.isAccepting();
    }

    @Override
    public Void getTransitionProperty(Transition t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RALocation addState(Boolean sp) {
        RALocation lNew = new RALocation(ids++, sp);
        this.locations.add(lNew);
        return lNew;
    }

    @Override
    public RALocation addInitialState(Boolean sp) {
        this.initial = addState(sp);
        return this.initial;
    }

    @Override
    public RALocation addState() {
        return addState(true);
    }

    @Override
    public RALocation addInitialState() {
        return addInitialState(true);
    }

    @Override
    public void setInitial(RALocation s, boolean bln) {
        if (bln) {
            this.initial = s;
        }
        else {
            if (this.initial.equals(s)) {
                this.initial = null;
            }
        }
    }

    @Override
    public void setStateProperty(RALocation s, Boolean sp) {
        s.setAccepting(sp);
    }

    @Override
    public void setTransitionProperty(Transition t, Void tp) {
    }

    @Override
    public Transition createTransition(RALocation s, Void tp) {
        throw new UnsupportedOperationException(
                "Unsupported: A RA can have input and output transitions."); 
    }

    @Override
    public void addTransition(RALocation s, ParameterizedSymbol i, Transition t) {
        s.addOut(t);
    }

    @Override
    public void addTransitions(RALocation s, ParameterizedSymbol i, Collection<? extends Transition> clctn) {
        for (Transition t : clctn) {
            addTransition(s, i, t);
        }
    }

    @Override
    public void setTransitions(RALocation s, ParameterizedSymbol i, Collection<? extends Transition> clctn) {
        for (Transition t : clctn) {
            addTransition(s, i, t);
        }
    }

    @Override
    public void removeTransition(RALocation s, ParameterizedSymbol i, Transition t) {
        s.getOut(i).remove(t);
    }

    @Override
    public void removeAllTransitions(RALocation s, ParameterizedSymbol i) {
        Collection<Transition> cltn = s.getOut(i);
        if (cltn != null) {
            cltn.clear();
        }
    }

    @Override
    public void removeAllTransitions(RALocation s) {
        s.clear();
    }

    @Override
    public Transition addTransition(RALocation s, ParameterizedSymbol i, RALocation s1, Void tp) {
        throw new UnsupportedOperationException(
                "More information needed for a transition of a RA"); 
    }

    @Override
    public Transition copyTransition(Transition t, RALocation s) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


}
