package de.learnlib.ralib.equivalence;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IORandomWpEquivalenceOracle.Options;
import de.learnlib.ralib.example.stack.StackAutomatonExample;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.MeasuringOracle;
import de.learnlib.ralib.learning.sllambda.SLLambda;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;

public class RandomWpTest extends RaLibTestSuite {

    @Test
    public void TestRandomWp() {
        final int seed = 123456;
        final int minimalSize = 1;
        final int rndLength = 1;
        final boolean uniform = false;
        final double freshProbability = 0.2;
        final long maxRuns = 50;

        Constants consts = new Constants();
        RegisterAutomaton sul = StackAutomatonExample.AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        Measurements mes = new Measurements();

        MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(
              dwOracle, teachers, new Constants(), solver), mes);

        SLLambda sllambda = new SLLambda(mto, teachers, consts, false, solver, I_PUSH, I_POP);

        sllambda.learn();
        RegisterAutomaton hyp = sllambda.getHypothesis();

        Options options = new Options(new Random(seed),
                minimalSize,
                rndLength,
                uniform,
                freshProbability,
                maxRuns);
        RandomWpEquivalenceOracle wpEq = new RandomWpEquivalenceOracle(sul, teachers, consts, options, I_PUSH, I_POP);

        DefaultQuery<PSymbolInstance, Boolean> ce = wpEq.findCounterExample(hyp, null);
        while (ce != null) {
            sllambda.addCounterexample(ce);
            sllambda.learn();
            hyp = sllambda.getHypothesis();
            ce = wpEq.findCounterExample(hyp, null);
        }

        RAEquivalenceTest modelChecker = new RAEquivalenceTest(sul, teachers, consts, false, I_PUSH, I_POP);
        Assert.assertNull(modelChecker.findCounterExample(hyp, null));
    }
}
