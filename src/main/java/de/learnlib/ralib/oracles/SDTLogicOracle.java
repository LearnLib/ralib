/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.oracles;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
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
    
    public GeneralizedSymbolicSuffix suffixForCounterexample(SymbolicDecisionTree sutSdt, Word<ParameterizedSymbol> actions);
    
    /**
     * computes a generalized symbolic suffix that exhibits a counterexample
     * between to SDTs.
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
     * @param actions
     * @return 
     */    
    public GeneralizedSymbolicSuffix suffixForCounterexample(
            Word<PSymbolInstance> prefix, 
            SymbolicDecisionTree sdt1, PIV piv1, 
            SymbolicDecisionTree sdt2, PIV piv2, 
            TransitionGuard guard, Word<ParameterizedSymbol> actions);
    
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
