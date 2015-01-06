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
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * Learning algorithm for register automata
 * 
 * @author falk
 */
public class RaStar {
    
    public static final Word<PSymbolInstance> EMPTY_PREFIX = Word.epsilon();
    
    public static final SymbolicSuffix EMPTY_SUFFIX = new SymbolicSuffix(
            Word.<PSymbolInstance>epsilon(), Word.<PSymbolInstance>epsilon());
    
    private final ObservationTable obs;
    
    private final Constants consts;

    public RaStar(TreeOracle oracle, Constants consts, ParameterizedSymbol ... inputs) {
        this.obs = new ObservationTable(oracle, inputs);
        this.consts = consts;
        
        this.obs.addPrefix(EMPTY_PREFIX);
        this.obs.addSuffix(EMPTY_SUFFIX);
    }
    
    
    
    public void learn() {        
        while(!(obs.complete())) {};        
    }
    
    public boolean addCounterexample(Word<PSymbolInstance> ce) {
        throw new UnsupportedOperationException("not implemented yet.");
    }
    
    public RegisterAutomaton getHypothesis() {
        AutomatonBuilder ab = new AutomatonBuilder(obs.getComponents(), consts);
        return ab.toRegisterAutomaton();
    }
    
}
