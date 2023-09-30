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
package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.TransitionSequenceTransformer;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.commons.util.Pair;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class Hypothesis extends MutableRegisterAutomaton
implements AccessSequenceTransformer<PSymbolInstance>, TransitionSequenceTransformer<PSymbolInstance> {

    private final Map<RALocation, Word<PSymbolInstance>> accessSequences = new LinkedHashMap<>();

    private final Map<Transition, Word<PSymbolInstance>> transitionSequences = new LinkedHashMap<>();

    public Hypothesis(Constants consts) {
        super(consts);
    }

    public void setAccessSequence(RALocation loc, Word<PSymbolInstance> as) {
        accessSequences.put(loc, as);
    }

    public void setTransitionSequence(Transition t, Word<PSymbolInstance> as) {
        transitionSequences.put(t, as);
    }

    public Map<RALocation, Word<PSymbolInstance>> getAccessSequences() {
        return accessSequences;
    }

    public Map<Transition, Word<PSymbolInstance>> getTransitionSequences() {
        return transitionSequences;
    }

    @Override
    public Word<PSymbolInstance> transformAccessSequence(Word<PSymbolInstance> word) {
        RALocation loc = getLocation(word);
        return accessSequences.get(loc);
    }

    public Set<Word<PSymbolInstance>> possibleAccessSequences(Word<PSymbolInstance> word) {
    	Set<Word<PSymbolInstance>> ret = new LinkedHashSet<Word<PSymbolInstance>>();
    	ret.add(transformAccessSequence(word));
    	return ret;
    }

    @Override
    public boolean isAccessSequence(Word<PSymbolInstance> word) {
        return accessSequences.containsValue(word);
    }

    @Override
    public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word) {
        List<Transition> tseq = getTransitions(word);
        //System.out.println("TSEQ: " + tseq);
        if (tseq == null)
        	return null;
        Transition last = tseq.get(tseq.size() -1);
        return transitionSequences.get(last);
    }

    public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word, Word<PSymbolInstance> loc) {
    	return transformTransitionSequence(word);
    }

	public Word<PSymbolInstance> branchWithSameGuard(Word<PSymbolInstance> word, Branching branching) {
		ParameterizedSymbol ps = word.lastSymbol().getBaseSymbol();

        List<Pair<Transition, VarValuation>> tvseq = getTransitionsAndValuations(word);
        VarValuation vars = tvseq.get(tvseq.size()-1).getSecond();
        ParValuation pval = new ParValuation(word.lastSymbol());

		for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : branching.getBranches().entrySet()) {
			if (e.getKey().lastSymbol().getBaseSymbol().equals(ps)) {
			    if (e.getValue().isSatisfied(vars, pval, constants)) {
			        return e.getKey();
			    }
			}
		}
		return null;
	}

	public VarMapping<Register, ? extends SymbolicDataValue> getLastTransitionAssignment(Word<PSymbolInstance> word) {
		List<Transition> tseq = getTransitions(word);
		return tseq.get(tseq.size() - 1).getAssignment().getAssignment();
	}
}
