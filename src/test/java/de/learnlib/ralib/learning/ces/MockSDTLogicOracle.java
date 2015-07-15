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
package de.learnlib.ralib.learning.ces;

import net.automatalib.words.Word;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 *
 * @author falk
 */
public class MockSDTLogicOracle implements SDTLogicOracle {

    public static enum TestCase {LEFT, MID, RIGHT };
    
    private final boolean hasCE;

    public MockSDTLogicOracle(boolean hasCE) {
        this.hasCE = hasCE;
    }
    
    @Override
    public boolean hasCounterexample(Word<PSymbolInstance> prefix, 
            SymbolicDecisionTree sdt1, PIV piv1, 
            SymbolicDecisionTree sdt2, PIV piv2, 
            TransitionGuard guard, Word<PSymbolInstance> rep) {

        return hasCE;
    }

    @Override
    public boolean doesRefine(TransitionGuard refined, PIV pivRefined, 
            TransitionGuard refining, PIV pivRefining) {
        
        return false;
    }
    
}
