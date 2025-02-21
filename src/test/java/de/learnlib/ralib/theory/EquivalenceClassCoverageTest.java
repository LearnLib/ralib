package de.learnlib.ralib.theory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.word.Word;

public class EquivalenceClassCoverageTest extends RaLibTestSuite {

	@Test
	public void testEqualityTheory() {

        DataType type = new DataType("int");

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(type);
        dit.setUseSuffixOpt(false);
        teachers.put(type, dit);

        ConstraintSolver solver = new ConstraintSolver();
        EqCRecordingOracle oracle = new EqCRecordingOracle();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                oracle, teachers, new Constants(), solver);

        InputSymbol A = new InputSymbol("a", type);
        DataValue d0 = new DataValue(type, BigDecimal.ZERO);
        DataValue d1 = new DataValue(type, BigDecimal.ONE);
        DataValue d2 = new DataValue(type, new BigDecimal(2));
        DataValue d3 = new DataValue(type, new BigDecimal(3));
        PSymbolInstance symbol = new PSymbolInstance(A, d0);

        Word<PSymbolInstance> prefix = Word.epsilon();
        Word<PSymbolInstance> suffix = Word.fromSymbols(
        		symbol, symbol, symbol, symbol);
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);

        mto.treeQuery(prefix, symSuffix);
        Collection<Word<PSymbolInstance>> actual = oracle.queries;
        Collection<Word<PSymbolInstance>> expected = permutationsEqWith4Params(A);

        Assert.assertEquals(actual.size(), expected.size());
        for (Word<PSymbolInstance> q : expected) {
        	Assert.assertTrue(actual.contains(q));
        }
	}

	@Test
	public void testDoubleInequalityTheory() {
		DataType type = new DataType("double");

		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		DoubleInequalityTheory dit = new DoubleInequalityTheory(type);
		dit.setUseSuffixOpt(false);
		teachers.put(type, dit);

        ConstraintSolver jsolv = TestUtil.getZ3Solver();
        EqCRecordingOracle oracle = new EqCRecordingOracle();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
        		oracle, teachers, new Constants(), jsolv);

        InputSymbol A = new InputSymbol("a", type);
        DataValue d1 = new DataValue(type, BigDecimal.ONE);
        DataValue d2 = new DataValue(type, BigDecimal.valueOf(2));
        PSymbolInstance symbol1 = new PSymbolInstance(A, d1);
        PSymbolInstance symbol2 = new PSymbolInstance(A, d2);

        Word<PSymbolInstance> prefix = Word.fromSymbols(symbol1);
        Word<PSymbolInstance> suffix = Word.fromSymbols(
        		symbol2, symbol2, symbol1);
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);

        mto.treeQuery(prefix, symSuffix);
        Collection<Word<PSymbolInstance>> actual = oracle.queries;
        Collection<Word<PSymbolInstance>> expected = permutationsIneqWith4Params(A, dit);

        Assert.assertEquals(actual.size(), expected.size());
        for (Word<PSymbolInstance> q : expected) {
        	Assert.assertTrue(actual.contains(q), q.toString() + " not in list of queries:");
        }
	}

	private static Collection<Word<PSymbolInstance>> permutationsEqWith4Params(InputSymbol A) {
		DataType type = A.getPtypes()[0];
        DataValue d0 = new DataValue(type, BigDecimal.ZERO);
        DataValue d1 = new DataValue(type, BigDecimal.ONE);
        DataValue d2 = new DataValue(type, new BigDecimal(2));
        DataValue d3 = new DataValue(type, new BigDecimal(3));
        Collection<Word<PSymbolInstance>> expected = new ArrayList<>();
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d0)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d0)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d1)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d2)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d0)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d2)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d0)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d1)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d2)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d2),
        		new PSymbolInstance(A, d0)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d2),
        		new PSymbolInstance(A, d1)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d2),
        		new PSymbolInstance(A, d2)));
        expected.add(Word.fromSymbols(
        		new PSymbolInstance(A, d0),
        		new PSymbolInstance(A, d1),
        		new PSymbolInstance(A, d2),
        		new PSymbolInstance(A, d3)));
        return expected;
	}

	private static Collection<Word<PSymbolInstance>> permutationsIneqWith4Params(InputSymbol A, DoubleInequalityTheory theory) {
		DataType t = A.getPtypes()[0];
		DataValue dm2 = new DataValue(t, BigDecimal.valueOf(-2));
		DataValue dm1 = new DataValue(t, BigDecimal.valueOf(-1));
		DataValue d0 = new DataValue(t, BigDecimal.ZERO);
		DataValue d1 = new DataValue(t, BigDecimal.ONE);
		DataValue d2 = new DataValue(t, BigDecimal.valueOf(2));
		DataValue d3 = new DataValue(t, BigDecimal.valueOf(3));
		DataValue d4 = new DataValue(t, BigDecimal.valueOf(4));
		DataValue dm05 = generateDecimal(dm1, d0, theory, t);
		DataValue d05 = generateDecimal(d0, d1, theory, t);
		DataValue d025 = generateDecimal(d0, d05, theory, t);
		DataValue d075 = generateDecimal(d05, d1, theory, t);
		DataValue d15 = generateDecimal(d1, d2, theory, t);
		DataValue d125 = generateDecimal(d1, d15, theory, t);
		DataValue d175 = generateDecimal(d15, d2, theory, t);
		DataValue d25 = generateDecimal(d2, d3, theory, t);

		Word<PSymbolInstance> sw10m1 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d0),
				new PSymbolInstance(A, dm1));
		Word<PSymbolInstance> sw100 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d0),
				new PSymbolInstance(A, d0));
		Word<PSymbolInstance> sw101 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d0),
				new PSymbolInstance(A, d1));
		Word<PSymbolInstance> sw1005 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d0),
				new PSymbolInstance(A, d05));
		Word<PSymbolInstance> sw102 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d0),
				new PSymbolInstance(A, d2));
		Word<PSymbolInstance> sw110 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d0));
		Word<PSymbolInstance> sw111 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d1));
		Word<PSymbolInstance> sw112 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d2));
		Word<PSymbolInstance> sw120 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d2),
				new PSymbolInstance(A, d0));
		Word<PSymbolInstance> sw121 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d2),
				new PSymbolInstance(A, d1));
		Word<PSymbolInstance> sw1215 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d2),
				new PSymbolInstance(A, d15));
		Word<PSymbolInstance> sw122 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d2),
				new PSymbolInstance(A, d2));
		Word<PSymbolInstance> sw123 = Word.fromSymbols(
				new PSymbolInstance(A, d1),
				new PSymbolInstance(A, d2),
				new PSymbolInstance(A, d3));

		Collection<Word<PSymbolInstance>> expected = new ArrayList<>();
		expected.add(sw10m1.concat(Word.fromSymbols(new PSymbolInstance(A, dm2))));
		expected.add(sw10m1.concat(Word.fromSymbols(new PSymbolInstance(A, dm1))));
		expected.add(sw10m1.concat(Word.fromSymbols(new PSymbolInstance(A, dm05))));
		expected.add(sw10m1.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw10m1.concat(Word.fromSymbols(new PSymbolInstance(A, d05))));
		expected.add(sw10m1.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw10m1.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw100.concat(Word.fromSymbols(new PSymbolInstance(A, dm1))));
		expected.add(sw100.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw100.concat(Word.fromSymbols(new PSymbolInstance(A, d05))));
		expected.add(sw100.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw100.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw1005.concat(Word.fromSymbols(new PSymbolInstance(A, dm1))));
		expected.add(sw1005.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw1005.concat(Word.fromSymbols(new PSymbolInstance(A, d025))));
		expected.add(sw1005.concat(Word.fromSymbols(new PSymbolInstance(A, d05))));
		expected.add(sw1005.concat(Word.fromSymbols(new PSymbolInstance(A, d075))));
		expected.add(sw1005.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw1005.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw101.concat(Word.fromSymbols(new PSymbolInstance(A, dm1))));
		expected.add(sw101.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw101.concat(Word.fromSymbols(new PSymbolInstance(A, d05))));
		expected.add(sw101.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw101.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw102.concat(Word.fromSymbols(new PSymbolInstance(A, dm1))));
		expected.add(sw102.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw102.concat(Word.fromSymbols(new PSymbolInstance(A, d05))));
		expected.add(sw102.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw102.concat(Word.fromSymbols(new PSymbolInstance(A, d15))));
		expected.add(sw102.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw102.concat(Word.fromSymbols(new PSymbolInstance(A, d3))));

		expected.add(sw110.concat(Word.fromSymbols(new PSymbolInstance(A, dm1))));
		expected.add(sw110.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw110.concat(Word.fromSymbols(new PSymbolInstance(A, d05))));
		expected.add(sw110.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw110.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw111.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw111.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw111.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw112.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw112.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw112.concat(Word.fromSymbols(new PSymbolInstance(A, d15))));
		expected.add(sw112.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw112.concat(Word.fromSymbols(new PSymbolInstance(A, d3))));

		expected.add(sw120.concat(Word.fromSymbols(new PSymbolInstance(A, dm1))));
		expected.add(sw120.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw120.concat(Word.fromSymbols(new PSymbolInstance(A, d05))));
		expected.add(sw120.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw120.concat(Word.fromSymbols(new PSymbolInstance(A, d15))));
		expected.add(sw120.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw120.concat(Word.fromSymbols(new PSymbolInstance(A, d3))));
		expected.add(sw121.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw121.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw121.concat(Word.fromSymbols(new PSymbolInstance(A, d15))));
		expected.add(sw121.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw121.concat(Word.fromSymbols(new PSymbolInstance(A, d3))));
		expected.add(sw1215.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw1215.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw1215.concat(Word.fromSymbols(new PSymbolInstance(A, d125))));
		expected.add(sw1215.concat(Word.fromSymbols(new PSymbolInstance(A, d15))));
		expected.add(sw1215.concat(Word.fromSymbols(new PSymbolInstance(A, d175))));
		expected.add(sw1215.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw1215.concat(Word.fromSymbols(new PSymbolInstance(A, d3))));
		expected.add(sw122.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw122.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw122.concat(Word.fromSymbols(new PSymbolInstance(A, d15))));
		expected.add(sw122.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw122.concat(Word.fromSymbols(new PSymbolInstance(A, d3))));
		expected.add(sw123.concat(Word.fromSymbols(new PSymbolInstance(A, d0))));
		expected.add(sw123.concat(Word.fromSymbols(new PSymbolInstance(A, d1))));
		expected.add(sw123.concat(Word.fromSymbols(new PSymbolInstance(A, d15))));
		expected.add(sw123.concat(Word.fromSymbols(new PSymbolInstance(A, d2))));
		expected.add(sw123.concat(Word.fromSymbols(new PSymbolInstance(A, d25))));
		expected.add(sw123.concat(Word.fromSymbols(new PSymbolInstance(A, d3))));
		expected.add(sw123.concat(Word.fromSymbols(new PSymbolInstance(A, d4))));

		return expected;
	}

	private static DataValue generateDecimal(DataValue d1, DataValue d2, DoubleInequalityTheory theory, DataType t) {
		SuffixValueGenerator sgen = new SuffixValueGenerator();
		SuffixValue s1 = sgen.next(t);
		SuffixValue s2 = sgen.next(t);
		SuffixValue s3 = sgen.next(t);
		SDTGuard ig = new IntervalGuard(s3, s1, s2);
		Valuation vals1 = new Valuation();
		vals1.setValue(s1, d1.getValue());
		vals1.setValue(s2, d2.getValue());
		Collection<DataValue> usedVals1 = new ArrayList<>();
		usedVals1.add(d1);
		usedVals1.add(d2);
		return theory.instantiate(ig, vals1, new Constants(), usedVals1);
	}
}
