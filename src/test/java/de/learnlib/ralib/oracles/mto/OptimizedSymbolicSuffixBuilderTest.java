package de.learnlib.ralib.oracles.mto;

import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.CONTAINS;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.INSERT;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.INT_TYPE;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.POP;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.PUSH;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.dv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ConstantGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.example.list.BoundedList;
import de.learnlib.ralib.example.list.BoundedListDataWordOracle;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.ConstraintSolverFactory;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class OptimizedSymbolicSuffixBuilderTest {

    @Test
    public void extendDistinguishingSuffixTest() {
        BoundedListDataWordOracle dwOracle = new BoundedListDataWordOracle(() -> new BoundedList(2, false));

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(INT_TYPE);
        teachers.put(INT_TYPE, dit);
        ConstraintSolver solver = ConstraintSolverFactory.createZ3ConstraintSolver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);

        Constants consts = new Constants();

        SymbolicSuffixRestrictionBuilder restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);

        Word<PSymbolInstance> word = Word.fromSymbols(
                new PSymbolInstance(PUSH, dv(1)),
                new PSymbolInstance(INSERT, dv(1),dv(2)),
                new PSymbolInstance(POP, dv(2)),
                new PSymbolInstance(CONTAINS, dv(1)));

        equalsSuffixesFromConcretePrefixSuffix(word, mto, consts, solver);


        InputSymbol A = new InputSymbol("a", INT_TYPE, INT_TYPE);
        InputSymbol B = new InputSymbol("b");
        InputSymbol C = new InputSymbol("c", INT_TYPE);

        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts, restrictionBuilder);

        SuffixValueGenerator svGen = new SuffixValueGenerator();
        SuffixValue s1 = svGen.next(INT_TYPE);
        SuffixValue s2 = svGen.next(INT_TYPE);
        SuffixValue s3 = svGen.next(INT_TYPE);
        SuffixValue s4 = svGen.next(INT_TYPE);

        RegisterGenerator rGen = new RegisterGenerator();
        Register r1 = rGen.next(INT_TYPE);
        Register r2 = rGen.next(INT_TYPE);

        ParameterGenerator pGen = new ParameterGenerator();
        Parameter p1 = pGen.next(INT_TYPE);
        Parameter p2 = pGen.next(INT_TYPE);
        Parameter p3 = pGen.next(INT_TYPE);
        Parameter p4 = pGen.next(INT_TYPE);

        ConstantGenerator cGen = new ConstantGenerator();
        SymbolicDataValue.Constant c1 = cGen.next(INT_TYPE);

        PIV piv1 = new PIV();
        piv1.put(p1, r1);
        piv1.put(p3, r2);
        PIV piv2 = new PIV();
        piv2.put(p3, r1);
        piv2.put(p4, r2);
        PIV piv3 = new PIV();
        piv3.put(p2, r1);
        PIV piv5 = new PIV();
        piv5.put(p1, r1);

        Constants consts2 = new Constants();
        consts2.put(c1, new DataValue(INT_TYPE, 2));
        SymbolicSuffixRestrictionBuilder restrictionBuilder2 = new SymbolicSuffixRestrictionBuilder(consts2, teachers);

        Word<PSymbolInstance> word1 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 1), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 1)));
        Word<PSymbolInstance> word2 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 2), new DataValue(INT_TYPE, 3)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 3), new DataValue(INT_TYPE, 4)));
        Word<PSymbolInstance> word3 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 1), new DataValue(INT_TYPE, 2)));
        Word<PSymbolInstance> word4 = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 2)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 2)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)));
        Word<PSymbolInstance> word5 = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)));
        Word<PSymbolInstance> word6 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 2), new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 2)));
        Word<PSymbolInstance> word7a = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(B));
        Word<PSymbolInstance> word7b = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 2)),
        		new PSymbolInstance(B));
        Word<PSymbolInstance> word8a = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 1)));
        Word<PSymbolInstance> word8b = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 1)));
        Word<PSymbolInstance> word8c = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, 0)));
        SymbolicSuffix suffix1 = new SymbolicSuffix(word1.prefix(2), word1.suffix(1), restrictionBuilder);
        SymbolicSuffix suffix2 = new SymbolicSuffix(word2.prefix(2), word2.suffix(1), restrictionBuilder);
        SymbolicSuffix suffix3 = new SymbolicSuffix(word3.prefix(2), word3.suffix(1), restrictionBuilder2);
        SymbolicSuffix suffix4 = new SymbolicSuffix(word4.prefix(2), word4.suffix(4), restrictionBuilder);
        SymbolicSuffix suffix5 = new SymbolicSuffix(word5.prefix(2), word5.suffix(4), restrictionBuilder);
        SymbolicSuffix suffix6 = new SymbolicSuffix(word6.prefix(1), word6.suffix(1), restrictionBuilder);
        SymbolicSuffix suffix7 = new SymbolicSuffix(word7a.prefix(3), word7a.suffix(1), restrictionBuilder);
        SymbolicSuffix suffix8 = new SymbolicSuffix(word8a.prefix(2), word8a.suffix(1), restrictionBuilder);

        SDT sdt1 = new SDT(Map.of(
        		new EqualityGuard(s1, r1), new SDT(Map.of(
        				new EqualityGuard(s2, r2), SDTLeaf.ACCEPTING,
        				new DisequalityGuard(s2, r2), SDTLeaf.REJECTING)),
        		new DisequalityGuard(s1, r1), new SDT(Map.of(
        				new SDTTrueGuard(s2), SDTLeaf.REJECTING))));
        SDT sdt2 = new SDT(Map.of(
        		new SDTOrGuard(s1, new EqualityGuard(s1, r1), new EqualityGuard(s1, r2)), new SDT(Map.of(
        				new SDTTrueGuard(s2), SDTLeaf.ACCEPTING)),
        		new SDTAndGuard(s1, new DisequalityGuard(s1, r1), new DisequalityGuard(s1, r2)), new SDT(Map.of(
        				new SDTTrueGuard(s2), SDTLeaf.REJECTING))));
        SDT sdt3 = new SDT(Map.of(
        		new EqualityGuard(s1, r1), new SDT(Map.of(
        				new EqualityGuard(s2, c1), SDTLeaf.ACCEPTING,
        				new DisequalityGuard(s2, c1), SDTLeaf.REJECTING)),
        		new DisequalityGuard(s1, r1), new SDT(Map.of(
        				new SDTTrueGuard(s2), SDTLeaf.REJECTING))));
        SDT sdt4 = new SDT(Map.of(
        		new SDTTrueGuard(s1), new SDT(Map.of(
        				new EqualityGuard(s2, s1), new SDT(Map.of(
        						new EqualityGuard(s3, r1), new SDT(Map.of(
        								new EqualityGuard(s4, r1), SDTLeaf.ACCEPTING,
        								new DisequalityGuard(s4, r1), SDTLeaf.REJECTING)),
        						new DisequalityGuard(s3, r1), new SDT(Map.of(
        								new SDTTrueGuard(s4), SDTLeaf.REJECTING)))),
        				new DisequalityGuard(s2, s1), new SDT(Map.of(
        						new SDTTrueGuard(s3), new SDT(Map.of(
        								new SDTTrueGuard(s4), SDTLeaf.REJECTING))))))));
        SDT sdt5 = new SDT(Map.of(
        		new SDTTrueGuard(s1), new SDT(Map.of(
        				new EqualityGuard(s2, s1), new SDT(Map.of(
        						new EqualityGuard(s3, r1), new SDT(Map.of(
        								new SDTTrueGuard(s4), SDTLeaf.REJECTING)),
        						new DisequalityGuard(s3, r1), new SDT(Map.of(
        								new EqualityGuard(s4, s3), SDTLeaf.ACCEPTING,
        								new DisequalityGuard(s4, s3), SDTLeaf.REJECTING))))))));
        SDT sdt6 = new SDT(Map.of(
        		new EqualityGuard(s1, r1), SDTLeaf.ACCEPTING,
        		new DisequalityGuard(s1, r1), SDTLeaf.REJECTING));
        SDT sdt7 = new SDT(Map.of(
        		new SDTTrueGuard(s1), SDTLeaf.REJECTING));

        SymbolicSuffix expected1 = new SymbolicSuffix(word1.prefix(1), word1.suffix(2), restrictionBuilder);
        SymbolicSuffix actual1 = builder.extendSuffix(word1.prefix(2), sdt1, piv1, suffix1);
        Assert.assertEquals(actual1, expected1);

        SymbolicSuffix actual2 = builder.extendSuffix(word2.prefix(2), sdt2, piv2, suffix2);
        Assert.assertEquals(actual2.toString(), "[s3]((a[s1, s2] a[s3, s4]))");

        SymbolicSuffix expected3 = new SymbolicSuffix(word3.prefix(1), word3.suffix(2), restrictionBuilder2);
        SymbolicSuffix actual3 = builder.extendSuffix(word3.prefix(2), sdt3, piv3, suffix3);
        Assert.assertEquals(actual3, expected3);

        SymbolicSuffix expected4 = new SymbolicSuffix(word4.prefix(1), word4.suffix(5), restrictionBuilder);
        SymbolicSuffix actual4 = builder.extendSuffix(word4.prefix(2), sdt4, piv5, suffix4);
        Assert.assertEquals(actual4, expected4);

        SymbolicSuffix expected5 = new SymbolicSuffix(word5.prefix(1), word5.suffix(5), restrictionBuilder);
        SymbolicSuffix actual5 = builder.extendSuffix(word5.prefix(2), sdt5, piv5, suffix5);
        Assert.assertEquals(actual5, expected5);

        SymbolicSuffix expected6 = new SymbolicSuffix(Word.epsilon(), word6, restrictionBuilder);
        SymbolicSuffix actual6 = builder.extendSuffix(word6.prefix(1), sdt6, piv5, suffix6);
        Assert.assertEquals(actual6, expected6);

        OptimizedSymbolicSuffixBuilder constBuilder = new OptimizedSymbolicSuffixBuilder(consts2, restrictionBuilder2);
        SymbolicSuffix expected7 = new SymbolicSuffix(word7b.prefix(2), word7b.suffix(2), restrictionBuilder2);
        SymbolicSuffix actual7 = constBuilder.extendDistinguishingSuffix(word7a.prefix(3), SDTLeaf.ACCEPTING, new PIV(), word7b.prefix(3), SDTLeaf.REJECTING, new PIV(), suffix7);
        Assert.assertEquals(actual7, expected7);

        SymbolicSuffix expected8 = new SymbolicSuffix(word8c.prefix(1), word8c.suffix(2), restrictionBuilder);
        SymbolicSuffix actual8 = builder.extendDistinguishingSuffix(word8a.prefix(2), sdt6, piv3, word8b.prefix(2), sdt7, new PIV(), suffix8);
        Assert.assertEquals(actual8, expected8);
    }

    /*
     * Checks for equality optimized suffixes built by prepending using an SDT against those built from concrete prefix/suffix
     */
    private void equalsSuffixesFromConcretePrefixSuffix(Word<PSymbolInstance> word, MultiTheoryTreeOracle mto, Constants consts, ConstraintSolver solver) {
        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts);
        TreeQueryResult tqr = mto.treeQuery(word, new SymbolicSuffix(Word.epsilon()));
        SymbolicSuffix actual = new SymbolicSuffix(word, Word.epsilon());
        int suffixLength;
        for (suffixLength=1; suffixLength<word.length(); suffixLength++) {
            Word<PSymbolInstance> suffix = word.suffix(suffixLength);
            Word<PSymbolInstance> sub = word.prefix(word.length() - suffixLength);
            Word<PSymbolInstance> prefix = word.prefix(sub.length()+1);
            SymbolicSuffix expected = new SymbolicSuffix(sub, suffix);
            actual = builder.extendSuffix(prefix, (SDT) tqr.getSdt(), (PIV) tqr.getPiv(), actual);
            Assert.assertEquals(actual, expected);
            tqr = mto.treeQuery(sub, expected);
        }
    }

    @Test
    public void extendSuffixTest() {

        DataType type = new DataType("int",Integer.class);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(type);
        teachers.put(type, dit);

        InputSymbol A = new InputSymbol("a", type, type);
        InputSymbol B = new InputSymbol("b", type);
        InputSymbol C = new InputSymbol("c");
        SuffixValueGenerator sgen = new SymbolicDataValueGenerator.SuffixValueGenerator();
        SuffixValue s1 = sgen.next(type);
        SuffixValue s2 = sgen.next(type);
        SuffixValue s3 = sgen.next(type);
        SuffixValue s4 = sgen.next(type);
        SuffixValue s5 = sgen.next(type);

        RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
        Register r1 = rgen.next(type);
        Register r2 = rgen.next(type);
        Register r3 = rgen.next(type);

        ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
        Parameter p1 = pgen.next(type);
        Parameter p2 = pgen.next(type);
        Parameter p3 = pgen.next(type);
        Parameter p4 = pgen.next(type);

        ConstantGenerator cGen = new ConstantGenerator();
        SymbolicDataValue.Constant c1 = cGen.next(INT_TYPE);
        SymbolicDataValue.Constant c2 = cGen.next(INT_TYPE);

        PIV piv1 = new PIV();
        piv1.put(p1, r1);
        piv1.put(p3, r2);
        piv1.put(p4, r3);
        PIV piv2 = new PIV();

        Constants consts1 = new Constants();
        Constants consts2 = new Constants();
        consts2.put(c1, new DataValue(INT_TYPE, 3));
        consts2.put(c2, new DataValue(INT_TYPE, 4));

        SymbolicSuffixRestrictionBuilder restrictionBuilder1 = new SymbolicSuffixRestrictionBuilder(consts1, teachers);
        SymbolicSuffixRestrictionBuilder restrictionBuilder2 = new SymbolicSuffixRestrictionBuilder(consts2, teachers);

        Word<PSymbolInstance> word1 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 1), new DataValue(INT_TYPE, 2)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 2), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(B, new DataValue(INT_TYPE, 2)));
        Word<PSymbolInstance> word2 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 1), new DataValue(INT_TYPE, 2)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 1), new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 3), new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(B, new DataValue(INT_TYPE, 4)));
        Word<PSymbolInstance> word3 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 1), new DataValue(INT_TYPE, 2)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 3), new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(B, new DataValue(INT_TYPE, 5)));
        Word<PSymbolInstance> word4 = Word.fromSymbols(
        		new PSymbolInstance(B, new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(B, new DataValue(INT_TYPE, 3)),
        		new PSymbolInstance(C));
        SymbolicSuffix suffix1 = new SymbolicSuffix(word1.prefix(2), word1.suffix(3), restrictionBuilder1);
        SymbolicSuffix suffix2 = new SymbolicSuffix(word2.prefix(2), word2.suffix(3), restrictionBuilder1);
        SymbolicSuffix suffix3 = new SymbolicSuffix(word3.prefix(2), word3.suffix(2), restrictionBuilder2);
        SymbolicSuffix suffix4 = new SymbolicSuffix(word4.prefix(2), word4.suffix(1), restrictionBuilder2);

        List<SDTGuard> sdtPath1 = new ArrayList<>();
        sdtPath1.add(new EqualityGuard(s1, r1));
        sdtPath1.add(new EqualityGuard(s2, r2));
        sdtPath1.add(new EqualityGuard(s3, r3));
        sdtPath1.add(new EqualityGuard(s4, s1));
        sdtPath1.add(new EqualityGuard(s5, s3));
        List<SDTGuard> sdtPath2 = new ArrayList<>();
        sdtPath2.add(new DisequalityGuard(s1, r1));
        sdtPath2.add(new DisequalityGuard(s2, r2));
        sdtPath2.add(new DisequalityGuard(s3, r3));
        sdtPath2.add(new DisequalityGuard(s4, s1));
        sdtPath2.add(new DisequalityGuard(s5, s3));
        List<SDTGuard> sdtPath3 = new ArrayList<>();
        sdtPath3.add(new EqualityGuard(s1, c1));
        sdtPath3.add(new DisequalityGuard(s2, c2));
        sdtPath3.add(new SDTTrueGuard(s3));
        List<List<SDTGuard>> sdtPaths4 = SDTLeaf.ACCEPTING.getPaths(true);
        List<SDTGuard> sdtPath4 = SDTLeaf.ACCEPTING.getPaths(true).get(0);

        OptimizedSymbolicSuffixBuilder builder1 = new OptimizedSymbolicSuffixBuilder(consts1, restrictionBuilder1);
        OptimizedSymbolicSuffixBuilder builder2 = new OptimizedSymbolicSuffixBuilder(consts2, restrictionBuilder2);
        ConstraintSolver solver = new SimpleConstraintSolver();

        SymbolicSuffix expected1 = new SymbolicSuffix(word1.prefix(1), word1.suffix(4), restrictionBuilder1);
        SymbolicSuffix actual1 = builder1.extendSuffix(word1.prefix(2), sdtPath1, piv1, suffix1.getActions());
        Assert.assertEquals(actual1, expected1);

        // this test does not seem to be correct, it needs to be examined more closely
//        SymbolicSuffix expected2 = new SymbolicSuffix(word2.prefix(1), word2.suffix(4), restrictionBuilder1);
//        SymbolicSuffix actual2 = builder1.extendSuffix(word2.prefix(2), sdtPath2, piv1, suffix2.getActions());
//        Assert.assertEquals(actual2, expected2);

        SymbolicSuffix expected3 = new SymbolicSuffix(word3.prefix(1), word3.suffix(3), restrictionBuilder2);
        SymbolicSuffix actual3 = builder1.extendSuffix(word3.prefix(2), sdtPath3, piv2, suffix3.getActions());
        Assert.assertEquals(actual1, expected1);

        SymbolicSuffix actual4 = builder2.extendSuffix(word4.prefix(2), sdtPath4, new PIV(), suffix4.getActions());
        Assert.assertEquals(actual4.getFreeValues().size(), 1);
    }

    @Test
    public void buildOptimizedSuffixTest() {

        DataType type = new DataType("int",Integer.class);
        InputSymbol a = new InputSymbol("a", type);


        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(type);
        teachers.put(type, dit);

        SuffixValueGenerator sgen = new SymbolicDataValueGenerator.SuffixValueGenerator();
        SuffixValue s1 = sgen.next(type);
        SuffixValue s2 = sgen.next(type);
        SuffixValue s3 = sgen.next(type);

        RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
        Register r1 = rgen.next(type);
        Register r2 = rgen.next(type);

        ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
        Parameter p1 = pgen.next(type);
        Parameter p2 = pgen.next(type);

        Constants consts = new Constants();

        Word<PSymbolInstance> prefix1 = Word.fromSymbols(new PSymbolInstance(a, new DataValue<Integer>(type, 0)));
        Word<PSymbolInstance> prefix2 = Word.fromSymbols(new PSymbolInstance(a, new DataValue<Integer>(type, 0)),
                new PSymbolInstance(a, new DataValue<Integer>(type, 1)));

        SDT sdt1 = new SDT(Map.of(
                new EqualityGuard(s1, r1), new SDT(Map.of(
                        new SDTTrueGuard(s2), new SDT(Map.of(
                                new EqualityGuard(s3, s2), SDTLeaf.ACCEPTING,
                                new DisequalityGuard(s3, s2), SDTLeaf.REJECTING
                                )))),
                new DisequalityGuard(s1, r1), new SDT(Map.of(
                        new SDTTrueGuard(s2), new SDT(Map.of(
                                new SDTTrueGuard(s3), SDTLeaf.ACCEPTING))))));
        SDT sdt2 = new SDT(Map.of(
                new EqualityGuard(s1, r1), new SDT(Map.of(
                        new SDTTrueGuard(s2), new SDT(Map.of(
                                new SDTTrueGuard(s3), SDTLeaf.ACCEPTING)))),
                new DisequalityGuard(s1, r1), new SDT(Map.of(
                        new SDTTrueGuard(s2), new SDT(Map.of(
                                new SDTTrueGuard(s3), SDTLeaf.ACCEPTING))))));

        PIV piv1 = new PIV();
        piv1.put(p1, r1);
        PIV piv2 = new PIV();
        piv2.put(p2, r1);

        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts);
        SymbolicSuffix suffix12 = builder.distinguishingSuffixFromSDTs(prefix1, sdt1, piv1, prefix2, sdt2, piv2, Word.fromSymbols(a, a, a), new SimpleConstraintSolver());
        Assert.assertEquals(suffix12.toString(), "[]((a[s1] a[s1] a[s2] a[s3]))");

        Word<PSymbolInstance> prefix3 = prefix1;
        Word<PSymbolInstance> prefix4 = prefix2;
        SDT sdt3 = new SDT(Map.of(
                new EqualityGuard(s1, r1), new SDT(Map.of(
                        new SDTTrueGuard(s2), new SDT(Map.of(
                                new EqualityGuard(s3, s2), SDTLeaf.ACCEPTING,
                                new DisequalityGuard(s3, s2), SDTLeaf.REJECTING)))),
                new DisequalityGuard(s1, r1), new SDT(Map.of(
                        new SDTTrueGuard(s2), new SDT(Map.of(
                                new SDTTrueGuard(s3), SDTLeaf.REJECTING))))));


        SDT sdt4 = new SDT(Map.of(
                new EqualityGuard(s1, r1), new SDT(Map.of(
                        new EqualityGuard(s2, r2), new SDT(Map.of(
                                new EqualityGuard(s3, s2), SDTLeaf.ACCEPTING)),
                        new DisequalityGuard(s2, r2), new SDT(Map.of(
                                new EqualityGuard(s3, s2), SDTLeaf.REJECTING)))),
                new DisequalityGuard(s1, r1), new SDT(Map.of(
                        new SDTTrueGuard(s2), new SDT(Map.of(
                                new SDTTrueGuard(s3), SDTLeaf.REJECTING))))));

        PIV piv3 = new PIV();
        piv3.put(p1, r1);
        PIV piv4 = new PIV();
        piv4.put(p1, r1);
        piv4.put(p2, r2);

        SymbolicSuffix suffix34 = builder.distinguishingSuffixFromSDTs(prefix3, sdt3, piv3, prefix4, sdt4, piv4,  Word.fromSymbols(a, a, a), new SimpleConstraintSolver());
        Assert.assertEquals(suffix34.toString(), "[s2]((a[s1] a[s2] a[s3] a[s3]))");
    }

    @Test
    public void extendSuffixRevealingRegistersTest() {
        DataType type = new DataType("int",Integer.class);
        InputSymbol a = new InputSymbol("a", type);
        InputSymbol b = new InputSymbol("b", type, type);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(type);
        teachers.put(type, dit);

        SuffixValueGenerator sgen = new SymbolicDataValueGenerator.SuffixValueGenerator();
        SuffixValue s1 = sgen.next(type);
        SuffixValue s2 = sgen.next(type);

        RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
        Register r1 = rgen.next(type);
        Register r2 = rgen.next(type);
        Register r3 = rgen.next(type);

        ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
        Parameter p1 = pgen.next(type);
        Parameter p2 = pgen.next(type);
        Parameter p3 = pgen.next(type);

        Constants consts = new Constants();
        SymbolicSuffixRestrictionBuilder restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);
        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts, restrictionBuilder);

        Word<PSymbolInstance> word1 = Word.fromSymbols(
        		new PSymbolInstance(a, new DataValue(type, 0)),
        		new PSymbolInstance(a, new DataValue(type, 1)),
        		new PSymbolInstance(a, new DataValue(type, 2)),
        		new PSymbolInstance(a, new DataValue(type, 0)));
        SymbolicSuffix suffix1 = new SymbolicSuffix(word1.prefix(2), word1.suffix(2), restrictionBuilder);
        SymbolicSuffix expectedSuffix1 = new SymbolicSuffix(word1.prefix(1), word1.suffix(3), restrictionBuilder);
        SDT sdt1 = new SDT(Map.of(
        		new EqualityGuard(s1, r2), new SDT(Map.of(
        				new SDTTrueGuard(s2), SDTLeaf.ACCEPTING)),
        		new DisequalityGuard(s1, r2), new SDT(Map.of(
        				new EqualityGuard(s2, r1), SDTLeaf.REJECTING,
        				new DisequalityGuard(s2, r1), SDTLeaf.ACCEPTING))));
        PIV piv1 = new PIV();
        piv1.put(p1, r1);
        piv1.put(p2, r2);
        SymbolicSuffix actualSuffix1 = builder.extendSuffix(word1.prefix(2), sdt1, piv1, suffix1, r1);
        Assert.assertEquals(actualSuffix1, expectedSuffix1);


        Word<PSymbolInstance> word2 = Word.fromSymbols(
        		new PSymbolInstance(a, new DataValue(type, 0)),
        		new PSymbolInstance(a, new DataValue(type, 1)),
        		new PSymbolInstance(a, new DataValue(type, 2)),
        		new PSymbolInstance(a, new DataValue(type, 1)),
        		new PSymbolInstance(a, new DataValue(type, 0)));
        SymbolicSuffix suffix2 = new SymbolicSuffix(word2.prefix(3), word2.suffix(2), restrictionBuilder);
        SymbolicSuffix expectedSuffix2 = new SymbolicSuffix(word2.prefix(2), word2.suffix(3), restrictionBuilder);
        SDT sdt2 = new SDT(Map.of(
        		new EqualityGuard(s1, r2), new SDT(Map.of(
        				new EqualityGuard(s2, r1), SDTLeaf.ACCEPTING,
        				new DisequalityGuard(s2, r1), SDTLeaf.REJECTING)),
        		new DisequalityGuard(s1, r2), new SDT(Map.of(
        				new EqualityGuard(s2, r1), SDTLeaf.REJECTING,
        				new DisequalityGuard(s2, r1), SDTLeaf.ACCEPTING))));
        PIV piv2 = new PIV();
        piv2.putAll(piv1);
        SymbolicSuffix actualSuffix2 = builder.extendSuffix(word2.prefix(2), sdt2, piv2, suffix2, r1, r2);
        Assert.assertEquals(actualSuffix2, expectedSuffix2);


        Word<PSymbolInstance> word3 = Word.fromSymbols(
        		new PSymbolInstance(a, new DataValue(type, 0)),
        		new PSymbolInstance(b, new DataValue(type, 1), new DataValue(type, 2)),
        		new PSymbolInstance(a, new DataValue(type, 0)),
        		new PSymbolInstance(a, new DataValue(type, 0)));
        SymbolicSuffix suffix3 = new SymbolicSuffix(word3.prefix(2), word3.suffix(2), restrictionBuilder);
        SymbolicSuffix expectedSuffix3 = new SymbolicSuffix(word3.prefix(1), word3.suffix(3), restrictionBuilder);
        SDT sdt3 = new SDT(Map.of(
        		new EqualityGuard(s1, r1), new SDT(Map.of(
        				new EqualityGuard(s2, r2), SDTLeaf.ACCEPTING,
        				new DisequalityGuard(s2, r2), SDTLeaf.REJECTING)),
        		new DisequalityGuard(s1, r1), new SDT(Map.of(
        				new EqualityGuard(s2, r3), SDTLeaf.ACCEPTING,
        				new DisequalityGuard(s2, r3), SDTLeaf.REJECTING))));
        PIV piv3 = new PIV();
        piv3.put(p1, r1);
        piv3.put(p2, r2);
        piv3.put(p3, r3);
        SymbolicSuffix actualSuffix3 = builder.extendSuffix(word3.prefix(2), sdt3, piv3, suffix3, r2, r3);
        Assert.assertEquals(actualSuffix3, expectedSuffix3);
    }

    @Test
    private void sdtPruneTest() {

        DataType type = new DataType("int",Integer.class);
        InputSymbol a = new InputSymbol("a", type);
        InputSymbol b = new InputSymbol("b", type, type);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(type);
        teachers.put(type, dit);

        SuffixValueGenerator sgen = new SymbolicDataValueGenerator.SuffixValueGenerator();
        SuffixValue s1 = sgen.next(type);
        SuffixValue s2 = sgen.next(type);
        SuffixValue s3 = sgen.next(type);

        RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
        Register r1 = rgen.next(type);
        Register r2 = rgen.next(type);
        Register r3 = rgen.next(type);

        ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
        Parameter p1 = pgen.next(type);
        Parameter p2 = pgen.next(type);
        Parameter p3 = pgen.next(type);

        Constants consts = new Constants();
        SymbolicSuffixRestrictionBuilder restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);
        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts, restrictionBuilder);

        SDT subSDT1 = new SDT(Map.of(
        		new EqualityGuard(s2, r1), new SDT(Map.of(
        				new SDTTrueGuard(s3), SDTLeaf.ACCEPTING)),
        		new DisequalityGuard(s2, r1), new SDT(Map.of(
        				new EqualityGuard(s3, r1), SDTLeaf.ACCEPTING,
        				new DisequalityGuard(s3, r1), SDTLeaf.REJECTING))));
        SDT subSDT2 = new SDT(Map.of(
        		new SDTTrueGuard(s2), new SDT(Map.of(
        				new EqualityGuard(s3, r1), SDTLeaf.ACCEPTING,
        				new DisequalityGuard(s3, r1), SDTLeaf.REJECTING))));
        SDT subSDT3 = new SDT(Map.of(
        		new SDTTrueGuard(s2), new SDT(Map.of(
        				new SDTTrueGuard(s3), SDTLeaf.REJECTING))));

        SDT sdt1 = new SDT(Map.of(
        		new EqualityGuard(s1, r2), subSDT1,
        		new DisequalityGuard(s1, r2), subSDT2));
        SDT sdt2 = new SDT(Map.of(
        		new EqualityGuard(s1, r2), subSDT1,
        		new DisequalityGuard(s1, r2), subSDT3));
        Map<SDTGuard, SDT> branches2 = new LinkedHashMap<>();
        for (Map.Entry<SDTGuard, SDT> e : sdt2.getChildren().entrySet()) {
        	if (e.getKey() instanceof EqualityGuard)
        		branches2.put(e.getKey(), e.getValue());
        }

        SDT sdt3 = new SDT(Map.of(
        		new EqualityGuard(s1, r1), new SDT(Map.of(
        				new EqualityGuard(s2, r1), new SDT(Map.of(
        						new EqualityGuard(s3, r2), SDTLeaf.ACCEPTING,
        						new DisequalityGuard(s3, r2), SDTLeaf.REJECTING)),
        				new DisequalityGuard(s2, r1), new SDT(Map.of(
        						new SDTTrueGuard(s3), SDTLeaf.REJECTING)))),
        		new DisequalityGuard(s1, r1), new SDT(Map.of(
        				new EqualityGuard(s2, r1), new SDT(Map.of(
        						new EqualityGuard(s3, r2), SDTLeaf.REJECTING,
        						new DisequalityGuard(s3, r2), SDTLeaf.ACCEPTING)),
        				new DisequalityGuard(s2, r1), new SDT(Map.of(
        						new SDTTrueGuard(s3), SDTLeaf.REJECTING))))));

        SDT expected1 = sdt1;
        SDT expected2 = new SDT(branches2);
        SDT expected3 = new SDT(Map.of(
        		new EqualityGuard(s1, r1), new SDT(Map.of(
        				new EqualityGuard(s2, r1), new SDT(Map.of(
        						new EqualityGuard(s3, r2), SDTLeaf.ACCEPTING)),
        				new DisequalityGuard(s2, r1), new SDT(Map.of(
        						new SDTTrueGuard(s3), SDTLeaf.REJECTING)))),
        		new DisequalityGuard(s1, r1), new SDT(Map.of(
        				new EqualityGuard(s2, r1), new SDT(Map.of(
        						new DisequalityGuard(s3, r2), SDTLeaf.ACCEPTING)),
        				new DisequalityGuard(s2, r1), new SDT(Map.of(
        						new SDTTrueGuard(s3), SDTLeaf.REJECTING))))));

        SDT actual1 = builder.pruneSDT(sdt1, new SymbolicDataValue[] {r1});
