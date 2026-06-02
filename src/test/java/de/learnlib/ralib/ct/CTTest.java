package de.learnlib.ralib.ct;

import static de.learnlib.ralib.example.priority.PriorityQueueOracle.OFFER;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.POLL;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.doubleType;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
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
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class CTTest {
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

        PSymbolInstance push0 = new PSymbolInstance(I_PUSH, dv0);
        PSymbolInstance push1 = new PSymbolInstance(I_PUSH, dv1);
        PSymbolInstance pop0 = new PSymbolInstance(I_POP, dv0);
        PSymbolInstance pop1 = new PSymbolInstance(I_POP, dv1);

        Word<PSymbolInstance> w1 = Word.fromSymbols(push0);
        Word<PSymbolInstance> w3 = Word.fromSymbols(push0, push1);
        Word<PSymbolInstance> w4 = Word.fromSymbols(push0, pop0);
        Word<PSymbolInstance> w5 = Word.fromSymbols(push0, push1, pop1);

        SymbolicSuffix s1 = new SymbolicSuffix(w1, Word.fromSymbols(push1));
        SymbolicSuffix s2 = new SymbolicSuffix(Word.epsilon(), Word.fromSymbols(push0, push1));
        SymbolicSuffix s3 = new SymbolicSuffix(w1, Word.fromSymbols(pop0));

        ClassificationTree ct = new ClassificationTree(mto, solver, restrBuilder, suffixBuilder, consts, false, I_PUSH, I_POP);

        ct.sift(Word.epsilon());
        boolean consistent = ct.checkLocationClosedness();
        Assert.assertFalse(consistent);
        Assert.assertEquals(ct.getLeaves().size(), 2);
        Assert.assertEquals(ct.getPrefixes().size(), 3);

        consistent = ct.checkLocationClosedness();
        Assert.assertFalse(consistent);
        consistent = ct.checkLocationClosedness();
        Assert.assertTrue(consistent);

        ct.expand(w1);
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
        consistent = ct.checkTransitionClosedness();
        Assert.assertFalse(consistent);
        Assert.assertEquals(ct.getLeaves().size(), 4);
        Assert.assertEquals(ct.getPrefixes().size(), 10);
        Assert.assertTrue(ct.getLeaf(Word.epsilon()).getPrefixes().contains(w4));

        ct.refine(ct.getLeaf(w3), s3);
        consistent = ct.checkTransitionClosedness();
        Assert.assertFalse(consistent);
        Assert.assertEquals(ct.getLeaves().size(), 4);
        Assert.assertEquals(ct.getPrefixes().size(), 11);
        Assert.assertTrue(ct.getLeaf(w1).getPrefixes().contains(w5));

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

        Word<PSymbolInstance> o1 = Word.fromSymbols(offer1);
        Word<PSymbolInstance> o1o2 = Word.fromSymbols(offer1, offer2);
        Word<PSymbolInstance> o1o0 = Word.fromSymbols(offer1, offer0);
        Word<PSymbolInstance> o1p1 = Word.fromSymbols(offer1, poll1);
        Word<PSymbolInstance> o1o2p1 = Word.fromSymbols(offer1, offer2, poll1);
        Word<PSymbolInstance> o1o0p0 = Word.fromSymbols(offer1, offer0, poll0);

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

        CTAutomatonBuilder ab = new CTAutomatonBuilder(ct, new Constants(), false, solver);
        Hypothesis hyp = ab.buildHypothesis();

        Assert.assertEquals(hyp.getStates().size(), ct.getLeaves().size());
        Assert.assertEquals(hyp.getTransitions().size(), ct.getPrefixes().size() - 1);
        Assert.assertTrue(hyp.accepts(o1o2p1));
        Assert.assertTrue(hyp.accepts(o1o0p0));;
	}
}
