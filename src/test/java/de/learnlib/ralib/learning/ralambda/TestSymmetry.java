package de.learnlib.ralib.learning.ralambda;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
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
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class TestSymmetry extends RaLibTestSuite {

    private static final DataType T_INT = new DataType("int");

    private static final InputSymbol A = new InputSymbol("a", T_INT);
    private static final InputSymbol B = new InputSymbol("b", T_INT);

    @Test
    public void learnSymmetryExampleCT2() {
	Constants consts = new Constants();
	RegisterAutomaton sul = buildCT2();
	DataWordOracle dwOracle = new SimulatorOracle(sul);

	final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

	ConstraintSolver solver = new ConstraintSolver();

	MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);

	SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

	TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
	    new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
				      new Constants(), solver);

	// System.out.println(sul);
	// System.out.println("---------------------------------");

	RaLambda learner = new RaLambda(mto, hypFactory, slo, consts, false, false, A, B);
	learner.setSolver(solver);
	learner.learn();

	Word<PSymbolInstance> ce =
	    Word.fromSymbols(new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)),
			     new PSymbolInstance(A, new DataValue(T_INT, new BigDecimal(2) )),
			     new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)));
	learner.addCounterexample(new DefaultQuery<>(ce, true));
	learner.learn();

	Hypothesis hyp = learner.getHypothesis();
	// System.out.println(hyp.toString());
	// System.out.println(learner.getDT());

	ce = Word.fromSymbols(new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)),
			      new PSymbolInstance(A, new DataValue(T_INT, new BigDecimal(2))),
			      new PSymbolInstance(B, new DataValue(T_INT, new BigDecimal(3) )),
			      new PSymbolInstance(B, new DataValue(T_INT, new BigDecimal(4))),
			      new PSymbolInstance(B, new DataValue(T_INT, new BigDecimal(2))),
			      new PSymbolInstance(B, new DataValue(T_INT, BigDecimal.ONE)));

	Assert.assertTrue(sul.accepts(ce));
	Assert.assertFalse(hyp.accepts(ce));

	learner.addCounterexample(new DefaultQuery<>(ce, true));
	learner.learn();

	// System.out.println(learner.getHypothesis().toString());
	// System.out.println(learner.getDT());
	Assert.assertTrue(learner.getDTHyp().accepts(ce));
    }

    private RegisterAutomaton buildCT2() {
	MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

	RALocation l_eps = ra.addInitialState(false);
	RALocation l_a = ra.addState(false);
	RALocation l_aa = ra.addState(false);
	RALocation l_aab = ra.addState(false);
	RALocation l_aabb = ra.addState(false);
	RALocation l_aabbb = ra.addState(false);

	RALocation l_sink = ra.addState(false);
	RALocation l_final = ra.addState(true);

	SymbolicDataValueGenerator.RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
	SymbolicDataValue.Register r1 = rgen.next(T_INT);
	SymbolicDataValue.Register r2 = rgen.next(T_INT);
	SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
	SymbolicDataValue.Parameter p1 = pgen.next(T_INT);

	Expression<Boolean> trueGuard = ExpressionUtil.TRUE;
		Expression<Boolean> equalR1 = new NumericBooleanExpression(r1, NumericComparator.EQ, p1);
		Expression<Boolean> notEqualR1 = new NumericBooleanExpression(r1, NumericComparator.NE, p1);

		Expression<Boolean> disjunctionGuard = ExpressionUtil.or(
				new NumericBooleanExpression(r1, NumericComparator.EQ, p1),
				new NumericBooleanExpression(r2, NumericComparator.EQ, p1));

		Expression<Boolean> elseGuard = ExpressionUtil.and(
				new NumericBooleanExpression(r1, NumericComparator.NE, p1),
				new NumericBooleanExpression(r2, NumericComparator.NE, p1));


	VarMapping<SymbolicDataValue.Register, SymbolicDataValue> storeR1Mapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
	storeR1Mapping.put(r1, p1);
	VarMapping<SymbolicDataValue.Register, SymbolicDataValue> storeR1R2Mapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
	storeR1R2Mapping.put(r1, p1);
	storeR1R2Mapping.put(r2, r1);
	VarMapping<SymbolicDataValue.Register, SymbolicDataValue> copyMapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
	copyMapping.put(r1, r1);
	copyMapping.put(r2, r2);

	VarMapping<SymbolicDataValue.Register, SymbolicDataValue> forgetMapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
	forgetMapping.put(r1, r2);

	VarMapping<Register, SymbolicDataValue> noMapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();

	Assignment storeR1 = new Assignment(storeR1Mapping);
	Assignment storeR1R2 = new Assignment(storeR1R2Mapping);
	Assignment copy = new Assignment(copyMapping);
	Assignment forget = new Assignment(forgetMapping);
	Assignment noAssign = new Assignment(noMapping);

	ra.addTransition(l_eps, A, new InputTransition(trueGuard, A, l_eps, l_a, storeR1));
	ra.addTransition(l_eps, B, new InputTransition(trueGuard, B, l_eps, l_sink, noAssign));

	ra.addTransition(l_a, A, new InputTransition(trueGuard, A, l_a, l_aa, storeR1R2));
	ra.addTransition(l_a, B, new InputTransition(trueGuard, B, l_eps, l_sink, noAssign));

	ra.addTransition(l_aa, A, new InputTransition(elseGuard, A, l_aa, l_sink, noAssign));
	ra.addTransition(l_aa, A, new InputTransition(disjunctionGuard, A, l_aa, l_final, noAssign));
	ra.addTransition(l_aa, B, new InputTransition(trueGuard, B, l_aa, l_aab, copy));

	ra.addTransition(l_aab, A, new InputTransition(trueGuard, A, l_aab, l_sink, noAssign));
	ra.addTransition(l_aab, B, new InputTransition(trueGuard, B, l_aab, l_aabb, copy));

	ra.addTransition(l_aabb, A, new InputTransition(trueGuard, A, l_aabb, l_sink, noAssign));
	ra.addTransition(l_aabb, B, new InputTransition(notEqualR1, B, l_aabb, l_sink, noAssign));
	ra.addTransition(l_aabb, B, new InputTransition(equalR1, B, l_aabb, l_aabbb, forget));

	ra.addTransition(l_aabbb, A, new InputTransition(trueGuard, A, l_aabbb, l_sink, noAssign));
	ra.addTransition(l_aabbb, B, new InputTransition(notEqualR1, B, l_aabbb, l_sink, noAssign));
	ra.addTransition(l_aabbb, B, new InputTransition(equalR1, B, l_aabbb, l_final, noAssign));

	ra.addTransition(l_final, A, new InputTransition(trueGuard, A, l_final, l_final, noAssign));
	ra.addTransition(l_final, B, new InputTransition(trueGuard, B, l_final, l_final, noAssign));
	ra.addTransition(l_sink, A, new InputTransition(trueGuard, A, l_sink, l_sink, noAssign));
	ra.addTransition(l_sink, B, new InputTransition(trueGuard, B, l_sink, l_sink, noAssign));

	return ra;
    }

    @Test
    public void learnSymmetryExampleCT() {
	Constants consts = new Constants();
	RegisterAutomaton sul = buildCT();
	DataWordOracle dwOracle = new SimulatorOracle(sul);

	final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

	ConstraintSolver solver = new ConstraintSolver();

	MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
	    new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
				      new Constants(), solver);

        RaLambda learner = new RaLambda(mto, hypFactory, slo, consts, false, false, A, B);
        learner.setSolver(solver);

        learner.learn();

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(A, new DataValue(T_INT, new BigDecimal(2))),
        		new PSymbolInstance(B, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(B, new DataValue(T_INT, new BigDecimal(2))));

        learner.addCounterexample(new DefaultQuery<>(ce, true));
        learner.learn();

        Hypothesis hyp = learner.getHypothesis();
        // System.out.println(learner.getHypothesis().toString());

        ce = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(B, new DataValue(T_INT, new BigDecimal(2))),
        		new PSymbolInstance(B, new DataValue(T_INT, new BigDecimal(3))),
        		new PSymbolInstance(B, new DataValue(T_INT, new BigDecimal(2))));

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

	Expression<Boolean> trueGuard = ExpressionUtil.TRUE;
		Expression<Boolean> equalR1 = new NumericBooleanExpression(r1, NumericComparator.EQ, p1);
		Expression<Boolean> notEqualR1 = new NumericBooleanExpression(r1, NumericComparator.NE, p1);
		Expression<Boolean> disjunctionGuard = ExpressionUtil.or(
        				new NumericBooleanExpression(r1, NumericComparator.EQ, p1),
        				new NumericBooleanExpression(r2, NumericComparator.EQ, p1));

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
