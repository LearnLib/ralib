package de.learnlib.ralib.oracles.mto;

import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.CONTAINS;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.INSERT;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.INT_TYPE;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.POP;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.PUSH;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.dv;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
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

        Word<PSymbolInstance> word = Word.fromSymbols(
                new PSymbolInstance(PUSH, dv(1)),
                new PSymbolInstance(INSERT, dv(1),dv(2)),
                new PSymbolInstance(POP, dv(2)),
                new PSymbolInstance(CONTAINS, dv(1)));

        equalsSuffixesFromConcretePrefixSuffix(word, mto, consts, solver);


        InputSymbol A = new InputSymbol("a", INT_TYPE, INT_TYPE);

        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts);

        SuffixValueGenerator svGen = new SuffixValueGenerator();
        SuffixValue s1 = svGen.next(INT_TYPE);
        SuffixValue s2 = svGen.next(INT_TYPE);

        RegisterGenerator rGen = new RegisterGenerator();
        Register r1 = rGen.next(INT_TYPE);
        Register r2 = rGen.next(INT_TYPE);

        ParameterGenerator pGen = new ParameterGenerator();
        Parameter p1 = pGen.next(INT_TYPE);
        Parameter p2 = pGen.next(INT_TYPE);
        Parameter p3 = pGen.next(INT_TYPE);
        Parameter p4 = pGen.next(INT_TYPE);

        PIV piv1 = new PIV();
        piv1.put(p1, r1);
        piv1.put(p3, r2);
        PIV piv2 = new PIV();
        piv2.put(p3, r1);
        piv2.put(p4, r2);

        Word<PSymbolInstance> word1 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 0)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 1), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 1)));
        Word<PSymbolInstance> word2 = Word.fromSymbols(
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 0), new DataValue(INT_TYPE, 1)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 2), new DataValue(INT_TYPE, 3)),
        		new PSymbolInstance(A, new DataValue(INT_TYPE, 3), new DataValue(INT_TYPE, 4)));
        SymbolicSuffix suffix1 = new SymbolicSuffix(word1.prefix(2), word1.suffix(1));
        SymbolicSuffix suffix2 = new SymbolicSuffix(word2.prefix(2), word2.suffix(1));

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

        SymbolicSuffix expected = new SymbolicSuffix(word1.prefix(1), word1.suffix(2));
        SymbolicSuffix actual = builder.extendSuffix(word1.prefix(2), sdt1, piv1, suffix1);
        Assert.assertEquals(actual, expected);

        actual = builder.extendSuffix(word2.prefix(2), sdt2, piv2, suffix2);
        Assert.assertEquals(actual.toString(), "[s3]((a[s1, s2] a[s3, s4]))");
    }


    /*
     * Checks for equality optimized suffixes built by pre-pending using an SDT against those built from concrete prefix/suffix
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
    public void buildOptimizedSuffixTest() {

        DataType type = new DataType("int",Integer.class);
        InputSymbol a = new InputSymbol("a", type);
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
        Assert.assertEquals(suffix12.toString(), "[s1]((a[s1] a[s2] a[s3]))");


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
        Assert.assertEquals(suffix34.toString(), "[s1]((a[s1] a[s2] a[s2]))");
    }
}
