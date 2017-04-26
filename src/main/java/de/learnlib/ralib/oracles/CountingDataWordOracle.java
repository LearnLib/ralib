package de.learnlib.ralib.oracles;

import java.util.Collection;

import de.learnlib.api.Query;
import de.learnlib.ralib.oracles.io.DataWordIOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class CountingDataWordOracle implements DataWordIOOracle{
	private DataWordIOOracle oracle;
	private QueryCounter queryCounter;

	public CountingDataWordOracle(DataWordIOOracle oracle, QueryCounter queryCounter) {
		this.oracle = oracle;
		this.queryCounter = queryCounter;
	}

	@Override
	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		this.oracle.processQueries(queries);
		this.queryCounter.countQueries(queries.size());
	}

	public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
		Word<PSymbolInstance> trace = this.oracle.trace(query);
		return trace;
	}
}