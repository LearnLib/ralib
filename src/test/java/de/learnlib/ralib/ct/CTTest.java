package de.learnlib.ralib.ct;

import static de.learnlib.ralib.example.priority.PriorityQueueOracle.OFFER;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.POLL;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.doubleType;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class CTTest {
	private final OutputSymbol YES = new OutputSymbol("yes");
	private final OutputSymbol NO = new OutputSymbol("no");

	@Test
	public void testStackCT() {
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

        Word<PSymbolInstance> w1 = Word.fromSymbols(push0);
        Word<PSymbolInstance> w2 = Word.fromSymbols(pop0);
        Word<PSymbolInstance> w3 = Word.fromSymbols(push0, push1);
        Word<PSymbolInstance> w4 = Word.fromSymbols(push0, pop0);
        Word<PSymbolInstance> w5 = Word.fromSymbols(push0, push1, pop1);

        SymbolicSuffix s1 = new SymbolicSuffix(w1, Word.fromSymbols(push1));
        SymbolicSuffix s2 = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(push0, push1));
        SymbolicSuffix s3 = new SymbolicSuffix(w1, Word.fromSymbols(pop0));
        SymbolicSuffix s4 = new SymbolicSuffix(w2, Word.fromSymbols(pop1, pop0));

        ClassificationTree ct = new ClassificationTree(mto, solver, restrBuilder, suffixBuilder, consts, false, I_PUSH, I_POP);

        ct.sift(Word.epsilon());
//        ct.expand(Word.epsilon());
        boolean consistent = ct.checkLocationClosedness();
        Assert.assertFalse(consistent);
        Assert.assertEquals(ct.getLeaves().size(), 2);
        Assert.assertEquals(ct.getPrefixes().size(), 3);

        consistent = ct.checkLocationClosedness();
        Assert.assertFalse(consistent);
        consistent = ct.checkLocationClosedness();
        Assert.assertTrue(consistent);

        ct.expand(w1);
//        ct.expand(w2);
        Assert.assertEquals(ct.getLeaves().size(), 2);
        Assert.assertEquals(ct.getPrefixes().size(), 7);

        ct.refine(ct.getLeaf(w3), s1);
        Assert.assertEquals(ct.getLeaves().size(), 3);
        Assert.assertEquals(ct.getPrefixes().size(), 7);

        consistent = ct.checkLocationClosedness();
        Assert.assertFalse(consistent);

        ct.refine(ct.getLeaf(w1), s2);
        Assert.assertEquals(ct.getLeaves().size(), 4);
        Assert.assertEquals(ct.getPrefixes().size(), 9);

        ct.refine(ct.getLeaf(w1), s3);
//        ct.sift(w4);
        consistent = ct.checkTransitionClosedness();
        Assert.assertFalse(consistent);
        Assert.assertEquals(ct.getLeaves().size(), 4);
        Assert.assertEquals(ct.getPrefixes().size(), 10);
        Assert.assertTrue(ct.getLeaf(Word.epsilon()).getPrefixes().contains(w4));

        ct.refine(ct.getLeaf(w3), s3);
//        ct.expand(w3);
        consistent = ct.checkTransitionClosedness();
        Assert.assertFalse(consistent);
        Assert.assertEquals(ct.getLeaves().size(), 4);
        Assert.assertEquals(ct.getPrefixes().size(), 11);
        Assert.assertTrue(ct.getLeaf(w1).getPrefixes().contains(w5));

//        Register r1 = new Register(T_INT, 1);
//        Register r2 = new Register(T_INT, 2);
//        ct.refine(ct.getLeaf(w3), s4);
        consistent = ct.checkRegisterClosedness();
        Assert.assertFalse(consistent);
        Assert.assertTrue(ct.getLeaf(w3).getRepresentativePrefix().getRegisters().contains(dv0));
        Assert.assertTrue(ct.getLeaf(w3).getRepresentativePrefix().getRegisters().contains(dv1));

        CTAutomatonBuilder ab = new CTAutomatonBuilder(ct, consts, false, solver);
        Hypothesis hyp = ab.buildHypothesis();

        Assert.assertEquals(hyp.getStates().size(), 4);
        Assert.assertEquals(hyp.getTransitions().size(), 10);
        Assert.assertEquals(hyp.getAccessSequences().size(), hyp.getStates().size());
	}

	@Test
	public void testPQCT() {
        DataWordOracle dwOracle = new de.learnlib.ralib.example.priority.PriorityQueueOracle(2);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(doubleType, new DoubleInequalityTheory(doubleType));

		Constants consts = new Constants();

		SymbolicSuffixRestrictionBuilder restrBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);
		OptimizedSymbolicSuffixBuilder suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrBuilder);

        ConstraintSolver solver = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);

        DataValue dv0 = new DataValue(doubleType, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(doubleType, BigDecimal.ONE);
        DataValue dv2 = new DataValue(doubleType, BigDecimal.valueOf(2));

        PSymbolInstance offer0 = new PSymbolInstance(OFFER, dv0);
        PSymbolInstance offer1 = new PSymbolInstance(OFFER, dv1);
        PSymbolInstance offer2 = new PSymbolInstance(OFFER, dv2);
        PSymbolInstance poll0 = new PSymbolInstance(POLL, dv0);
        PSymbolInstance poll1 = new PSymbolInstance(POLL, dv1);
        PSymbolInstance poll2 = new PSymbolInstance(POLL, dv2);

        Word<PSymbolInstance> o1 = Word.fromSymbols(offer1);
        Word<PSymbolInstance> o1o2 = Word.fromSymbols(offer1, offer2);
        Word<PSymbolInstance> o1o1 = Word.fromSymbols(offer1, offer1);
        Word<PSymbolInstance> o1o0 = Word.fromSymbols(offer1, offer0);
        Word<PSymbolInstance> o1p1 = Word.fromSymbols(offer1, poll1);
        Word<PSymbolInstance> o1o2p1 = Word.fromSymbols(offer1, offer2, poll1);
        Word<PSymbolInstance> o1o0p0 = Word.fromSymbols(offer1, offer0, poll0);

        SymbolicSuffix s1 = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(offer1, poll1));
        SymbolicSuffix s2 = new SymbolicSuffix(o1, Word.fromSymbols(poll1));
        SymbolicSuffix s3 = new SymbolicSuffix(o1o2, Word.fromSymbols(poll1, poll2));

        ClassificationTree ct = new ClassificationTree(mto, solver, restrBuilder, suffixBuilder, consts, false, OFFER, POLL);

        ct.sift(Word.epsilon());
        boolean closed = ct.checkLocationClosedness();
        Assert.assertFalse(closed);
        closed = ct.checkTransitionClosedness();
        Assert.assertTrue(closed);
        closed = ct.checkLocationClosedness();
        Assert.assertFalse(closed);

        ct.expand(o1);
        ct.sift(o1p1);
        boolean consistent = ct.checkTransitionConsistency();
        Assert.assertFalse(consistent);

        ct.expand(o1o2);
        consistent = ct.checkLocationConsistency();
        Assert.assertFalse(consistent);
        closed = ct.checkRegisterClosedness();
        Assert.assertFalse(closed);

        ct.sift(o1o0);
        consistent = ct.checkTransitionConsistency();
        Assert.assertFalse(consistent);

        closed = ct.checkRegisterClosedness();
        Assert.assertTrue(closed);
