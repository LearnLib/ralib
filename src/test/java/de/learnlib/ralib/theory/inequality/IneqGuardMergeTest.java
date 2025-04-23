package de.learnlib.ralib.theory.inequality;



import de.learnlib.ralib.RaLibTestSuite;

public class IneqGuardMergeTest extends RaLibTestSuite {
/*
    @Test
    public void intervalMergeTest() {

        final DataType D_TYPE = new DataType("double", BigDecimal.class);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        dit.useSuffixOptimization(true);
        teachers.put(D_TYPE, dit);

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        SuffixValue s1 = svgen.next(D_TYPE);
        Register r1 = rgen.next(D_TYPE);
        Register r2 = rgen.next(D_TYPE);
        Register r3 = rgen.next(D_TYPE);

        DataValue dv0 = new DataValue(D_TYPE, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(D_TYPE, BigDecimal.ONE);
        DataValue dv2 = new DataValue(D_TYPE, BigDecimal.valueOf(2));
        DataValue dv3 = new DataValue(D_TYPE, BigDecimal.valueOf(3));
        DataValue dv4 = new DataValue(D_TYPE, BigDecimal.valueOf(4));
        DataValue dv5 = new DataValue(D_TYPE, BigDecimal.valueOf(5));
        DataValue dv6 = new DataValue(D_TYPE, BigDecimal.valueOf(6));

        SDTGuard g0 = new SDTGuard.IntervalGuard(s1, null, r1);
        SDTGuard g1 = new SDTGuard.EqualityGuard(s1, r1);
        SDTGuard g2 = new SDTGuard.IntervalGuard(s1, r1, r2);
        SDTGuard g3 = new SDTGuard.EqualityGuard(s1, r2);
        SDTGuard g4 = new SDTGuard.IntervalGuard(s1, r2, r3);
        SDTGuard g5 = new SDTGuard.EqualityGuard(s1, r3);
        SDTGuard g6 = new SDTGuard.IntervalGuard(s1, r3, null);

        Map<DataValue, SDTGuard> equivClasses = new LinkedHashMap<>();
        equivClasses.put(dv0, g0);
        equivClasses.put(dv1, g1);
        equivClasses.put(dv2, g2);
        equivClasses.put(dv3, g3);
        equivClasses.put(dv4, g4);
        equivClasses.put(dv5, g5);
        equivClasses.put(dv6, g6);

        // test 1
        Map<SDTGuard, SDT> sdts1 = new LinkedHashMap<>();
        sdts1.put(g0, SDTLeaf.ACCEPTING);
        sdts1.put(g1, SDTLeaf.ACCEPTING);
        sdts1.put(g2, SDTLeaf.REJECTING);
        sdts1.put(g3, SDTLeaf.REJECTING);
        sdts1.put(g4, SDTLeaf.REJECTING);
        sdts1.put(g5, SDTLeaf.ACCEPTING);
        sdts1.put(g6, SDTLeaf.ACCEPTING);

        Map<SDTGuard, SDT> expected1 = new LinkedHashMap<>();
        expected1.put(SDTGuard.IntervalGuard.lessOrEqualGuard(s1, r1), SDTLeaf.ACCEPTING);
        expected1.put(new SDTGuard.IntervalGuard(s1, r1, r3), SDTLeaf.REJECTING);
        expected1.put(SDTGuard.IntervalGuard.greaterOrEqualGuard(s1, r3), SDTLeaf.ACCEPTING);

        Map<SDTGuard, SDT> actual1 = dit.mergeGuards(sdts1, equivClasses, new ArrayList<DataValue<BigDecimal>>());

        Assert.assertEquals(actual1.size(), expected1.size());
        Assert.assertTrue(actual1.entrySet().containsAll(expected1.entrySet()));

        // test 2
        Map<SDTGuard, SDT> sdts2 = new LinkedHashMap<>();
        sdts2.put(g0, SDTLeaf.ACCEPTING);
        sdts2.put(g1, SDTLeaf.ACCEPTING);
        sdts2.put(g2, SDTLeaf.ACCEPTING);
        sdts2.put(g3, SDTLeaf.REJECTING);
        sdts2.put(g4, SDTLeaf.REJECTING);
        sdts2.put(g5, SDTLeaf.REJECTING);
        sdts2.put(g6, SDTLeaf.ACCEPTING);

        Map<SDTGuard, SDT> expected2 = new LinkedHashMap<>();
        expected2.put(IntervalGuard.lessGuard(s1, r2), SDTLeaf.ACCEPTING);
        expected2.put(new SDTGuard.IntervalGuard(s1, r2, r3, true, true), SDTLeaf.REJECTING);
        expected2.put(g6, SDTLeaf.ACCEPTING);

        Map<SDTGuard, SDT> actual2 = dit.mergeGuards(sdts2, equivClasses, new ArrayList<DataValue<BigDecimal>>());

        Assert.assertEquals(actual2.size(), expected2.size());
        Assert.assertTrue(actual2.entrySet().containsAll(expected2.entrySet()));

        // test 3
        Map<SDTGuard, SDT> sdts3 = new LinkedHashMap<>();
        sdts3.put(g0, SDTLeaf.ACCEPTING);
        sdts3.put(g1, SDTLeaf.REJECTING);
        sdts3.put(g2, SDTLeaf.ACCEPTING);
        sdts3.put(g3, SDTLeaf.ACCEPTING);
        sdts3.put(g4, SDTLeaf.REJECTING);
        sdts3.put(g5, SDTLeaf.REJECTING);
        sdts3.put(g6, SDTLeaf.REJECTING);

        Map<SDTGuard, SDT> expected3 = new LinkedHashMap<>();
        expected3.put(g0, SDTLeaf.ACCEPTING);
        expected3.put(g1, SDTLeaf.REJECTING);
        expected3.put(new IntervalGuard(s1, r1, r2, false, true), SDTLeaf.ACCEPTING);
        expected3.put(IntervalGuard.greaterGuard(s1, r2), SDTLeaf.REJECTING);

        Map<SDTGuard, SDT> actual3 = dit.mergeGuards(sdts3, equivClasses, new ArrayList<DataValue<BigDecimal>>());

        Assert.assertEquals(actual3.size(), expected3.size());
        Assert.assertTrue(actual3.entrySet().containsAll(expected3.entrySet()));

        // test 4
        Map<SDTGuard, SDT> sdts4 = new LinkedHashMap<>();
        sdts4.put(g0, SDTLeaf.ACCEPTING);
        sdts4.put(g1, SDTLeaf.REJECTING);
        sdts4.put(g2, SDTLeaf.REJECTING);
        sdts4.put(g3, SDTLeaf.ACCEPTING);
        sdts4.put(g4, SDTLeaf.REJECTING);
        sdts4.put(g5, SDTLeaf.ACCEPTING);
        sdts4.put(g6, SDTLeaf.REJECTING);

        Map<SDTGuard, SDT> expected4 = new LinkedHashMap<>();
        expected4.put(g0, SDTLeaf.ACCEPTING);
        expected4.put(new IntervalGuard(s1, r1, r2, true, false), SDTLeaf.REJECTING);
        expected4.put(g3, SDTLeaf.ACCEPTING);
        expected4.put(g4, SDTLeaf.REJECTING);
        expected4.put(g5, SDTLeaf.ACCEPTING);
        expected4.put(g6, SDTLeaf.REJECTING);

        Map<SDTGuard, SDT> actual4 = dit.mergeGuards(sdts4, equivClasses, new ArrayList<DataValue<BigDecimal>>());

        Assert.assertEquals(actual4.size(), expected4.size());
        Assert.assertTrue(actual4.entrySet().containsAll(expected4.entrySet()));
    }

    @Test
    public void trueGuardTest() {

        final DataType D_TYPE = new DataType("double", BigDecimal.class);

        final Map<DataType, Theory<BigDecimal>> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        dit.useSuffixOptimization(true);
        teachers.put(D_TYPE, dit);

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        SuffixValue s1 = svgen.next(D_TYPE);
        Register r1 = rgen.next(D_TYPE);
        Register r2 = rgen.next(D_TYPE);

        DataValue<BigDecimal> dv0 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ZERO);
        DataValue<BigDecimal> dv1 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ONE);
        DataValue<BigDecimal> dv2 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(2));
        DataValue<BigDecimal> dv3 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(3));
        DataValue<BigDecimal> dv4 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(4));

        SDTGuard g0 = new IntervalGuard(s1, null, r1);
        SDTGuard g1 = new EqualityGuard(s1, r1);
        SDTGuard g2 = new IntervalGuard(s1, r1, r2);
        SDTGuard g3 = new EqualityGuard(s1, r2);
        SDTGuard g4 = new IntervalGuard(s1, r2, null);

        Map<DataValue<BigDecimal>, SDTGuard> equivClasses = new LinkedHashMap<>();
        equivClasses.put(dv0, g0);
        equivClasses.put(dv1, g1);
        equivClasses.put(dv2, g2);
        equivClasses.put(dv3, g3);
        equivClasses.put(dv4, g4);

        Map<SDTGuard, SDT> sdts1 = new LinkedHashMap<>();
        sdts1.put(g0, SDTLeaf.ACCEPTING);
        sdts1.put(g1, SDTLeaf.ACCEPTING);
        sdts1.put(g2, SDTLeaf.ACCEPTING);
        sdts1.put(g3, SDTLeaf.ACCEPTING);
        sdts1.put(g4, SDTLeaf.ACCEPTING);

        Map<SDTGuard, SDT> merged = dit.mergeGuards(sdts1, equivClasses, new ArrayList<>());

        Assert.assertEquals(merged.size(), 1);
        Assert.assertTrue(merged.containsKey(new SDTTrueGuard(s1)));
    }

    @Test
    public void filteredDataValuesTest() {

        final DataType D_TYPE = new DataType("double", BigDecimal.class);

        final Map<DataType, Theory<BigDecimal>> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        dit.useSuffixOptimization(true);
        teachers.put(D_TYPE, dit);

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        SuffixValue s1 = svgen.next(D_TYPE);
        Register r1 = rgen.next(D_TYPE);
        Register r2 = rgen.next(D_TYPE);
        Register r3 = rgen.next(D_TYPE);

        DataValue dv0 = new DataValue(D_TYPE, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(D_TYPE, BigDecimal.ONE);
        DataValue dv2 = new DataValue(D_TYPE, BigDecimal.valueOf(2));
        DataValue dv3 = new DataValue(D_TYPE, BigDecimal.valueOf(3));
        DataValue dv4 = new DataValue(D_TYPE, BigDecimal.valueOf(4));
        DataValue dv5 = new DataValue(D_TYPE, BigDecimal.valueOf(5));
        DataValue dv6 = new DataValue(D_TYPE, BigDecimal.valueOf(6));

        SDTGuard g0 = new IntervalGuard(s1, null, r1);
        SDTGuard g1 = new EqualityGuard(s1, r1);
        SDTGuard g2 = new IntervalGuard(s1, r1, r2);
        SDTGuard g3 = new EqualityGuard(s1, r2);
        SDTGuard g4 = new IntervalGuard(s1, r2, r3);
        SDTGuard g5 = new EqualityGuard(s1, r3);
        SDTGuard g6 = new IntervalGuard(s1, r3, null);

        Map<DataValue, SDTGuard> equivClasses = new LinkedHashMap<>();
        equivClasses.put(dv0, g0);
        equivClasses.put(dv1, g1);
        equivClasses.put(dv2, g2);
        equivClasses.put(dv3, g3);
        equivClasses.put(dv4, g4);
        equivClasses.put(dv5, g5);
        equivClasses.put(dv6, g6);

        Map<SDTGuard, SDT> sdts = new LinkedHashMap<>();
        sdts.put(g1, SDTLeaf.ACCEPTING);
        sdts.put(g2, SDTLeaf.ACCEPTING);
        sdts.put(g5, SDTLeaf.REJECTING);

        List<DataValue<BigDecimal>> filtered = new ArrayList<>();
        filtered.add(dv0);
        filtered.add(dv3);
        filtered.add(dv4);
        filtered.add(dv6);

        Map<SDTGuard, SDT> expected = new LinkedHashMap<>();
        expected.put(new IntervalGuard(s1, r1, r2, true, false), SDTLeaf.ACCEPTING);
        expected.put(g5, SDTLeaf.REJECTING);

        Map<SDTGuard, SDT> actual = dit.mergeGuards(sdts, equivClasses, filtered);

        Assert.assertEquals(actual.size(), expected.size());
        Assert.assertTrue(actual.entrySet().containsAll(expected.entrySet()));
    }

    @Test
    public void sdtSubtreeTest() {

        final DataType D_TYPE = new DataType("double", BigDecimal.class);

        final Map<DataType, Theory<BigDecimal>> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        dit.useSuffixOptimization(true);
        teachers.put(D_TYPE, dit);

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        SuffixValue s1 = svgen.next(D_TYPE);
        Register r1 = rgen.next(D_TYPE);
        Register r2 = rgen.next(D_TYPE);

        DataValue<BigDecimal> dv0 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ZERO);
        DataValue<BigDecimal> dv1 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ONE);
        DataValue<BigDecimal> dv2 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(2));
        DataValue<BigDecimal> dv3 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(3));
        DataValue<BigDecimal> dv4 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(4));

        SDTGuard g0 = new IntervalGuard(s1, null, r1);
        SDTGuard g1 = new EqualityGuard(s1, r1);
        SDTGuard g2 = new IntervalGuard(s1, r1, r2);
        SDTGuard g3 = new EqualityGuard(s1, r2);
        SDTGuard g4 = new IntervalGuard(s1, r2, null);

        Map<DataValue<BigDecimal>, SDTGuard> equivClasses = new LinkedHashMap<>();
        equivClasses.put(dv0, g0);
        equivClasses.put(dv1, g1);
        equivClasses.put(dv2, g2);
        equivClasses.put(dv3, g3);
        equivClasses.put(dv4, g4);

        SDT sdt1 = new SDT(Map.of(
        		new EqualityGuard(s1, r1), SDTLeaf.ACCEPTING,
        		new DisequalityGuard(s1, r1), SDTLeaf.REJECTING));
        SDT sdt2 = new SDT(Map.of(
        		new IntervalGuard(s1, r1, r2), SDTLeaf.ACCEPTING));
        SDT sdt3 = new SDT(Map.of(
        		new IntervalGuard(s1, r2, r1), SDTLeaf.ACCEPTING));

        Map<SDTGuard, SDT> sdts = new LinkedHashMap<>();
        sdts.put(g0, sdt1);
        sdts.put(g1, sdt1);
        sdts.put(g2, sdt2);
        sdts.put(g3, sdt2);
        sdts.put(g4, sdt3);

        Map<SDTGuard, SDT> expected = new LinkedHashMap<>();
        expected.put(IntervalGuard.lessOrEqualGuard(s1, r1), sdt1);
        expected.put(new IntervalGuard(s1, r1, r2, false, true), sdt2);
        expected.put(g4, sdt3);

        Map<SDTGuard, SDT> actual = dit.mergeGuards(sdts, equivClasses, new ArrayList<>());

        Assert.assertEquals(actual.size(), expected.size());
        Assert.assertTrue(actual.entrySet().containsAll(expected.entrySet()));
    }

    @Test
    public void disequalityGuardTest() {

        final DataType D_TYPE = new DataType("double", BigDecimal.class);

        final Map<DataType, Theory<BigDecimal>> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        dit.useSuffixOptimization(true);
        teachers.put(D_TYPE, dit);

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        SuffixValue s1 = svgen.next(D_TYPE);
        SuffixValue s2 = svgen.next(D_TYPE);
        Register r1 = rgen.next(D_TYPE);
        Register r2 = rgen.next(D_TYPE);

        DataValue<BigDecimal> dv0 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ZERO);
        DataValue<BigDecimal> dv1 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ONE);
        DataValue<BigDecimal> dv2 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(2));
        DataValue<BigDecimal> dv3 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(3));
        DataValue<BigDecimal> dv4 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(4));

        SDTGuard g0 = new IntervalGuard(s1, null, r1);
        SDTGuard g1 = new EqualityGuard(s1, r1);
        SDTGuard g2 = new IntervalGuard(s1, r1, r2);
        SDTGuard g3 = new EqualityGuard(s1, r2);
        SDTGuard g4 = new IntervalGuard(s1, r2, null);

        Map<DataValue<BigDecimal>, SDTGuard> equivClasses = new LinkedHashMap<>();
        equivClasses.put(dv0, g0);
        equivClasses.put(dv1, g1);
        equivClasses.put(dv2, g2);
        equivClasses.put(dv3, g3);
        equivClasses.put(dv4, g4);

        Map<SDTGuard, SDT> sdts1 = new LinkedHashMap<>();
        sdts1.put(g0, SDTLeaf.REJECTING);
        sdts1.put(g1, SDTLeaf.ACCEPTING);
        sdts1.put(g2, SDTLeaf.REJECTING);
        sdts1.put(g3, SDTLeaf.REJECTING);
        sdts1.put(g4, SDTLeaf.REJECTING);

        List<DataValue<BigDecimal>> filteredOut = new ArrayList<>();
        filteredOut.add(dv1);

        Map<SDTGuard, SDT> expected1 = new LinkedHashMap<>();
        expected1.put(new SDTTrueGuard(s1), SDTLeaf.REJECTING);
        Map<SDTGuard, SDT> actual1 = dit.mergeGuards(sdts1, equivClasses, filteredOut);

        Assert.assertEquals(actual1.size(), expected1.size());
        Assert.assertTrue(actual1.entrySet().containsAll(expected1.entrySet()));

        // test 2
        Map<SDTGuard, SDT> sdts2 = sdts1;

        Map<SDTGuard, SDT> expected2 = new LinkedHashMap<>();
        expected2.put(new DisequalityGuard(s1, r1), SDTLeaf.REJECTING);
        expected2.put(g1, SDTLeaf.ACCEPTING);
        Map<SDTGuard, SDT> actual2 = dit.mergeGuards(sdts2, equivClasses, new ArrayList<>());

        Assert.assertEquals(actual2.size(), expected2.size());
        Assert.assertTrue(actual2.entrySet().containsAll(expected2.entrySet()));

        // test 3
        SDT sdt1 = new SDT(Map.of(
        		new EqualityGuard(s2, r1), SDTLeaf.ACCEPTING,
        		new DisequalityGuard(s2, r1), SDTLeaf.REJECTING));
        SDT sdt2 = new SDT(Map.of(
        		new IntervalGuard(s2, r1, null), SDTLeaf.REJECTING,
        		new IntervalGuard(s2, r1, r2), SDTLeaf.ACCEPTING,
        		new IntervalGuard(s2, null, r2), SDTLeaf.REJECTING));
        SDT sdt3 = new SDT(Map.of(
        		new EqualityGuard(s2, r1), SDTLeaf.REJECTING,
        		new DisequalityGuard(s2, r2), SDTLeaf.ACCEPTING));

        Map<SDTGuard, SDT> sdts3 = new LinkedHashMap<>();
        sdts3.put(g0, sdt1);
        sdts3.put(g1, sdt1);
        sdts3.put(g2, sdt1);
        sdts3.put(g3, sdt2);
        sdts3.put(g4, sdt3);

        Map<SDTGuard, SDT> expected3 = new LinkedHashMap<>();
        expected3.put(new IntervalGuard(s1, null, r2), sdt1);
        expected3.put(g3, sdt2);
        expected3.put(g4, sdt3);
        Map<SDTGuard, SDT> actual3 = dit.mergeGuards(sdts3, equivClasses, new ArrayList<>());

        Assert.assertEquals(actual3.size(), expected3.size());
        Assert.assertTrue(actual3.entrySet().containsAll(expected3.entrySet()));
    }

 */
}
