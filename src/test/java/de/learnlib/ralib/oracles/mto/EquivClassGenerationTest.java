package de.learnlib.ralib.oracles.mto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.theory.equality.EqualRestriction;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class EquivClassGenerationTest extends RaLibTestSuite {

	// TODO: add similar test for equality theory

	@Test
	public void eqcGenIneqTheoryTest() {
		final DataType D_TYPE = new DataType("double", BigDecimal.class);
		final InputSymbol A = new InputSymbol("a", new DataType[] {D_TYPE});

        final Map<DataType, Theory<BigDecimal>> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        dit.useSuffixOptimization(true);
        teachers.put(D_TYPE, dit);

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        ParameterGenerator pgen = new ParameterGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        SuffixValue s1 = svgen.next(D_TYPE);
        SuffixValue s2 = svgen.next(D_TYPE);
        Parameter p1 = pgen.next(D_TYPE);
        Parameter p2 = pgen.next(D_TYPE);
        Register r1 = rgen.next(D_TYPE);
        Register r2 = rgen.next(D_TYPE);

        Constants consts = new Constants();
        PIV piv = new PIV();
        piv.put(p1, r1);
        piv.put(p2, r2);

        DataValue<BigDecimal> dv0 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ZERO);
        DataValue<BigDecimal> dv1 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ONE);
        DataValue<BigDecimal> dv2 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(2));
        DataValue<BigDecimal> dv3 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(3));
        DataValue<BigDecimal> dv4 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(4));
        DataValue<BigDecimal> dv5 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(5));
        DataValue<BigDecimal> dv6 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(6));

        Word<PSymbolInstance> prefix1 = Word.fromSymbols(
        		new PSymbolInstance(A, dv3),
        		new PSymbolInstance(A, dv5));
        Word<ParameterizedSymbol> suffixActions1 = Word.fromSymbols(A, A);

        Map<SuffixValue, SuffixValueRestriction> restr1 = new LinkedHashMap<>();
        restr1.put(s1, new UnrestrictedSuffixValue(s1));
        restr1.put(s2, new UnrestrictedSuffixValue(s2));
        SymbolicSuffix suffix1 = new SymbolicSuffix(suffixActions1, restr1);

        Map<DataValue<BigDecimal>, SymbolicDataValue> potValuation1 = new LinkedHashMap<>();
        potValuation1.put(dv1, s1);
        potValuation1.put(dv3, r1);
        potValuation1.put(dv5, r2);
        SuffixValuation suffixVals1 = new SuffixValuation();
        suffixVals1.put(s1, dv1);

        Map<DataValue<BigDecimal>, SDTGuard> valueGuardsExpected1 = new LinkedHashMap<>();
        valueGuardsExpected1.put(dv0, new IntervalGuard(s2, null, s1));
        valueGuardsExpected1.put(dv1, new EqualityGuard(s2, s1));
        valueGuardsExpected1.put(dv2, new IntervalGuard(s2, s1, r1));
        valueGuardsExpected1.put(dv3, new EqualityGuard(s2, r1));
        valueGuardsExpected1.put(dv4, new IntervalGuard(s2, r1, r2));
        valueGuardsExpected1.put(dv5, new EqualityGuard(s2, r2));
        valueGuardsExpected1.put(dv6, new IntervalGuard(s2, r2, null));
        Map<DataValue<BigDecimal>, SDTGuard> valueGuardsActual1 = dit.equivalenceClasses(prefix1, suffix1, s2, potValuation1, suffixVals1, consts);

        Assert.assertEquals(valueGuardsActual1.size(), valueGuardsExpected1.size());
        Assert.assertTrue(valueGuardsActual1.entrySet().containsAll(valueGuardsExpected1.entrySet()));

        Word<PSymbolInstance> prefix2 = prefix1;
        Word<ParameterizedSymbol> suffixActions2 = suffixActions1;
        Map<SuffixValue, SuffixValueRestriction> restr2 = new LinkedHashMap<>();
        // TODO: change s1 from fresh to greatest?
        restr2.put(s1, new FreshSuffixValue(s1));
        restr2.put(s2, new EqualRestriction(s2, s1));
        SymbolicSuffix suffix2 = new SymbolicSuffix(suffixActions2, restr2);
        Map<DataValue<BigDecimal>, SymbolicDataValue> potValuation2 = potValuation1;
        SuffixValuation suffixVals2 = suffixVals1;

        Map<DataValue<BigDecimal>, SDTGuard> valueGuardsExpected2 = new LinkedHashMap<>();
        valueGuardsExpected2.put(dv1, new EqualityGuard(s2, s1));
        Map<DataValue<BigDecimal>, SDTGuard> valueGuardsActual2 = dit.equivalenceClasses(prefix2, suffix2, s2, potValuation2, suffixVals2, consts);

        Assert.assertEquals(valueGuardsActual2.size(), valueGuardsExpected2.size());
        Assert.assertTrue(valueGuardsActual2.entrySet().containsAll(valueGuardsExpected2.entrySet()));

        // empty potential test
        Word<PSymbolInstance> prefix3 = Word.epsilon();
        Word<ParameterizedSymbol> suffixActions3 = Word.fromSymbols(A);
        SymbolicSuffix suffix3 = new SymbolicSuffix(suffixActions3);

        Map<DataValue<BigDecimal>, SDTGuard> valueGuardsActual3 = dit.equivalenceClasses(prefix3, suffix3, s1, new LinkedHashMap<>(), new SuffixValuation(), consts);

        Assert.assertEquals(valueGuardsActual3.size(), 1);
        Assert.assertTrue(valueGuardsActual3.containsValue(new SDTTrueGuard(s1)));
	}
}