//        ct.sift(o1p1);


//        ct.refine(ct.getLeaf(Word.epsilon()), s1);
//        ct.expand(Word.epsilon());
//        Assert.assertEquals(ct.getLeaves().size(), 3);
//
//        ct.expand(o1);
//        Assert.assertEquals(ct.getLeaves().size(), 4);
//
//        ct.refine(ct.getLeaf(o1), s2);
//        ct.sift(o1p1);

//        ct.refine(ct.getLeaf(o1o1), s3);
//        ct.expand(o1o1);
//        ct.expand(o1o0);
//        Assert.assertEquals(ct.getLeaves().size(), 5);



        CTAutomatonBuilder ab = new CTAutomatonBuilder(ct, new Constants(), false, solver);
        Hypothesis hyp = ab.buildHypothesis();

        Assert.assertEquals(hyp.getStates().size(), ct.getLeaves().size());
        Assert.assertEquals(hyp.getTransitions().size(), ct.getPrefixes().size() - 1);
        Assert.assertTrue(hyp.accepts(o1o2p1));
        Assert.assertTrue(hyp.accepts(o1o0p0));;
	}

	@Test(enabled=false)
	public void testCTAutomatonBuilder() {
		RegisterAutomaton sul = buildTestRA();
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(doubleType, new DoubleInequalityTheory(doubleType));

		Constants consts = new Constants();

		SymbolicSuffixRestrictionBuilder restrBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);
		OptimizedSymbolicSuffixBuilder suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrBuilder);

        ConstraintSolver solver = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, consts, solver);

		System.out.println(sul);

        DataValue dv0 = new DataValue(doubleType, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(doubleType, BigDecimal.ONE);
        DataValue dv2 = new DataValue(doubleType, BigDecimal.valueOf(2));
        DataValue dv3 = new DataValue(doubleType, BigDecimal.valueOf(3));

        PSymbolInstance offer0 = new PSymbolInstance(OFFER, dv0);
        PSymbolInstance offer1 = new PSymbolInstance(OFFER, dv1);
        PSymbolInstance offer2 = new PSymbolInstance(OFFER, dv2);
        PSymbolInstance offer3 = new PSymbolInstance(OFFER, dv3);
        PSymbolInstance yes = new PSymbolInstance(YES);
        PSymbolInstance no = new PSymbolInstance(NO);

        Word<PSymbolInstance> o1 = Word.fromSymbols(offer1);
        Word<PSymbolInstance> o1y = Word.fromSymbols(offer1, yes);
        Word<PSymbolInstance> o1yo2 = Word.fromSymbols(offer1, yes, offer2);
        Word<PSymbolInstance> o1yo2y = Word.fromSymbols(offer1, yes, offer2, yes);
        Word<PSymbolInstance> o1yo2yo3 = Word.fromSymbols(offer1, yes, offer2, yes, offer3);
        Word<PSymbolInstance> o1yo2yo2 = Word.fromSymbols(offer1, yes, offer2, yes, offer2);
        Word<PSymbolInstance> o1yo2yo0 = Word.fromSymbols(offer1, yes, offer2, yes, offer0);
        Word<PSymbolInstance> o1yo2yo3y = Word.fromSymbols(offer1, yes, offer2, yes, offer3, yes);
        Word<PSymbolInstance> o1yo2yo2y = Word.fromSymbols(offer1, yes, offer2, yes, offer2, yes);
        Word<PSymbolInstance> o1yo2yo0y = Word.fromSymbols(offer1, yes, offer2, yes, offer0, yes);
        Word<PSymbolInstance> o1yo2yo3yo3 = Word.fromSymbols(offer1, yes, offer2, yes, offer3, yes, offer3);
        Word<PSymbolInstance> o1yo2yo3yo2 = Word.fromSymbols(offer1, yes, offer2, yes, offer3, yes, offer2);
        Word<PSymbolInstance> o1yo2yo3yo3y = Word.fromSymbols(offer1, yes, offer2, yes, offer3, yes, offer3, yes);
        Word<PSymbolInstance> o1yo2yo3yo2n = Word.fromSymbols(offer1, yes, offer2, yes, offer3, yes, offer2, no);

        SymbolicSuffix sy = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(yes));
        SymbolicSuffix sn = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(no));
        SymbolicSuffix so = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(offer1));
        SymbolicSuffix syoyoyoy = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(yes, offer2, yes, offer3, yes, offer3, yes));
        SymbolicSuffix soyoyoy = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(offer2, yes, offer3, yes, offer3, yes));
        SymbolicSuffix syoyoy = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(yes, offer3, yes, offer3, yes));

        ClassificationTree ct = new ClassificationTree(mto, solver, restrBuilder, suffixBuilder, consts, true, OFFER, YES, NO);

        ct.sift(Word.epsilon());
        ct.refine(ct.getLeaf(Word.epsilon()), sn);
        ct.refine(ct.getLeaf(Word.epsilon()), sy);
        ct.refine(ct.getLeaf(Word.epsilon()), so);
        ct.refine(ct.getLeaf(Word.epsilon()), syoyoyoy);
        ct.expand(Word.epsilon());
        ct.refine(ct.getLeaf(Word.epsilon()), soyoyoy);
        ct.expand(o1);
        ct.refine(ct.getLeaf(o1), syoyoy);
        ct.expand(o1y);
        ct.expand(o1yo2);
        ct.expand(o1yo2y);
        ct.expand(o1yo2yo0);
        ct.expand(o1yo2yo2);
        ct.expand(o1yo2yo0y);
        ct.expand(o1yo2yo2y);

        System.out.println(ct);

        CTAutomatonBuilder ab = new CTAutomatonBuilder(ct, new Constants(), true, solver);
        Hypothesis hyp = ab.buildHypothesis();

        System.out.println(hyp);
	}

	private RegisterAutomaton buildTestRA() {
		MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

		RALocation l0i = ra.addInitialState();
		RALocation l0o = ra.addState();
		RALocation l1i = ra.addState();
		RALocation l1o = ra.addState();
		RALocation l2i = ra.addState();
		RALocation l2o = ra.addState();
		RALocation l3i = ra.addState();
		RALocation l3o1 = ra.addState();
		RALocation l3o2 = ra.addState();

		Parameter p1 = new Parameter(doubleType, 1);
		Register r1 = new Register(doubleType, 1);
		Register r2 = new Register(doubleType, 2);
		SuffixValue s1 = new SuffixValue(doubleType, 1);

		Expression<Boolean> gT = ExpressionUtil.TRUE;
		Expression<Boolean> gL = new NumericBooleanExpression(r1, NumericComparator.LE, p1);
		Expression<Boolean> gG = new NumericBooleanExpression(r1, NumericComparator.GE, p1);
		Expression<Boolean> gEq = new NumericBooleanExpression(r1, NumericComparator.EQ, p1);
		Expression<Boolean> gNe = new NumericBooleanExpression(r1, NumericComparator.NE, p1);

		VarMapping<Register, SymbolicDataValue> storeP1 = new VarMapping<>();
		storeP1.put(r1, p1);
		VarMapping<Register, SymbolicDataValue> storeR1 = new VarMapping<>();
		storeR1.put(r1, r1);
		VarMapping<Register, SymbolicDataValue> storeP1R1 = new VarMapping<>();
		storeP1R1.put(r1, p1);
		storeP1R1.put(r2, r1);
		VarMapping<Register, SymbolicDataValue> storeR1P1 = new VarMapping<>();
		storeR1P1.put(r1, r1);
		storeR1P1.put(r2, p1);
		VarMapping<Register, SymbolicDataValue> storeR1R1 = new VarMapping<>();
		storeR1R1.put(r1, r1);
		storeR1R1.put(r2, r2);
		VarMapping<Register, SymbolicDataValue> emptyMapping = new VarMapping<>();

		Assignment assP1 = new Assignment(storeP1);
		Assignment assR1 = new Assignment(storeR1);
		Assignment assP1R1 = new Assignment(storeP1R1);
		Assignment assR1P1 = new Assignment(storeR1P1);
		Assignment assR1R1 = new Assignment(storeR1R1);
		Assignment assNo = new Assignment(emptyMapping);
		OutputMapping om = new OutputMapping(new ArrayList<>(), new VarMapping<>());

		ra.addTransition(l0i, OFFER, new InputTransition(gT, OFFER, l0i, l0o, assNo));
		ra.addTransition(l0o, YES, new OutputTransition(gT, om, YES, l0o, l1i, assNo));
		ra.addTransition(l1i, OFFER, new InputTransition(gT, OFFER, l1i, l1o, assP1));
		ra.addTransition(l1o, YES, new OutputTransition(gT, om, YES, l1o, l2i, assR1));
		ra.addTransition(l2i, OFFER, new InputTransition(gL, OFFER, l2i, l2o, assR1P1));
		ra.addTransition(l2i, OFFER, new InputTransition(gG, OFFER, l2i, l2o, assP1R1));
		ra.addTransition(l2o, YES, new OutputTransition(gT, om, YES, l2o, l3i, assR1R1));
		ra.addTransition(l3i, OFFER, new InputTransition(gEq, OFFER, l3i, l3o1, assR1));
		ra.addTransition(l3o1, YES, new OutputTransition(gT, om, YES, l3o1, l1i, assNo));
		ra.addTransition(l3i, OFFER, new InputTransition(gNe, OFFER, l3i, l3o2, assNo));
		ra.addTransition(l3o2, YES, new OutputTransition(gT, om, NO, l3o2, l3i, assR1R1));

		return ra;
	}
}
