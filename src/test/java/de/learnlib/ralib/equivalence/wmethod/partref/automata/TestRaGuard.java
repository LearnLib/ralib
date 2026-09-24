package de.learnlib.ralib.equivalence.wmethod.partref.automata;

import java.util.LinkedHashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class TestRaGuard extends RaLibTestSuite {

    private DataType TINT = new DataType("int");

    @Test
    public void testGuardConversion() {
        Parameter p1 = new Parameter(TINT, 1);
        Parameter p2 = new Parameter(TINT, 2);
        Parameter p3 = new Parameter(TINT, 3);
        Register x1 = new Register(TINT, 1);
        Register x2 = new Register(TINT, 2);
        Constant c1 = new Constant(TINT, 1);

        Expression<Boolean> p1Ex1 = new NumericBooleanExpression(p1, NumericComparator.EQ, x1);
        Expression<Boolean> p2Ex2 = new NumericBooleanExpression(p2, NumericComparator.EQ, x2);
        Expression<Boolean> p3Ec1 = new NumericBooleanExpression(p3, NumericComparator.NE, c1);

        RaGuard g1 = new RaEqualityGuard(p1, Relation.EQ, x1);
        RaGuard g2 = new RaEqualityGuard(p2, Relation.EQ, x2);
        RaGuard g3 = new RaEqualityGuard(p3, Relation.NEQ, c1);
        RaGuard g4 = new RaTrueGuard();

        Expression<Boolean> con1 = ExpressionUtil.and(p2Ex2, p3Ec1);
        Expression<Boolean> con2 = ExpressionUtil.and(p1Ex1, con1);
        Expression<Boolean> con3 = ExpressionUtil.and(con2, ExpressionUtil.TRUE);

        Set<RaGuard> actual1 = RaGuard.fromRaGuard(con1);
        Set<RaGuard> actual2 = RaGuard.fromRaGuard(con2);
        Set<RaGuard> actual3 = RaGuard.fromRaGuard(con3);

        Set<RaGuard> expected1 = new LinkedHashSet<>();
        expected1.add(g2);
        expected1.add(g3);
        Set<RaGuard> expected2 = new LinkedHashSet<>(actual1);
        expected2.add(g1);
        Set<RaGuard> expected3 = new LinkedHashSet<>(actual2);
        expected3.add(g4);

        Assert.assertEquals(actual1, expected1);
        Assert.assertEquals(actual2, expected2);
        Assert.assertEquals(actual3, expected3);
    }
}
