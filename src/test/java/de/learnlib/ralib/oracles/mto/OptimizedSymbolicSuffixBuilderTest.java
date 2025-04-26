package de.learnlib.ralib.oracles.mto;

import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.example.list.BoundedList;
import de.learnlib.ralib.example.list.BoundedListDataWordOracle;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.*;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class OptimizedSymbolicSuffixBuilderTest {

    @Test
    public void extendDistinguishingSuffixTest() {
        BoundedListDataWordOracle dwOracle = new BoundedListDataWordOracle(() -> new BoundedList(2, false));

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(INT_TYPE);
        teachers.put(INT_TYPE, dit);
        ConstraintSolver solver = new ConstraintSolver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);

        Constants consts = new Constants();

        SymbolicSuffixRestrictionBuilder restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);

        Word<PSymbolInstance> word = Word.fromSymbols(
                new PSymbolInstance(PUSH, dv(1)),
                new PSymbolInstance(INSERT, dv(1),dv(2)),
                new PSymbolInstance(POP, dv(2)),
                new PSymbolInstance(CONTAINS, dv(1)));

        equalsSuffixesFromConcretePrefixSuffix(word, mto, consts);

        InputSymbol A = new InputSymbol("a", INT_TYPE, INT_TYPE);
        InputSymbol B = new InputSymbol("b");
        InputSymbol C = new InputSymbol("c", INT_TYPE);

        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts, restrictionBuilder);

        SymbolicDataValueGenerator.SuffixValueGenerator svGen =
                new SymbolicDataValueGenerator.SuffixValueGenerator();

        SymbolicDataValue.SuffixValue s1 = svGen.next(INT_TYPE);
        SymbolicDataValue.SuffixValue s2 = svGen.next(INT_TYPE);
        SymbolicDataValue.SuffixValue s3 = svGen.next(INT_TYPE);
        SymbolicDataValue.SuffixValue s4 = svGen.next(INT_TYPE);

        SymbolicDataValueGenerator.RegisterGenerator rGen =
                new SymbolicDataValueGenerator.RegisterGenerator();

        SymbolicDataValueGenerator.ConstantGenerator cGen =
                new SymbolicDataValueGenerator.ConstantGenerator();
        SymbolicDataValue.Constant c1 = cGen.next(INT_TYPE);

        DataValue d0 = new DataValue(INT_TYPE, BigDecimal.ZERO);
        DataValue d1 = new DataValue(INT_TYPE, BigDecimal.ONE);
        DataValue d2 = new DataValue(INT_TYPE, new BigDecimal(2));
        DataValue d3 = new DataValue(INT_TYPE, new BigDecimal(3));

        Constants consts2 = new Constants();
        consts2.put(c1, new DataValue(INT_TYPE,new BigDecimal(2)));
        SymbolicSuffixRestrictionBuilder restrictionBuilder2 = new SymbolicSuffixRestrictionBuilder(consts2, teachers);

        Word<PSymbolInstance> word1 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ZERO), new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ONE), new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ZERO), new DataValue(INT_TYPE, BigDecimal.ONE)));
        Word<PSymbolInstance> word2 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ZERO), new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE,new BigDecimal(2)), new DataValue(INT_TYPE,new BigDecimal(3))),
        		new PSymbolInstance(A, new DataValue(INT_TYPE,new BigDecimal(3)), new DataValue(INT_TYPE,new BigDecimal(4))));
        Word<PSymbolInstance> word3 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ZERO), new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ONE), new DataValue(INT_TYPE,new BigDecimal(2))));
        Word<PSymbolInstance> word4 = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(C, new DataValue(INT_TYPE,new BigDecimal(2))),
        		new PSymbolInstance(C, new DataValue(INT_TYPE,new BigDecimal(2))),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)));
        Word<PSymbolInstance> word5 = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)));
        Word<PSymbolInstance> word6 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE,new BigDecimal(2)), new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE,new BigDecimal(2))));
        Word<PSymbolInstance> word7a = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(B));
        Word<PSymbolInstance> word7b = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(B),
        		new PSymbolInstance(C, new DataValue(INT_TYPE,new BigDecimal(2))),
        		new PSymbolInstance(B));
        Word<PSymbolInstance> word8a = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ONE)));
        Word<PSymbolInstance> word8b = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ONE)));
        Word<PSymbolInstance> word8c = Word.fromSymbols(
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(C, new DataValue(INT_TYPE, BigDecimal.ZERO)));
        SymbolicSuffix suffix1 = new SymbolicSuffix(word1.prefix(2), word1.suffix(1), restrictionBuilder);
        SymbolicSuffix suffix2 = new SymbolicSuffix(word2.prefix(2), word2.suffix(1), restrictionBuilder);
        SymbolicSuffix suffix3 = new SymbolicSuffix(word3.prefix(2), word3.suffix(1), restrictionBuilder2);
        SymbolicSuffix suffix4 = new SymbolicSuffix(word4.prefix(2), word4.suffix(4), restrictionBuilder);
        SymbolicSuffix suffix5 = new SymbolicSuffix(word5.prefix(2), word5.suffix(4), restrictionBuilder);
        SymbolicSuffix suffix6 = new SymbolicSuffix(word6.prefix(1), word6.suffix(1), restrictionBuilder);
        SymbolicSuffix suffix7 = new SymbolicSuffix(word7a.prefix(3), word7a.suffix(1), restrictionBuilder);
        SymbolicSuffix suffix8 = new SymbolicSuffix(word8a.prefix(2), word8a.suffix(1), restrictionBuilder);
        SDT sdt1 = new SDT(Map.of(
        		new SDTGuard.EqualityGuard(s1, d0), new SDT(Map.of(
        				new SDTGuard.EqualityGuard(s2, d1), SDTLeaf.ACCEPTING,
        				new SDTGuard.DisequalityGuard(s2, d1), SDTLeaf.REJECTING)),
        		new SDTGuard.DisequalityGuard(s1, d0), new SDT(Map.of(
        				new SDTGuard.SDTTrueGuard(s2), SDTLeaf.REJECTING))));
        SDT sdt2 = new SDT(Map.of(
        		new SDTGuard.SDTOrGuard(s1, List.of(new SDTGuard.EqualityGuard(s1, d2), new SDTGuard.EqualityGuard(s1, d3))), new SDT(Map.of(
        				new SDTGuard.SDTTrueGuard(s2), SDTLeaf.ACCEPTING)),
        		new SDTGuard.SDTAndGuard(s1, List.of(new SDTGuard.DisequalityGuard(s1, d2), new SDTGuard.DisequalityGuard(s1, d3))), new SDT(Map.of(
        				new SDTGuard.SDTTrueGuard(s2), SDTLeaf.REJECTING))));
        SDT sdt3 = new SDT(Map.of(
        		new SDTGuard.EqualityGuard(s1, d1), new SDT(Map.of(
        				new SDTGuard.EqualityGuard(s2, c1), SDTLeaf.ACCEPTING,
        				new SDTGuard.DisequalityGuard(s2, c1), SDTLeaf.REJECTING)),
        		new SDTGuard.DisequalityGuard(s1, d1), new SDT(Map.of(
        				new SDTGuard.SDTTrueGuard(s2), SDTLeaf.REJECTING))));
        SDT sdt4 = new SDT(Map.of(
        		new SDTGuard.SDTTrueGuard(s1), new SDT(Map.of(
        				new SDTGuard.EqualityGuard(s2, s1), new SDT(Map.of(
        						new SDTGuard.EqualityGuard(s3, d0), new SDT(Map.of(
        								new SDTGuard.EqualityGuard(s4, d0), SDTLeaf.ACCEPTING,
        								new SDTGuard.DisequalityGuard(s4, d0), SDTLeaf.REJECTING)),
        						new SDTGuard.DisequalityGuard(s3, d0), new SDT(Map.of(
        								new SDTGuard.SDTTrueGuard(s4), SDTLeaf.REJECTING)))),
        				new SDTGuard.DisequalityGuard(s2, s1), new SDT(Map.of(
        						new SDTGuard.SDTTrueGuard(s3), new SDT(Map.of(
        								new SDTGuard.SDTTrueGuard(s4), SDTLeaf.REJECTING))))))));
        SDT sdt5 = new SDT(Map.of(
        		new SDTGuard.SDTTrueGuard(s1), new SDT(Map.of(
        				new SDTGuard.EqualityGuard(s2, s1), new SDT(Map.of(
        						new SDTGuard.EqualityGuard(s3, d0), new SDT(Map.of(
        								new SDTGuard.SDTTrueGuard(s4), SDTLeaf.REJECTING)),
        						new SDTGuard.DisequalityGuard(s3, d0), new SDT(Map.of(
        								new SDTGuard.EqualityGuard(s4, s3), SDTLeaf.ACCEPTING,
        								new SDTGuard.DisequalityGuard(s4, s3), SDTLeaf.REJECTING))))))));
        SDT sdt6 = new SDT(Map.of(
        		new SDTGuard.EqualityGuard(s1, d2), SDTLeaf.ACCEPTING,
        		new SDTGuard.DisequalityGuard(s1, d2), SDTLeaf.REJECTING));
        SDT sdt7 = new SDT(Map.of(
        		new SDTGuard.SDTTrueGuard(s1), SDTLeaf.REJECTING));

        SymbolicSuffix expected1 = new SymbolicSuffix(word1.prefix(1), word1.suffix(2), restrictionBuilder);
        SymbolicSuffix actual1 = builder.extendSuffix(word1.prefix(2), sdt1, suffix1);
        Assert.assertEquals(actual1, expected1);

        SymbolicSuffix actual2 = builder.extendSuffix(word2.prefix(2), sdt2, suffix2);
