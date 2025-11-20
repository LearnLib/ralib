package de.learnlib.ralib.example.list;

import static de.learnlib.ralib.example.list.ArrayListDataWordOracle.ADD;
import static de.learnlib.ralib.example.list.ArrayListDataWordOracle.REMOVE;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import de.learnlib.query.Query;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class ArrayListIODataWordOracle implements DataWordOracle {

	public static final OutputSymbol VOID = new OutputSymbol("void");
	public static final OutputSymbol TRUE = new OutputSymbol("true");
	public static final OutputSymbol FALSE = new OutputSymbol("false");

	private final Supplier<ArrayListWrapper> factory;

	public ArrayListIODataWordOracle(Supplier<ArrayListWrapper> factory) {
		this.factory = factory;
	}

	@Override
	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		for (Query<PSymbolInstance, Boolean> query : queries) {
			query.answer(answer(query.getInput()));
		}
	}

	private boolean answer(Word<PSymbolInstance> query) {
		if (query.size() < 1) {
			return true;
		}
		if (!isValid(query)) {
			return false;
		}

		ArrayListWrapper list = factory.get();

		Optional<OutputSymbol> expectedOutput = Optional.empty();
		for (PSymbolInstance psi : query) {
			if (expectedOutput.isEmpty()) {
				expectedOutput = Optional.of(answer(psi, list));
			} else {
				if (!psi.getBaseSymbol().equals(expectedOutput.get())) {
					return false;
				}
				expectedOutput = Optional.empty();
			}
		}
		return true;
	}

	private OutputSymbol answer(PSymbolInstance in, ArrayListWrapper list) {
		ParameterizedSymbol symbol = in.getBaseSymbol();
		if (!(symbol instanceof InputSymbol || symbol.equals(ADD) || symbol.equals(REMOVE))) {
			throw new IllegalArgumentException("Not a valid input symbol: " + symbol);
		}

		BigDecimal val = in.getParameterValues()[0].getValue();

		if (symbol.equals(ADD)) {
			list.add(val);
			return VOID;
		}
		return list.remove(val) ? TRUE : FALSE;
	}

	private boolean isValid(Word<PSymbolInstance> query) {
		boolean inExpected = true;
		for (PSymbolInstance psi : query) {
			ParameterizedSymbol symbol = psi.getBaseSymbol();
			if (inExpected ^ (symbol instanceof InputSymbol)) {
				return false;
			}
			inExpected = !inExpected;
		}
		return true;
	}
}
