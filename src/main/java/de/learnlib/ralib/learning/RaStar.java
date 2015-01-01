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

package de.learnlib.ralib.learning;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.theory.TreeOracle;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class RaStar {
    
    private final Observations obs;

    public RaStar(TreeOracle oracle, ParameterizedSymbol ... inputs) {
        this.obs = new Observations(oracle, inputs);
        
        Word<PSymbolInstance> epsPrefix = Word.epsilon();
        this.obs.addPrefix(epsPrefix);

        SymbolicSuffix epsSuffix = new SymbolicSuffix(epsPrefix, epsPrefix);
        this.obs.addSuffix(epsSuffix);
    }
    
    
    
    public void learn() {        
        while(!(obs.complete())) {};        
    }
    
    public boolean addCounterexample(Word<PSymbolInstance> ce) {
        throw new UnsupportedOperationException("not implemented yet.");
    }
    
    public RegisterAutomaton getHypothesis() {
        AutomatonBuilder ab = new AutomatonBuilder(obs.getComponents());
        return ab.toRegisterAutomaton();
    }
    
}
