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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public interface Branching {

    Map<Word<PSymbolInstance>, Expression<Boolean>> getBranches();

    Word<PSymbolInstance> transformPrefix(Word<PSymbolInstance> prefix);

    /**
     * Return a prefix {@code u} within this branching such that the parameters of the last
     * symbol of {@code u} satisfy {@code guard}.
     *
     * @param guard
     * @param solver
     * @return an {@code Optional} encasing a prefix matching {@code guard} if one exists,
     * or an empty {@code Optional} otherwise
     */
    default Optional<Word<PSymbolInstance>> getPrefix(Expression<Boolean> guard, ConstraintSolver solver) {
    	// if guard is a value, return the corresponding key
    	Optional<Word<PSymbolInstance>> prefix = getBranches().entrySet()
    			.stream()
    			.filter(e -> e.getValue().equals(guard))
    			.map(e -> e.getKey())
    			.findFirst();
    	if (prefix.isPresent()) {
    		return prefix;
    	}

    	// find a guard where the last symbol's values satisfy guard
    	for (Map.Entry<Word<PSymbolInstance>, Expression<Boolean>> e : getBranches().entrySet()) {
    		DataValue[] vals = e.getKey().lastSymbol().getParameterValues();
    		Mapping<SymbolicDataValue, DataValue> valuation = new Mapping<>();
    		for (int i = 0; i < vals.length; i++) {
    			SuffixValue sv = new SuffixValue(vals[i].getDataType(), i+1);
    			valuation.put(sv, vals[i]);
    		}
    		if (solver.isSatisfiable(guard, valuation)) {
    			return Optional.of(e.getKey());
    		}
    	}

    	return Optional.empty();
    }

    /**
     * @return a set of all initial guards in this branching
     */
    default Set<Expression<Boolean>> guardSet() {
    	Set<Expression<Boolean>> guards = new LinkedHashSet<>();
    	guards.addAll(getBranches().values());
    	return guards;
    }

}
