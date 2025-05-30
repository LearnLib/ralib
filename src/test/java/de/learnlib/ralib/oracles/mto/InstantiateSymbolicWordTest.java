package de.learnlib.ralib.oracles.mto;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class InstantiateSymbolicWordTest {

    @Test
    public void testInstantiateStack() {
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
	              dwOracle, teachers, new Constants(), solver);

        Word<PSymbolInstance> prefix = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)));
        Word<PSymbolInstance> suffix = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ZERO)));
        SymbolicSuffix symbSuffix = new SymbolicSuffix(prefix, suffix);

        SDT tqr = mto.treeQuery(prefix, symbSuffix);

        Map<Word<PSymbolInstance>, Boolean> words = mto.instantiate(prefix, symbSuffix, tqr);

        Word<PSymbolInstance> p1 = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ZERO)));
        Word<PSymbolInstance> p2 = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, new BigDecimal(2))));
        Word<PSymbolInstance> p3 = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ONE)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, new BigDecimal(2) )),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, new BigDecimal(3) )));

        Assert.assertEquals(words.size(), 3);
        Assert.assertTrue(words.containsKey(p1) &&
        		words.containsKey(p2) &&
        		words.containsKey(p3));
        Assert.assertTrue(words.get(p1).booleanValue());
    }

    @Test
    public void testInstantiateWithSuffixOpt() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        InputSymbol A = new InputSymbol("a", T_INT);
        InputSymbol B = new InputSymbol("b", T_INT);

        RALocation l0 = ra.addInitialState();
        RALocation l1 = ra.addState();
        RALocation ls = ra.addState(false);

        // registers and parameters
        SymbolicDataValueGenerator.RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
        SymbolicDataValue.Register r1 = rgen.next(T_INT);
        SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
        SymbolicDataValue.Parameter p1 = pgen.next(T_INT);

        // guards
        Expression<Boolean>  equal = new NumericBooleanExpression(r1, NumericComparator.EQ, p1);
        Expression<Boolean>  notEqual = new NumericBooleanExpression(r1, NumericComparator.NE, p1);

        Expression<Boolean> equalGuard = equal;
        Expression<Boolean>  notEqualGuard = notEqual;
        Expression<Boolean>  trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<SymbolicDataValue.Register, SymbolicDataValue> store = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
        store.put(r1, p1);
        VarMapping<Register, SymbolicDataValue> noMapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();

        Assignment storeAssign = new Assignment(store);
        Assignment noAssign = new Assignment(noMapping);

        ra.addTransition(l0, A, new InputTransition(trueGuard, A, l0, l1, storeAssign));
        ra.addTransition(l0, B, new InputTransition(trueGuard, B, l0, ls, noAssign));

        ra.addTransition(l1, B, new InputTransition(equalGuard, B, l1, l0, noAssign));
        ra.addTransition(l1, B, new InputTransition(notEqualGuard, B, l1, ls, noAssign));
        ra.addTransition(l1, A, new InputTransition(trueGuard, A, l1, ls, noAssign));

        ra.addTransition(ls, A, new InputTransition(trueGuard, A, ls, ls, noAssign));
        ra.addTransition(ls, B, new InputTransition(trueGuard, B, ls, ls, noAssign));

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory intEq = new IntegerEqualityTheory(T_INT);
        intEq.setUseSuffixOpt(true);
        teachers.put(T_INT, intEq);

        DataWordOracle dwOracle = new SimulatorOracle(ra);

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
              dwOracle, teachers, new Constants(), new ConstraintSolver());

        Word<PSymbolInstance> prefix = Word.fromSymbols(
              new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ZERO)),
              new PSymbolInstance(B, new DataValue(T_INT, BigDecimal.ZERO)));
        Word<PSymbolInstance> suffix = Word.fromSymbols(
              new PSymbolInstance(A, new DataValue(T_INT, BigDecimal.ONE)),
              new PSymbolInstance(B, new DataValue(T_INT, BigDecimal.ONE)));
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);

        SDT tqr = mto.treeQuery(prefix, symSuffix);
        Map<Word<PSymbolInstance>, Boolean> words = mto.instantiate(prefix, symSuffix, tqr);

        Assert.assertEquals(words.size(), 1);

        Word<PSymbolInstance> word = words.keySet().iterator().next();
        DataValue suffixVal1 = word.getSymbol(2).getParameterValues()[0];
        DataValue suffixVal2 = word.getSymbol(3).getParameterValues()[0];

        Assert.assertEquals(suffixVal1, suffixVal2);
    }
}