//        SuffixValue[] actualSV2 = actual2.getDataValues().toArray(new SuffixValue[actual2.getDataValues().size()]);
        Map<SymbolicDataValue.SuffixValue, SuffixValueRestriction> expectedRestr2 = new LinkedHashMap<>();
        expectedRestr2.put(s1, new FreshSuffixValue(s1));
        expectedRestr2.put(s2, new FreshSuffixValue(s2));
        expectedRestr2.put(s3, new UnrestrictedSuffixValue(s3));
        expectedRestr2.put(s4, new FreshSuffixValue(s4));
        SymbolicSuffix expected2 = new SymbolicSuffix(actual2.getActions(), expectedRestr2);
        Assert.assertEquals(actual2, expected2);
//        Assert.assertEquals(actual2.toString(), "[s3]((a[s1, s2] a[s3, s4]))");

        SymbolicSuffix expected3 = new SymbolicSuffix(word3.prefix(1), word3.suffix(2), restrictionBuilder2);
        SymbolicSuffix actual3 = builder.extendSuffix(word3.prefix(2), sdt3, suffix3);
        Assert.assertEquals(actual3, expected3);

        SymbolicSuffix expected4 = new SymbolicSuffix(word4.prefix(1), word4.suffix(5), restrictionBuilder);
        SymbolicSuffix actual4 = builder.extendSuffix(word4.prefix(2), sdt4, suffix4);
        Assert.assertEquals(actual4, expected4);

        SymbolicSuffix expected5 = new SymbolicSuffix(word5.prefix(1), word5.suffix(5), restrictionBuilder);
        SymbolicSuffix actual5 = builder.extendSuffix(word5.prefix(2), sdt5, suffix5);
        Assert.assertEquals(actual5, expected5);

        SymbolicSuffix expected6 = new SymbolicSuffix(Word.epsilon(), word6, restrictionBuilder);
        SymbolicSuffix actual6 = builder.extendSuffix(word6.prefix(1), sdt6, suffix6);
        Assert.assertEquals(actual6, expected6);

        OptimizedSymbolicSuffixBuilder constBuilder = new OptimizedSymbolicSuffixBuilder(consts2, restrictionBuilder2);
        SymbolicSuffix expected7 = new SymbolicSuffix(word7b.prefix(2), word7b.suffix(2), restrictionBuilder2);
        SymbolicSuffix actual7 = constBuilder.extendDistinguishingSuffix(word7a.prefix(3), SDTLeaf.ACCEPTING, word7b.prefix(3), SDTLeaf.REJECTING, suffix7);
        Assert.assertEquals(actual7, expected7);

        SymbolicSuffix expected8 = new SymbolicSuffix(word8c.prefix(1), word8c.suffix(2), restrictionBuilder);
        SymbolicSuffix actual8 = builder.extendDistinguishingSuffix(word8a.prefix(2), sdt6, word8b.prefix(2), sdt7, suffix8);
        Assert.assertEquals(actual8, expected8);
    }

    /*
     * Checks for equality optimized suffixes built by prepending using an SDT against those built from concrete prefix/suffix
     */
    private void equalsSuffixesFromConcretePrefixSuffix(Word<PSymbolInstance> word, MultiTheoryTreeOracle mto, Constants consts) {
        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts);
        SDT tqr = mto.treeQuery(word, new SymbolicSuffix(Word.epsilon()));
        SymbolicSuffix actual = new SymbolicSuffix(word, Word.epsilon());
        int suffixLength;
        for (suffixLength=1; suffixLength<word.length(); suffixLength++) {
            Word<PSymbolInstance> suffix = word.suffix(suffixLength);
            Word<PSymbolInstance> sub = word.prefix(word.length() - suffixLength);
            Word<PSymbolInstance> prefix = word.prefix(sub.length()+1);
            SymbolicSuffix expected = new SymbolicSuffix(sub, suffix);
            actual = builder.extendSuffix(prefix,  tqr, actual);
            Assert.assertEquals(actual, expected);
            tqr = mto.treeQuery(sub, expected);
        }
    }

    @Test
    public void extendSuffixTest() {
        DataType type = new DataType("int");

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(type);
        teachers.put(type, dit);

        InputSymbol A = new InputSymbol("a", type, type);
        InputSymbol B = new InputSymbol("b", type);
        InputSymbol C = new InputSymbol("c");
        SymbolicDataValueGenerator.SuffixValueGenerator sgen =
                new SymbolicDataValueGenerator.SuffixValueGenerator();
        SymbolicDataValue.SuffixValue s1 = sgen.next(type);
        SymbolicDataValue.SuffixValue s2 = sgen.next(type);
        SymbolicDataValue.SuffixValue s3 = sgen.next(type);
        SymbolicDataValue.SuffixValue s4 = sgen.next(type);
        SymbolicDataValue.SuffixValue s5 = sgen.next(type);

        SymbolicDataValueGenerator.ConstantGenerator cGen =
                new SymbolicDataValueGenerator.ConstantGenerator();
        SymbolicDataValue.Constant c1 = cGen.next(INT_TYPE);
        SymbolicDataValue.Constant c2 = cGen.next(INT_TYPE);

        DataValue d0 = new DataValue(INT_TYPE, BigDecimal.ZERO);
        DataValue d1 = new DataValue(INT_TYPE, BigDecimal.ONE);
        DataValue d2 = new DataValue(INT_TYPE, new BigDecimal(2));

        Constants consts1 = new Constants();
        Constants consts2 = new Constants();
        consts2.put(c1, new DataValue(INT_TYPE,new BigDecimal(3)));
        consts2.put(c2, new DataValue(INT_TYPE,new BigDecimal(4)));

        SymbolicSuffixRestrictionBuilder restrictionBuilder1 = new SymbolicSuffixRestrictionBuilder(consts1, teachers);
        SymbolicSuffixRestrictionBuilder restrictionBuilder2 = new SymbolicSuffixRestrictionBuilder(consts2, teachers);

        Word<PSymbolInstance> word1 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ZERO), new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ONE), new DataValue(INT_TYPE,new BigDecimal(2))),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ZERO), new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE,new BigDecimal(2)), new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(B, new DataValue(INT_TYPE,new BigDecimal(2))));
        Word<PSymbolInstance> word2 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ZERO), new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ONE), new DataValue(INT_TYPE, new BigDecimal(2))),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ONE), new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, new BigDecimal(3)), new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(B, new DataValue(INT_TYPE, new BigDecimal(4))));
        Word<PSymbolInstance> word3 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ZERO), new DataValue(INT_TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, BigDecimal.ONE), new DataValue(INT_TYPE, new BigDecimal(2))),
        		new PSymbolInstance(A, new DataValue(INT_TYPE,new BigDecimal(3)), new DataValue(INT_TYPE, new BigDecimal(6))),
        		new PSymbolInstance(B, new DataValue(INT_TYPE, new BigDecimal(5))));
        Word<PSymbolInstance> word4 = Word.fromSymbols(
        		new PSymbolInstance(B, new DataValue(INT_TYPE, BigDecimal.ZERO)),
        		new PSymbolInstance(B, new DataValue(INT_TYPE, new BigDecimal(3))),
        		new PSymbolInstance(C));
        SymbolicSuffix suffix1 = new SymbolicSuffix(word1.prefix(2), word1.suffix(3), restrictionBuilder1);
        SymbolicSuffix suffix2 = new SymbolicSuffix(word2.prefix(2), word2.suffix(3), restrictionBuilder1);
        SymbolicSuffix suffix3 = new SymbolicSuffix(word3.prefix(2), word3.suffix(2), restrictionBuilder2);
        SymbolicSuffix suffix4 = new SymbolicSuffix(word4.prefix(2), word4.suffix(1), restrictionBuilder2);

        List<SDTGuard> sdtPath1 = new ArrayList<>();
        sdtPath1.add(new SDTGuard.EqualityGuard(s1, d0));
        sdtPath1.add(new SDTGuard.EqualityGuard(s2, d1));
        sdtPath1.add(new SDTGuard.EqualityGuard(s3, d2));
        sdtPath1.add(new SDTGuard.EqualityGuard(s4, s1));
        sdtPath1.add(new SDTGuard.EqualityGuard(s5, s3));
        List<SDTGuard> sdtPath2 = new ArrayList<>();
        sdtPath2.add(new SDTGuard.DisequalityGuard(s1, d0));
        sdtPath2.add(new SDTGuard.DisequalityGuard(s2, d1));
        sdtPath2.add(new SDTGuard.DisequalityGuard(s3, d2));
        sdtPath2.add(new SDTGuard.DisequalityGuard(s4, s1));
        sdtPath2.add(new SDTGuard.DisequalityGuard(s5, s3));
        List<SDTGuard> sdtPath3 = new ArrayList<>();
        sdtPath3.add(new SDTGuard.EqualityGuard(s1, c1));
        sdtPath3.add(new SDTGuard.DisequalityGuard(s2, c2));
        sdtPath3.add(new SDTGuard.SDTTrueGuard(s3));
        List<List<SDTGuard>> sdtPaths4 = SDTLeaf.ACCEPTING.getPaths(true);
        List<SDTGuard> sdtPath4 = sdtPaths4.get(0);

        OptimizedSymbolicSuffixBuilder builder1 = new OptimizedSymbolicSuffixBuilder(consts1, restrictionBuilder1);
        OptimizedSymbolicSuffixBuilder builder2 = new OptimizedSymbolicSuffixBuilder(consts2, restrictionBuilder2);
        ConstraintSolver solver = new ConstraintSolver();

        SymbolicSuffix expected1 = new SymbolicSuffix(word1.prefix(1), word1.suffix(4), restrictionBuilder1);
        SymbolicSuffix actual1 = builder1.extendSuffix(word1.prefix(2), sdtPath1, suffix1.getActions());
        Assert.assertEquals(actual1, expected1);

        // this test does not seem to be correct, it needs to be examined more closely
