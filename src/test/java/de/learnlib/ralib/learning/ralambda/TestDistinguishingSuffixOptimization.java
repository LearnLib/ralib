package de.learnlib.ralib.learning.ralambda;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.impl.InputTransition;
import net.automatalib.automaton.ra.impl.MutableRegisterAutomaton;
import net.automatalib.automaton.ra.RALocation;
import net.automatalib.automaton.ra.impl.RegisterAutomaton;
import net.automatalib.automaton.ra.impl.TransitionGuard;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.VarMapping;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.word.Word;

public class TestDistinguishingSuffixOptimization {

	private static final InputSymbol A =
			new InputSymbol("a", new DataType[] {});
	private static final InputSymbol B =
			new InputSymbol("b", new DataType[] {});

	private RegisterAutomaton buildAutomaton() {
		MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

		RALocation l0 = ra.addInitialState(true);
		RALocation l1 = ra.addState(true);
		RALocation l2 = ra.addState(true);
		RALocation l3 = ra.addState(true);
		RALocation l4 = ra.addState(false);
		RALocation l5 = ra.addState(true);

		TransitionGuard trueGuard = new TransitionGuard();

		VarMapping<Register, SymbolicDataValue> noMapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
        Assignment noAssign = new Assignment(noMapping);

        ra.addTransition(l0, A, new InputTransition(trueGuard, A, l0, l1, noAssign));
        ra.addTransition(l1, A, new InputTransition(trueGuard, A, l1, l2, noAssign));
        ra.addTransition(l2, A, new InputTransition(trueGuard, A, l2, l3, noAssign));
        ra.addTransition(l3, A, new InputTransition(trueGuard, A, l3, l4, noAssign));
        ra.addTransition(l4, A, new InputTransition(trueGuard, A, l4, l4, noAssign));
        ra.addTransition(l5, A, new InputTransition(trueGuard, A, l5, l0, noAssign));

        ra.addTransition(l0, B, new InputTransition(trueGuard, B, l0, l5, noAssign));
        ra.addTransition(l1, B, new InputTransition(trueGuard, B, l1, l1, noAssign));
        ra.addTransition(l2, B, new InputTransition(trueGuard, B, l2, l2, noAssign));
        ra.addTransition(l3, B, new InputTransition(trueGuard, B, l3, l3, noAssign));
        ra.addTransition(l4, B, new InputTransition(trueGuard, B, l4, l4, noAssign));
        ra.addTransition(l5, B, new InputTransition(trueGuard, B, l5, l3, noAssign));

        return ra;
	}

	@Test
	public void testDistinguishingSuffixOptimization() {

		Constants consts = new Constants();
		RegisterAutomaton sul = buildAutomaton();
	    DataWordOracle dwOracle = new SimulatorOracle(sul);

	    final Map<DataType, Theory> teachers = new LinkedHashMap<>();

	    ConstraintSolver solver = new SimpleConstraintSolver();

	    MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        RaLambda learner = new RaLambda(mto, hypFactory, slo, consts, false, false, A, B);
        learner.setSolver(solver);

        learner.learn();

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(A),
        		new PSymbolInstance(A),
        		new PSymbolInstance(A),
        		new PSymbolInstance(A));

        learner.addCounterexample(new DefaultQuery<>(ce, false));
        learner.learn();

        ce = Word.fromSymbols(
        		new PSymbolInstance(B),
        		new PSymbolInstance(A),
        		new PSymbolInstance(A),
        		new PSymbolInstance(A),
        		new PSymbolInstance(A));
        learner.addCounterexample(new DefaultQuery<>(ce, true));
        learner.learn();

        SymbolicSuffix suffixUnoptimized = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(
        		new PSymbolInstance(A),
        		new PSymbolInstance(A),
        		new PSymbolInstance(A),
        		new PSymbolInstance(A)));
        SymbolicSuffix suffixOptimized = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(
        		new PSymbolInstance(B),
        		new PSymbolInstance(A)));

        Word<PSymbolInstance> b = Word.fromSymbols(new PSymbolInstance(B));
        Set<SymbolicSuffix> suffixes = learner.getDT().getLeaf(b).getPrefix(b).getTQRs().keySet();

        Assert.assertFalse(suffixes.contains(suffixUnoptimized));
        Assert.assertTrue(suffixes.contains(suffixOptimized));
	}
}
