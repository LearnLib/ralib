package de.learnlib.ralib.theory.inequality;

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import gov.nasa.jpf.constraints.api.Valuation;

public class IntervalGuardInstaniateTest extends RaLibTestSuite {

    @Test
    public void instantiateIntervalTest() {

        final DataType D_TYPE = new DataType("double", BigDecimal.class);

        final Map<DataType, Theory<BigDecimal>> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        teachers.put(D_TYPE, dit);

        SuffixValue s1 = new SuffixValue(D_TYPE, 1);
        Register r1 = new Register(D_TYPE, 1);
        Register r2 = new Register(D_TYPE, 2);

        DataValue<BigDecimal> dv0 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ZERO);
        DataValue<BigDecimal> dv1 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.ONE);
        DataValue<BigDecimal> dv2 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(2));
        DataValue<BigDecimal> dv3 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(3));
        DataValue<BigDecimal> dv4 = new DataValue<BigDecimal>(D_TYPE, BigDecimal.valueOf(4));

        Valuation val = new Valuation();
        val.setValue(toVariable(r1), dv1.getId());
        val.setValue(toVariable(r2), dv2.getId());

        Collection<DataValue<BigDecimal>> alreadyUsed = new ArrayList<>();
        alreadyUsed.add(dv1);
        alreadyUsed.add(dv2);

        Constants consts = new Constants();

        IntervalGuard lg = IntervalGuard.lessGuard(s1, r1);
        IntervalGuard leg = IntervalGuard.lessOrEqualGuard(s1, r1);
        IntervalGuard rg = IntervalGuard.greaterGuard(s1, r1);
        IntervalGuard reg = IntervalGuard.greaterOrEqualGuard(s1, r1);
        IntervalGuard ig = new IntervalGuard(s1, r1, r2);
        IntervalGuard igc = new IntervalGuard(s1, r1, r2, true, true);

        DataValue<BigDecimal> dvl = dit.instantiate(lg, val, consts, alreadyUsed);
        DataValue<BigDecimal> dvle = dit.instantiate(leg, val, consts, alreadyUsed);
        DataValue<BigDecimal> dvr = dit.instantiate(rg, val, consts, alreadyUsed);
        DataValue<BigDecimal> dvre = dit.instantiate(reg, val, consts, alreadyUsed);
        DataValue<BigDecimal> dvi = dit.instantiate(ig, val, consts, alreadyUsed);
        DataValue<BigDecimal> dvic = dit.instantiate(igc, val, consts, alreadyUsed);

        Assert.assertEquals(dvl.getId().compareTo(dv1.getId()), -1);
        Assert.assertNotEquals(dvle.getId().compareTo(dv1.getId()), 1);
        Assert.assertEquals(dvr.getId().compareTo(dv1.getId()), 1);
        Assert.assertNotEquals(dvre.getId().compareTo(dv1.getId()), -1);
        Assert.assertTrue(dvi.getId().compareTo(dv1.getId()) == 1 && dvi.getId().compareTo(dv3.getId()) == -1);
        Assert.assertFalse(dvic.getId().compareTo(dv1.getId()) == -1 && dvic.getId().compareTo(dv3.getId()) == 1);
    }
}
