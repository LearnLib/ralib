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

package de.learnlib.ralib.learning;

import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionSequenceTransformer;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class Hypothesis extends MutableRegisterAutomaton 
implements AccessSequenceTransformer<PSymbolInstance>, TransitionSequenceTransformer<PSymbolInstance> {

    private final Map<RALocation, Word<PSymbolInstance>> accessSequences = new HashMap<>();

    private final Map<Transition, Word<PSymbolInstance>> transitionSequences = new HashMap<>();
    
    public Hypothesis(Constants consts) {
        super(consts);
    }
    
    public void setAccessSequence(RALocation loc, Word<PSymbolInstance> as) {
        accessSequences.put(loc, as);
    }

    public void setTransitionSequence(Transition t, Word<PSymbolInstance> as) {
        transitionSequences.put(t, as);
    }
    
    @Override
    public Word<PSymbolInstance> transformAccessSequence(Word<PSymbolInstance> word) {
        RALocation loc = getLocation(word);
        return accessSequences.get(loc);
    }

    @Override
    public boolean isAccessSequence(Word<PSymbolInstance> word) {
        return accessSequences.containsValue(word);
    }

    @Override
    public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word) {
        List<Transition> tseq = getTransitions(word);
        Transition last = tseq.get(tseq.size() -1);
        return transitionSequences.get(last);        
    }
    
}
