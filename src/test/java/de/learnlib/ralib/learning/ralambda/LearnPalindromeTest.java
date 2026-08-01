package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.palindrome.PalindromeOracle.IN;
import static de.learnlib.ralib.example.palindrome.PalindromeOracle.TYPE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.CacheDataWordOracle;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.RAEquivalenceTest;
import de.learnlib.ralib.example.palindrome.Palindrome;
import de.learnlib.ralib.example.palindrome.PalindromeGenerator;
import de.learnlib.ralib.example.palindrome.PalindromeOracle;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.RaLearningAlgorithm;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class LearnPalindromeTest extends RaLibTestSuite {

	private static final int SIZE = Integer.valueOf(System.getProperty("palindrome.size", "10"));

	@Test(enabled=true)
	public void testLearnPalindrome() {
		System.out.println("=======================");
		System.out.println("========== " + SIZE + " ==========");
		System.out.println("=======================");

		SLLambda learnerLambda = makeLearner();

		Collection<DefaultQuery<PSymbolInstance, Boolean>> ces = generateCounterexamples(SIZE);

		RegisterAutomaton model = PalindromeGenerator.generate(SIZE);
		for (DefaultQuery<PSymbolInstance, Boolean> ce : ces) {
			Assert.assertEquals(model.accepts(ce.getInput()), ce.getOutput());
		}

		Map<DataType, Theory> teachers = new LinkedHashMap<>();
		teachers.put(TYPE, new IntegerEqualityTheory(TYPE));
		RAEquivalenceTest checker = new RAEquivalenceTest(model, teachers, new Constants(), true, IN);

		learn(learnerLambda, checker, "SLLambda");
		//learn(learnerStar, checker, "SLStar");
	}

	private SLLambda makeLearner() {
    	Constants consts = new Constants();
    	ConstraintSolver solver = new ConstraintSolver();

    	Palindrome pal = new Palindrome(SIZE);
    	DataWordOracle oracle = new PalindromeOracle(pal);
    	CacheDataWordOracle cacheOracle = new CacheDataWordOracle(oracle);

    	Map<DataType, Theory> teachers = new LinkedHashMap<>();
    	IntegerEqualityTheory iet = new IntegerEqualityTheory(TYPE);
    	teachers.put(TYPE, iet);

    	MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(cacheOracle, teachers, consts, solver);

    	Measurements mes = new Measurements();
    	QueryStatistics queryStats = new QueryStatistics(mes, cacheOracle);

//    	SLLambda learner = version > 0 ?
//    			new SLLambda(mto, teachers, consts, false, solver, SymbolicSuffixRestrictionBuilder.Version.fromInt(version), in) :
//    				new SLLambda(mto, teachers, consts, false, solver, in);
    	SLLambda learner = new SLLambda(mto, teachers, consts, false, solver, IN);
		learner.setStatisticCounter(queryStats);

		learner.learn();

		return learner;
	}


	private void learn(RaLearningAlgorithm learner, RAEquivalenceTest checker, String name) {
		String dashes = "----";
		for (int i = 0; i < name.length(); i++) {
			dashes = dashes + "-";
		}
		dashes = dashes + "----";
		System.out.println(dashes);
		System.out.println("--- " + name + " ---");
		System.out.println(dashes);

		DefaultQuery<PSymbolInstance, Boolean> ce = checker.findCounterExample(learner.getHypothesis(), null);
		while (ce != null) {
			System.out.println(ce);
			learner.addCounterexample(ce);
			learner.learn();
			ce = checker.findCounterExample(learner.getHypothesis(), null);
		}

		System.out.println(learner.getQueryStatistics());
	}

	private Collection<DefaultQuery<PSymbolInstance, Boolean>> generateCounterexamples(int size) {
		Collection<DefaultQuery<PSymbolInstance, Boolean>> ces = new ArrayList<>();
		if (size < 2) {
			return ces;
		}

		ces.add(new DefaultQuery<>(Word.fromSymbols(
				new PSymbolInstance(IN, new DataValue(TYPE, BigDecimal.ZERO)),
				new PSymbolInstance(IN, new DataValue(TYPE, BigDecimal.ONE))), false));

		if (size < 3) {
			return ces;
		}

		for (int s = 3; s <= size; s++) {
			int[] pal = generatePalindrome(s);
			Word<PSymbolInstance> ce = Word.epsilon();
			for (int p : pal) {
				PSymbolInstance psi = new PSymbolInstance(IN, new DataValue(TYPE, BigDecimal.valueOf(p)));
				ce = ce.append(psi);
			}
			ces.add(new DefaultQuery<>(ce, true));
		}

		Word<PSymbolInstance> ce = Word.epsilon();
		for (int i = 0; i <= size; i++) {
			PSymbolInstance psi = new PSymbolInstance(IN, new DataValue(TYPE, BigDecimal.ZERO));
			ce = ce.append(psi);
		}
		ces.add(new DefaultQuery<>(ce, false));

		return ces;
	}

	private boolean checkCe(DefaultQuery<PSymbolInstance, Boolean> q) {
		Palindrome pal = new Palindrome(SIZE);

		boolean acc = true;
		for (PSymbolInstance psi : q.getInput()) {
			int d = psi.getParameterValues()[0].getValue().intValue();
			acc = pal.in(d);
		}
		return q.getOutput().booleanValue() == acc;
	}

	private int[] generatePalindrome(int n) {
		int[] arr = new int[n];
		for (int i = 0; i < n/2; i++) {
			arr[i] = i;
		}
		if (n % 2 == 1) {
			arr[n/2] = n/2;
		}
		for (int i = 0; i < n/2; i++) {
			arr[n-i-1] = arr[i];
		}
		return arr;
	}
}
