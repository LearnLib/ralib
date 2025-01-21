package de.learnlib.ralib.example.array;

import java.util.ArrayList;
import java.util.Collection;

import de.learnlib.query.Query;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public abstract class ArrayListDataWordOracle<T> implements DataWordOracle {

	public static int DEFAULT_CAPACITY = 3;

	private final InputSymbol PUSH = pushSymbol();
	private final InputSymbol REMOVE = removeSymbol();

	private int capacity;

	public ArrayListDataWordOracle() {
		this(DEFAULT_CAPACITY);
	}

	public ArrayListDataWordOracle(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		for (Query<PSymbolInstance, Boolean> query : queries) {
			query.answer(answer(query.getInput()));
		}
	}

	private boolean answer(Word<PSymbolInstance> word) {
		ArrayList<T> arr = new ArrayList<>();
		try {
			for(PSymbolInstance psi : word) {
				if (!accepts(psi, arr)) {
					return false;
				}
			}
		} catch(Exception ex) {
			return false;
		}
		return true;
	}

	private boolean accepts(PSymbolInstance psi, ArrayList<T> arr) {
		if (psi.getBaseSymbol().equals(PUSH) && arr.size() < capacity) {
			arr.add((T)psi.getParameterValues()[0].getId());
			return true;
		} else if (psi.getBaseSymbol().equals(REMOVE)) {
			return arr.remove(psi.getParameterValues()[0].getId());
		}
		return false;
	}

	protected abstract InputSymbol pushSymbol();
	protected abstract InputSymbol removeSymbol();
}
