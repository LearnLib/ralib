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

import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.tools.theories.TraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * A canonizing SULOracle is a SULOracle that also performs canonicalization of the whole query 
 * prior to the execution of each input. This ensures that in the end, a fully canonical trace
 * is run on the SUL.
 * 
 * Example:
 * reg 0 / out 3  in 1 4 / false is transformed to
 * reg 0 / out 1  in 2 3 / false 
 *  
 */
public class CanonizingSULOracle extends IOOracle {

    private final DataWordSUL canonizedSul;

    private final ParameterizedSymbol error;

	private final TraceCanonizer traceFixer;

  
    public CanonizingSULOracle(DataWordSUL canonizedSul, ParameterizedSymbol error, TraceCanonizer traceFixer) {
        this.canonizedSul = canonizedSul;
        this.error = error;
        this.traceFixer = traceFixer;
    }

    
    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        countQueries(1);
        canonizedSul.pre();
        Word<PSymbolInstance> trace = Word.epsilon();
        Word<PSymbolInstance> fixedQuery = query;

        for (int i = 0; i < query.length(); i += 2) {
        	fixedQuery = trace.concat(fixedQuery.suffix(fixedQuery.size() - trace.size()));
        	fixedQuery = traceFixer.fixTrace(fixedQuery);
            PSymbolInstance in = fixedQuery.getSymbol(i);
            
//            PSymbolInstance in = query.getSymbol(i);
            
            PSymbolInstance out = canonizedSul.step(in);
            
           // out = canonizer.canonize(out, false);

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
        
        canonizedSul.post();
        return trace;
    }
}
