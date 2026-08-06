package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.priority.PriorityQueueOracle.OFFER;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.POLL;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.doubleType;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.IPUT;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.OECHO;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.TINT;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.CacheDataWordOracle;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.ct.CTPath;
import de.learnlib.ralib.ct.Prefix;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.equivalence.RAEquivalenceTest;
import de.learnlib.ralib.example.repeater.RepeaterSUL;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.MeasuringOracle;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SLLambdaEqRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.DisjunctionRestriction;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTLeaf;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.UnmappedEqualityRestriction;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class TestSuffixOptimization extends RaLibTestSuite {

	private static final InputSymbol A = new InputSymbol("a", TINT);
	private static final InputSymbol B = new InputSymbol("b", TINT, TINT);
	private static final InputSymbol C = new InputSymbol("c");

    @Test
    public void testLearnRepeaterSuffixOpt() {

        Constants consts = new Constants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory theory = new IntegerEqualityTheory(TINT);
        theory.setUseSuffixOpt(true);
        teachers.put(TINT, theory);

        RepeaterSUL sul = new RepeaterSUL(-1, 2);
        IOOracle ioOracle = new SULOracle(sul, RepeaterSUL.ERROR);
	IOCache ioCache = new IOCache(ioOracle);
	IOFilter oracle = new IOFilter(ioCache, sul.getInputSymbols());

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto =
	    new MultiTheoryTreeOracle(oracle, teachers, consts, solver);

        Measurements measurements = new Measurements();
        QueryStatistics stats = new QueryStatistics(measurements, sul);

        SLLambda learner = new SLLambda(mto, teachers, consts, true, solver, sul.getActionSymbols());
        learner.setStatisticCounter(stats);

        learner.learn();

        Word<PSymbolInstance> ce =
	    Word.fromSymbols(new PSymbolInstance(IPUT, new DataValue(TINT, BigDecimal.ZERO)),
			     new PSymbolInstance(OECHO, new DataValue(TINT, BigDecimal.ZERO)),
			     new PSymbolInstance(IPUT, new DataValue(TINT, BigDecimal.ONE)),
			     new PSymbolInstance(OECHO, new DataValue(TINT, BigDecimal.ONE)),
			     new PSymbolInstance(IPUT, new DataValue(TINT, new BigDecimal(2))),
			     new PSymbolInstance(OECHO, new DataValue(TINT, new BigDecimal(2))));

        learner.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, false));

        learner.learn();

	Hypothesis hyp = learner.getHypothesis();
        Assert.assertEquals(hyp.getStates().size(), 7);

	String str = stats.toString();
	Assert.assertTrue(str.contains("Counterexamples: 1"));
        Assert.assertTrue(str.contains("CE max length: 6"));
        Assert.assertTrue(str.contains("CE Analysis: {TQ: 0, Resets: 2, Inputs: 5}"));
        Assert.assertTrue(str.contains("Processing / Refinement: {TQ: 0, Resets: 1, Inputs: 4}"));
        Assert.assertTrue(str.contains("Other: {TQ: 0, Resets: 1, Inputs: 1}"));
        Assert.assertTrue(str.contains("Total: {TQ: 0, Resets: 4, Inputs: 10}"));
    }

    @Test
    public void testLearnRepeaterSuffixOptSLLEq() {

        Constants consts = new Constants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory theory = new IntegerEqualityTheory(TINT);
        theory.setUseSuffixOpt(true);
        teachers.put(TINT, theory);

        RepeaterSUL sul = new RepeaterSUL(-1, 2);
        IOOracle ioOracle = new SULOracle(sul, RepeaterSUL.ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter oracle = new IOFilter(ioCache, sul.getInputSymbols());

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto =
	    new MultiTheoryTreeOracle(oracle, teachers, consts, solver);

        Measurements measurements = new Measurements();
        QueryStatistics stats = new QueryStatistics(measurements, sul);

        SLLambda learner = new SLLambdaEq(mto, teachers, consts, true, solver, sul.getActionSymbols());
        learner.setStatisticCounter(stats);

        learner.learn();

        Word<PSymbolInstance> ce =
	    Word.fromSymbols(new PSymbolInstance(IPUT, new DataValue(TINT, BigDecimal.ZERO)),
			     new PSymbolInstance(OECHO, new DataValue(TINT, BigDecimal.ZERO)),
			     new PSymbolInstance(IPUT, new DataValue(TINT, BigDecimal.ONE)),
			     new PSymbolInstance(OECHO, new DataValue(TINT, BigDecimal.ONE)),
			     new PSymbolInstance(IPUT, new DataValue(TINT, new BigDecimal(2))),
			     new PSymbolInstance(OECHO, new DataValue(TINT, new BigDecimal(2))));

        learner.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, false));

        learner.learn();

        Hypothesis hyp = learner.getHypothesis();
        Assert.assertEquals(hyp.getStates().size(), 7);

        String str = stats.toString();
        Assert.assertTrue(str.contains("Counterexamples: 1"));
        Assert.assertTrue(str.contains("CE max length: 6"));
        Assert.assertTrue(str.contains("CE Analysis: {TQ: 0, Resets: 2, Inputs: 5}"));
        Assert.assertTrue(str.contains("Processing / Refinement: {TQ: 0, Resets: 1, Inputs: 4}"));
        Assert.assertTrue(str.contains("Other: {TQ: 0, Resets: 1, Inputs: 1}"));
        Assert.assertTrue(str.contains("Total: {TQ: 0, Resets: 4, Inputs: 10}"));
    }

    @Test
    public void testLearnPQSuffixOpt() {

        Constants consts = new Constants();
        DataWordOracle dwOracle = new de.learnlib.ralib.example.priority.PriorityQueueOracle(2);
        CacheDataWordOracle ioCache = new CacheDataWordOracle(dwOracle);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(doubleType);
        dit.useSuffixOptimization(true);
        teachers.put(doubleType, dit);

        Measurements m = new Measurements();
        ConstraintSolver solver = TestUtil.getZ3Solver();
        QueryStatistics stats = new QueryStatistics(m, ioCache);
        MeasuringOracle mto = new MeasuringOracle(new MultiTheoryTreeOracle(ioCache, teachers, consts, solver), m);

        SLLambda learner = new SLLambda(mto, teachers, consts, false, solver, OFFER, POLL);
        learner.setStatisticCounter(stats);
        learner.learn();

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(OFFER, new DataValue(doubleType, BigDecimal.ONE)),
                new PSymbolInstance(OFFER, new DataValue(doubleType, BigDecimal.ZERO)),
                new PSymbolInstance(POLL, new DataValue(doubleType, BigDecimal.ZERO)),
                new PSymbolInstance(POLL, new DataValue(doubleType, BigDecimal.ONE)));
        learner.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, true));

        learner.learn();
        Hypothesis hyp = learner.getHypothesis();

        Assert.assertEquals(hyp.getStates().size(), 4);
        Assert.assertEquals(hyp.getTransitions().size(), 11);

        String str = stats.toString();
        Assert.assertTrue(str.contains("Counterexamples: 1"));
        Assert.assertTrue(str.contains("CE max length: 4"));
        Assert.assertTrue(str.contains("CE Analysis: {TQ: 81, Resets: 92, Inputs: 0}"));
        Assert.assertTrue(str.contains("Processing / Refinement: {TQ: 27, Resets: 514, Inputs: 0}"));
        Assert.assertTrue(str.contains("Other: {TQ: 5, Resets: 5, Inputs: 0}"));
        Assert.assertTrue(str.contains("Total: {TQ: 113, Resets: 611, Inputs: 0}"));
    }

    @Test
    public void testExtendSuffixLocation() {
    	IntegerEqualityTheory iet = new IntegerEqualityTheory(TINT);
    	iet.setUseSuffixOpt(true);
    	Map<DataType, Theory> teachers = Map.of(TINT, iet);

    	SLLambdaEqRestrictionBuilder builder = new SLLambdaEqRestrictionBuilder(new Constants(), teachers, new ConstraintSolver());

    	SuffixValue s1 = new SuffixValue(TINT, 1);
    	SuffixValue s2 = new SuffixValue(TINT, 2);
    	SuffixValue s3 = new SuffixValue(TINT, 3);
    	SuffixValue s4 = new SuffixValue(TINT, 4);
    	SuffixValue s5 = new SuffixValue(TINT, 5);

    	DataValue d0 = new DataValue(TINT, BigDecimal.ZERO);
    	DataValue d1 = new DataValue(TINT, BigDecimal.ONE);
    	DataValue d2 = new DataValue(TINT, BigDecimal.valueOf(2));

    	PSymbolInstance a1 = new PSymbolInstance(A, d1);
    	PSymbolInstance a2 = new PSymbolInstance(A, d2);
    	PSymbolInstance b12 = new PSymbolInstance(B, d1, d2);
    	PSymbolInstance b21 = new PSymbolInstance(B, d2, d1);

    	SDT u1ExtSdt = new SDT(Map.of(
    			new SDTGuard.EqualityGuard(s1, d2), new SDT(Map.of(
    					new SDTGuard.EqualityGuard(s2, d2), new SDT(Map.of(
    							new SDTGuard.SDTTrueGuard(s3), SDTLeaf.REJECTING)),
    					new SDTGuard.DisequalityGuard(s2, d2), new SDT(Map.of(
    							new SDTGuard.SDTTrueGuard(s3), SDTLeaf.ACCEPTING)))),
    			new SDTGuard.DisequalityGuard(s1, d2), new SDT(Map.of(
    					new SDTGuard.SDTTrueGuard(s2), new SDT(Map.of(
    							new SDTGuard.SDTTrueGuard(s3), SDTLeaf.REJECTING))))));
    	SDT u2ExtSdt = new SDT(Map.of(
    			new SDTGuard.SDTTrueGuard(s1), new SDT(Map.of(
    					new SDTGuard.SDTTrueGuard(s2), new SDT(Map.of(
    							new SDTGuard.SDTTrueGuard(s3), SDTLeaf.REJECTING))))));

    	SDT u1ExtPriorSdt = new SDT(Map.of(
    			new SDTGuard.EqualityGuard(s1, d1), SDTLeaf.ACCEPTING,
    			new SDTGuard.DisequalityGuard(s1, d1), SDTLeaf.REJECTING));
    	SDT u2ExtPriorSdt = new SDT(Map.of(
    			new SDTGuard.EqualityGuard(s1, d1), SDTLeaf.ACCEPTING,
    			new SDTGuard.DisequalityGuard(s1, d1), SDTLeaf.REJECTING));
    	SDT u1Prior = new SDT(Map.of(
    			new SDTGuard.EqualityGuard(s1, d2), SDTLeaf.ACCEPTING,
    			new SDTGuard.DisequalityGuard(s1, d2), SDTLeaf.REJECTING));
    	SDT u2Prior = new SDT(Map.of(
    			new SDTGuard.EqualityGuard(s1, d1), SDTLeaf.ACCEPTING,
    			new SDTGuard.DisequalityGuard(s1, d1), SDTLeaf.REJECTING));

    	Map<SuffixValue, AbstractSuffixValueRestriction> restr1 = new LinkedHashMap<>();
    	restr1.put(s1, DisjunctionRestriction.create(s1, new UnmappedEqualityRestriction(s1), new FreshSuffixValue(s1)));
    	restr1.put(s2, DisjunctionRestriction.create(s2, new UnmappedEqualityRestriction(s2), new FreshSuffixValue(s2)));
    	restr1.put(s3, SuffixValueRestriction.equalityRestriction(s3, d2));
    	SymbolicSuffix suffix = new SymbolicSuffix(Word.fromSymbols(B, A), restr1);
    	SymbolicSuffix suffixPrior = new SymbolicSuffix(Word.fromSymbols(A), Map.of(s1, DisjunctionRestriction.create(s1, new UnmappedEqualityRestriction(s1), new FreshSuffixValue(s1))));

    	Bijection<DataValue> u1rp = new Bijection<>();
    	u1rp.put(d2, d0);
    	Bijection<DataValue> u2rp = new Bijection<>();
    	u2rp.put(d1, d0);
    	Bijection<DataValue> u1ExtRp = new Bijection<>();
    	u1ExtRp.put(d1, d1);
    	u1ExtRp.put(d2, d2);
    	Bijection<DataValue> u2ExtRp = new Bijection<>();
    	u2ExtRp.put(d1, d1);
    	Bijection<DataValue> u1ExtPriorRp = new Bijection<>();
    	u1ExtPriorRp.put(d1, d2);
    	Bijection<DataValue> u2ExtPriorRp = new Bijection<>();
    	u2ExtPriorRp.put(d1, d2);

    	CTPath u1Path = new CTPath(false);
    	u1Path.putSDT(suffixPrior, u1Prior);
    	CTPath u2Path =  new CTPath(false);
    	u2Path.putSDT(suffixPrior, u2Prior);
    	CTPath u1ExtPath = new CTPath(false);
    	u1ExtPath.putSDT(suffixPrior, u1ExtPriorSdt);
    	u1ExtPath.putSDT(suffix, u1ExtSdt);
    	CTPath u2ExtPath = new CTPath(false);
    	u2ExtPath.putSDT(suffixPrior, u2ExtPriorSdt);
    	u2ExtPath.putSDT(suffix, u2ExtSdt);

    	Prefix u1 = new Prefix(Word.fromSymbols(b12), u1rp, u1Path);
    	Prefix u2 = new Prefix(Word.fromSymbols(a1, a2), u2rp, u2Path);
    	Prefix u1Ext = new Prefix(Word.fromSymbols(b12, b21), u1ExtRp, u1ExtPath);
    	u1Ext.putBijection(suffixPrior, u1ExtPriorRp);
    	Prefix u2Ext = new Prefix(Word.fromSymbols(a1, a2, b12), u2ExtRp, u2ExtPath);
    	u2Ext.putBijection(suffixPrior, u2ExtPriorRp);

    	Map<SuffixValue, AbstractSuffixValueRestriction> expRestrAlt1 = new LinkedHashMap<>();
    	expRestrAlt1.put(s1, SuffixValueRestriction.equalityRestriction(s1, d0));
    	expRestrAlt1.put(s2, DisjunctionRestriction.create(s2, new UnmappedEqualityRestriction(s2), new FreshSuffixValue(s2)));
    	expRestrAlt1.put(s3, SuffixValueRestriction.equalityRestriction(s3, d0));
    	expRestrAlt1.put(s4, new FreshSuffixValue(s4));
    	expRestrAlt1.put(s5, SuffixValueRestriction.equalityRestriction(s5, s1, s2));
    	SymbolicSuffix expectedAlt1 = new SymbolicSuffix(Word.fromSymbols(B, B, A), expRestrAlt1);

    	SymbolicSuffix actual = builder.extendSuffix(u1, u1Ext, u2, u2Ext, suffix, u1ExtSdt, u2ExtSdt);
    	Assert.assertEquals(actual, expectedAlt1);
    }

    @Test
    public void testMultipleParamsEquality() {
        Constants consts = new Constants();
        RegisterAutomaton ra = buildAutomaton();
        DataWordOracle dwOracle = new SimulatorOracle(ra);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory theory = new IntegerEqualityTheory(TINT);
        theory.setUseSuffixOpt(true);
        teachers.put(TINT, theory);

        DataValue d0 = new DataValue(TINT, BigDecimal.ZERO);

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto =
	    new MultiTheoryTreeOracle(dwOracle, teachers, consts, solver);

        SLLambda learner = new SLLambda(mto, teachers, consts, false, solver, B, C);

        learner.learn();

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(B, d0, d0),
        		new PSymbolInstance(B, d0, d0),
        		new PSymbolInstance(C));
        learner.addCounterexample(new DefaultQuery<>(ce, true));

        learner.learn();

        RAEquivalenceTest ioEquiv = new RAEquivalenceTest(
                ra, teachers, consts, true, B, C);

        Hypothesis hyp = learner.getHypothesis();
        DefaultQuery<PSymbolInstance, Boolean> finalCe = ioEquiv.findCounterExample(hyp, null);

        Assert.assertNull(finalCe);
    }

    private RegisterAutomaton buildAutomaton() {
    	MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

    	RALocation l0 = ra.addInitialState();
    	RALocation l1 = ra.addState();
    	RALocation l2 = ra.addState();
    	RALocation ls = ra.addState(false);

    	Parameter p1 = new Parameter(TINT, 1);
    	Parameter p2 = new Parameter(TINT, 2);
    	Register r1 = new Register(TINT, 1);
    	Register r2 = new Register(TINT, 2);

    	VarMapping<Register, Parameter> storeMap = new VarMapping<>();
    	storeMap.put(r1, p1);
    	storeMap.put(r2, p2);

    	Assignment store = new Assignment(storeMap);
    	Assignment no = new Assignment(new VarMapping<>());

    	Expression<Boolean> gEq1 = new NumericBooleanExpression(r1, NumericComparator.EQ, p1);
    	Expression<Boolean> gEq2 = new NumericBooleanExpression(r2, NumericComparator.EQ, p2);
    	Expression<Boolean> gNe1 = new NumericBooleanExpression(r1, NumericComparator.NE, p1);
    	Expression<Boolean> gNe2 = new NumericBooleanExpression(r2, NumericComparator.NE, p2);
    	Expression<Boolean> gEq = ExpressionUtil.and(gEq1, gEq2);
    	Expression<Boolean> gNe = ExpressionUtil.or(gNe1, gNe2);
    	Expression<Boolean> gT = ExpressionUtil.TRUE;

    	ra.addTransition(l0, B, new InputTransition(gT, B, l0, l1, store));
    	ra.addTransition(l0, C, new InputTransition(gT, C, l0, ls, no));
    	ra.addTransition(l1, B, new InputTransition(gEq, B, l1, l2, no));
    	ra.addTransition(l1, B, new InputTransition(gNe, B, l1, ls, no));
    	ra.addTransition(l1, C, new InputTransition(gT, C, l1, ls, no));
    	ra.addTransition(l2, B, new InputTransition(gT, B, l2, ls, no));
    	ra.addTransition(l2, C, new InputTransition(gT, C, l2, l0, no));
    	ra.addTransition(ls, B, new InputTransition(gT, B, ls, ls, no));
    	ra.addTransition(ls, C, new InputTransition(gT, C, ls, ls, no));

    	return ra;
    }
}
