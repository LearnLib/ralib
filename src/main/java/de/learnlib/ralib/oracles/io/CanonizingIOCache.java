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

import de.learnlib.api.Query;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.words.OutputSymbol;
import java.util.Collection;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * The IO-Cache can be used to reduce queries for deterministic IO-Systems,
 * i.e., where even fresh values are chosen deterministically!
 * 
 * This implementation only stores canonized traces in the cache. Every 
 * 
 *
 * @author falk
 */
public class CanonizingIOCache extends IOOracle implements DataWordOracle {

    private final IOOracle sul;

	private Supplier<ValueCanonizer> canonizerSupplier;

	private IOCache cache;

    private static LearnLogger log = LearnLogger.getLogger(CanonizingIOCache.class);

    public CanonizingIOCache(IOOracle sul, Supplier<ValueCanonizer> canonizerSupplier) {
        this.sul = sul;
        this.canonizerSupplier = canonizerSupplier;
        this.cache = new IOCache();
    }
    
    

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        if (query.length() % 2 != 0) {
            query = query.append(new PSymbolInstance(new OutputSymbol("__cache_dummy")));
        }
        
        Word<PSymbolInstance> canonicalQuery = this.canonizerSupplier.get().canonize(query, false);

        Word<PSymbolInstance> trace = this.cache.traceFromCache(canonicalQuery);
        if (trace != null) {
            return trace;
        }
        trace = this.sul.trace(query);
        Word<PSymbolInstance> canonicalTrace = this.canonizerSupplier.get().canonize(trace, false);
        
        this.cache.addToCache(canonicalTrace);
        
        return canonicalTrace;
    }

    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
        countQueries(clctn.size());
        for (Query<PSymbolInstance, Boolean> q : clctn) {
            log.log(Level.FINEST, "MQ: {0}", q.getInput());
            boolean accepted = traceBoolean(q.getInput());
            q.answer(accepted);
        }
    }

    private boolean traceBoolean(Word<PSymbolInstance> query) {
    	ValueCanonizer canonizer = this.canonizerSupplier.get();
    	Word<PSymbolInstance> canonicalTrace = canonizer.canonize(query, false);
    	
        Boolean ret = this.cache.answerFromCache(canonicalTrace);
        if (ret != null) {
            return ret;
        }
        
        Word<PSymbolInstance> test = query;
        if (query.length() % 2 != 0) {
            test = query.append(new PSymbolInstance(new OutputSymbol("__cache_dummy")));
        }
        Word<PSymbolInstance> trace = sul.trace(test);
        canonicalTrace = this.canonizerSupplier.get().canonize(trace, false);
        
        this.cache.addToCache(canonicalTrace);
        ret = this.cache.answerFromCache(canonicalTrace);
        return ret;
    }
}
