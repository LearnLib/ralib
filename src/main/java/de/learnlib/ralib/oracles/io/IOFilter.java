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
package de.learnlib.ralib.oracles.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;

import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.query.Query;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * filters out queries that do not alternate input and output
 *
 * @author falk
 */
public class IOFilter extends QueryCounter implements DataWordOracle {

    private final Collection<ParameterizedSymbol> inputs;

    private final DataWordOracle back;

    private static LearnLogger log = LearnLogger.getLogger(IOFilter.class);

    public IOFilter(DataWordOracle back, ParameterizedSymbol ... inputs) {
        this.inputs = new LinkedHashSet<>(Arrays.asList(inputs));
        this.back = back;
    }

    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
        countQueries(clctn.size());
        List<Query<PSymbolInstance, Boolean>> valid = new ArrayList<>();
        for (Query<PSymbolInstance, Boolean> q : clctn) {
            log.log(Level.FINEST, "MQ: {0}", q.getInput());
            if (isValid(q.getInput())) {
                valid.add(q);
            } else {
                q.answer(Boolean.FALSE);
            }
        }
       back.processQueries(valid);
    }

    private boolean isValid(Word<PSymbolInstance> query) {
        boolean inExp = true;
        for (PSymbolInstance psi : query) {
            boolean isInput = (this.inputs.contains(psi.getBaseSymbol()));
            if (inExp ^ isInput) {
                return false;
            }
            inExp = !inExp;
        }

        return true;
    }

}
