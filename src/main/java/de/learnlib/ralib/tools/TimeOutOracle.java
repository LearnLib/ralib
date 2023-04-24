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
package de.learnlib.ralib.tools;

import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.api.Query;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Collection;

/**
 * Uses a Register Automaton to simulate a SUL.
 *
 * @author falk
 */
public class TimeOutOracle implements DataWordOracle {

    private final DataWordOracle back;

    private final long timeoutMillis;

    public TimeOutOracle(DataWordOracle back, long timeoutMillis) {
        this.back = back;
        this.timeoutMillis = System.currentTimeMillis() + timeoutMillis;
    }

    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {

        if (System.currentTimeMillis() > timeoutMillis) {
            throw new TimeOutException();
        }

        back.processQueries(clctn);
    }

}
