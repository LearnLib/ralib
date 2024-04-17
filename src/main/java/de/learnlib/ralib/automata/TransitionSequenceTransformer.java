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
package de.learnlib.ralib.automata;

import net.automatalib.word.Word;

/**
 * Interface for Automata that can transform an input word
 * into a representative word for the last passed transition
 * when processing said input word. (Usually this would be a
 * word from the set of prefixes in active automata learning).
 *
 * @author falk
 * @param <I> class of input symbols
 */
public interface TransitionSequenceTransformer<I> {

    /**
     * Returns the word representing the last transition
     * passed when processing the input word.
     *
     * @param word  the input word
     * @return  the word representing the last passed transition
     */
    public Word<I> transformTransitionSequence(Word<I> word);

}
