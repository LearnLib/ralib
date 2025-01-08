package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.Collection;

import de.learnlib.query.Query;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class EqCRecordingOracle implements DataWordOracle {

	public final Collection<Word<PSymbolInstance>> queries = new ArrayList<>();

	@Override
	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		for (Query<PSymbolInstance, Boolean> query : queries) {
			this.queries.add(query.getInput());
			query.answer(true);
		}
	}
}
