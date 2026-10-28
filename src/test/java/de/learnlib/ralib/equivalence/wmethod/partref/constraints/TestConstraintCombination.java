package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import java.util.Optional;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;

public class TestConstraintCombination extends RaLibTestSuite {

    private static final DataType TINT = new DataType("int");

    @Test
    public void testCombine() {
        Register x1 = new Register(TINT, 1);
        Register x2 = new Register(TINT, 2);
        SDV d1 = new SDV(TINT, 1);
        SDV d2 = new SDV(TINT, 2);
        SDV d3 = new SDV(TINT, 3);
        SDV d4 = new SDV(TINT, 4);

        Constraint c1 = Constraint.construct(Constraint.construct(d1, Relation.EQ, x1),
                Constraint.construct(d2, Relation.EQ, x2),
                Constraint.construct(d3, Relation.EQ, x2),
                Constraint.construct(d4, Relation.NEQ, x1));
        Constraint c2 = Constraint.construct(Constraint.construct(d1, Relation.NEQ, x1),
                Constraint.construct(d2, Relation.NEQ, x2),
                Constraint.construct(d3, Relation.EQ, x2),
                Constraint.construct(d4, Relation.NEQ, x1));
        Constraint exp1 = Constraint.construct(Constraint.construct(d3, Relation.EQ, x2),
                Constraint.construct(d4, Relation.NEQ, x1));
        Optional<Constraint> act1Opt = Constraint.combine(c1, c2);
        Optional<Constraint> act2Opt = Constraint.combine(c2, c1);
        Assert.assertTrue(act1Opt.isPresent());
        Assert.assertEquals(act1Opt.get(), exp1);
        Assert.assertTrue(act2Opt.isPresent());
        Assert.assertEquals(act2Opt.get(), exp1);

        Constraint c3 = Constraint.construct(Constraint.construct(d1, Relation.NEQ, x1),
                Constraint.construct(d2, Relation.NEQ, x2),
                Constraint.construct(d3, Relation.EQ, x2),
                Constraint.construct(d4, Relation.NEQ, x2));
        Optional<Constraint> act3Opt = Constraint.combine(c1, c3);
        Assert.assertTrue(act3Opt.isEmpty());
    }
}
