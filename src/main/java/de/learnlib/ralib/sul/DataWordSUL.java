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
package de.learnlib.ralib.sul;

import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.sul.SUL;

/**
 *
 * @author falk
 */
public abstract class DataWordSUL implements SUL<PSymbolInstance, PSymbolInstance> {

    private long resets = 0;

    private long inputs = 0;

    protected void countResets(int n) {
        resets += n;
    }

    protected void countInputs(int n) {
        inputs += n;
    }

    /**
     * @return the resets
     */
    public long getResets() {
        return resets;
    }

    /**
     * @return the inputs
     */
    public long getInputs() {
        return inputs;
    }


}
