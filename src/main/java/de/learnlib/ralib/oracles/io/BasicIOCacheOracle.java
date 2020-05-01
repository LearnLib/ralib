/*
 * Copyright (C) 2015 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.oracles.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import de.learnlib.api.Query;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.oracles.io.IOCache.CacheNode;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * The IO-CacheOracle can be used to reduce queries for deterministic IO-Systems,
 * i.e., where even fresh values are chosen deterministically!
 *
 * @author falk
 */
public class BasicIOCacheOracle extends QueryCounter implements DataWordIOOracle {

    private final IOOracle sul;

	private IOCache cache;

    private static LearnLogger log = LearnLogger.getLogger(BasicIOCacheOracle.class);

    public BasicIOCacheOracle(IOOracle sul) {
        this.sul = sul;
        this.cache = new IOCache();
    }
    
    public BasicIOCacheOracle(IOOracle sul, IOCache cache) {
    	this.sul = sul;
    	this.cache = cache;
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
        Boolean ret = answerFromCache(query);
        if (ret != null) {
            return ret;
        }
        Word<PSymbolInstance> test = query;
        if (query.length() % 2 != 0) {
            test = query.append(new PSymbolInstance(new OutputSymbol("__cache_dummy")));
        }
        Word<PSymbolInstance> trace = sul.trace(test);
        cache.addToCache(trace);
        ret = answerFromCache(query);
        return ret;
    }

    private Boolean answerFromCache(Word<PSymbolInstance> query) {
        Iterator<PSymbolInstance> iter = query.iterator();
        PSymbolInstance out = null;
        CacheNode cur = cache.getRoot();

        Map<DataValue, DataValue> replacements = new HashMap<>();

        while (iter.hasNext()) {

            PSymbolInstance in = iter.next();
            if (!iter.hasNext()) {
                // only input is left ...
                return Boolean.TRUE;
            }

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

            PSymbolInstance ref = iter.next();

            out = cur.output.get(in);
            cur = cur.next.get(in);

            if (out == null) {
                return null;
            }

            if (!out.getBaseSymbol().equals(ref.getBaseSymbol())) {
                return Boolean.FALSE;
            }

            DataValue[] dvRefRepl = new DataValue[ref.getBaseSymbol().getArity()];

            // process new replacements
            for (int i = 0; i < dvRefRepl.length; i++) {
                DataValue f = out.getParameterValues()[i];
                DataValue d = ref.getParameterValues()[i];
                if (f instanceof FreshValue) {
                    assert !replacements.containsKey(d);
                    replacements.put(d, f);
                }
                DataValue r = replacements.containsKey(d) ? replacements.get(d) : d;
                dvRefRepl[i] = r;
            }

            ref = new PSymbolInstance(ref.getBaseSymbol(), dvRefRepl);

            if (!out.equals(ref)) {
                return Boolean.FALSE;
            }

        }
        return Boolean.TRUE;
    }

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        if (query.length() % 2 != 0) {
            query = query.append(new PSymbolInstance(new OutputSymbol("__cache_dummy")));
        }

        Word<PSymbolInstance> trace = cache.traceFromCache(query);
        if (trace != null) {
            return trace;
        }
        trace = sul.trace(query);
        cache.addToCache(trace);
        return trace;
    }
}
