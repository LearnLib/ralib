package de.learnlib.ralib.theory.inequality;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import gov.nasa.jpf.constraints.api.Valuation;

public class IntervalGuardInstaniateTest extends RaLibTestSuite {

    @Test
    public void testInstantiateInterval() {

        final DataType D_TYPE = new DataType("double");

        //final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        //teachers.put(D_TYPE, dit);

        SuffixValue s1 = new SuffixValue(D_TYPE, 1);
        Register r1 = new Register(D_TYPE, 1);
        Register r2 = new Register(D_TYPE, 2);

        DataValue dv0 = new DataValue(D_TYPE, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(D_TYPE, BigDecimal.ONE);
        DataValue dv2 = new DataValue(D_TYPE, BigDecimal.valueOf(2));
        DataValue dv3 = new DataValue(D_TYPE, BigDecimal.valueOf(3));
        DataValue dv4 = new DataValue(D_TYPE, BigDecimal.valueOf(4));

        Valuation val = new Valuation();
        val.setValue(r1 , dv1.getValue());
        val.setValue(r2, dv2.getValue());

        Collection<DataValue> alreadyUsed = new ArrayList<>();
        alreadyUsed.add(dv1);
        alreadyUsed.add(dv2);

        Constants consts = new Constants();

        SDTGuard.IntervalGuard lg = SDTGuard.IntervalGuard.lessGuard(s1, dv1);
        SDTGuard.IntervalGuard leg = SDTGuard.IntervalGuard.lessOrEqualGuard(s1, dv1);
        SDTGuard.IntervalGuard rg = SDTGuard.IntervalGuard.greaterGuard(s1, dv1);
        SDTGuard.IntervalGuard reg = SDTGuard.IntervalGuard.greaterOrEqualGuard(s1, dv1);
        SDTGuard.IntervalGuard ig = new SDTGuard.IntervalGuard(s1, dv1, dv2);
        SDTGuard.IntervalGuard igc = new SDTGuard.IntervalGuard(s1, dv1, dv2, true, true);

        DataValue dvl = dit.instantiate(lg, val, consts, alreadyUsed);
        DataValue dvle = dit.instantiate(leg, val, consts, alreadyUsed);
        DataValue dvr = dit.instantiate(rg, val, consts, alreadyUsed);
        DataValue dvre = dit.instantiate(reg, val, consts, alreadyUsed);
        DataValue dvi = dit.instantiate(ig, val, consts, alreadyUsed);
        DataValue dvic = dit.instantiate(igc, val, consts, alreadyUsed);

        Assert.assertEquals(dvl.getValue().compareTo(dv1.getValue()), -1);
        Assert.assertNotEquals(dvle.getValue().compareTo(dv1.getValue()), 1);
        Assert.assertEquals(dvr.getValue().compareTo(dv1.getValue()), 1);
        Assert.assertNotEquals(dvre.getValue().compareTo(dv1.getValue()), -1);
        Assert.assertTrue(dvi.getValue().compareTo(dv1.getValue()) > 0 && dvi.getValue().compareTo(dv3.getValue()) < 0);
        Assert.assertFalse(dvic.getValue().compareTo(dv1.getValue()) < 0 && dvic.getValue().compareTo(dv3.getValue()) > 0);
    }
}
