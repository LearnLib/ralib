package de.learnlib.ralib.ct;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class CTConsistencyTest extends RaLibTestSuite {

	private static DataType DT = new DataType("double");
	private static InputSymbol A = new InputSymbol("a", new DataType[] {DT});
	private static InputSymbol B = new InputSymbol("b");

	private static InputSymbol ALPHA = new InputSymbol("α", new DataType[] {T_INT});
	private static InputSymbol BETA = new InputSymbol("β", new DataType[] {T_INT});

	@Test
	public void testConsistencyStack() {

		Map<DataType, Theory> teachers = new LinkedHashMap<>();
		teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

		Constants consts = new Constants();

		SymbolicSuffixRestrictionBuilder restrBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);
		OptimizedSymbolicSuffixBuilder suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrBuilder);

        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);
        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, consts, solver);

        DataValue dv0 = new DataValue(T_INT, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(T_INT, BigDecimal.ONE);
        DataValue dv2 = new DataValue(T_INT, BigDecimal.valueOf(2));

        PSymbolInstance push0 = new PSymbolInstance(I_PUSH, dv0);
        PSymbolInstance push1 = new PSymbolInstance(I_PUSH, dv1);
        PSymbolInstance pop0 = new PSymbolInstance(I_POP, dv0);
        PSymbolInstance pop1 = new PSymbolInstance(I_POP, dv1);

        Word<PSymbolInstance> pu0 = Word.fromSymbols(push0);
        Word<PSymbolInstance> po0 = Word.fromSymbols(pop0);
        Word<PSymbolInstance> pu0pu1 = Word.fromSymbols(push0, push1);
        Word<PSymbolInstance> pu0po0 = Word.fromSymbols(push0, pop0);
        Word<PSymbolInstance> pu0pu1po1 = Word.fromSymbols(push0, push1, pop1);

        SymbolicSuffix s1 = new SymbolicSuffix(pu0, Word.fromSymbols(push1));
        SymbolicSuffix s2 = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(push0, push1));
        SymbolicSuffix s3 = new SymbolicSuffix(pu0, Word.fromSymbols(pop0));
        SymbolicSuffix s4 = new SymbolicSuffix(pu0pu1, Word.fromSymbols(pop1, pop0));

        ClassificationTree ct = new ClassificationTree(mto, solver, restrBuilder, suffixBuilder, consts, false, I_PUSH, I_POP);

        ct.initialize();
        boolean closed = ct.checkLocationClosedness();
        closed = ct.checkLocationClosedness();
        Assert.assertFalse(closed);

        ct.expand(pu0);
        closed = ct.checkLocationClosedness();
        Assert.assertTrue(closed);

        ct.expand(pu0pu1);
        closed = ct.checkLocationClosedness();
        Assert.assertTrue(closed);

        ct.refine(ct.getLeaf(pu0pu1), s1);
        boolean consistent = ct.checkLocationConsistency();
        Assert.assertFalse(consistent);

        ct.sift(pu0po0);
        consistent = ct.checkTransitionConsistency();
        Assert.assertFalse(consistent);
	}

	@Test
	public void testLocationConsistency() {
		RegisterAutomaton sul = buildLocationConsistencyAutomaton();
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(DT, new DoubleInequalityTheory(DT));

		Constants consts = new Constants();

		SymbolicSuffixRestrictionBuilder restrBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);
		OptimizedSymbolicSuffixBuilder suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrBuilder);

        ConstraintSolver solver = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, consts, solver);

        System.out.println(sul);

        DataValue dv0 = new DataValue(DT, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(DT, BigDecimal.ONE);
        DataValue dv2 = new DataValue(DT, BigDecimal.valueOf(2));

        Word<PSymbolInstance> a1 = Word.fromSymbols(new PSymbolInstance(A, dv1));
        Word<PSymbolInstance> a1a2 = Word.fromSymbols(
        		new PSymbolInstance(A, dv1),
        		new PSymbolInstance(A, dv2));
        Word<PSymbolInstance> a1a0 = Word.fromSymbols(
        		new PSymbolInstance(A, dv1),
        		new PSymbolInstance(A, dv0));
        Word<PSymbolInstance> a1a2a2 = Word.fromSymbols(
        		new PSymbolInstance(A, dv1),
        		new PSymbolInstance(A, dv2),
        		new PSymbolInstance(A, dv2));
        Word<PSymbolInstance> a1a0a0 = Word.fromSymbols(
        		new PSymbolInstance(A, dv1),
        		new PSymbolInstance(A, dv0),
        		new PSymbolInstance(A, dv0));

        ClassificationTree ct = new ClassificationTree(mto, solver, restrBuilder, suffixBuilder, consts, true, A, B);
        ct.initialize();

        boolean closed = ct.checkLocationClosedness();
        Assert.assertFalse(closed);
        closed = ct.checkLocationClosedness();
//        Assert.assertFalse(closed);
//        closed = ct.checkLocationClosedness();
        Assert.assertTrue(closed);

        boolean consistent = ct.checkLocationConsistency();
        Assert.assertTrue(consistent);
        ct.expand(a1);
        consistent = ct.checkLocationConsistency();
//        Assert.assertFalse(consistent);
//        consistent = ct.checkLocationConsistency();
        Assert.assertTrue(consistent);

        ct.expand(a1a2);
        closed = ct.checkLocationClosedness();
        Assert.assertFalse(closed);
        consistent = ct.checkLocationConsistency();
        Assert.assertFalse(consistent);

        ct.sift(a1a0);
        ct.expand(a1a0);
        consistent = ct.checkLocationConsistency();
        Assert.assertFalse(consistent);
        consistent = ct.checkLocationConsistency();
        Assert.assertTrue(consistent);
        consistent = ct.checkTransitionConsistency();
        Assert.assertTrue(consistent);

        ct.sift(a1a2a2);
        ct.sift(a1a0a0);
        consistent = ct.checkLocationConsistency();
        Assert.assertTrue(consistent);
        consistent = ct.checkTransitionConsistency();
        Assert.assertFalse(consistent);
	}

	private RegisterAutomaton buildLocationConsistencyAutomaton() {
		MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

		RALocation l0 = ra.addInitialState(true);
		RALocation l1 = ra.addState(true);
		RALocation l2 = ra.addState(true);
		RALocation l3 = ra.addState(true);
		RALocation l4 = ra.addState(false);
//		RALocation l5 = ra.addState(false);

		Register r1 = new Register(DT, 1);
		Parameter p1 = new Parameter(DT, 1);

		Expression<Boolean> gT = ExpressionUtil.TRUE;
		Expression<Boolean> gGE = NumericBooleanExpression.create(p1, NumericComparator.GE, r1);
		Expression<Boolean> gLt = NumericBooleanExpression.create(p1, NumericComparator.LT, r1);
		Expression<Boolean> gEq = NumericBooleanExpression.create(p1, NumericComparator.EQ, r1);
		Expression<Boolean> gNE = NumericBooleanExpression.create(p1, NumericComparator.NE, r1);

		VarMapping<Register, Parameter> storeP1 = new VarMapping<>();
		storeP1.put(r1, p1);
		VarMapping<Register, Register> storeR1 = new VarMapping<>();
		storeR1.put(r1, r1);

		Assignment assP1 = new Assignment(storeP1);
		Assignment assR1 = new Assignment(storeR1);
		Assignment assNo = new Assignment(new VarMapping<>());

		ra.addTransition(l0, A, new InputTransition(gT, A, l0, l1, assP1));
		ra.addTransition(l0, B, new InputTransition(gT, B, l0, l0, assNo));

		ra.addTransition(l1, A, new InputTransition(gGE, A, l1, l2, assP1));
		ra.addTransition(l1, A, new InputTransition(gLt, A, l1, l3, assP1));
		ra.addTransition(l1, B, new InputTransition(gT, B, l1, l0, assNo));

		ra.addTransition(l2, A, new InputTransition(gEq, A, l2, l4, assNo));
		ra.addTransition(l2, A, new InputTransition(gNE, A, l2, l0, assNo));
		ra.addTransition(l2, B, new InputTransition(gT, B, l2, l4, assNo));

		ra.addTransition(l3, A, new InputTransition(gEq, A, l3, l1, assR1));
		ra.addTransition(l3, A, new InputTransition(gNE, A, l3, l0, assNo));
		ra.addTransition(l3, B, new InputTransition(gT, B, l3, l4, assNo));

		ra.addTransition(l4, A, new InputTransition(gT, A, l4, l4, assNo));
		ra.addTransition(l4, B, new InputTransition(gT, B, l4, l4, assNo));
//		ra.addTransition(l5, A, new InputTransition(gT, A, l5, l5, assNo));
//		ra.addTransition(l5, B, new InputTransition(gT, B, l5, l5, assNo));

		return ra;
	}

	@Test
	public void testRegisterConsistency() {
		RegisterAutomaton sul = buildRegisterConsistencyAutomaton();
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory iet = new IntegerEqualityTheory(T_INT);
        iet.setUseSuffixOpt(false);
        teachers.put(T_INT, iet);

		Constants consts = new Constants();

		SymbolicSuffixRestrictionBuilder restrBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);
		OptimizedSymbolicSuffixBuilder suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrBuilder);

        ConstraintSolver solver = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, consts, solver);

        System.out.println(sul);

        DataValue dv0 = new DataValue(T_INT, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(T_INT, BigDecimal.ONE);
        DataValue dv2 = new DataValue(T_INT, BigDecimal.valueOf(2));
        DataValue dv3 = new DataValue(T_INT, BigDecimal.valueOf(3));

        Word<PSymbolInstance> b0 = Word.fromSymbols(new PSymbolInstance(BETA, dv0));
        Word<PSymbolInstance> a0 = Word.fromSymbols(new PSymbolInstance(ALPHA, dv0));
        Word<PSymbolInstance> a0a1 = Word.fromSymbols(
        		new PSymbolInstance(ALPHA, dv0),
        		new PSymbolInstance(ALPHA, dv1));
        Word<PSymbolInstance> a0b1 = Word.fromSymbols(
        		new PSymbolInstance(ALPHA, dv0),
        		new PSymbolInstance(BETA, dv1));
        Word<PSymbolInstance> a0a1a0 = Word.fromSymbols(
        		new PSymbolInstance(ALPHA, dv0),
        		new PSymbolInstance(ALPHA, dv1),
        		new PSymbolInstance(ALPHA, dv0));
        Word<PSymbolInstance> a0a1b2 = Word.fromSymbols(
        		new PSymbolInstance(ALPHA, dv0),
        		new PSymbolInstance(ALPHA, dv1),
        		new PSymbolInstance(BETA, dv2));
        Word<PSymbolInstance> b2b1 = Word.fromSymbols(
        		new PSymbolInstance(BETA, dv2),
        		new PSymbolInstance(BETA, dv1));

        SymbolicSuffix sa = new SymbolicSuffix(RaStar.EMPTY_PREFIX, a0);
        SymbolicSuffix sb = new SymbolicSuffix(RaStar.EMPTY_PREFIX, b0);
        SymbolicSuffix sab = new SymbolicSuffix(RaStar.EMPTY_PREFIX, a0b1);
        SymbolicSuffix sbb = new SymbolicSuffix(a0a1, b2b1);

        ClassificationTree ct = new ClassificationTree(mto, solver, restrBuilder, suffixBuilder, consts, false, ALPHA, BETA);
        ct.initialize();

        ct.refine(ct.getLeaf(RaStar.EMPTY_PREFIX), sa);
        ct.refine(ct.getLeaf(RaStar.EMPTY_PREFIX), sb);
        ct.refine(ct.getLeaf(RaStar.EMPTY_PREFIX), sab);

        ct.sift(a0a1a0);
        ct.sift(a0a1);
        ct.sift(a0);
        ct.sift(b0);

        Assert.assertEquals(ct.getLeaves().size(), 5);
        for (int i = 0; i < ct.getLeaves().size(); i++) {
        	Assert.assertFalse(ct.checkLocationClosedness());
        }
        Assert.assertTrue(ct.checkLocationClosedness());

        boolean consistent = ct.checkRegisterConsistency();
        Assert.assertFalse(consistent);
        consistent = ct.checkRegisterConsistency();
        Assert.assertTrue(consistent);

        Assert.assertTrue(ct.getLeaf(a0a1)
        		.getRepresentativePrefix()
        		.getPath()
        		.getSDTs()
        		.keySet()
        		.contains(sbb));
	}

	private RegisterAutomaton buildRegisterConsistencyAutomaton() {
		MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

		RALocation l0 = ra.addInitialState(false);
		RALocation l1 = ra.addState(false);
		RALocation l2 = ra.addState(false);
		RALocation l3 = ra.addState(true);
		RALocation ls = ra.addState(false);

		Register r1 = new Register(T_INT, 1);
		Register r2 = new Register(T_INT, 2);
		Parameter p1 = new Parameter(T_INT, 1);

		Expression<Boolean> gT = ExpressionUtil.TRUE;
		Expression<Boolean> gEqR1 = NumericBooleanExpression.create(p1, NumericComparator.EQ, r1);
		Expression<Boolean> gEqR2 = NumericBooleanExpression.create(p1, NumericComparator.EQ, r2);
		Expression<Boolean> gNER1 = NumericBooleanExpression.create(p1, NumericComparator.NE, r1);
		Expression<Boolean> gNER1R2 = ExpressionUtil.and(
				NumericBooleanExpression.create(p1, NumericComparator.NE, r1),
				NumericBooleanExpression.create(p1, NumericComparator.NE, r2));

		VarMapping<Register, Parameter> storeP1inR1 = new VarMapping<>();
		storeP1inR1.put(r1, p1);
		VarMapping<Register, Parameter> storeP1inR2 = new VarMapping<>();
		storeP1inR2.put(r2, p1);
		VarMapping<Register, SymbolicDataValue> storeP1inR2andR2inR1 = new VarMapping<>();
		storeP1inR2andR2inR1.put(r1, p1);
		storeP1inR2andR2inR1.put(r2, r1);
		VarMapping<Register, Register> storeR2inR1 = new VarMapping<>();
		storeR2inR1.put(r1, r2);

		Assignment assP1inR1 = new Assignment(storeP1inR1);
		Assignment assP1inR2 = new Assignment(storeP1inR2);
		Assignment assP1inR1andR2inR1 = new Assignment(storeP1inR2andR2inR1);
		Assignment assR2inR1 = new Assignment(storeR2inR1);
		Assignment assNo = new Assignment(new VarMapping<>());

		ra.addTransition(l0, ALPHA, new InputTransition(gT, ALPHA, l0, l1, assP1inR1));
		ra.addTransition(l0, BETA, new InputTransition(gT, BETA, l0, ls, assNo));

		ra.addTransition(l1, ALPHA, new InputTransition(gT, ALPHA, l1, l2, assP1inR2));
		ra.addTransition(l1, BETA, new InputTransition(gNER1, BETA, l1, l2, assP1inR1andR2inR1));
		ra.addTransition(l1, BETA, new InputTransition(gEqR1, BETA, l1, l3, assNo));

		ra.addTransition(l2, ALPHA, new InputTransition(gEqR1, ALPHA, l2, l3, assNo));
		ra.addTransition(l2, ALPHA, new InputTransition(gEqR2, ALPHA, l2, l3, assNo));
		ra.addTransition(l2, ALPHA, new InputTransition(gNER1R2, ALPHA, l2, ls, assNo));
		ra.addTransition(l2, BETA, new InputTransition(gT, BETA, l2, l1, assR2inR1));

		ra.addTransition(l3, ALPHA, new InputTransition(gT, ALPHA, l3, ls, assNo));
		ra.addTransition(l3, BETA, new InputTransition(gT, BETA, l3, ls, assNo));

		ra.addTransition(ls, ALPHA, new InputTransition(gT, ALPHA, ls, ls, assNo));
		ra.addTransition(ls, BETA, new InputTransition(gT, BETA, ls, ls, assNo));

		return ra;
	}
}
