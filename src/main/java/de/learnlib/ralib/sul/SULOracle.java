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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class SULOracle extends IOOracle {

    private final DataWordSUL sul;

    private final ParameterizedSymbol error;

    private static LearnLogger log = LearnLogger.getLogger(SULOracle.class);

    private final Map<DataValue, Set<DataValue>> replacements = new HashMap<>();

    public SULOracle(DataWordSUL sul, ParameterizedSymbol error) {
        this.sul = sul;
        this.error = error;
    }

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        countQueries(1);
        Word<PSymbolInstance> act = query;
        log.log(Level.FINEST, "MQ: {0}", query);
        sul.pre();
        replacements.clear();
        Word<PSymbolInstance> trace = Word.epsilon();
        for (int i = 0; i < query.length(); i += 2) {
            PSymbolInstance in = applyReplacements(act.getSymbol(i));

            PSymbolInstance out = sul.step(in);
            updateReplacements(act.getSymbol(i + 1), out);

            trace = trace.append(in).append(out);

            if (out.getBaseSymbol().equals(error)) {
                break;
            }
        }

        if (trace.length() < query.length()) {

            // fill with errors
            for (int i = trace.length(); i < query.length(); i += 2) {
                trace = trace.append(query.getSymbol(i)).append(new PSymbolInstance(error));
            }
        }

        sul.post();
        return trace;
    }

    private PSymbolInstance applyReplacements(PSymbolInstance symbol) {
        DataValue[] vals = new DataValue[symbol.getBaseSymbol().getArity()];
        for (int i = 0; i < symbol.getBaseSymbol().getArity(); i++) {
            Set<DataValue> set = getOrCreate(symbol.getParameterValues()[i]);
            if (set.size() < 1) {
                vals[i] = symbol.getParameterValues()[i];
            } else {
                vals[i] = set.iterator().next();
            }
        }

        return new PSymbolInstance(symbol.getBaseSymbol(), vals);
    }

    private void updateReplacements(
            PSymbolInstance outTest, PSymbolInstance outSys) {

        for (int i = 0; i < outSys.getBaseSymbol().getArity(); i++) {
            Set<DataValue> set = getOrCreate(outSys.getParameterValues()[i]);
            set.add(outSys.getParameterValues()[i]);
            assert set.size() <= 1;
        }
    }

    private Set<DataValue> getOrCreate(DataValue key) {
        Set<DataValue> ret = replacements.get(key);
        if (ret == null) {
            ret = new HashSet<>();
            replacements.put(key, ret);
        }
        return ret;
    }

}
