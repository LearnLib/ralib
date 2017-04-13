package de.learnlib.ralib.oracles.io;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class ConcurrentIOOracle implements IOOracle{
	
	private List<IOOracle> independentOracles;

	public ConcurrentIOOracle(List<IOOracle> independentOracles) {
		this.independentOracles = independentOracles;
		
	}

	public List<Word<PSymbolInstance>> processQueries(List<Word<PSymbolInstance>> queries) {
		final List<Word<PSymbolInstance>> answers = new ArrayList<>(queries.size());
		System.out.println("Processing " + queries.size() + " queries in parallel");
		for (int i=0; i<queries.size(); i=i+independentOracles.size()) {
			ExecutorService executorService = Executors.newFixedThreadPool(this.independentOracles.size());
			List<Future<Word<PSymbolInstance>>> submittedQueries = new ArrayList<>(independentOracles.size());
			for (int j=0; i+j<queries.size() && j<independentOracles.size(); j++) {
				IOOracle oracle = independentOracles.get(j);
				Word<PSymbolInstance> query = queries.get(i+j);
				Future<Word<PSymbolInstance>> submitted = executorService.submit(() -> oracle.trace(query));
				submittedQueries.add(submitted);
			}
			try {
				executorService.shutdown();
				boolean terminated = executorService.awaitTermination(100L * (1 + queries.size()/independentOracles.size()), TimeUnit.SECONDS);
				if (!terminated) {
					throw new DecoratedRuntimeException("Took too long to terminate");
				}
				submittedQueries.forEach(q -> {
					try {
						answers.add(q.get());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						if (e.getCause() instanceof DecoratedRuntimeException) {
							throw ((DecoratedRuntimeException) e.getCause());
						} else {
							DecoratedRuntimeException exc = new DecoratedRuntimeException();
							exc.addSuppressed(e.getCause());
							throw exc;
						}
					}
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new DecoratedRuntimeException("Interrupted");
			}
		}
		
		return answers;
	}

	@Override
	public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
		return this.independentOracles.get(0).trace(query);
	}

}
