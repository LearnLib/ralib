package de.learnlib.ralib.oracles.mto;

import static de.learnlib.ralib.example.priority.PriorityQueueOracle.OFFER;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.POLL;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.doubleType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTLeaf;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.example.sdts.SDTOracle;

import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;

import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.Theory;

import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.word.Word;

public class BranchMergingTest extends RaLibTestSuite {
/*
    @Test
    public void branchMerginPQTest() {

        Constants consts = new Constants();
        DataWordOracle dwOracle =
                new de.learnlib.ralib.example.priority.PriorityQueueOracle(2);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(doubleType);
        teachers.put(doubleType, dit);

        ConstraintSolver jsolv = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, consts, jsolv);

        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, BigDecimal.ONE)));
        Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, BigDecimal.valueOf(2))),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, BigDecimal.ONE)),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, BigDecimal.valueOf(2))));
        SymbolicSuffix symbSuffix = new SymbolicSuffix(prefix, suffix);

        TreeQueryResult tqr = mto.treeQuery(prefix, symbSuffix);
        SDT sdt = (SDT) tqr.sdt();

        SuffixValue s1 = new SuffixValue(doubleType, 1);
        Register r1 = new Register(doubleType, 1);

        SDTGuard guardLeq = SDTGuard.IntervalGuard.lessOrEqualGuard(s1, r1);
        SDTGuard guardG = SDTGuard.IntervalGuard.greaterGuard(s1, r1);

        Assert.assertEquals(sdt.getSDTGuards(s1).size(), 2);
        Assert.assertTrue(sdt.getSDTGuards(s1).contains(guardLeq));
        Assert.assertTrue(sdt.getSDTGuards(s1).contains(guardG));
    }

    @Test
    public void branchMergingEqTest() {
        final DataType INT_TYPE = new DataType("int");
        final InputSymbol A = new InputSymbol("a", new DataType[] {INT_TYPE});

        SDTOracle oracle = new SDTOracle();
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(INT_TYPE);
        teachers.put(INT_TYPE, dit);
        ConstraintSolver solver = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(oracle, teachers, new Constants(), solver);

        Constants consts = new Constants();

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        ParameterGenerator pgen = new ParameterGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        SuffixValue s1 = svgen.next(INT_TYPE);
        SuffixValue s2 = svgen.next(INT_TYPE);
        Parameter p1 = pgen.next(INT_TYPE);
        Parameter p2 = pgen.next(INT_TYPE);
        Register r1 = rgen.next(INT_TYPE);
        Register r2 = rgen.next(INT_TYPE);
        DataValue dv1 = new DataValue(INT_TYPE, 1);
        DataValue dv2 = new DataValue(INT_TYPE, 2);

        Word<PSymbolInstance> prefix1 = Word.fromSymbols(
        		new PSymbolInstance(A, dv1),
        		new PSymbolInstance(A, dv2));
        Word<ParameterizedSymbol> suffixActions1 = Word.fromSymbols(A, A);
        SymbolicSuffix suffix1 = new SymbolicSuffix(suffixActions1);

        Word<PSymbolInstance> prefix2 = Word.fromSymbols(
        		new PSymbolInstance(A, dv1),
        		new PSymbolInstance(A, dv2));
        Word<ParameterizedSymbol> suffixActions2 = Word.fromSymbols(A);
        SymbolicSuffix suffix2 = new SymbolicSuffix(suffixActions2);

        PIV piv1 = new PIV();
        piv1.put(p1, r1);
        piv1.put(p2, r2);
        Mapping<SymbolicDataValue, DataValue<?>> vals1 = new Mapping<SymbolicDataValue, DataValue<?>>();
        vals1.put(r1, dv1);
        vals1.put(r2, dv2);

        SDT sdt1 = new SDT(Map.of(
                new SDTGuard.EqualityGuard(s1, r1), new SDT(Map.of(
                        new SDTGuard.SDTTrueGuard(s2), SDTLeaf.ACCEPTING)),
                new SDTGuard.EqualityGuard(s1, r2), new SDT(Map.of(
                        new SDTGuard.EqualityGuard(s2, r1), SDTLeaf.REJECTING,
                        new SDTGuard.DisequalityGuard(s2, r1), SDTLeaf.ACCEPTING)),
                new SDTGuard.SDTAndGuard(s1, new SDTGuard.DisequalityGuard(s1, r1), new SDTGuard.DisequalityGuard(s1, r2)), new SDT(Map.of(
                        new SDTGuard.SDTTrueGuard(s2), SDTLeaf.REJECTING))));

        SDT sdt2 = new SDT(Map.of(
                new SDTGuard.EqualityGuard(s1, r1), SDTLeaf.ACCEPTING,
                new SDTGuard.EqualityGuard(s1, r2), SDTLeaf.ACCEPTING,
				new SDTGuard.SDTAndGuard(s1, new SDTGuard.DisequalityGuard(s1, r1), new SDTGuard.DisequalityGuard(s1, r2)), SDTLeaf.REJECTING));

        oracle.changeSDT(sdt1, vals1, consts);
        SDT actualSDT1 = mto.treeQuery(prefix1, suffix1).sdt();
        Assert.assertTrue(actualSDT1.isEquivalent(sdt1, new VarMapping()));

        oracle.changeSDT(sdt2, vals1);
        SDT actualSDT2 = mto.treeQuery(prefix2, suffix2).sdt();
        Assert.assertTrue(actualSDT2.isEquivalent(sdt2, new VarMapping()));
    }

    @Test
    public void branchMergingIneqTest() {
        final DataType D_TYPE = new DataType("double", BigDecimal.class);
        final InputSymbol A = new InputSymbol("a", new DataType[] {D_TYPE});

        SDTOracle oracle = new SDTOracle();
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        teachers.put(D_TYPE, dit);

        Constants consts = new Constants();
        JConstraintsConstraintSolver solver = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(oracle, teachers, consts, solver);

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        ParameterGenerator pgen = new ParameterGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        SuffixValue s1 = svgen.next(D_TYPE);
        SuffixValue s2 = svgen.next(D_TYPE);
        Parameter p1 = pgen.next(D_TYPE);
        Parameter p2 = pgen.next(D_TYPE);
        Parameter p3 = pgen.next(D_TYPE);
        Register r1 = rgen.next(D_TYPE);
        Register r2 = rgen.next(D_TYPE);
        Register r3 = rgen.next(D_TYPE);
        DataValue dv1 = new DataValue(D_TYPE, BigDecimal.ONE);
        DataValue dv2 = new DataValue(D_TYPE, BigDecimal.valueOf(2));
        DataValue dv3 = new DataValue(D_TYPE, BigDecimal.valueOf(3));

        Word<PSymbolInstance> prefix1 = Word.fromSymbols(new PSymbolInstance(A, dv1));
        Word<ParameterizedSymbol> suffixActions1 = Word.fromSymbols(A, A);
        SymbolicSuffix suffix1 = new SymbolicSuffix(suffixActions1);

        Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(A, dv1),
                new PSymbolInstance(A, dv2));
        Word<ParameterizedSymbol> suffixActions2 = Word.fromSymbols(A);
        SymbolicSuffix suffix2 = new SymbolicSuffix(suffixActions2);

        PIV piv1 = new PIV();
        piv1.put(p1, r1);
        Mapping<SymbolicDataValue, DataValue<?>> vals1 = new Mapping<SymbolicDataValue, DataValue<?>>();
        vals1.put(r1, dv1);
        PIV piv2 = new PIV();
        piv2.put(p1, r1);
        piv2.put(p2, r2);
        Mapping<SymbolicDataValue, DataValue<?>> vals2 = new Mapping<SymbolicDataValue, DataValue<?>>();
        vals2.put(r1, dv1);
        vals2.put(r2, dv2);

        SDT sdt1 = new SDT(Map.of(
                new SDTTrueGuard(s1), new SDT(Map.of(
                        new SDTAndGuard(s2,
                                new IntervalGuard(s2, s1, null),
                                new DisequalityGuard(s2, r1)),
                        SDTLeaf.ACCEPTING,
                        new IntervalGuard(s2, null, s1), SDTLeaf.REJECTING,
                        new DisequalityGuard(s2, s1), SDTLeaf.REJECTING,
                        new EqualityGuard(s2, r1), SDTLeaf.REJECTING))));

        SDT sdt2 = new SDT(Map.of(
                new EqualityGuard(s1, r1), SDTLeaf.ACCEPTING,
                new EqualityGuard(s1, r2), SDTLeaf.ACCEPTING,
                new SDTAndGuard(s1, new DisequalityGuard(s1, r1), new DisequalityGuard(s1, r2)), SDTLeaf.REJECTING));

        oracle.changeSDT(sdt1, vals1);
        TreeQueryResult tqr1 = mto.treeQuery(prefix1, suffix1);
        SDT actualSdt1 = (SDT)mto.treeQuery(prefix1, suffix1).getSdt();

        oracle.changeSDT(sdt2, vals2);
        TreeQueryResult tqr2 = mto.treeQuery(prefix2, suffix2);
        SDT actualSdt2 = (SDT)tqr2.getSdt();

        Assert.assertTrue(equivalentSDTs(actualSdt1, tqr1.getPiv(), (SDT)sdt1, piv1, mto, prefix1, suffix1, dit, D_TYPE));
        Assert.assertTrue(equivalentSDTs(actualSdt2, tqr2.getPiv(), (SDT)sdt2, piv2, mto, prefix2, suffix2, dit, D_TYPE));
    }

    private boolean equivalentSDTs(SDT sdt1, PIV piv1, SDT sdt2, PIV piv2,
            TreeOracle oracle, Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            DoubleInequalityTheory theory, DataType type) {
            Constants consts = new Constants();
        Set<DataValue<BigDecimal>> prefixValsCast = new LinkedHashSet<>();
        List<DataValue<BigDecimal>> prefixVals = new ArrayList<>();
        ParValuation prefixPars = DataWords.computeParValuation(prefix);
        for (DataValue<?> dv : prefixPars.values()) {
            if (dv.getId() instanceof BigDecimal) {
                prefixValsCast.add(new DataValue<BigDecimal>(dv.getType(), (BigDecimal)dv.getId()));
            }
        }
        prefixVals.addAll(prefixValsCast);
        prefixVals.sort((DataValue<BigDecimal> d1, DataValue<BigDecimal> d2) -> d1.getId().compareTo(d2.getId()));
        Set<Mapping<SuffixValue, DataValue<BigDecimal>>> equivClasses = genIneqEquiv(
                prefixVals,
                new Mapping<SuffixValue, DataValue<BigDecimal>>(),
                1, suffix.getValues().size(),
                theory, type);

        Mapping<Register, DataValue<BigDecimal>> regMap1 = getRegisterVals(prefixPars, piv1);
        Mapping<Register, DataValue<BigDecimal>> regMap2 = getRegisterVals(prefixPars, piv2);

        for (Mapping<SuffixValue, DataValue<BigDecimal>> ec : equivClasses) {
            Mapping<SymbolicDataValue, DataValue<?>> mapping1 = new Mapping<>();
            Mapping<SymbolicDataValue, DataValue<?>> mapping2 = new Mapping<>();
            mapping1.putAll(ec);
            mapping2.putAll(ec);
            mapping1.putAll(regMap1);
            mapping2.putAll(regMap2);

            boolean o1 = sdt1.isAccepting(mapping1, consts);
            boolean o2 = sdt2.isAccepting(mapping2, consts);
            if (o1 != o2) {
                return false;
            }
        }
        return true;
    }

    private Set<Mapping<SuffixValue, DataValue<BigDecimal>>> genIneqEquiv(List<DataValue<BigDecimal>> prefixVals,
            Mapping<SuffixValue, DataValue<BigDecimal>> prior,
            int idx, int suffixValues,
            DoubleInequalityTheory theory, DataType type) {
        Set<Mapping<SuffixValue, DataValue<BigDecimal>>> ret = new LinkedHashSet<>();
        SuffixValue sv = new SuffixValue(type, idx);

        Set<DataValue<BigDecimal>> potSet = new LinkedHashSet<>();
        List<DataValue<BigDecimal>> potential = new ArrayList<>();
        potSet.addAll(prefixVals);
        potSet.addAll(prior.values());
        potential.addAll(potSet);
        potential.sort((DataValue<BigDecimal> d1, DataValue<BigDecimal> d2) -> d1.getId().compareTo(d2.getId()));
        List<DataValue<BigDecimal>> intervals = generateInterval(potential, theory, type);

        for (DataValue<BigDecimal> dv : intervals) {
            Mapping<SuffixValue, DataValue<BigDecimal>> mapping = new Mapping<>();
            mapping.putAll(prior);
            mapping.put(sv, dv);
            if (idx == suffixValues) {
                ret.add(mapping);
            } else {
                ret.addAll(genIneqEquiv(prefixVals, mapping, idx+1, suffixValues, theory, type));
            }
        }
        return ret;
    }

    private List<DataValue<BigDecimal>> generateInterval(List<DataValue<BigDecimal>> potential, DoubleInequalityTheory theory, DataType type) {
        int n = potential.size();
        ArrayList<DataValue<BigDecimal>> intervals = new ArrayList<>(2*n+1);
        Constants consts = new Constants();
        Register r1 = new Register(type, 1);
        Register r2 = new Register(type, 2);
        SuffixValue sv = new SuffixValue(type, 1);
        IntervalGuard sg = new IntervalGuard(sv, null, r1);
        IntervalGuard ig = new IntervalGuard(sv, r1, r2);
        IntervalGuard gg = new IntervalGuard(sv, r2, null);

        Valuation valLeast = new Valuation();
        valLeast.setValue(JContraintsUtil.toVariable(r1), potential.get(0).getId());
        intervals.add(theory.instantiate(sg, valLeast, consts, intervals));

        for (int i = 0; i < n-1; i++) {
            intervals.add(potential.get(i));

            Valuation val = new Valuation();
            val.setValue(JContraintsUtil.toVariable(r1), potential.get(i).getId());
            val.setValue(JContraintsUtil.toVariable(r2), potential.get(i+1).getId());
            intervals.add(theory.instantiate(ig, val, consts, intervals));
        }
        intervals.add(potential.get(n-1));

        Valuation valGreatest = new Valuation();
        valGreatest.setValue(JContraintsUtil.toVariable(r2), potential.get(n-1).getId());
        intervals.add(theory.instantiate(gg, valGreatest, consts, intervals));

        return intervals;
    }

    private Mapping<Register, DataValue<BigDecimal>> getRegisterVals(ParValuation pars, PIV piv) {
        Mapping<Register, DataValue<BigDecimal>> mapping = new Mapping<>();
        for (Map.Entry<Parameter, Register> e : piv.entrySet()) {
            DataValue<?> val = pars.get(e.getKey());
            if (val != null && val.getId() instanceof BigDecimal) {
                mapping.put(e.getValue(), new DataValue<BigDecimal>(val.getType(), (BigDecimal)val.getId()));
            }
        }
        return mapping;
    }

 */
}
