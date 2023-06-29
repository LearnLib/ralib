package de.learnlib.ralib.oracles.mto;

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
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class OptimizedSymbolicSuffixBuilderTest {

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

        OptimizedSymbolicSuffixBuilder builder = new OptimizedSymbolicSuffixBuilder(consts, new SimpleConstraintSolver());
        SymbolicSuffix suffix12 = builder.distinguishingSuffixFromSDTs(prefix1, sdt1, piv1, prefix2, sdt2, piv2, Word.fromSymbols(a, a, a));
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


        SymbolicSuffix suffix34 = builder.distinguishingSuffixFromSDTs(prefix3, sdt3, piv3, prefix4, sdt4, piv4,  Word.fromSymbols(a, a, a));
        Assert.assertEquals(suffix34.toString(), "[s1]((a[s1] a[s2] a[s2]))");
        System.out.println(suffix34);
    }
}
