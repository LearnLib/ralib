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
import java.util.Optional;
import java.util.logging.Level;

import de.learnlib.api.Query;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.tools.theories.TraceCanonizer;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * The IO-Cache can be used to reduce queries for deterministic IO-Systems,
 * i.e., where even fresh values are chosen deterministically!
 *
 * @author falk
 */
public class IOCacheOracle extends IOOracle implements DataWordOracle {

    private final IOOracle sul;

	private final IOCache ioCache;

	private Optional<TraceCanonizer> fixer;

    private static LearnLogger log = LearnLogger.getLogger(IOCacheOracle.class);

    public IOCacheOracle(IOOracle sul) {
        this.sul = sul;
        this.ioCache = new IOCache();
        this.fixer = Optional.empty();
    }
    
    public IOCacheOracle(IOOracle sul, TraceCanonizer fixer) {
        this.sul = sul;
        this.ioCache = new IOCache();
        this.fixer = Optional.of(fixer);
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
    	query = this.fixer.isPresent() ? 
    			this.fixer.get().fixTrace(query) 
    			: query; 
        Boolean ret = this.ioCache.answerFromCache(query);
        if (ret != null) {
            return ret;
        }
        Word<PSymbolInstance> test = query;
        if (query.length() % 2 != 0) {
            test = query.append(new PSymbolInstance(new OutputSymbol("__cache_dummy")));
        }
        Word<PSymbolInstance> trace = sul.trace(test);
        this.ioCache.addToCache(trace);
        ret = this.ioCache.answerFromCache(query);
        return ret;
    }

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        if (query.length() % 2 != 0) {
            query = query.append(new PSymbolInstance(new OutputSymbol("__cache_dummy")));
        }

        Word<PSymbolInstance> trace = this.ioCache.traceFromCache(query);
        if (trace != null) {
            return trace;
        }
        trace = sul.trace(query);
        this.ioCache.addToCache(trace);
        return trace;
    }
}
