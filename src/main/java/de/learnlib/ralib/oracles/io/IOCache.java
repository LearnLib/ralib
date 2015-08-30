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

import de.learnlib.api.Query;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * The IO-Cache can be used to reduce queries for deterministic IO-Systems,
 * i.e., where even fresh values are chosen deterministically!
 *
 * @author falk
 */
public class IOCache extends IOOracle implements DataWordOracle {

    private static class CacheNode {

        final Map<PSymbolInstance, PSymbolInstance> output = new LinkedHashMap<>();
        final Map<PSymbolInstance, CacheNode> next = new LinkedHashMap<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Entry<PSymbolInstance, PSymbolInstance> e : output.entrySet()) {
                sb.append(e.getKey()).append(":").append(e.getValue()).append(", ");
            }
            return sb.toString();
        }
        
        
    }

    private final CacheNode root = new CacheNode();

    private final IOOracle sul;

    private static LearnLogger log = LearnLogger.getLogger(IOCache.class);

    public IOCache(IOOracle sul) {
        this.sul = sul;
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
        //System.out.println("adding to cache: " + trace);
        addToCache(trace);
        ret = answerFromCache(query);
        //System.out.println("getting from cache: " + ret);        
        return ret;
    }

    private Boolean answerFromCache(Word<PSymbolInstance> query) {
        Iterator<PSymbolInstance> iter = query.iterator();
        PSymbolInstance out = null;
        CacheNode cur = root;

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
                    //System.out.println("Making " + d + " rep.");
                    replacements.put(d, d);
                    r = d;
                }
                //System.out.println("Using " + r + " as rep. for " + d);
                dvInRepl[i] = r;
            }

            in = new PSymbolInstance(in.getBaseSymbol(), dvInRepl);

            PSymbolInstance ref = iter.next();

            out = cur.output.get(in);
            cur = cur.next.get(in);

            if (out == null) {
                //System.err.println("Output null for input " + in);
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
                    //System.out.println("Making " + f + " rep. for " + d);
                    replacements.put(d, f);
                }
                DataValue r = replacements.containsKey(d) ? replacements.get(d) : d;
                //System.out.println("Using " + r + " as rep. for " + d);
                dvRefRepl[i] = r;
            }

            ref = new PSymbolInstance(ref.getBaseSymbol(), dvRefRepl);

            if (!out.equals(ref)) {
                return Boolean.FALSE;
            }

        }
        return Boolean.TRUE;
    }

    private void addToCache(Word<PSymbolInstance> query) {
        assert query.length() % 2 == 0;
        Iterator<PSymbolInstance> iter = query.iterator();
        CacheNode cur = root;
        Word<PSymbolInstance> trace = Word.epsilon();
        while (iter.hasNext()) {
            PSymbolInstance in = iter.next();
            PSymbolInstance out = iter.next();
            trace = trace.append(in);
            CacheNode next = cur.next.get(in);
            //System.out.println("Cache node for " + trace + ": " + next);
            if (next == null) {
                next = new CacheNode();
                cur.next.put(in, next);
                cur.output.put(in, out);
            }

            if (!out.equals(cur.output.get(in))) {
                System.err.println("Cache Error: " + out + " vs " + cur.output.get(in) + " after " + trace);
                assert false;
            }
            trace.append(out);
            cur = next;
        }
    }

    private Word<PSymbolInstance> traceFromCache(Word<PSymbolInstance> query) {
        Word<PSymbolInstance> trace = Word.epsilon();
        
        Iterator<PSymbolInstance> iter = query.iterator();
        PSymbolInstance out = null;
        CacheNode cur = root;

        while (iter.hasNext()) {

            PSymbolInstance in = iter.next();           
            PSymbolInstance ref = iter.next();

            out = cur.output.get(in);
            cur = cur.next.get(in);

            if (out == null) {
                return null;
            }

            trace = trace.append(in).append(out);
            
        }
        return trace;
    }

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        if (query.length() % 2 != 0) {
            query = query.append(new PSymbolInstance(new OutputSymbol("__cache_dummy")));
        }

        Word<PSymbolInstance> trace = traceFromCache(query);
        if (trace != null) {
            return trace;
        }
        trace = sul.trace(query);
        addToCache(trace);
        return trace;
    }
}
