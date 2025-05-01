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


import de.learnlib.ralib.data.*;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

/**
 * The SDTLogicOracle offers functions that are needed for
 * processing counterexamples and constructing branching.
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
     * @param sdt2
     * @param guard
     * @param rep
     * @return
     */
    boolean hasCounterexample(Word<PSymbolInstance> prefix,
            SDT sdt1, SDT sdt2, Expression<Boolean> guard, Word<PSymbolInstance> rep);

    /**
     *
     * @param prefix
     * @param sdt1
     * @param sdt2
     * @return
     */
    // TODO: write documentation
    Expression<Boolean> getCEGuard(Word<PSymbolInstance> prefix, SDT sdt1, SDT sdt2);

    /**
     * checks if one guard refine another guard.
     *
     * @param refining
     * @param refined
     * @return
     */
    boolean doesRefine(Expression<Boolean> refining, Expression<Boolean> refined,
                       Mapping<SymbolicDataValue, DataValue> valuation);

    /**
     * Returns true if two guards are mutually exclusive (they cannot be both true)
     */
    boolean areMutuallyExclusive(Expression<Boolean> guard1, Expression<Boolean> guard2,
			Mapping<SymbolicDataValue, DataValue> valuation);

    /**
     * Returns true if two guards are equivalent (guard1 is true iff guard2 is true)
     */
    boolean areEquivalent(Expression<Boolean> guard1, Bijection<DataValue> remapping, Expression<Boolean> guard2,
                          Mapping<SymbolicDataValue, DataValue> valuation);
    /**
     * Returns true if the word leads to an accepting leaf on the SDT.
     */
    // FIXME: this method is only used by tests, maybe it can go?
    boolean accepts(Word<PSymbolInstance> word, Word<PSymbolInstance> prefix, SDT sdt);
}
