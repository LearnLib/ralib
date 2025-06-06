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
package de.learnlib.ralib.example.sdts;

import java.util.Map;

import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class LoginExampleBranching implements Branching {

    private final Map<Word<PSymbolInstance>, Expression<Boolean>> branches;

    public LoginExampleBranching(Map<Word<PSymbolInstance>, Expression<Boolean>> branches) {
        this.branches = branches;
    }

    @Override
    public Map<Word<PSymbolInstance>, Expression<Boolean>> getBranches() {
        return branches;
    }

    @Override
    public Word<PSymbolInstance> transformPrefix(Word<PSymbolInstance> dw) {
    	throw new RuntimeException("Not implemented");
    }

}
