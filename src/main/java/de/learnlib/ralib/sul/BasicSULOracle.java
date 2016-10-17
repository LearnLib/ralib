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

import java.util.logging.Level;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class BasicSULOracle extends IOOracle {

    private final DataWordSUL sul;

    private final ParameterizedSymbol error;

    private static LearnLogger log = LearnLogger.getLogger(BasicSULOracle.class);

    public BasicSULOracle(DataWordSUL sul, ParameterizedSymbol error) {
        this.sul = sul;
        this.error = error;
    }

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        countQueries(1);
        Word<PSymbolInstance> act = query;
        log.log(Level.FINEST, "MQ: {0}", query);
        sul.pre();
        Word<PSymbolInstance> trace = Word.epsilon();
        for (int i = 0; i < query.length(); i += 2) {
            PSymbolInstance in = act.getSymbol(i);
            
            PSymbolInstance out = sul.step(in);

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
}
