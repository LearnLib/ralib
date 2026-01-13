package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.priority.PriorityQueueOracle.OFFER;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.POLL;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.doubleType;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.IPUT;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.OECHO;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.TINT;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.CacheDataWordOracle;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.ct.CTPath;
import de.learnlib.ralib.ct.Prefix;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.example.repeater.RepeaterSUL;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.Measurements;
import de.learnlib.ralib.learning.MeasuringOracle;
import de.learnlib.ralib.learning.QueryStatistics;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SLLambdaRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTLeaf;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.TrueRestriction;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class TestSuffixOptimization extends RaLibTestSuite {

	private static final InputSymbol A = new InputSymbol("a", TINT);

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
        Assert.assertTrue(str.contains("Processing / Refinement: {TQ: 32, Resets: 510, Inputs: 0}"));
        Assert.assertTrue(str.contains("Other: {TQ: 10, Resets: 5, Inputs: 0}"));
        Assert.assertTrue(str.contains("Total: {TQ: 123, Resets: 607, Inputs: 0}"));
    }

    @Test
    public void testExtendSuffixLocation() {
    	IntegerEqualityTheory iet = new IntegerEqualityTheory(TINT);
    	iet.setUseSuffixOpt(true);
    	Map<DataType, Theory> teachers = Map.of(TINT, iet);

    	SLLambdaRestrictionBuilder builder = new SLLambdaRestrictionBuilder(new Constants(), teachers);

    	SuffixValue s1 = new SuffixValue(TINT, 1);
    	SuffixValue s2 = new SuffixValue(TINT, 2);
    	SuffixValue s3 = new SuffixValue(TINT, 3);

    	DataValue d1 = new DataValue(TINT, BigDecimal.ONE);
    	DataValue d2 = new DataValue(TINT, BigDecimal.valueOf(2));
    	DataValue d3 = new DataValue(TINT, BigDecimal.valueOf(3));
    	DataValue d4 = new DataValue(TINT, BigDecimal.valueOf(4));
    	DataValue d5 = new DataValue(TINT, BigDecimal.valueOf(5));

    	PSymbolInstance a1 = new PSymbolInstance(A, d1);
    	PSymbolInstance a2 = new PSymbolInstance(A, d2);
    	PSymbolInstance a3 = new PSymbolInstance(A, d3);
    	PSymbolInstance a4 = new PSymbolInstance(A, d4);
    	PSymbolInstance a5 = new PSymbolInstance(A, d5);

    	SDT u1ExtSdt = new SDT(Map.of(
    			new SDTGuard.EqualityGuard(s1, d3), new SDT(Map.of(
    					new SDTGuard.EqualityGuard(s2, d4), SDTLeaf.ACCEPTING,
    					new SDTGuard.DisequalityGuard(s2, d4), SDTLeaf.REJECTING)),
    			new SDTGuard.DisequalityGuard(s1, d3), new SDT(Map.of(
    					new SDTGuard.SDTTrueGuard(s2), SDTLeaf.REJECTING))));
    	SDT u2ExtSdt = new SDT(Map.of(
    			new SDTGuard.SDTTrueGuard(s1), new SDT(Map.of(
    					new SDTGuard.SDTTrueGuard(s2), SDTLeaf.REJECTING))));

    	SDT u1Sdt = new SDT(Map.of(new SDTGuard.SDTTrueGuard(s1), SDTLeaf.ACCEPTING));

    	Map<SuffixValue, AbstractSuffixValueRestriction> restr1 = new LinkedHashMap<>();
    	restr1.put(s1, new TrueRestriction(s1));
    	restr1.put(s2, new TrueRestriction(s2));
    	SymbolicSuffix suffix1 = new SymbolicSuffix(Word.fromSymbols(A, A), restr1);

    	Bijection<DataValue> u1rp = new Bijection<>();
    	Bijection<DataValue> u2rp = new Bijection<>();
    	Bijection<DataValue> u1ExtRp = new Bijection<>();
    	u1ExtRp.put(d3, d1);
    	u1ExtRp.put(d4, d2);
    	Bijection<DataValue> u2ExtRp = u1rp;

    	CTPath u1Path = new CTPath(false);
    	u1Path.putSDT(RaStar.EMPTY_SUFFIX, u1Sdt);
    	CTPath u2Path = u1Path;
    	CTPath u1ExtPath = new CTPath(false);
    	u1ExtPath.putSDT(suffix1, u1ExtSdt);
    	CTPath u2ExtPath = new CTPath(false);
    	u2ExtPath.putSDT(suffix1, u2ExtSdt);

    	Prefix u1 = new Prefix(Word.fromSymbols(a3), u1rp, u1Path);
    	Prefix u2 = new Prefix(Word.epsilon(), u2rp, u2Path);
    	Prefix u1Ext = new Prefix(Word.fromSymbols(a3, a4), u1ExtRp, u1ExtPath);
    	Prefix u2Ext = new Prefix(Word.fromSymbols(a1), u2ExtRp, u2ExtPath);

    	SymbolicSuffix actual1 = builder.extendSuffix(u1, u1Ext, u2, u2Ext, u1, suffix1, u1ExtSdt, u2ExtSdt, true);
    	Assert.assertEquals(actual1.toString(), "((?a[int] ?a[int] ?a[int]))[Fresh(s1), (s2 == 1[int]), (s3 == s1)]");
    }

    @Test
    public void testExtendSuffixRegister() {
    	IntegerEqualityTheory iet = new IntegerEqualityTheory(TINT);
    	iet.setUseSuffixOpt(true);
    	Map<DataType, Theory> teachers = Map.of(TINT, iet);

    	SLLambdaRestrictionBuilder builder = new SLLambdaRestrictionBuilder(new Constants(), teachers);

    	SuffixValue s1 = new SuffixValue(TINT, 1);
    	SuffixValue s2 = new SuffixValue(TINT, 2);
    	SuffixValue s3 = new SuffixValue(TINT, 3);

    	DataValue d1 = new DataValue(TINT, BigDecimal.ONE);
    	DataValue d2 = new DataValue(TINT, BigDecimal.valueOf(2));
    	DataValue d3 = new DataValue(TINT, BigDecimal.valueOf(3));
    	DataValue d4 = new DataValue(TINT, BigDecimal.valueOf(4));
    	DataValue d5 = new DataValue(TINT, BigDecimal.valueOf(5));

    	PSymbolInstance a1 = new PSymbolInstance(A, d1);
    	PSymbolInstance a2 = new PSymbolInstance(A, d2);
    	PSymbolInstance a3 = new PSymbolInstance(A, d3);
    	PSymbolInstance a4 = new PSymbolInstance(A, d4);
    	PSymbolInstance a5 = new PSymbolInstance(A, d5);

    	SDT uExtSdt = new SDT(Map.of(
    			new SDTGuard.EqualityGuard(s1, d1), new SDT(Map.of(
    					new SDTGuard.SDTTrueGuard(s2), new SDT(Map.of(
    							new SDTGuard.EqualityGuard(s3, d3), SDTLeaf.ACCEPTING,
    							new SDTGuard.DisequalityGuard(s3, d3), SDTLeaf.REJECTING)))),
    			new SDTGuard.EqualityGuard(s1, d2), new SDT(Map.of(
    					new SDTGuard.EqualityGuard(s2, d1), new SDT(Map.of(
    							new SDTGuard.EqualityGuard(s3, d3), SDTLeaf.ACCEPTING,
    							new SDTGuard.DisequalityGuard(s3, d3), SDTLeaf.REJECTING)),
    					new SDTGuard.DisequalityGuard(s2, d1), new SDT(Map.of(
    							new SDTGuard.SDTTrueGuard(s3), SDTLeaf.REJECTING)))),
    			new SDTGuard.SDTAndGuard(s1,
    					List.of(
    							new SDTGuard.DisequalityGuard(s1, d1),
    							new SDTGuard.DisequalityGuard(s1, d2))),
    			new SDT(Map.of(
    					new SDTGuard.SDTTrueGuard(s2), new SDT(Map.of(
    							new SDTGuard.EqualityGuard(s3, d2), SDTLeaf.ACCEPTING,
    							new SDTGuard.DisequalityGuard(s3, d2), SDTLeaf.REJECTING))))));

    	SDT rpSdt = new SDT(Map.of(
    			new SDTGuard.EqualityGuard(s1, d1), SDTLeaf.ACCEPTING,
    			new SDTGuard.EqualityGuard(s1, d3), SDTLeaf.ACCEPTING,
    			new SDTGuard.SDTOrGuard(s1, Arrays.asList(
    					new SDTGuard.DisequalityGuard(s1, d1),
    					new SDTGuard.DisequalityGuard(s1, d3))), SDTLeaf.REJECTING));

    	SymbolicSuffix rpSuff = new SymbolicSuffix(Word.fromSymbols(A));
    	SymbolicSuffix suffix = new SymbolicSuffix(Word.fromSymbols(A, A, A),
    			Map.of(s1, new TrueRestriction(s1),
    					s2, new TrueRestriction(s2),
    					s3, new TrueRestriction(s3)));

    	Bijection<DataValue> uB = new Bijection<DataValue>();
    	Bijection<DataValue> rpB = new Bijection<DataValue>();
    	Bijection<DataValue> uExtB = new Bijection<DataValue>();
    	uB.put(d1, d1);
    	uB.put(d3, d3);
    	rpB.put(d1, d1);
    	rpB.put(d3, d3);
    	uExtB.put(d1, d1);
    	uExtB.put(d2, d2);
    	uExtB.put(d3, d3);

    	CTPath uPath = new CTPath(false);
    	CTPath rpPath = new CTPath(false);
    	CTPath uExtPath = new CTPath(false);
    	uPath.putSDT(rpSuff, rpSdt);
    	rpPath.putSDT(rpSuff, rpSdt);
    	uExtPath.putSDT(suffix, uExtSdt);

    	Prefix u = new Prefix(Word.fromSymbols(a1, a2, a3), uB, uPath);
    	Prefix rp = new Prefix(Word.fromSymbols(a1, a2, a3), rpB, rpPath);
    	Prefix uExt = new Prefix(Word.fromSymbols(a1, a2, a3, a3), uExtB, uExtPath);

    	SymbolicSuffix actual = builder.extendSuffix(u, uExt, rp, suffix, uExtSdt, Set.of(d2));

    	System.out.println(uExtSdt);
    	System.out.println(actual);
    }
}
