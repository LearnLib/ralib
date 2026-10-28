package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation;
import de.learnlib.ralib.words.InputSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class TestIsAcceptable extends RaLibTestSuite {

    private static final DataType T = new DataType("t");
    private static final InputSymbol A = new InputSymbol("a", T);
    private static final InputSymbol B = new InputSymbol("b");

    @Test
    public void testIsAcceptableOnSingletonState() {
        RaModel model = new RaModel(buildAutomaton(), false);
        SplittingTree tree = new SplittingTree(model, A, B);

        Location[] locs = {model.getLocation(0), model.getLocation(1)};

        Register r1 = new Register(T, 1);
        SDV d1 = new SDV(T, 1);
        DSymbolInstance b = new DSymbolInstance(B);
        DSymbolInstance a = new DSymbolInstance(A, new SymbolicDataVariable[] {d1});

        Leaf l0t = new Leaf(Set.of(new SymbolicState(locs[0], Constraint.TRUE)));
        Leaf l1t = new Leaf(Set.of(new SymbolicState(locs[1], Constraint.TRUE)));
        Leaf l1eq = new Leaf(Set.of(new SymbolicState(locs[1], Constraint.construct(d1, Relation.EQ, r1))));
        Leaf l1ne = new Leaf(Set.of(new SymbolicState(locs[1], Constraint.construct(d1, Relation.NEQ, r1))));

        boolean success = tree.splitByOutput(tree.getLeaves().getFirst(), b);
        Assert.assertTrue(success);
        Assert.assertTrue(tree.getLeaves().contains(l0t));
        Assert.assertTrue(tree.getLeaves().contains(l1t));

        Assert.assertFalse(tree.isAcceptable());

        success = tree.splitByOutput(l1t, a);
        Assert.assertTrue(success);
        Assert.assertTrue(tree.getLeaves().contains(l1eq));
        Assert.assertTrue(tree.getLeaves().contains(l1ne));
        Assert.assertTrue(tree.isAcceptable());
        Assert.assertTrue(tree.isStable());
    }

    private RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        // locations
        RALocation l0 = ra.addInitialState(false);
        RALocation l1 = ra.addState(true);

        // registers and parameters
        Parameter p1 = new Parameter(T, 1);
        Register r1 = new Register(T, 1);

        // guards
        Expression<Boolean> geq    = new NumericBooleanExpression(r1, NumericComparator.EQ, p1);
        Expression<Boolean> gneq    = new NumericBooleanExpression(r1, NumericComparator.NE, p1);
        Expression<Boolean> gt   = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register, SymbolicDataValue> r1p1 = new VarMapping<Register, SymbolicDataValue>();
        r1p1.put(r1, p1);
        VarMapping<Register, SymbolicDataValue> r1r1 = new VarMapping<Register, SymbolicDataValue>();
        r1r1.put(r1, r1);

        Assignment store    = new Assignment(r1p1);
        Assignment copy     = new Assignment(r1r1);
        Assignment noAssign = new Assignment(new VarMapping<>());

        // initial location
        ra.addTransition(l0, A, new InputTransition(gt, A, l0, l1, store));
        ra.addTransition(l0, B, new InputTransition(gt, B, l0, l0, noAssign));

        ra.addTransition(l1, A, new InputTransition(geq, A, l1, l1, copy));
        ra.addTransition(l1, A, new InputTransition(gneq, A, l1, l0, noAssign));
        ra.addTransition(l1, B, new InputTransition(gt, B, l1, l1, copy));

        return ra;
    }
}
