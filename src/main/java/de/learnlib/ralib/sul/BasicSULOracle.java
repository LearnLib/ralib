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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * Previous implementation of the SUL Oracle. Works well for fresh and in/equality. Doesn't work for
 * sumC/increments. 
 */
public class BasicSULOracle implements IOOracle {

    private final DataWordSUL sul;

    private final ParameterizedSymbol error;

    private static LearnLogger log = LearnLogger.getLogger(BasicSULOracle.class);

    private final Map<DataValue, Set<DataValue>> replacements = new HashMap<>();
  
    public BasicSULOracle(DataWordSUL sul, ParameterizedSymbol error) {
        this.sul = sul;
        this.error = error;
    }

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        Word<PSymbolInstance> act = query;
        sul.pre();
        replacements.clear();
        Word<PSymbolInstance> trace = Word.epsilon();
        for (int i = 0; i < query.length(); i += 2) {
            PSymbolInstance in = applyReplacements(act.getSymbol(i));
            trace = trace.append(in);
            PSymbolInstance out = sul.step(in);
            if (act.size() > i+1) {
	            updateReplacements(out);
	
	            trace = trace.append(out);
	
	            if (out.getBaseSymbol().equals(error)) {
	                break;
	            } 
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

    private void updateReplacements( PSymbolInstance outSys) {

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
    
    
    public TraceCanonizer getTraceCanonizer() {
    	return new BasicTraceCanonizer();
    }
    
    
    // This trace canonizer should be consistent with the above implementation
    private static class BasicTraceCanonizer implements TraceCanonizer {

    	@Override
		public Word<PSymbolInstance> canonizeTrace(Word<PSymbolInstance> trace) {
		    Iterator<PSymbolInstance> iter = trace.iterator();
		    Word<PSymbolInstance> canonizedTrace = Word.epsilon();
		    PSymbolInstance out = null;

		    Map<DataValue, DataValue> replacements = new HashMap<>();

		    while (iter.hasNext()) {
		        PSymbolInstance in = iter.next();

		        DataValue[] dvInRepl = new DataValue[in.getBaseSymbol().getArity()];
		        for (int i = 0; i < dvInRepl.length; i++) {
		            DataValue d = in.getParameterValues()[i];
		            DataValue r = replacements.get(d);
		            if (r == null) {
		                replacements.put(d, d);
		                r = d;
		            }
		            dvInRepl[i] = r;
		        }

		        in = new PSymbolInstance(in.getBaseSymbol(), dvInRepl);
		        canonizedTrace = canonizedTrace.append(in);
		        
		        if (!iter.hasNext()) {
		        	return canonizedTrace;
		        }

		        PSymbolInstance ref = iter.next();

		        DataValue[] dvRefRepl = new DataValue[ref.getBaseSymbol().getArity()];

		        // process new replacements
		        for (int i = 0; i < dvRefRepl.length; i++) {
		            DataValue d = ref.getParameterValues()[i];
		            DataValue r = replacements.containsKey(d) ? replacements.get(d) : d;
		            dvRefRepl[i] = r;
		        }

		        ref = new PSymbolInstance(ref.getBaseSymbol(), dvRefRepl);
		        
		        canonizedTrace = canonizedTrace.append(ref);

		    }
			return canonizedTrace;
		}
    	
    }

}
