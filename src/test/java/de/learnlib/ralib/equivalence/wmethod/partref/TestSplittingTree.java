package de.learnlib.ralib.equivalence.wmethod.partref;

import static de.learnlib.ralib.example.stack.IOStackAutomatonExample.O_OK;
import static de.learnlib.ralib.example.stack.PureIOStackAutomatonExample.O_OUT;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.util.LinkedHashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation;
import de.learnlib.ralib.example.stack.IOStackAutomatonExample;
import de.learnlib.ralib.example.stack.PureIOStackAutomatonExample;
import de.learnlib.ralib.example.stack.StackAutomatonExample;

public class TestSplittingTree extends RaLibTestSuite {

    @Test
    public void testSplittingTreeOnStack() {
        RaModel model = new RaModel(StackAutomatonExample.AUTOMATON, false);
        SplittingTree tree = new SplittingTree(model, I_POP, I_PUSH);
        Location[] locs = getLocations(model);

        Register r1 = new Register(T_INT, 1);
        Register r2 = new Register(T_INT, 2);

        Assert.assertFalse(tree.isAcceptable());

        Leaf b0 = makeBlock(new SymbolicState(locs[0], Constraint.TRUE),
                new SymbolicState(locs[1], Constraint.TRUE),
                new SymbolicState(locs[2], Constraint.TRUE),
                new SymbolicState(locs[3], Constraint.TRUE));
        Assert.assertTrue(tree.getLeaves().contains(b0));

        // SPLIT BY OUTPUT

        SDV d1 = new SDV(T_INT, 1);
        DSymbolInstance in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {d1});

        boolean success = tree.splitByOutput(b0, in);
        Assert.assertTrue(success);

        Constraint d1Er1 = Constraint.construct(d1, Relation.EQ, r1);
        Constraint d1Er2 = Constraint.construct(d1, Relation.EQ, r2);
        Constraint d1Nr1 = Constraint.construct(d1, Relation.NEQ, r1);
        Constraint d1Nr2 = Constraint.construct(d1, Relation.NEQ, r2);

        Leaf b1 = makeBlock(new SymbolicState(locs[0], Constraint.TRUE),
                new SymbolicState(locs[1], d1Nr1),
                new SymbolicState(locs[2], d1Nr2),
                new SymbolicState(locs[3], Constraint.TRUE));
        Leaf b2 = makeBlock(new SymbolicState(locs[1], d1Er1),
                new SymbolicState(locs[2], d1Er2));

        Assert.assertTrue(tree.getLeaves().contains(b1));
        Assert.assertTrue(tree.getLeaves().contains(b2));

