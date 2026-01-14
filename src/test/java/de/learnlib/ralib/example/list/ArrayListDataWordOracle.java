package de.learnlib.ralib.example.list;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Supplier;

import de.learnlib.query.Query;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class ArrayListDataWordOracle implements DataWordOracle {
	public static final DataType TYPE = new DataType("int");

	public static final InputSymbol ADD = new InputSymbol("add", TYPE);
	public static final InputSymbol REMOVE = new InputSymbol("remove", TYPE);

	private final Supplier<ArrayListWrapper> factory;

	public ArrayListDataWordOracle(Supplier<ArrayListWrapper> factory) {
		this.factory = factory;
	}

	@Override
	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		for (Query<PSymbolInstance, Boolean> query : queries) {
			query.answer(answer(query.getInput()));
		}
	}

	private boolean answer(Word<PSymbolInstance> w) {
		ArrayListWrapper list = factory.get();
		for (PSymbolInstance psi : w) {
			try {
				if (!accepts(psi, list)) {
					return false;
				}
			} catch(Exception ignore) {
				return false;
			}
		}
		return true;
	}

	private boolean accepts(PSymbolInstance psi, ArrayListWrapper list) {
		ParameterizedSymbol ps = psi.getBaseSymbol();
		BigDecimal val = psi.getParameterValues()[0].getValue();
		if (ps.equals(ADD)) {
			return list.add(val);
		}
		if (ps.equals(REMOVE)) {
			return list.remove(val);
		}
		return false;
	}
}
