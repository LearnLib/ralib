package de.learnlib.ralib.equivalence.wmethod;

import static de.learnlib.ralib.example.stack.IOStackAutomatonExample.O_NOK;
import static de.learnlib.ralib.example.stack.IOStackAutomatonExample.O_OK;
import static de.learnlib.ralib.example.stack.PureIOStackAutomatonExample.O_OUT;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.equivalence.IOWpEquivalenceOracle;
import de.learnlib.ralib.equivalence.RAEquivalenceTest;
import de.learnlib.ralib.equivalence.WpEquivalenceOracle;
import de.learnlib.ralib.example.stack.IOStackAutomatonExample;
import de.learnlib.ralib.example.stack.PureIOStackAutomatonExample;
import de.learnlib.ralib.example.stack.StackAutomatonExample;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.MeasuringOracle;
import de.learnlib.ralib.learning.sllambda.SLLambda;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;

public class TestWpMethod extends RaLibTestSuite {

    @Test
    public void testWpOnStack() {

        final int k = 2;
        final WpMethodStrategy strat = WpMethodStrategy.SYSTEMATIC;

        Constants consts = new Constants();
        RegisterAutomaton sul = StackAutomatonExample.AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        RAEquivalenceTest modelChecker = new RAEquivalenceTest(sul, teachers, consts, false, I_PUSH, I_POP);
        WpEquivalenceOracle wpEq = new WpEquivalenceOracle(sul, strat, k, teachers, consts, false, I_PUSH, I_POP);

        Measurements mes = new Measurements();

        MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(
              dwOracle, teachers, new Constants(), solver), mes);

        SLLambda sllambda = new SLLambda(mto, teachers, consts, false, solver, I_PUSH, I_POP);

        sllambda.learn();
        RegisterAutomaton hyp = sllambda.getHypothesis();

        DefaultQuery<PSymbolInstance, Boolean> ce = wpEq.findCounterExample(hyp, null);
        while (ce != null) {
            sllambda.addCounterexample(ce);
            sllambda.learn();
            hyp = sllambda.getHypothesis();
            ce = wpEq.findCounterExample(hyp, null);
        }

        Assert.assertNull(modelChecker.findCounterExample(hyp, null));
    }

    @Test
    public void testWpOnIOStack() {

        final int k = 2;
        final WpMethodStrategy strat = WpMethodStrategy.SYSTEMATIC;

        Constants consts = new Constants();
        RegisterAutomaton ra = IOStackAutomatonExample.AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(ra);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        DataWordSUL sul = new SimulatorSUL(ra, teachers, consts);

        ConstraintSolver solver = new ConstraintSolver();

        IOEquivalenceTest modelChecker = new IOEquivalenceTest(ra, teachers, consts, false, I_PUSH, I_POP);
        IOWpEquivalenceOracle wpEq = new IOWpEquivalenceOracle(sul, strat, k, teachers, consts, I_PUSH, I_POP);

        Measurements mes = new Measurements();

        MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(
              dwOracle, teachers, new Constants(), solver), mes);

        SLLambda sllambda = new SLLambda(mto, teachers, consts, true, solver, I_PUSH, I_POP, O_OK, O_NOK);

        sllambda.learn();
        RegisterAutomaton hyp = sllambda.getHypothesis();

        DefaultQuery<PSymbolInstance, Boolean> ce = wpEq.findCounterExample(hyp, null);
        while (ce != null) {
            sllambda.addCounterexample(ce);
            sllambda.learn();
            hyp = sllambda.getHypothesis();
            ce = wpEq.findCounterExample(hyp, null);
        }

        Assert.assertNull(modelChecker.findCounterExample(hyp, null));
    }

    @Test
    public void testWpOnPureIOStack() {

        final int k = 2;
        final WpMethodStrategy strat = WpMethodStrategy.SYSTEMATIC;

        Constants consts = new Constants();
        RegisterAutomaton ra = PureIOStackAutomatonExample.AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(ra);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        DataWordSUL sul = new SimulatorSUL(ra, teachers, consts);

        ConstraintSolver solver = new ConstraintSolver();

        IOEquivalenceTest modelChecker = new IOEquivalenceTest(ra, teachers, consts, false, I_PUSH, I_POP);
        IOWpEquivalenceOracle wpEq = new IOWpEquivalenceOracle(sul, strat, k, teachers, consts, I_PUSH, I_POP);

        Measurements mes = new Measurements();

        MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(
              dwOracle, teachers, new Constants(), solver), mes);

        SLLambda sllambda = new SLLambda(mto, teachers, consts, true, solver, I_PUSH, I_POP, O_OK, O_NOK, O_OUT);

        sllambda.learn();
        RegisterAutomaton hyp = sllambda.getHypothesis();

        DefaultQuery<PSymbolInstance, Boolean> ce = wpEq.findCounterExample(hyp, null);
        while (ce != null) {
            sllambda.addCounterexample(ce);
            sllambda.learn();
            hyp = sllambda.getHypothesis();
            ce = wpEq.findCounterExample(hyp, null);
        }

        Assert.assertNull(modelChecker.findCounterExample(hyp, null));
    }
}
