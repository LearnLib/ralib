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
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * The IO-Cache can be used to reduce queries for deterministic IO-Systems,
 * i.e., where even fresh values are chosen deterministically!
 *
 * It maintains a cache in which it stores only canonized traces. All queries
 * are canonized, and then searched for in the cache. 
 * @author falk
 */
public class IOCacheOracle implements DataWordIOOracle {

    private final IOOracle sul;

	private final IOCache ioCache;

	private final TraceCanonizer traceCanonizer;

    private static LearnLogger log = LearnLogger.getLogger(IOCacheOracle.class);
    
    public final static PSymbolInstance CACHE_DUMMY = new PSymbolInstance(new OutputSymbol("__cache_dummy"));

    
    public IOCacheOracle(IOOracle sul) {
    	this(sul, new IOCache(), trace -> trace);
    }
    
    public IOCacheOracle(IOOracle sul, TraceCanonizer canonizer) {
    	this(sul, new IOCache(), canonizer);
    }
    
    public IOCacheOracle(IOOracle sul,  IOCache ioCache, TraceCanonizer canonizer) {
        this.sul = sul;
        this.ioCache = ioCache;
        this.traceCanonizer = canonizer;
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
    	Word<PSymbolInstance> fixedQuery = this.traceCanonizer.canonizeTrace(query); 
        Boolean ret = this.ioCache.answerFromCache(fixedQuery);
        if (ret != null) {
            return ret;
        }
        Word<PSymbolInstance> test = fixedQuery;
        if (fixedQuery.length() % 2 != 0) {
            test = fixedQuery.append(CACHE_DUMMY);
        }
        Word<PSymbolInstance> trace  = null;
        boolean added = false;
	    trace = this.sul.trace(test);
	    added = this.ioCache.addToCache(trace);
	
        //assert added;
        ret = this.ioCache.answerFromCache(fixedQuery);
        if (ret == null)  {
        	for (int i=0; i<5; i++) {
        		System.out.println(this.sul.trace(test));
        	}
        	throw new DecoratedRuntimeException("Could not find answer for query, even after "
        			+ "it had been added to cache")
        	.addDecoration("fixedQuery", fixedQuery).addDecoration("original query", query)
        	.addDecoration("trace", trace);
        }
        return ret;
    }

    
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
    	Word<PSymbolInstance> fixedQuery = this.traceCanonizer.canonizeTrace(query); 
        if (fixedQuery.length() % 2 != 0) {
        	fixedQuery = fixedQuery.append(new PSymbolInstance(new OutputSymbol("__cache_dummy")));
        }

        Word<PSymbolInstance> trace = this.ioCache.traceFromCache(fixedQuery, this.traceCanonizer);
        if (trace != null) {
            return trace;
        }
        trace = sul.trace(fixedQuery);
        boolean added = this.ioCache.addToCache(trace);
//        assert added;
        return trace;
    }
}
