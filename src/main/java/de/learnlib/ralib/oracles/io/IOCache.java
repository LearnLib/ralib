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
import de.learnlib.ralib.words.OutputSymbol;
import java.util.Collection;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
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
        addToCache(trace);
        ret = answerFromCache(query);
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

    private void addToCache(Word<PSymbolInstance> query) {
        assert query.length() % 2 == 0;
        Iterator<PSymbolInstance> iter = query.iterator();
        CacheNode cur = root;
        while (iter.hasNext()) {
            PSymbolInstance in = iter.next();
            PSymbolInstance out = iter.next();

            CacheNode next = cur.next.get(in);
            if (next == null) {
                next = new CacheNode();
                cur.next.put(in, next);
                cur.output.put(in, out);
            }

            assert out.equals(cur.output.get(in));
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
