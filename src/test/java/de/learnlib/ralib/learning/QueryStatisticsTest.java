package de.learnlib.ralib.learning;

import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class QueryStatisticsTest extends RaLibTestSuite {

	public static final InputSymbol A = new InputSymbol("a", new DataType[] {});

	@Test
	public void testCounterexampleStatistics() {

		DataWordSUL sul = null;
		QueryStatistics qs = new QueryStatistics(null, sul);

		Word<PSymbolInstance> ceLen6 = buildCE(2);
		Word<PSymbolInstance> ceLen2 = buildCE(6);
		Word<PSymbolInstance> ceLen7 = buildCE(7);

		qs.analyzeCE(ceLen2);
		qs.analyzeCE(ceLen6);
		qs.analyzeCE(ceLen7);

		Set<Word<PSymbolInstance>> ces = qs.getCEs();

		Assert.assertEquals(ces.size(), 3);
		Assert.assertTrue(ces.contains(ceLen2));
		Assert.assertTrue(ces.contains(ceLen6));
		Assert.assertTrue(ces.contains(ceLen7));

		String str = qs.toString();

		Assert.assertTrue(str.contains("Counterexamples: 3"));
		Assert.assertTrue(str.contains("CE max length: 7"));
		Assert.assertTrue(str.contains("CE avg length: 5"));
	}

	private Word<PSymbolInstance> buildCE(int length) {
		Word<PSymbolInstance> ce = Word.epsilon();
		for (int i = 0; i < length; i++) {
			ce = ce.append(new PSymbolInstance(A));
		}
		return ce;
	}
}
