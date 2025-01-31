package de.learnlib.ralib.theory.inequality;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class IneqTheoryInstantiateTest extends RaLibTestSuite {

	@Test
	public void testInstantiate() {

		final DataType D_TYPE = new DataType("double", BigDecimal.class);
        final InputSymbol A = new InputSymbol("a", new DataType[] {D_TYPE, D_TYPE});

        final Map<DataType, Theory<BigDecimal>> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        teachers.put(D_TYPE, dit);

        DataValue<BigDecimal> dv0 = new DataValue<>(D_TYPE, BigDecimal.ZERO);
        DataValue<BigDecimal> dv1 = new DataValue<>(D_TYPE, BigDecimal.ONE);
        DataValue<BigDecimal> dv2 = new DataValue<>(D_TYPE, BigDecimal.valueOf(2));
        DataValue<BigDecimal> dv3 = new DataValue<>(D_TYPE, BigDecimal.valueOf(3));
        DataValue<BigDecimal> dv4 = new DataValue<>(D_TYPE, BigDecimal.valueOf(4));
        DataValue<BigDecimal> dv5 = new DataValue<>(D_TYPE, BigDecimal.valueOf(5));

        Parameter p1 = new Parameter(D_TYPE, 1);
        Parameter p2 = new Parameter(D_TYPE, 2);
        Parameter p3 = new Parameter(D_TYPE, 3);
        Parameter p4 = new Parameter(D_TYPE, 4);
        Register r1 = new Register(D_TYPE, 1);
        Register r2 = new Register(D_TYPE, 2);
        Register r3 = new Register(D_TYPE, 3);
        SuffixValue s1 = new SuffixValue(D_TYPE, 1);
        SuffixValue s2 = new SuffixValue(D_TYPE, 2);
        Constant c1 = new Constant(D_TYPE, 1);

        ParameterGenerator pgen = new ParameterGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        Word<PSymbolInstance> prefix = Word.fromSymbols(
        		new PSymbolInstance(A, dv0, dv1),
        		new PSymbolInstance(A, dv2, dv3));
        ParameterizedSymbol ps = prefix.firstSymbol().getBaseSymbol();
        PIV piv = new PIV();
        piv.put(p1, r1);
        piv.put(p2, r2);
        piv.put(p3, r3);
        ParValuation pars = new ParValuation();
        pars.put(p1, dv4);
        Constants consts = new Constants();
        consts.put(c1, dv5);

        // test equality
        // equal to prefix value
        SDTGuard gEqR = new EqualityGuard(s1, r1);
        DataValue<?> dvEqR = dit.instantiate(prefix, ps, piv, new ParValuation(), new Constants(), gEqR, p4, new LinkedHashSet<>());
        Assert.assertEquals(dvEqR.getId(), dv0.getId());
        // equal to suffix value
        SDTGuard gEqS = new EqualityGuard(s2, s1);
        DataValue<?> dvEqS = dit.instantiate(prefix, ps, piv, pars, new Constants(), gEqS, p4, new LinkedHashSet<>());
        Assert.assertEquals(dvEqS.getId(), dv4.getId());
        // equal to constant
        SDTGuard gEqC = new EqualityGuard(s1, c1);
        DataValue<?> dvEqC = dit.instantiate(prefix, ps, piv, new ParValuation(), consts, gEqC, p4, new LinkedHashSet<>());
        Assert.assertEquals(dvEqC.getId(), dv5.getId());

        // test disequality
        SDTGuard gDeq = new DisequalityGuard(s1, r1);
        DataValue<?> dvDeq = dit.instantiate(prefix, ps, piv, new ParValuation(), new Constants(), gDeq, p4, new LinkedHashSet<>());
        Assert.assertNotEquals(dvDeq.getId(), dv0.getId());

        // test fresh
        SDTGuard gTrue = new SDTTrueGuard(s1);
        DataValue<?> dvTrue = dit.instantiate(prefix, ps, piv, new ParValuation(), new Constants(), gTrue, p4, new LinkedHashSet<>());
        Assert.assertTrue(isFresh(dvTrue, dv0, dv1, dv2, dv3));

        // test interval and and-guard
        SDTGuard gAnd = new SDTAndGuard(s1,
        		IntervalGuard.greaterGuard(s1, r1),
        		new IntervalGuard(s1, r2, r3));
        DataValue<?> dAnd = teachers.get(D_TYPE).instantiate(prefix, ps, piv, new ParValuation(), new Constants(), gAnd, p4, new LinkedHashSet<DataValue<BigDecimal>>());
        BigDecimal val = (BigDecimal)dAnd.getId();
        Assert.assertTrue(val.compareTo(dv0.getId()) == 1);
        Assert.assertTrue(val.compareTo(dv1.getId()) == 1);
        Assert.assertTrue(val.compareTo(dv2.getId()) == -1);

        // test or-guard and equalities
        SDTGuard gOr = new SDTOrGuard(s1,
        		new EqualityGuard(s1, r1),
        		new EqualityGuard(s1, r2));
        DataValue<?> dOr = teachers.get(D_TYPE).instantiate(prefix, ps, piv, new ParValuation(), new Constants(), gOr, p4, new LinkedHashSet<DataValue<BigDecimal>>());
        val = (BigDecimal)dOr.getId();
        Assert.assertTrue(val.equals(dv0.getId()) || val.equals(dv1.getId()));
	}

	private boolean isFresh(DataValue<?> dataValue, DataValue<?>... dataValues) {
		return !Arrays.stream(dataValues).anyMatch((dv) -> (dv.getId().equals(dataValue.getId())));
	}
}
