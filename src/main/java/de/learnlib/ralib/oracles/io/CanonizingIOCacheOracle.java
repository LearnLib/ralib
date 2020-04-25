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

import java.util.Collection;
import java.util.logging.Level;

import de.learnlib.api.Query;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * The {@link CanonizingIOCacheOracle} is caching-enabled DataWordOracle which is used when support for arbitrary fresh values is required.
 * All queries are canonized via a {@link TraceCanonizer}, and then looked up in the cache.
 * If the answer is not found, the encapsulated {@link IOOracle} is used.
 * 
 * Using a {@link TraceCanonizer} enables support also for storing traces with fresh values.
 *   
 * @author falk, paul
 */
public class CanonizingIOCacheOracle implements DataWordIOOracle {

    private final IOOracle sul;

	private final IOCache ioCache;

	private final TraceCanonizer traceCanonizer;

    private static LearnLogger log = LearnLogger.getLogger(CanonizingIOCacheOracle.class);
    
    public final static PSymbolInstance CACHE_DUMMY = new PSymbolInstance(new OutputSymbol("__cache_dummy"));

    
    public CanonizingIOCacheOracle(IOOracle sul) {
    	this(sul, new IOCache());
    }
    
    public CanonizingIOCacheOracle(IOOracle sul,  IOCache ioCache) {
        this.sul = sul;
        this.ioCache = ioCache;
        this.traceCanonizer = sul.getTraceCanonizer();
    }

    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
        for (Query<PSymbolInstance, Boolean> q : clctn) {
            log.log(Level.FINEST, "MQ: {0}", q.getInput());
            boolean accepted = traceBoolean(q.getInput());
            q.answer(accepted);
        }
    }

    private boolean traceBoolean(Word<PSymbolInstance> query) {
    	Word<PSymbolInstance> fixedQuery = traceCanonizer.canonize(query); 
        Boolean ret = ioCache.answerFromCache(fixedQuery);
        if (ret != null) {
            return ret;
        }
        Word<PSymbolInstance> test = fixedQuery;
        if (fixedQuery.length() % 2 != 0) {
            test = fixedQuery.append(CACHE_DUMMY);
        }
        Word<PSymbolInstance> trace  = null;
        boolean added = false;
	    trace = sul.trace(test);
	    added = ioCache.addToCache(trace);
	
        //assert added;
        ret = ioCache.answerFromCache(fixedQuery);
        if (ret == null)  {
        	throw new DecoratedRuntimeException("Could not find answer for query, even after "
        			+ "it had been added to cache")
        	.addDecoration("fixedQuery", fixedQuery).addDecoration("original query", query)
        	.addDecoration("trace", trace);
        }
        return ret;
    }

    
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
    	Word<PSymbolInstance> fixedQuery = traceCanonizer.canonize(query); 
        if (fixedQuery.length() % 2 != 0) {
        	fixedQuery = fixedQuery.append(new PSymbolInstance(new OutputSymbol("__cache_dummy")));
        }

        Word<PSymbolInstance> trace = ioCache.traceFromCache(fixedQuery, traceCanonizer);
        if (trace != null) {
            return trace;
        }
        trace = sul.trace(fixedQuery);
        boolean added = ioCache.addToCache(trace);
//        assert added;
        return trace;
    }
}
