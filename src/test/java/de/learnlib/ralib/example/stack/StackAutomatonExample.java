package de.learnlib.ralib.example.stack;

import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.impl.InputTransition;
import net.automatalib.automaton.ra.impl.MutableRegisterAutomaton;
import net.automatalib.automaton.ra.impl.RALocation;
import net.automatalib.automaton.ra.impl.RegisterAutomaton;
import net.automatalib.automaton.ra.impl.TransitionGuard;
import net.automatalib.automaton.ra.guard.impl.AtomicGuardExpression;
import net.automatalib.automaton.ra.guard.impl.Relation;
import net.automatalib.data.DataType;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.VarMapping;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.symbol.impl.InputSymbol;

public class StackAutomatonExample {

    public static final DataType<Integer> T_INT = new DataType<>("T_int", Integer.class);

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
        Register<Integer> rVal1 = rgen.next(T_INT);
        Register<Integer> rVal2 = rgen.next(T_INT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pVal = pgen.next(T_INT);

        // guards
        TransitionGuard okGuard1    = new TransitionGuard(new AtomicGuardExpression<>(rVal1, Relation.EQUALS, pVal));
        TransitionGuard okGuard2    = new TransitionGuard(new AtomicGuardExpression<>(rVal2, Relation.EQUALS, pVal));
        TransitionGuard errorGuard1 = new TransitionGuard(new AtomicGuardExpression<>(rVal1, Relation.NOT_EQUALS, pVal));
        TransitionGuard errorGuard2 = new TransitionGuard(new AtomicGuardExpression<>(rVal2, Relation.NOT_EQUALS, pVal));
        TransitionGuard trueGuard   = new TransitionGuard();

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>();
        copyMapping.put(rVal1, rVal1);

        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping1 = new VarMapping<>();
        storeMapping1.put(rVal1, pVal);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping2 = new VarMapping<>();
        storeMapping2.put(rVal1, rVal1);
        storeMapping2.put(rVal2, pVal);

        VarMapping<Register<?>, SymbolicDataValue<?>> noMapping = new VarMapping<>();

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
