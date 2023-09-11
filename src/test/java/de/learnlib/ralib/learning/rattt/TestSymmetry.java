package de.learnlib.ralib.learning.rattt;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class TestSymmetry extends RaLibTestSuite {

	private static final DataType T_INT = new DataType("int", Integer.class);

	private static final InputSymbol A =
			new InputSymbol("a", new DataType[] {T_INT});
	private static final InputSymbol B =
			new InputSymbol("b", new DataType[] {T_INT});

	@Test
	public void learnSymmetryExampleCT() {

		Constants consts = new Constants();
		RegisterAutomaton sul = buildCT();
	    DataWordOracle dwOracle = new SimulatorOracle(sul);

	    final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	    teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

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
        		new PSymbolInstance(A, new DataValue(T_INT, 1)),
        		new PSymbolInstance(A, new DataValue(T_INT, 2)),
        		new PSymbolInstance(B, new DataValue(T_INT, 1)),
        		new PSymbolInstance(B, new DataValue(T_INT, 2)));

        learner.addCounterexample(new DefaultQuery<>(ce, true));
        learner.learn();

        Hypothesis hyp = learner.getHypothesis();
        System.out.println(learner.getHypothesis().toString());

        ce = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(T_INT, 1)),
        		new PSymbolInstance(B, new DataValue(T_INT, 2)),
        		new PSymbolInstance(B, new DataValue(T_INT, 3)),
        		new PSymbolInstance(B, new DataValue(T_INT, 1)));

        Assert.assertTrue(hyp.accepts(ce));
	}

	private RegisterAutomaton buildCT() {
		MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

		RALocation l0 = ra.addInitialState(false);
		RALocation l1 = ra.addState(false);
		RALocation l2 = ra.addState(false);
		RALocation l3 = ra.addState(true);
		RALocation l4 = ra.addState(false);

		SymbolicDataValueGenerator.RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
		SymbolicDataValue.Register r1 = rgen.next(T_INT);
		SymbolicDataValue.Register r2 = rgen.next(T_INT);
		SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
		SymbolicDataValue.Parameter p1 = pgen.next(T_INT);

		TransitionGuard trueGuard = new TransitionGuard();
        TransitionGuard equalR1 = new TransitionGuard(new AtomicGuardExpression(r1, Relation.EQUALS, p1));
        TransitionGuard notEqualR1 = new TransitionGuard(new AtomicGuardExpression(r1, Relation.NOT_EQUALS, p1));
        TransitionGuard disjunctionGuard = new TransitionGuard(new Disjunction(
        				new AtomicGuardExpression(r1, Relation.EQUALS, p1),
        				new AtomicGuardExpression(r2, Relation.EQUALS, p1)));

		VarMapping<SymbolicDataValue.Register, SymbolicDataValue> storeR1Mapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
        storeR1Mapping.put(r1, p1);
		VarMapping<SymbolicDataValue.Register, SymbolicDataValue> storeR2Mapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
        storeR2Mapping.put(r2, p1);
		VarMapping<SymbolicDataValue.Register, SymbolicDataValue> storeR1R2Mapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
		storeR1R2Mapping.put(r2, r1);
        storeR1R2Mapping.put(r1, p1);
		VarMapping<SymbolicDataValue.Register, SymbolicDataValue> copyMapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
        copyMapping.put(r1, r2);
        VarMapping<Register, SymbolicDataValue> noMapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();

        Assignment storeR1 = new Assignment(storeR1Mapping);
        Assignment storeR2 = new Assignment(storeR2Mapping);
        Assignment storeR1R2 = new Assignment(storeR1R2Mapping);
        Assignment copy = new Assignment(copyMapping);
        Assignment noAssign = new Assignment(noMapping);

        ra.addTransition(l0, A, new InputTransition(trueGuard, A, l0, l1, storeR1));
        ra.addTransition(l0, B, new InputTransition(trueGuard, B, l0, l4, noAssign));
        ra.addTransition(l1, A, new InputTransition(trueGuard, A, l1, l2, storeR2));
        ra.addTransition(l1, B, new InputTransition(notEqualR1, B, l1, l2, storeR1R2));
        ra.addTransition(l1, B, new InputTransition(equalR1, B, l1, l3, noAssign));
        ra.addTransition(l2, A, new InputTransition(disjunctionGuard, A, l2, l3, noAssign));
        ra.addTransition(l2, B, new InputTransition(trueGuard, B, l2, l1, copy));
        ra.addTransition(l3, A, new InputTransition(trueGuard, A, l3, l3, noAssign));
        ra.addTransition(l3, B, new InputTransition(trueGuard, B, l3, l3, noAssign));
        ra.addTransition(l4, A, new InputTransition(trueGuard, A, l4, l4, noAssign));
        ra.addTransition(l4, B, new InputTransition(trueGuard, B, l4, l4, noAssign));

        return ra;
	}
}
