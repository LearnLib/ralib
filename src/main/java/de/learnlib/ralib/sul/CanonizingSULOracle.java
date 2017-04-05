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

import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * A canonizing SULOracle performs canonicalization on the trace + input
 * before executing each input.  
 *  
 */
public class CanonizingSULOracle implements IOOracle{

    private final DataWordSUL canonizedSul;

    private final ParameterizedSymbol error;

	private final TraceCanonizer traceCanonizer;

  
    public CanonizingSULOracle(DataWordSUL canonizedSul, ParameterizedSymbol error, TraceCanonizer traceCanonizer) {
        this.canonizedSul = canonizedSul;
        this.error = error;
        this.traceCanonizer = traceCanonizer;
    }

    /**
     * Returns a canonical trace.
     */
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        Word<PSymbolInstance> trace = Word.epsilon();
        Word<PSymbolInstance> fixedQuery = query;
        canonizedSul.pre();
       
        for (int i = 0; i < query.length(); i += 2) {
        	fixedQuery = trace.concat(fixedQuery.suffix(fixedQuery.size() - trace.size()));
        	fixedQuery = traceCanonizer.canonizeTrace(fixedQuery);
            PSymbolInstance in = fixedQuery.getSymbol(i);
            trace = trace.append(in);
            PSymbolInstance out = null;
            try {
            	out = canonizedSul.step(in);
            } catch(DecoratedRuntimeException exc) {
            	exc.addDecoration("trace", trace);
            	throw exc;
            }
            trace = trace.append(out);

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
        
        canonizedSul.post();
        return trace;
    }
}
