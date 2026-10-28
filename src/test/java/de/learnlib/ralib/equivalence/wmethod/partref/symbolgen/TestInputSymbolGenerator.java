package de.learnlib.ralib.equivalence.wmethod.partref.symbolgen;

import java.util.LinkedHashSet;
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
import de.learnlib.ralib.data.SymbolicDataValue.FreshSDV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.equivalence.wmethod.partref.Leaf;
import de.learnlib.ralib.equivalence.wmethod.partref.SymbolicState;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation;
import de.learnlib.ralib.words.InputSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class TestInputSymbolGenerator extends RaLibTestSuite {

    public static final DataType T = new DataType("t");
    public static final InputSymbol A = new InputSymbol("a", new DataType[] {T, T, T});
    public static final InputSymbol B = new InputSymbol("b", new DataType[] {T, T});
    public static final InputSymbol C = new InputSymbol("c");
    public static final RegisterAutomaton AUTOMATON = buildAutomaton();

    @Test
    public void testInputSymbolGenerator() {
        RaModel model = new RaModel(AUTOMATON, false);
        Leaf block = buildBlock(model);
        InputSymbolGenerator gen = new InputSymbolGenerator(1, block, model, A, B, C);

        SDV d1 = new SDV(T, 1);
        SDV d2 = new SDV(T, 2);
        SDV d3 = new SDV(T, 3);
        FreshSDV f1 = new FreshSDV(T, 1);
        FreshSDV f2 = new FreshSDV(T, 2);

        Set<DSymbolInstance> expected = new LinkedHashSet<>();
        expected.add(new DSymbolInstance(A, new SymbolicDataVariable[] {d1, d2, d3}));
        expected.add(new DSymbolInstance(A, new SymbolicDataVariable[] {d1, f2, d3}));
        expected.add(new DSymbolInstance(A, new SymbolicDataVariable[] {f1, d2, d3}));
        expected.add(new DSymbolInstance(A, new SymbolicDataVariable[] {f1, f2, d3}));
        expected.add(new DSymbolInstance(B, new SymbolicDataVariable[] {d1, d2}));
        expected.add(new DSymbolInstance(B, new SymbolicDataVariable[] {d1, f2}));
        expected.add(new DSymbolInstance(B, new SymbolicDataVariable[] {f1, d2}));
        expected.add(new DSymbolInstance(B, new SymbolicDataVariable[] {f1, f2}));
        expected.add(new DSymbolInstance(C));

        Set<DSymbolInstance> actual = new LinkedHashSet<>();
        while (gen.hasNext()) {
            actual.add(gen.next());
        }

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testInputSymbolGeneratorNoParams() {
        RaModel model = new RaModel(AUTOMATON, false);
        Leaf block = buildBlock(model);
        InputSymbolGenerator gen = new InputSymbolGenerator(1, block, model, C);

        Set<DSymbolInstance> expected = new LinkedHashSet<>();
        expected.add(new DSymbolInstance(C));

        Set<DSymbolInstance> actual = new LinkedHashSet<>();
        while (gen.hasNext()) {
            actual.add(gen.next());
        }

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testInputSymbolGeneratorEmpty() {
        RaModel model = new RaModel(AUTOMATON, false);
        Leaf block = buildBlock(model);
        InputSymbolGenerator gen = new InputSymbolGenerator(1, block, model);

        Set<DSymbolInstance> expected = new LinkedHashSet<>();

        Set<DSymbolInstance> actual = new LinkedHashSet<>();
        while (gen.hasNext()) {
            actual.add(gen.next());
        }

        Assert.assertEquals(actual, expected);
    }
    private static RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        RALocation l0 = ra.addInitialState(false);
        RALocation l1 = ra.addState(false);
        RALocation l2 = ra.addState(true);

        Parameter p1 = new Parameter(T, 1);
        Parameter p2 = new Parameter(T, 2);
        Parameter p3 = new Parameter(T, 3);
        Register r1 = new Register(T, 1);
        Register r2 = new Register(T, 2);
        Register r3 = new Register(T, 3);

        Expression<Boolean> gt = ExpressionUtil.TRUE;
        Expression<Boolean> ge1 = new NumericBooleanExpression(p1, NumericComparator.EQ, r1);
        Expression<Boolean> ge2 = new NumericBooleanExpression(p2, NumericComparator.EQ, r2);
        Expression<Boolean> gneq1 = new NumericBooleanExpression(p1, NumericComparator.NE, r1);
        Expression<Boolean> gneq2 = new NumericBooleanExpression(p2, NumericComparator.NE, r2);
        Expression<Boolean> gn = ExpressionUtil.and(gneq1, gneq2);

        VarMapping<Register, Parameter> r1r2 = new VarMapping<>();
        r1r2.put(r1, p1);
        r1r2.put(r2, p2);
        VarMapping<Register, Parameter> r1r2r3 = new VarMapping<>();
        r1r2r3.put(r1, p1);
        r1r2r3.put(r2, p2);
        r1r2r3.put(r3, p3);

        Assignment store2 = new Assignment(r1r2);
        Assignment store3 = new Assignment(r1r2r3);
        Assignment empty = new Assignment(new VarMapping<>());

        ra.addTransition(l0, A, new InputTransition(gt, A, l0, l1, store3));
        ra.addTransition(l0, B, new InputTransition(gt, B, l0, l2, store2));
        ra.addTransition(l0, C, new InputTransition(gt, C, l0, l0, empty));

        ra.addTransition(l1, A, new InputTransition(ge1, A, l1, l0, empty));
        ra.addTransition(l1, A, new InputTransition(ge2, A, l1, l0, empty));
        ra.addTransition(l1, A, new InputTransition(gn, A, l1, l1, store3));
        ra.addTransition(l1, B, new InputTransition(ge1, B, l1, l2, store2));
        ra.addTransition(l1, B, new InputTransition(ge2, B, l1, l2, store2));
        ra.addTransition(l1, B, new InputTransition(gn, B, l1, l0, empty));
        ra.addTransition(l1, C, new InputTransition(gt, C, l1, l0, empty));

        ra.addTransition(l2, A, new InputTransition(gt, A, l2, l2, store2));
        ra.addTransition(l2, B, new InputTransition(gt, B, l2, l0, empty));
        ra.addTransition(l2, C, new InputTransition(gt, C, l1, l0, empty));

        return ra;
    }

    protected static Leaf buildBlock(RaModel model) {
        Set<Location> locs = model.getLocations();
        Location[] l = locs.toArray(new Location[locs.size()]);
        Set<SymbolicState> states = new LinkedHashSet<>();
        SymbolicDataVariable d1 = new SDV(T, 1);
        Register r1 = new Register(T, 1);
        states.add(new SymbolicState(l[1], Constraint.construct(d1, Relation.EQ, r1)));
        return new Leaf(states);
    }
}
