package de.learnlib.ralib;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import de.learnlib.api.Query;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * Basic caching wrapper that can be used when learning acceptor RAs.
 */
public class CacheDataWordOracle extends QueryCounter implements DataWordOracle {
	private static LearnLogger log = LearnLogger.getLogger(CacheDataWordOracle.class);

	private static class CacheNode {
		final Map<PSymbolInstance, CacheNode> next = new LinkedHashMap<>();
		Boolean output = null;
	}

	private final CacheNode root = new CacheNode();
	private DataWordOracle oracle;

	public CacheDataWordOracle(DataWordOracle oracle) {
		this.oracle = oracle;
	}

	@Override
	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		for (Query<PSymbolInstance, Boolean> query : queries) {
			log.log(Level.FINEST, "MQ: {0}", query.getInput());
			boolean answer = traceBoolean(query.getInput());
			query.answer(answer);
		}
		countQueries(queries.size());
	}

    private boolean traceBoolean(Word<PSymbolInstance> query) {
        Boolean ret = answerFromCache(query);
        if (ret != null) {
            return ret;
        }
        ret = oracle.answerQuery(query);
        addToCache(query, ret);
        return ret;
    }

	private void addToCache(Word<PSymbolInstance> query, boolean response) {
		Iterator<PSymbolInstance> iter = query.iterator();
		CacheNode cur = root;
		while (iter.hasNext()) {
			PSymbolInstance in = iter.next();

			CacheNode next = cur.next.get(in);
			if (next == null) {
				next = new CacheNode();
				cur.next.put(in, next);
			}

		}
		cur.output = response;
	}

	private Boolean answerFromCache(Word<PSymbolInstance> query) {
		Iterator<PSymbolInstance> iter = query.iterator();
		CacheNode cur = root;

		while (iter.hasNext()) {
			PSymbolInstance in = iter.next();
			cur = cur.next.get(in);
			if (cur == null) {
				return null;
			}
		}
		return cur.output;
	}
}
