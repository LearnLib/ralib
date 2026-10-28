package de.learnlib.ralib.equivalence.wmethod.partref.symbolgen;

import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.A;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.AUTOMATON;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.B;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.C;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.T;
import static de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.TestInputSymbolGenerator.buildBlock;

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
import de.learnlib.ralib.data.SymbolicDataValue.FreshSDV;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.equivalence.wmethod.partref.Leaf;
import de.learnlib.ralib.equivalence.wmethod.partref.SymbolicState;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class TestQuantifiedInputSymbolGenerator extends RaLibTestSuite {

    @Test
    public void testPrefixEqInputSymbolGenerator() {
        RaModel model = new RaModel(AUTOMATON, false);
        Leaf block = buildBlock(model);
        QuantifiedInputSymbolGenerator gen = new QuantifiedInputSymbolGenerator(1, block, model, A, B, C);

        SDV d1 = new SDV(T, 1);
        SDV d2 = new SDV(T, 2);
        SDV d3 = new SDV(T, 3);
        FreshSDV f1 = new FreshSDV(T, 1);
        FreshSDV f2 = new FreshSDV(T, 2);
        QuantifiedSDV e1 = new QuantifiedSDV(T, 1);
        QuantifiedSDV e2 = new QuantifiedSDV(T, 2);

        Set<DSymbolInstance> expected = new LinkedHashSet<>();
        expected.add(new DSymbolInstance(A, new SymbolicDataVariable[] {d1, e2, d3}));
        expected.add(new DSymbolInstance(A, new SymbolicDataVariable[] {e1, d2, d3}));
        expected.add(new DSymbolInstance(A, new SymbolicDataVariable[] {e1, e2, d3}));
        expected.add(new DSymbolInstance(A, new SymbolicDataVariable[] {f1, e2, d3}));
        expected.add(new DSymbolInstance(A, new SymbolicDataVariable[] {e1, f2, d3}));
        expected.add(new DSymbolInstance(B, new SymbolicDataVariable[] {d1, e2}));
        expected.add(new DSymbolInstance(B, new SymbolicDataVariable[] {e1, d2}));
        expected.add(new DSymbolInstance(B, new SymbolicDataVariable[] {e1, e2}));
        expected.add(new DSymbolInstance(B, new SymbolicDataVariable[] {f1, e2}));
        expected.add(new DSymbolInstance(B, new SymbolicDataVariable[] {e1, f2}));

        Set<DSymbolInstance> actual = new LinkedHashSet<>();
        while (gen.hasNext()) {
            actual.add(gen.next());
        }

        Assert.assertEquals(actual, expected);
    }

    @Test
    private void testInputSymbolGeneratorNoGuardedParams() {
        RaModel model = new RaModel(buildEpsilonAutomaton(), false);
        Location l0 = model.getLocation(0);
        Leaf block = new Leaf(Set.of(new SymbolicState(l0, Constraint.TRUE)));
        QuantifiedInputSymbolGenerator gen = new QuantifiedInputSymbolGenerator(1, block, model, A, B, C);

        Assert.assertFalse(gen.hasNext());
    }

    private RegisterAutomaton buildEpsilonAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        RALocation l0 = ra.addInitialState(true);
        Assignment none = new Assignment(new VarMapping<>());
        ra.addTransition(l0, B, new InputTransition(ExpressionUtil.TRUE, B, l0, l0, none));

        return ra;
    }
}
