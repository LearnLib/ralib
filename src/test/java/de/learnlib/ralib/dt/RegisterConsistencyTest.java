package de.learnlib.ralib.dt;

import java.math.BigDecimal;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class RegisterConsistencyTest extends RaLibTestSuite {

	private static final DataType T_INT = new DataType("int");

	private static final InputSymbol A =
			new InputSymbol("a", T_INT);

	private static class DummyDT extends DT {

		private static class DummyOracle implements TreeOracle {

			@Override
			public TreeQueryResult treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
				return null;
			}

			@Override
			public Branching getInitialBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv,
					SymbolicDecisionTree... sdts) {
				return null;
			}

			@Override
			public Branching updateBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, Branching current,
					PIV piv, SymbolicDecisionTree... sdts) {
				return null;
			}

			@Override
			public Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
					SymbolicDecisionTree sdt, PIV piv) {
				return null;
			}

			@Override
			public SymbolicSuffixRestrictionBuilder getRestrictionBuilder() {
				return null;
			}

		}

		private final DTLeaf prefixLeaf;
		private final DTLeaf leaf;

		public SymbolicSuffix addedSuffix = null;

		public DummyDT(MappedPrefix word, MappedPrefix prefix) {
			super(new DummyOracle(), false, new Constants(), null);
			leaf = new DTLeaf(word, null);
			prefixLeaf = new DTLeaf(prefix, null);
		}

		@Override
		public DTLeaf getLeaf(Word<PSymbolInstance> word) {
			if (word.equals(leaf.getAccessSequence()))
				return leaf;
			if (word.equals(prefixLeaf.getAccessSequence()))
				return prefixLeaf;
			return null;
		}

		@Override
		public void addSuffix(SymbolicSuffix suffix, DTLeaf leaf) {
			addedSuffix = suffix;
		}
	}

	@Test
	public void testSymmetry() {
		Word<PSymbolInstance> word = Word.fromSymbols(
				new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ZERO)),
				new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)),
				new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)));
		Word<PSymbolInstance> suffixWord = Word.fromSymbols(
				new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ZERO)),
				new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)));
		Word<PSymbolInstance> suffixExpected = Word.fromSymbols(
				new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)),
				new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ZERO)),
				new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)));
		Word<PSymbolInstance> prefix = word.prefix(2);
		SymbolicSuffix symSuffixEps = new SymbolicSuffix(Word.epsilon(), Word.epsilon());
		SymbolicSuffix symSuffixWord = new SymbolicSuffix(word, suffixWord);
		SymbolicSuffix symSuffixPrefix = new SymbolicSuffix(prefix, word.suffix(1));
		SymbolicSuffix symSuffixExpected = new SymbolicSuffix(prefix, suffixExpected);

		RegisterGenerator rgen = new RegisterGenerator();
		ParameterGenerator pgen = new ParameterGenerator();
		SuffixValueGenerator svgen = new SuffixValueGenerator();

		Parameter p1 = pgen.next(T_INT);
		Parameter p2 = pgen.next(T_INT);
		Register r1 = rgen.next(T_INT);
		Register r2 = rgen.next(T_INT);
		SuffixValue s1 = svgen.next(T_INT);
		SuffixValue s2 = svgen.next(T_INT);

		PIV pivWord = new PIV();
		pivWord.put(p1, r1);
		pivWord.put(p2, r2);
		PIV pivPrefix = new PIV();
		pivPrefix.putAll(pivWord);

		Constants consts = new Constants();

		SDT sdtEps = SDTLeaf.ACCEPTING;
		SDT sdtPrefix = new SDT(Map.of(
				new SDTOrGuard(s1, new EqualityGuard(s1, r1), new EqualityGuard(s1, r2)), SDTLeaf.ACCEPTING,
				new SDTAndGuard(s1, new DisequalityGuard(s1, r1), new DisequalityGuard(s1, r2)), SDTLeaf.REJECTING));
		SDT sdtWord = new SDT(Map.of(
				new EqualityGuard(s1, r1), new SDT(Map.of(
						new EqualityGuard(s2, r2), SDTLeaf.ACCEPTING,
						new DisequalityGuard(s2, r2), SDTLeaf.REJECTING)),
				new DisequalityGuard(s1, r1), new SDT(Map.of(
						new SDTTrueGuard(s2), SDTLeaf.REJECTING))));

		TreeQueryResult tqrEps = new TreeQueryResult(new PIV(), sdtEps);
		TreeQueryResult tqrPrefix = new TreeQueryResult(pivPrefix, sdtPrefix);
		TreeQueryResult tqrWord = new TreeQueryResult(pivWord, sdtWord);

		MappedPrefix mpWord = new MappedPrefix(word, pivWord);
		MappedPrefix mpPrefix = new MappedPrefix(prefix, pivPrefix);

		mpWord.addTQR(new SymbolicSuffix(Word.epsilon(), Word.epsilon()), tqrEps);
		mpWord.addTQR(symSuffixWord, tqrWord);
		mpPrefix.addTQR(symSuffixEps, tqrEps);
		mpPrefix.addTQR(symSuffixPrefix, tqrPrefix);

		DummyDT dt = new DummyDT(mpWord, mpPrefix);
		DTLeaf leafWord = dt.getLeaf(word);

		boolean consistent = leafWord.checkRegisterConsistency(dt, consts, null);
		Assert.assertFalse(consistent);
        Assert.assertEquals(dt.addedSuffix.getActions(), symSuffixExpected.getActions());
	}

}
