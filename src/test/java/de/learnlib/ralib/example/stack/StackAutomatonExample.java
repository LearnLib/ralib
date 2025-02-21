package de.learnlib.ralib.example.stack;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.words.InputSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class StackAutomatonExample {

    public static final DataType T_INT = new DataType("T_int");

    public static final InputSymbol I_PUSH =
            new InputSymbol("push", new DataType[] {T_INT});

    public static final InputSymbol I_POP =
            new InputSymbol("pop", new DataType[] {T_INT});

    public static final RegisterAutomaton AUTOMATON = buildAutomaton();

    private StackAutomatonExample() {
    }

    private static RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        // locations
        RALocation l0 = ra.addInitialState(true);
        RALocation l1 = ra.addState(true);
        RALocation l2 = ra.addState(true);
        RALocation ls = ra.addState(false);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register rVal1 = rgen.next(T_INT);
        Register rVal2 = rgen.next(T_INT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter pVal = pgen.next(T_INT);

        // guards
        Expression<Boolean> okGuard1    = new NumericBooleanExpression(rVal1, NumericComparator.EQ, pVal);
        Expression<Boolean> okGuard2    = new NumericBooleanExpression(rVal2, NumericComparator.EQ, pVal);
        Expression<Boolean> errorGuard1 = new NumericBooleanExpression(rVal1, NumericComparator.NE, pVal);
        Expression<Boolean> errorGuard2 = new NumericBooleanExpression(rVal2, NumericComparator.NE, pVal);
        Expression<Boolean> trueGuard   = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register, SymbolicDataValue> copyMapping = new VarMapping<Register, SymbolicDataValue>();
        copyMapping.put(rVal1, rVal1);

        VarMapping<Register, SymbolicDataValue> storeMapping1 = new VarMapping<Register, SymbolicDataValue>();
        storeMapping1.put(rVal1, pVal);
        VarMapping<Register, SymbolicDataValue> storeMapping2 = new VarMapping<Register, SymbolicDataValue>();
        storeMapping2.put(rVal1, rVal1);
        storeMapping2.put(rVal2, pVal);

        VarMapping<Register, SymbolicDataValue> noMapping = new VarMapping<Register, SymbolicDataValue>();

        Assignment copyAssign   = new Assignment(copyMapping);
        Assignment storeAssign1 = new Assignment(storeMapping1);
        Assignment storeAssign2 = new Assignment(storeMapping2);
        Assignment noAssign     = new Assignment(noMapping);

        // initial location
        ra.addTransition(l0, I_PUSH, new InputTransition(trueGuard, I_PUSH, l0, l1, storeAssign1));
        ra.addTransition(l0, I_POP, new InputTransition(trueGuard, I_POP, l0, ls, noAssign));

        // push location
        ra.addTransition(l1, I_POP, new InputTransition(okGuard1, I_POP, l1, l0, copyAssign));
        ra.addTransition(l1, I_POP, new InputTransition(errorGuard1, I_POP, l1, ls, noAssign));
        ra.addTransition(l1, I_PUSH, new InputTransition(trueGuard, I_PUSH, l1, l2, storeAssign2));

        // push push location
        ra.addTransition(l2, I_POP, new InputTransition(okGuard2, I_POP, l2, l1, copyAssign));
        ra.addTransition(l2, I_POP, new InputTransition(errorGuard2, I_POP, l2, ls, noAssign));
        ra.addTransition(l2, I_PUSH, new InputTransition(trueGuard, I_PUSH, l2, ls, noAssign));

        // sink location
        ra.addTransition(ls, I_POP, new InputTransition(trueGuard, I_POP, ls, ls, noAssign));
        ra.addTransition(ls, I_PUSH, new InputTransition(trueGuard, I_PUSH, ls, ls, noAssign));

        return ra;
    }

}
