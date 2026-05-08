package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.palindrome.PalindromeOracle.IN;
import static de.learnlib.ralib.example.palindrome.PalindromeOracle.TYPE;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibLearningExperimentRunner;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.RAEquivalenceTest;
import de.learnlib.ralib.example.palindrome.PalindromeGenerator;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class LearnPalindromeTest extends RaLibTestSuite {
	private static final int SIZE = 6;

	@Test
    public void testLearnPalindrome() {

        final Constants consts = new Constants();

        RegisterAutomaton model = PalindromeGenerator.generate(SIZE);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(TYPE, new IntegerEqualityTheory(TYPE));
        SimulatorOracle dwOracle = new SimulatorOracle(model);
        ParameterizedSymbol action = IN;

        ConstraintSolver solver = TestUtil.getZ3Solver();

        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        RAEquivalenceTest checker = new RAEquivalenceTest(model, teachers, new Constants(), true, IN);
        runner.setEqOracle(checker);
        Hypothesis result = runner.run(RaLearningAlgorithmName.RALAMBDA, dwOracle, teachers, consts, solver, new ParameterizedSymbol[] {action});
        DefaultQuery<PSymbolInstance, Boolean> ce = checker.findCounterExample(result, null);
        Assert.assertNull(ce);
    }
}