//        SymbolicSuffix expected2 = new SymbolicSuffix(word2.prefix(1), word2.suffix(4), restrictionBuilder1);
//        SymbolicSuffix actual2 = builder1.extendSuffix(word2.prefix(2), sdtPath2, piv1, suffix2.getActions());
//        Assert.assertEquals(actual2, expected2);

        SymbolicSuffix expected3 = new SymbolicSuffix(word3.prefix(1), word3.suffix(3), restrictionBuilder2);
        SymbolicSuffix actual3 = builder1.extendSuffix(word3.prefix(2), sdtPath3, suffix3.getActions());
        Assert.assertEquals(actual3, expected3);

        SymbolicSuffix actual4 = builder2.extendSuffix(word4.prefix(2), sdtPath4, suffix4.getActions());
        Assert.assertEquals(actual4.getFreeValues().size(), 1);
    }

    @Test
    public void testCoalesce() {
        DataType type = new DataType("int");
        InputSymbol a = new InputSymbol("a", type);

        DataValue dv1 = new DataValue(type, BigDecimal.ZERO);
        DataValue dv2 = new DataValue(type, BigDecimal.ONE);
        DataValue dv3 = new DataValue(type, new BigDecimal(2));

        Constants consts = new Constants();
        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts);

        Word<PSymbolInstance> word1 = Word.fromSymbols(
        		new PSymbolInstance(a, dv1),
        		new PSymbolInstance(a, dv1),
        		new PSymbolInstance(a, dv2),
        		new PSymbolInstance(a, dv2),
        		new PSymbolInstance(a, dv3));
        Word<PSymbolInstance> word2 = Word.fromSymbols(
        		new PSymbolInstance(a, dv1),
        		new PSymbolInstance(a, dv2),
        		new PSymbolInstance(a, dv2),
        		new PSymbolInstance(a, dv3),
        		new PSymbolInstance(a, dv3));
        Word<PSymbolInstance> word3 = Word.fromSymbols(
        		new PSymbolInstance(a, dv1),
        		new PSymbolInstance(a, dv1),
        		new PSymbolInstance(a, dv1),
        		new PSymbolInstance(a, dv1),
        		new PSymbolInstance(a, dv1));
        SymbolicSuffix suffix1 = new SymbolicSuffix(word1.prefix(1), word1.suffix(4));
        SymbolicSuffix suffix2 = new SymbolicSuffix(word2.prefix(1), word2.suffix(4));
        SymbolicSuffix suffixExpected = new SymbolicSuffix(word3.prefix(1), word3.suffix(4));
        SymbolicSuffix suffixActual = builder.coalesceSuffixes(suffix1, suffix2);
        Assert.assertEquals(suffixActual, suffixExpected);
    }

}
