package de.learnlib.ralib.oracles.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class IOCache {
	
    private final CacheNode root = new CacheNode();
	
    private static class CacheNode {

        final Map<PSymbolInstance, PSymbolInstance> output = new LinkedHashMap<>();
        final Map<PSymbolInstance, CacheNode> next = new LinkedHashMap<>();
    }

	
    void addToCache(Word<PSymbolInstance> query) {
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
}
