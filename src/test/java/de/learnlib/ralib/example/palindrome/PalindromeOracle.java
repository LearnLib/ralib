package de.learnlib.ralib.example.palindrome;

import java.util.Collection;

import de.learnlib.query.Query;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class PalindromeOracle implements DataWordOracle {

	public static final DataType TYPE = new DataType("int");
	public static final InputSymbol IN = new InputSymbol("in", new DataType[] {TYPE});

	private final Palindrome pal;

	public PalindromeOracle(Palindrome pal) {
		this.pal = pal;
	}

	@Override
	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		for (Query<PSymbolInstance, Boolean> q : queries) {
			q.answer(answer(q.getInput()));
		}
	}

	private boolean answer(Word<PSymbolInstance> word) {
		pal.reset();
		boolean isPalindrome = true;
		for (PSymbolInstance psi : word) {
			isPalindrome = answer(psi);
		}
		return isPalindrome;
	}

	private boolean answer(PSymbolInstance psi) {
		if (!psi.getBaseSymbol().equals(IN)) {
			return false;
		}
		int d = psi.getParameterValues()[0].getValue().intValue();
		return pal.in(d);
	}
}
