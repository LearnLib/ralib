package de.learnlib.ralib.learning;

import static de.learnlib.ralib.example.palindrome.PalindromeOracle.IN;
import static de.learnlib.ralib.example.palindrome.PalindromeOracle.TYPE;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.CacheDataWordOracle;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.RAEquivalenceTest;
import de.learnlib.ralib.example.palindrome.Palindrome;
import de.learnlib.ralib.example.palindrome.PalindromeGenerator;
import de.learnlib.ralib.example.palindrome.PalindromeOracle;
import de.learnlib.ralib.learning.ralambda.SLLambda;
import de.learnlib.ralib.learning.ralambda.SLLambdaEq;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;

public class LearnPalindromeTest extends RaLibTestSuite {

	// TODO Create a system for managing configuration of tests.
	private static final int PALINDROME_SIZE = Integer.valueOf(System.getProperty("palindrome.size", "6"));

	private void printHeader() {
		System.out.println("=======================");
		System.out.println("========== " + PALINDROME_SIZE + " ==========");
		System.out.println("=======================");
	}

	@Test(enabled = true)
	public void testLearnPalindromeSLLambda() {
		testLearnPalindrome(PALINDROME_SIZE, RaLearningAlgorithmName.RALAMBDA);
	}

	@Test(enabled = true)
	public void testLearnPalindromeSLLEq() {
		testLearnPalindrome(PALINDROME_SIZE, RaLearningAlgorithmName.RALAMBDAEQ);
	}

	@Test(enabled = true)
	public void testLearnPalindromeSLStar() {
		testLearnPalindrome(PALINDROME_SIZE, RaLearningAlgorithmName.RASTAR);
	}

	public void testLearnPalindrome(int size, RaLearningAlgorithmName name) {
		RegisterAutomaton model = PalindromeGenerator.generate(size);

		RaLearningAlgorithm algorithm = makeLearner(name);
//		printHeader();

		Map<DataType, Theory> teachers = new LinkedHashMap<>();
		teachers.put(TYPE, new IntegerEqualityTheory(TYPE));
		RAEquivalenceTest checker = new RAEquivalenceTest(model, teachers, new Constants(), true, IN);
		learn(algorithm, checker, name);
	}

	private RaLearningAlgorithm makeLearner(RaLearningAlgorithmName name) {
		Constants consts = new Constants();
		ConstraintSolver solver = new ConstraintSolver();

		Palindrome pal = new Palindrome(PALINDROME_SIZE);
		DataWordOracle oracle = new PalindromeOracle(pal);
		CacheDataWordOracle cacheOracle = new CacheDataWordOracle(oracle);

		Map<DataType, Theory> teachers = new LinkedHashMap<>();
		IntegerEqualityTheory iet = new IntegerEqualityTheory(TYPE);
		iet.setUseSuffixOpt(true);
		teachers.put(TYPE, iet);

		MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(cacheOracle, teachers, consts, solver);
		SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);
		TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp),
				teachers, consts, solver);

		Measurements mes = new Measurements();
		QueryStatistics queryStats = new QueryStatistics(mes, cacheOracle);

		RaLearningAlgorithm learner = null;
		switch (name) {
		case RALAMBDA -> learner = new SLLambda(mto, teachers, consts, false, solver, IN);
		case RALAMBDAEQ -> learner = new SLLambdaEq(mto, teachers, consts, false, solver, IN);
		case RASTAR -> learner = new RaStar(mto, hypFactory, slo, consts, false, IN);
		default -> throw new RuntimeException("Unsupported algorithm %s".formatted(name.name()));
		}
		learner.setStatisticCounter(queryStats);

		return learner;
	}

	private void learn(RaLearningAlgorithm learner, RAEquivalenceTest checker, RaLearningAlgorithmName name) {
		learner.learn();
		DefaultQuery<PSymbolInstance, Boolean> ce = checker.findCounterExample(learner.getHypothesis(), null);
		while (ce != null) {
//			System.out.println(ce);
			learner.addCounterexample(ce);
			learner.learn();
			ce = checker.findCounterExample(learner.getHypothesis(), null);
		}

//		System.out.println(learner.getQueryStatistics());
//		Hypothesis hyp = learner.getHypothesis();
//		System.out.println("Hyp. Locations: " + hyp.getStates().size());
//		System.out.println("Hyp. Transitions: " + hyp.getTransitions().size());

		// input locations + transitions
//		System.out.println("Hyp. Input Locations: " + hyp.getInputStates().size());
//		System.out.println("Hyp. Input Transitions: " + hyp.getInputTransitions().size());

//		System.out.println("Hyp. Registers: " + hyp.getRegisters().size());
	}
}
