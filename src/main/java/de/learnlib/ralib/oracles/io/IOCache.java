package de.learnlib.ralib.oracles.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class IOCache {
	
    private final CacheNode root;
	
    static class CacheNode {

        final Map<PSymbolInstance, PSymbolInstance> output = new LinkedHashMap<>();
        final Map<PSymbolInstance, CacheNode> next = new LinkedHashMap<>();
        
        public String toString() {
        	StringBuilder builder = new StringBuilder().append("(");
        	output.keySet().forEach(key -> builder.append(key).append("\n ").append(output.get(key)).append("\n ")
        			.append(next.get(key).toString()));
        	builder.append(")");
        	return builder.toString();
        }
    }
    
    public IOCache() {
    	this.root = new CacheNode();
    }

	IOCache(CacheNode root) {
		this.root = root;
	}
    
    /**
     * Adds query to cache. Returns true if the cache was updated with information from the query, false, if the 
     * query was already found in the cache. 
     */
    boolean addToCache(Word<PSymbolInstance> query) {
        assert query.length() % 2 == 0;
        Iterator<PSymbolInstance> iter = query.iterator();
        CacheNode cur = root;
        boolean cacheUpdated = false;
        while (iter.hasNext()) {
            PSymbolInstance in = iter.next();
            PSymbolInstance out = iter.next();

            CacheNode next = cur.next.get(in);
            if (next != null) {
                assert out.equals(cur.output.get(in));
            } else  {
                next = new CacheNode();
                cur.next.put(in, next);
                cur.output.put(in, out);
                cacheUpdated = true;
            }

            cur = next;
        }
        return cacheUpdated;
    }
    
    Boolean answerFromCache(Word<PSymbolInstance> query) {
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

            PSymbolInstance ref = iter.next();

            out = cur.output.get(in);
            cur = cur.next.get(in);

            if (out == null) {
                return null;
            }

            if (!out.getBaseSymbol().equals(ref.getBaseSymbol())) {
                return Boolean.FALSE;
            }

            if (!out.equals(ref)) {
                return Boolean.FALSE;
            }

        }
        return Boolean.TRUE;
    }

    Word<PSymbolInstance> traceFromCache(Word<PSymbolInstance> query) {
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
    
    CacheNode getRoot() {
    	return this.root;
    }
}