//        builder.printStuff = true;
        SDT actual2 = builder.pruneSDT(sdt2, new SymbolicDataValue[] {r1});
        SDT actual3 = builder.pruneSDT(sdt3, new SymbolicDataValue[] {r1});

        Set<List<SDTGuard>> expectedPaths1 = expected1.getAllPaths(new ArrayList<>()).keySet();
        Set<List<SDTGuard>> expectedPaths2 = expected2.getAllPaths(new ArrayList<>()).keySet();
        Set<List<SDTGuard>> expectedPaths3 = expected3.getAllPaths(new ArrayList<>()).keySet();
        Set<List<SDTGuard>> actualPaths1 = actual1.getAllPaths(new ArrayList<>()).keySet();
        Set<List<SDTGuard>> actualPaths2 = actual2.getAllPaths(new ArrayList<>()).keySet();
        Set<List<SDTGuard>> actualPaths3 = actual3.getAllPaths(new ArrayList<>()).keySet();

        Assert.assertEquals(actualPaths1.size(), expectedPaths1.size());
        Assert.assertTrue(actualPaths1.containsAll(expectedPaths1));

        Assert.assertEquals(actualPaths2.size(), expectedPaths2.size());
        Assert.assertTrue(actualPaths2.containsAll(expectedPaths2));

        Assert.assertEquals(actualPaths3.size(), expectedPaths3.size());
        Assert.assertTrue(actualPaths3.containsAll(expectedPaths3));
    }
}
