package de.learnlib.ralib.equivalence.wmethod.partref;

import static de.learnlib.ralib.equivalence.wmethod.partref.TestSplittingTree.makeBlock;
import static de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint.TRUE;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.A;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.AUTOMATON;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.B;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.C;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.T;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
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

public class TestMultipleGuards extends RaLibTestSuite {

    private static final InputSymbol D = new InputSymbol("d", T);

    @Test
    public void testMultiGuardDisjointedness() {
        RaModel model = new RaModel(AUTOMATON, false);
        SplittingTree tree = new SplittingTree(model, A, B, C, RaModel.EMPTY_INPUT);

        Location[] l = { model.getLocation(0), model.getLocation(1), model.getLocation(2) };

        Leaf b0 = makeBlock(new SymbolicState(l[0], TRUE),
                new SymbolicState(l[1], TRUE),
                new SymbolicState(l[2], TRUE));
        DSymbolInstance in = new DSymbolInstance(RaModel.EMPTY_INPUT);
        boolean success = tree.splitByOutput(b0, in);
        Assert.assertTrue(success);

        Leaf b1 = makeBlock(new SymbolicState(l[0], TRUE),
                new SymbolicState(l[1], TRUE));
        Leaf b2 = makeBlock(new SymbolicState(l[2], TRUE));
        Assert.assertTrue(tree.getLeaves().contains(b1));
        Assert.assertTrue(tree.getLeaves().contains(b2));

        SDV d1 = new SDV(T, 1);
        SDV d2 = new SDV(T, 2);
        in = new DSymbolInstance(B, new SymbolicDataVariable[] {d1, d2});
        success = tree.splitByOutput(b1, in);
        Assert.assertTrue(success);
    }

    @Test
    public void testMultiguardGrouping() {
        RaModel model = new RaModel(buildGroupTestAutomaton(), false);
        SplittingTree tree = new SplittingTree(model, D);

        Location[] l = { model.getLocation(0), model.getLocation(1), model.getLocation(2) };
        Register r1 = new Register(T, 1);
        Register r2 = new Register(T, 2);

        Leaf b0 = makeBlock(new SymbolicState(l[0], TRUE),
                new SymbolicState(l[1], TRUE),
                new SymbolicState(l[2], TRUE));
        DSymbolInstance in = new DSymbolInstance(RaModel.EMPTY_INPUT);
        boolean success = tree.splitByOutput(b0, in);
        Assert.assertTrue(success);

        Leaf b1 = makeBlock(new SymbolicState(l[0], TRUE));
        Leaf b2 = makeBlock(new SymbolicState(l[1], TRUE),
                new SymbolicState(l[2], TRUE));
        Assert.assertTrue(tree.getLeaves().contains(b1));
        Assert.assertTrue(tree.getLeaves().contains(b2));

        SymbolicDataVariable d1 = new QuantifiedSDV(T, 1);
        in = new DSymbolInstance(D, new SymbolicDataVariable[] {d1});
        success = tree.splitByOutput(b2, in);

        Constraint d1Er1 = Constraint.construct(d1, Relation.EQ, r1);
        Constraint d1Er2 = Constraint.construct(d1, Relation.EQ, r2);
        Leaf b2_1 = makeBlock(new SymbolicState(l[1], TRUE));
        Leaf b2_2 = makeBlock(new SymbolicState(l[2], Constraint.construct(d1Er1)));
        Leaf b2_3 = makeBlock(new SymbolicState(l[2], Constraint.construct(d1Er2)));
        Assert.assertTrue(tree.getLeaves().contains(b2_1));
        Assert.assertTrue(tree.getLeaves().contains(b2_2));
        Assert.assertTrue(tree.getLeaves().contains(b2_3));
    }

    private RegisterAutomaton buildGroupTestAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        RALocation l0 = ra.addInitialState(true);
        RALocation l1 = ra.addState(false);
        RALocation l2 = ra.addState(false);

        Parameter p1 = new Parameter(T, 1);
        Register r1 = new Register(T, 1);
        Register r2 = new Register(T, 2);

        Expression<Boolean> gt = ExpressionUtil.TRUE;
        Expression<Boolean> geq1 = new NumericBooleanExpression(p1, NumericComparator.EQ, r1);
        Expression<Boolean> geq2 = new NumericBooleanExpression(p1, NumericComparator.EQ, r2);
        Expression<Boolean> gne1 = new NumericBooleanExpression(p1, NumericComparator.NE, r1);
        Expression<Boolean> gne2 = new NumericBooleanExpression(p1, NumericComparator.NE, r2);
        Expression<Boolean> gneq = ExpressionUtil.and(gne1, gne2);

        VarMapping<Register, SymbolicDataValue> r1p1 = new VarMapping<>();
        r1p1.put(r1, p1);
        VarMapping<Register, SymbolicDataValue> r1r1r2p1 = new VarMapping<>();
        r1r1r2p1.put(r1, r1);
        r1r1r2p1.put(r2, p1);

        Assignment store1 = new Assignment(r1p1);
        Assignment store2 = new Assignment(r1r1r2p1);
        Assignment none = new Assignment(new VarMapping<>());

        ra.addTransition(l0, D, new InputTransition(gt, D, l0, l1, store1));
        ra.addTransition(l1, D, new InputTransition(gt, D, l1, l2, store2));
        ra.addTransition(l2, D, new InputTransition(geq1, D, l2, l0, none));
        ra.addTransition(l2, D, new InputTransition(geq2, D, l2, l1, store1));
        ra.addTransition(l2, D, new InputTransition(gneq, D, l2, l2, store2));

        return ra;
    }
}