        SDV d2 = new SDV(T_INT, 2);
        in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {d2});
        success = tree.splitByOutput(b1, in);
        Assert.assertFalse(success);

        in = new DSymbolInstance(I_PUSH, new SymbolicDataVariable[] {d2});
        success = tree.splitByOutput(b2, in);
        Assert.assertTrue(success);

        Leaf b2_1 = makeBlock(new SymbolicState(locs[1], d1Er1));
        Leaf b2_2 = makeBlock(new SymbolicState(locs[2], d1Er2));
        Assert.assertTrue(tree.getLeaves().contains(b2_1));
        Assert.assertTrue(tree.getLeaves().contains(b2_2));
        Assert.assertFalse(tree.isAcceptable());

        success = tree.splitByOutput(b1, in);
        Assert.assertTrue(success);

        Leaf b1_1 = makeBlock(new SymbolicState(locs[0], Constraint.TRUE),
                new SymbolicState(locs[1], d1Nr1));
        Leaf b1_2 = makeBlock(new SymbolicState(locs[2], d1Nr2),
                new SymbolicState(locs[3], Constraint.TRUE));
        Assert.assertTrue(tree.getLeaves().contains(b1_1));
        Assert.assertTrue(tree.getLeaves().contains(b1_2));

        // ACCEPTABLE

        Assert.assertFalse(tree.isAcceptable());

        // SPLIT BY STATE

        QuantifiedSDV dd3 = new QuantifiedSDV(T_INT, 1);
        in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {dd3});
        success = tree.splitByOutput(b1_1, in);
        Assert.assertTrue(success);

        Constraint eEr1 = Constraint.construct(dd3, Relation.EQ, r1);
        Leaf b1_1_1 = makeBlock(new SymbolicState(locs[0], Constraint.TRUE));
        Leaf b1_1_2 = makeBlock(new SymbolicState(locs[1], Constraint.construct(d1Nr1, eEr1)));
        Assert.assertTrue(tree.getLeaves().contains(b1_1_1));
        Assert.assertTrue(tree.getLeaves().contains(b1_1_2));
        Assert.assertFalse(tree.isAcceptable());

        success = tree.splitByOutput(b1_2, in);
        Assert.assertTrue(success);

        Constraint eEr2 = Constraint.construct(dd3, Relation.EQ, r2);
        Leaf b1_2_1 = makeBlock(new SymbolicState(locs[3], Constraint.TRUE));
        Leaf b1_2_2 = makeBlock(new SymbolicState(locs[2], Constraint.construct(d1Nr2, eEr2)));
        Assert.assertTrue(tree.getLeaves().contains(b1_2_1));
        Assert.assertTrue(tree.getLeaves().contains(b1_2_2));

        Assert.assertTrue(tree.isAcceptable());
        Assert.assertFalse(tree.isStable());

        SDV d3 = new SDV(T_INT, 3);
        in = new DSymbolInstance(I_POP, d3);
        DSymbolInstance out = new DSymbolInstance(RaModel.ACC);
        success = tree.splitByState(b2_2, in, out);
        Assert.assertTrue(success);

        // STABLE

        Assert.assertTrue(tree.isStable());
    }

    @Test
    public void testSplittingTreeOnIOStack() {
        RaModel model = new RaModel(IOStackAutomatonExample.AUTOMATON, true);
        Location[] locs = getLocations(model);

        SplittingTree tree = new SplittingTree(model, I_POP, I_PUSH);
        Assert.assertFalse(tree.isAcceptable());

        Register r1 = new Register(T_INT, 1);
        Register r2 = new Register(T_INT, 2);
        final DSymbolInstance OK = new DSymbolInstance(O_OK);

        Leaf b0 = makeBlock(new SymbolicState(locs[0], Constraint.TRUE),
                new SymbolicState(locs[1], Constraint.TRUE),
                new SymbolicState(locs[2], Constraint.TRUE));
        Assert.assertTrue(tree.getLeaves().contains(b0));

        // SPLIT BY OUTPUT

        SDV d1 = new SDV(T_INT, 1);
        DSymbolInstance in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {d1});

        boolean success = tree.splitByOutput(b0, in);
        Assert.assertTrue(success);

        Constraint d1Er1 = Constraint.construct(d1, Relation.EQ, r1);
        Constraint d1Er2 = Constraint.construct(d1, Relation.EQ, r2);
        Constraint d1Nr1 = Constraint.construct(d1, Relation.NEQ, r1);
        Constraint d1Nr2 = Constraint.construct(d1, Relation.NEQ, r2);
        Leaf b1 = makeBlock(new SymbolicState(locs[0], Constraint.TRUE),
                new SymbolicState(locs[1], d1Nr1),
                new SymbolicState(locs[2], d1Nr2));
        Leaf b2 = makeBlock(new SymbolicState(locs[1], d1Er1),
                new SymbolicState(locs[2], d1Er2));
        Assert.assertTrue(tree.getLeaves().contains(b1));
        Assert.assertTrue(tree.getLeaves().contains(b2));

        SDV d2 = new SDV(T_INT, 2);
        in = new DSymbolInstance(I_PUSH, new SymbolicDataVariable[] {d2});
        success = tree.splitByOutput(b2, in);
        Assert.assertFalse(success);

        in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {d2});
        success = tree.splitByOutput(b1, in);
        Assert.assertFalse(success);

        QuantifiedSDV dd1 = new QuantifiedSDV(T_INT, 1);
        in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {dd1});
        success = tree.splitByOutput(b1, in);
        Assert.assertTrue(success);

        Constraint eEr1 = Constraint.construct(dd1, Relation.EQ, r1);
        Constraint eEr2 = Constraint.construct(dd1, Relation.EQ, r2);
        Leaf b1_1 = makeBlock(new SymbolicState(locs[0], Constraint.TRUE));
        Leaf b1_2 = makeBlock(new SymbolicState(locs[1], Constraint.construct(d1Nr1, eEr1)),
                new SymbolicState(locs[2], Constraint.construct(d1Nr2, eEr2)));
        Assert.assertTrue(tree.getLeaves().contains(b1_1));
        Assert.assertTrue(tree.getLeaves().contains(b1_2));

        // ACCEPTABLE

        Assert.assertTrue(tree.isAcceptable());
        Assert.assertFalse(tree.isStable());

        // SPLIT BY STATE

        in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {d2});
        success = tree.splitByState(b2, in, OK);
        Assert.assertTrue(success);

        SDV d3 = new SDV(T_INT, 3);
        Constraint d2Er1 = Constraint.construct(d2, Relation.EQ, r1);
        Constraint d2Er2 = Constraint.construct(d2, Relation.EQ, r2);
        Constraint d3Er1 = Constraint.construct(d3, Relation.EQ, r1);
        Constraint d3Nr1 = Constraint.construct(d3, Relation.NEQ, r1);
        Leaf b2_1 = makeBlock(new SymbolicState(locs[2], Constraint.construct(d1Er2, d2Er2, d3Er1)));
        Leaf b2_2 = makeBlock(new SymbolicState(locs[1], Constraint.construct(d1Er1, d2Er1)),
                new SymbolicState(locs[2], Constraint.construct(d1Er2, d2Er2, d3Nr1)));
        Assert.assertTrue(tree.getLeaves().contains(b2_1));
        Assert.assertTrue(tree.getLeaves().contains(b2_2));

        Assert.assertFalse(tree.isStable());

        SDV d4 = new SDV(T_INT, 4);
        in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {d4});
        success = tree.splitByState(b2_2, in, OK);
        Assert.assertTrue(success);

        SDV d5 = new SDV(T_INT, 5);
        Constraint d4Er1 = Constraint.construct(d4, Relation.EQ, r1);
        Constraint d4Er2 = Constraint.construct(d4, Relation.EQ, r2);
        Constraint d5Nr1 = Constraint.construct(d5, Relation.NEQ, r1);
        Leaf b2_2_1 = makeBlock(new SymbolicState(locs[1], Constraint.construct(d1Er1, d2Er1, d4Er1)));
        Leaf b2_2_2 = makeBlock(new SymbolicState(locs[2], Constraint.construct(d1Er2, d2Er2, d4Er2, d3Nr1, d5Nr1, eEr1)));
        Assert.assertTrue(tree.getLeaves().contains(b2_2_1));
        Assert.assertTrue(tree.getLeaves().contains(b2_2_2));

        // STABLE

        Assert.assertTrue(tree.isStable());
    }

    @Test
    public void testSplittingTreeOnPureIOStack() {
        RaModel model = new RaModel(PureIOStackAutomatonExample.AUTOMATON, true);
        Location[] locs = getLocations(model);

        SplittingTree tree = new SplittingTree(model, I_POP, I_PUSH);
        Assert.assertFalse(tree.isAcceptable());

        Register r1 = new Register(T_INT, 1);
        Register r2 = new Register(T_INT, 2);

        Leaf b0 = makeBlock(new SymbolicState(locs[0], Constraint.TRUE),
                new SymbolicState(locs[1], Constraint.TRUE),
                new SymbolicState(locs[2], Constraint.TRUE));
        Assert.assertTrue(tree.getLeaves().contains(b0));

        // SPLIT BY OUTPUT

        SDV d1 = new SDV(T_INT, 1);
        DSymbolInstance in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {d1});
        boolean success = tree.splitByOutput(b0, in);
        Assert.assertTrue(success);

        Constraint d1Er1 = Constraint.construct(d1, Relation.EQ, r1);
        Constraint d1Er2 = Constraint.construct(d1, Relation.EQ, r2);
        Leaf b1 = makeBlock(new SymbolicState(locs[0], Constraint.TRUE));
        Leaf b2 = makeBlock(new SymbolicState(locs[1], d1Er1),
                new SymbolicState(locs[2], d1Er2));
        Assert.assertTrue(tree.getLeaves().contains(b1));
        Assert.assertTrue(tree.getLeaves().contains(b2));

        SDV d2 = new SDV(T_INT, 2);
        in = new DSymbolInstance(I_PUSH, new SymbolicDataVariable[] {d2});
        success = tree.splitByOutput(b2, in);
        Assert.assertFalse(success);

        // ACCEPTABLE

        Assert.assertTrue(tree.isAcceptable());
        Assert.assertFalse(tree.isStable());

        // SPLIT BY STATE

        SDV d3 = new SDV(T_INT, 3);
        in = new DSymbolInstance(I_POP, new SymbolicDataVariable[] {d2});
        DSymbolInstance out = new DSymbolInstance(O_OUT, new SymbolicDataVariable[] {d3});
        success = tree.splitByState(b2, in, out);
        Assert.assertTrue(success);

        SDV d4 = new SDV(T_INT, 4);
        Constraint d4Er1 = Constraint.construct(d4, Relation.EQ, r1);
        Constraint d3Er1 = Constraint.construct(d3, Relation.EQ, r1);
        Constraint d3Er2 = Constraint.construct(d3, Relation.EQ, r2);
        Leaf b2_1 = makeBlock(new SymbolicState(locs[1], Constraint.construct(d1Er1, d3Er1)));
        Leaf b2_2 = makeBlock(new SymbolicState(locs[2], Constraint.construct(d1Er2, d3Er2, d4Er1)));
        Assert.assertTrue(tree.getLeaves().contains(b2_1));
        Assert.assertTrue(tree.getLeaves().contains(b2_2));

        // STABLE

        Assert.assertTrue(tree.isStable());
    }

    private Location[] getLocations(RaModel model) {
        Location[] locArr = new Location[model.getStates().size()];
        for (Location loc : model.getLocations()) {
            locArr[loc.getRaLocation().getId()] = loc;
        }
        return locArr;
    }

    private Leaf makeBlock(SymbolicState ... states) {
        Set<SymbolicState> set = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            set.add(s);
        }
        return new Leaf(set);
    }
}
