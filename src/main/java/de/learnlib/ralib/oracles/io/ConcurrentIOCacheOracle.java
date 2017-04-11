package de.learnlib.ralib.oracles.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import de.learnlib.api.Query;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class ConcurrentIOCacheOracle extends QueryCounter implements DataWordIOOracle{
		private final IOCache ioCache;

		private final TraceCanonizer traceCanonizer;

		private ConcurrentIOOracle concurrentSulOracle;

	    private static LearnLogger log = LearnLogger.getLogger(IOCacheOracle.class);
	    
	    public final static PSymbolInstance CACHE_DUMMY = new PSymbolInstance(new OutputSymbol("__cache_dummy"));

	    
	    public ConcurrentIOCacheOracle(List<IOOracle> suls) {
	    	this(suls, new IOCache(), trace -> trace);
	    }
	    
	    public ConcurrentIOCacheOracle(List<IOOracle> suls, TraceCanonizer canonizer) {
	    	this(suls, new IOCache(), canonizer);
	    }
	    
	    public ConcurrentIOCacheOracle(List<IOOracle> suls,  IOCache ioCache, TraceCanonizer canonizer) {
	        this.concurrentSulOracle = new ConcurrentIOOracle(suls);
	        this.ioCache = ioCache;
	        this.traceCanonizer = canonizer;
	    }

	    @Override
	    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
	        countQueries(clctn.size());
	        List<Query<PSymbolInstance, Boolean>> queriesToProcess = answerQueriesFromCache(new ArrayList<>(clctn));
	        List<Word<PSymbolInstance>> traceQueries = queriesToProcess.stream().map(qtp -> qtp.getInput())
	        		.map(tr -> { 
	        			if (tr.size()%2==1) return tr.append(CACHE_DUMMY);
	        			else return tr;
	        		}).collect(Collectors.toList());
	        List<Word<PSymbolInstance>> traceAnswers = this.concurrentSulOracle.processQueries(traceQueries);
	        traceAnswers.forEach(ans -> this.ioCache.addToCache(ans));
	        List<Query<PSymbolInstance, Boolean>> remaining = answerQueriesFromCache(queriesToProcess);
	        if (!remaining.isEmpty()) {
	        	System.out.println(remaining);
	        	System.exit(0);
	        }
	        assert remaining.isEmpty();
	    }
	    
	    // Returns unanswered queries
	    public List<Query<PSymbolInstance, Boolean>> answerQueriesFromCache(List<Query<PSymbolInstance, Boolean>> clctn) {
	        List<Query<PSymbolInstance, Boolean>> unansweredQueries = new ArrayList<>(clctn.size());
	        for (Query<PSymbolInstance, Boolean> q : clctn) {
	            Boolean answer = traceBoolean(q.getInput());
	            if (answer != null)
	            	q.answer(answer);
	            else
	            	unansweredQueries.add(q);
	        }
	        return unansweredQueries;
	    }
	    
	    private Boolean traceBoolean(Word<PSymbolInstance> query) {
	    	Word<PSymbolInstance> fixedQuery = this.traceCanonizer.canonizeTrace(query); 
	        Boolean answer = this.ioCache.answerFromCache(fixedQuery);
	        return answer;
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
	        trace = this.concurrentSulOracle.trace(fixedQuery);
	        boolean added = this.ioCache.addToCache(trace);
	        return trace;
	    }

}
