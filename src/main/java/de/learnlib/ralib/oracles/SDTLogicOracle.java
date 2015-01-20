/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.oracles;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * The SDTLogicOracle offers functions that are needed for 
 * processing counterexamples.
 * 
 * @author falk
 */
public interface SDTLogicOracle {
    
    /**
     * checks if there is a counterexample (a word that is accepted by one sdt
     * and rejected by the other sdt). 
     * 
     * sdts are both after prefix. the piv objects describe respective assignments
     * of registers of the sdts. 
     * 
     * rep is the dataword that was used for one transition in a hypothesis
     * with guard. Guard should be used to constrain the initial parameter values
     * for the sdt.
     * 
     * @param prefix
     * @param sdt1
     * @param piv1
     * @param sdt2
     * @param piv2
     * @param guard
     * @param rep
     * @return 
     */
    public boolean hasCounterexample(Word<PSymbolInstance> prefix, 
            SymbolicDecisionTree sdt1, PIV piv1, 
            SymbolicDecisionTree sdt2, PIV piv2, 
            TransitionGuard guard, Word<PSymbolInstance> rep);
            
    /**
     * checks if one guard refine another guard.
     * 
     * @param refining
     * @param pivRefining
     * @param refined
     * @param pivRefined
     * @return 
     */
    public boolean doesRefine(TransitionGuard refining, PIV pivRefining,
            TransitionGuard refined, PIV pivRefined);
    
}
