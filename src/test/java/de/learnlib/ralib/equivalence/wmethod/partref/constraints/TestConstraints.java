package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;

public class TestConstraints extends RaLibTestSuite {

    private DataType TINT = new DataType("int");

    @Test
    public void testConstruction() {
        Register x1 = new Register(TINT, 1);
        Register x2 = new Register(TINT, 2);
        Register x3 = new Register(TINT, 3);
        SDV d1 = new SDV(TINT, 1);
        SDV d2 = new SDV(TINT, 2);
        QuantifiedSDV D1 = new QuantifiedSDV(TINT, 1);

        Map<SymbolicDataVariable, SymbolicDataValue> eq = new LinkedHashMap<>();
        eq.put(d1, x1);
        eq.put(d2, x2);
        eq.put(D1, x3);

        AtomicConstraint a1 = Constraint.construct(d1, Relation.EQ, x1);
        AtomicConstraint a2 = Constraint.construct(d2, Relation.EQ, x2);
        AtomicConstraint a3 = Constraint.construct(d2, Relation.NEQ, x1);
        AtomicConstraint a4 = Constraint.construct(d1, Relation.NEQ, x2);
        AtomicConstraint a5 = Constraint.construct(D1, Relation.EQ, x3);

        Constraint c1 = Constraint.construct(eq);
        Constraint c2 = Constraint.construct(a3, a4);
        Constraint c3 = Constraint.construct(c1, c2, a5);
        Constraint c4 = Constraint.construct(a1, a2, a3, a4, a5);

        Assert.assertEquals(c3, c4);
    }

    @Test
    public void testSatisfiability() {
        Register x1 = new Register(TINT, 1);
        Register x2 = new Register(TINT, 2);
        Register x3 = new Register(TINT, 3);
        SDV d1 = new SDV(TINT, 1);
        SDV d2 = new SDV(TINT, 2);

        Constraint c1 = Constraint.construct(
                Constraint.construct(d1, Relation.EQ, x1),
                Constraint.construct(d1, Relation.EQ, x2),
                Constraint.construct(d2, Relation.EQ, x3),
                Constraint.construct(d1, Relation.NEQ, d2));
        Constraint c2 = Constraint.construct(
                Constraint.construct(d1, Relation.EQ, x1),
                Constraint.construct(d1, Relation.EQ, x3),
                Constraint.construct(d2, Relation.EQ, x2),
                Constraint.construct(d2, Relation.EQ, x3),
                Constraint.construct(d1, Relation.NEQ, d2));

        Assert.assertTrue(Constraint.isSatisfiable(c1));
        Assert.assertFalse(Constraint.isSatisfiable(c2));
    }

    @Test
    public void testImplication() {
        Register x1 = new Register(TINT, 1);
        Register x2 = new Register(TINT, 2);
        SDV d1 = new SDV(TINT, 1);
        SDV d2 = new SDV(TINT, 2);

        Constraint c1 = Constraint.construct(
                Constraint.construct(d1, Relation.EQ, x1),
                Constraint.construct(d2, Relation.EQ, x1),
                Constraint.construct(d2, Relation.NEQ, x2));
        Constraint c2 = Constraint.construct(
                Constraint.construct(d1, Relation.EQ, d2),
                Constraint.construct(d1, Relation.NEQ, x2));

        Assert.assertTrue(Constraint.implies(c1, c2));
        Assert.assertTrue(Constraint.implies(c1, Constraint.trueConstraint()));
    }
}
