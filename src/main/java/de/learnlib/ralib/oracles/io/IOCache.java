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
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.words.OutputSymbol;
import java.util.Collection;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * The IO-Cache can be used to reduce queries for deterministic 
 * IO-Systems, i.e., where even fresh values are chosen 
 * deterministically!
 * 
 * @author falk
 */
public class IOCache extends QueryCounter implements DataWordOracle {
    
    private static class CacheNode {        
        final Map<PSymbolInstance, PSymbolInstance> output = new HashMap<>();
        final Map<PSymbolInstance, CacheNode> next = new HashMap<>();
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
            boolean accepted = trace(q.getInput());
            q.answer(accepted);
        }
    }
    
    public boolean trace(Word<PSymbolInstance> query) {
        Boolean ret = answerFromCache(query);
        if (ret != null) {
            return ret;
        }
        Word<PSymbolInstance> test = query;
        if(query.length() % 2 != 0) {
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
        while (iter.hasNext()) {
            PSymbolInstance in = iter.next();
            if (!iter.hasNext()) {
                // only input is left ...
                return Boolean.TRUE;
            }
            PSymbolInstance ref = iter.next();
            
            out = cur.output.get(in);
            cur = cur.next.get(in);

            if (out == null) {
                return null;
            }
            
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

}
