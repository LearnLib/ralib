package de.learnlib.ralib.equivalence.wmethod.partref;

import static de.learnlib.ralib.equivalence.wmethod.partref.TestSplittingTree.makeBlock;
import static de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint.TRUE;
import static de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint.construct;
import static de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation.EQ;
import static de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation.NEQ;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.FreshSDV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class TestDisequalitySplit extends RaLibTestSuite {

    private static final DataType T = new DataType("t");
    private static final InputSymbol A = new InputSymbol("a", T);
    private static final OutputSymbol Y = new OutputSymbol("y");
    private static final OutputSymbol N = new OutputSymbol("n");

    @Test
    public void testDisequalitySplit() {
        RaModel model = new RaModel(buildAutomaton(), true);
        SplittingTree tree = new SplittingTree(model, A);

        Location[] l = {model.getLocation(0), model.getLocation(2), model.getLocation(5)};

        DSymbolInstance n = new DSymbolInstance(N);

        Register r1 = new Register(T, 1);
        SDV d1 = new SDV(T, 1);
        DSymbolInstance in = new DSymbolInstance(A, new SymbolicDataVariable[] {d1});

        boolean success = tree.splitByOutput(tree.getLeaves().getFirst(), in);
        Assert.assertTrue(success);

        Constraint d1Er1 = construct(d1, EQ, r1);
        Constraint d1Nr1 = construct(d1, NEQ, r1);
        Leaf b1 = makeBlock(new SymbolicState(l[0], TRUE),
                new SymbolicState(l[1], construct(d1Er1)),
                new SymbolicState(l[2], construct(d1Er1)));
        Leaf b2 = makeBlock(new SymbolicState(l[1], construct(d1Nr1)),
                new SymbolicState(l[2], construct(d1Nr1)));
        Assert.assertTrue(tree.getLeaves().contains(b1));
        Assert.assertTrue(tree.getLeaves().contains(b2));

        SDV d2 = new SDV(T, 2);
        in = new DSymbolInstance(A, new SymbolicDataVariable[] {d2});
        success = tree.splitByOutput(b1, in) || tree.splitByOutput(b2, in);
        Assert.assertFalse(success);

        FreshSDV f1 = new FreshSDV(T, 1);
        in = new DSymbolInstance(A, new SymbolicDataVariable[] {f1});
        success = tree.splitByOutput(b1, in);
        Assert.assertTrue(success);

        Constraint fr1 = construct(f1, NEQ, r1);
        Leaf b1_1 = makeBlock(new SymbolicState(l[0], TRUE));
        Leaf b1_2 = makeBlock(new SymbolicState(l[1], construct(d1Er1, fr1)),
                new SymbolicState(l[2], construct(d1Er1, fr1)));
        Assert.assertTrue(tree.getLeaves().contains(b1_1));
        Assert.assertTrue(tree.getLeaves().contains(b1_2));

        Assert.assertTrue(tree.isAcceptable());
        Assert.assertFalse(tree.isStable());

        FreshSDV f2 = new FreshSDV(T, 2);
        in = new DSymbolInstance(A, new SymbolicDataVariable[] {f2});
        success = tree.splitByState(b1_2, in, n);
        Assert.assertTrue(success);
    }

    private RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        // locations
        RALocation l0 = ra.addInitialState();
        RALocation l0y = ra.addState();
        RALocation l1 = ra.addState();
        RALocation l1y = ra.addState();
        RALocation l1n = ra.addState();
        RALocation l2 = ra.addState();
        RALocation l2y = ra.addState();
        RALocation l2n = ra.addState();

        // registers and parameters
        Parameter p1 = new Parameter(T, 1);
        Register r1 = new Register(T, 1);

        // guards
        Expression<Boolean> geq    = new NumericBooleanExpression(r1, NumericComparator.EQ, p1);
        Expression<Boolean> gneq    = new NumericBooleanExpression(r1, NumericComparator.NE, p1);
        Expression<Boolean> gt   = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register, SymbolicDataValue> r1p1 = new VarMapping<>();
        r1p1.put(r1, p1);
        VarMapping<Register, SymbolicDataValue> r1r1 = new VarMapping<>();
        r1r1.put(r1, r1);

        Assignment store    = new Assignment(r1p1);
        Assignment copy     = new Assignment(r1r1);
        Assignment noAssign = new Assignment(new VarMapping<>());

        OutputMapping out = new OutputMapping(List.of(), new VarMapping<>());

        ra.addTransition(l0, A, new InputTransition(gt, A, l0, l0y, store));
        ra.addTransition(l1, A, new InputTransition(geq, A, l1, l1y, copy));
        ra.addTransition(l1, A, new InputTransition(gneq, A, l1, l1n, noAssign));
        ra.addTransition(l2, A, new InputTransition(geq, A, l2, l2y, copy));
        ra.addTransition(l2, A, new InputTransition(gneq, A, l2, l2n, copy));

        ra.addTransition(l0y, Y, new OutputTransition(gt, out, Y, l0y, l1, copy));
        ra.addTransition(l1y, Y, new OutputTransition(gt, out, Y, l1y, l2, copy));
        ra.addTransition(l1n, N, new OutputTransition(gt, out, N, l1n, l0, noAssign));
        ra.addTransition(l2y, Y, new OutputTransition(gt, out, Y, l2y, l2, copy));
        ra.addTransition(l2n, N, new OutputTransition(gt, out, N, l2n, l1, copy));

        return ra;
    }
}
