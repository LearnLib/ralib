package de.learnlib.ralib.example.stack;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.util.List;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.words.OutputSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class IOStackAutomatonExample {

    public static final OutputSymbol O_OK = new OutputSymbol("ok");
    public static final OutputSymbol O_NOK = new OutputSymbol("nok");

    public static final RegisterAutomaton AUTOMATON = buildAutomaton();

    public static RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        RALocation l0 = ra.addInitialState();
        RALocation l1 = ra.addState();
        RALocation l2 = ra.addState();

        RALocation l0_push = ra.addState();
        RALocation l0_pop_nok = ra.addState();
        RALocation l1_push = ra.addState();
        RALocation l1_pop_ok = ra.addState();
        RALocation l1_pop_nok = ra.addState();
        RALocation l2_push = ra.addState();
        RALocation l2_pop_ok = ra.addState();
        RALocation l2_pop_nok = ra.addState();

        Register r1 = new Register(T_INT, 1);
        Register r2 = new Register(T_INT, 2);
        Parameter p1 = new Parameter(T_INT, 1);

        Expression<Boolean> gt = ExpressionUtil.TRUE;
        Expression<Boolean> gEqR1 = new NumericBooleanExpression(p1, NumericComparator.EQ, r1);
        Expression<Boolean> gEqR2 = new NumericBooleanExpression(p1, NumericComparator.EQ, r2);
        Expression<Boolean> gNEqR1 = new NumericBooleanExpression(p1, NumericComparator.NE, r1);
        Expression<Boolean> gNEqR2 = new NumericBooleanExpression(p1, NumericComparator.NE, r2);

        VarMapping<Register, Parameter> r1p1 = new VarMapping<>();
        r1p1.put(r1, p1);
        VarMapping<Register, Register> r1r1 = new VarMapping<>();
        r1r1.put(r1, r1);
        VarMapping<Register, SymbolicDataValue> r1r1_r2p1 = new VarMapping<>();
        r1r1_r2p1.put(r1, r1);
        r1r1_r2p1.put(r2, p1);
        VarMapping<Register, Register> r1r1_r2r2 = new VarMapping<>();
        r1r1_r2r2.put(r1, r1);
        r1r1_r2r2.put(r2, r2);
        VarMapping<Register, SymbolicDataValue> r1r2_r2p1 = new VarMapping<>();
        r1r2_r2p1.put(r1, r2);
        r1r2_r2p1.put(r2, p1);

        Assignment r1Store = new Assignment(r1p1);
        Assignment r2Store = new Assignment(r1r1_r2p1);
        Assignment r1Copy = new Assignment(r1r1);
        Assignment r1r2Copy = new Assignment(r1r1_r2r2);
        Assignment r2Replace = new Assignment(r1r2_r2p1);
        Assignment noAssign = new Assignment(new VarMapping<>());

        OutputMapping out = new OutputMapping(List.of(), new VarMapping<>());

        ra.addTransition(l0, I_PUSH, new InputTransition(gt, I_PUSH, l0, l0_push, r1Store));
        ra.addTransition(l0, I_POP, new InputTransition(gt, I_POP, l0, l0_pop_nok, noAssign));
        ra.addTransition(l1, I_PUSH, new InputTransition(gt, I_PUSH, l1, l1_push, r2Store));
        ra.addTransition(l1, I_POP, new InputTransition(gEqR1, I_POP, l1, l1_pop_ok, noAssign));
        ra.addTransition(l1, I_POP, new InputTransition(gNEqR1, I_POP, l1, l1_pop_nok, r1Copy));
        ra.addTransition(l2, I_PUSH, new InputTransition(gt, I_PUSH, l2, l2_push, r2Replace));
        ra.addTransition(l2, I_POP, new InputTransition(gEqR2, I_POP, l2, l2_pop_ok, r1Copy));
        ra.addTransition(l2, I_POP, new InputTransition(gNEqR2, I_POP, l2, l2_pop_nok, r1r2Copy));

        ra.addTransition(l0_push, O_OK, new OutputTransition(gt, out, O_OK, l0_push, l1, r1Copy));
        ra.addTransition(l1_push, O_OK, new OutputTransition(gt, out, O_OK, l1_push, l2, r1r2Copy));
        ra.addTransition(l2_push, O_OK, new OutputTransition(gt, out, O_OK, l2_push, l2, r1r2Copy));
        ra.addTransition(l0_pop_nok, O_NOK, new OutputTransition(gt, out, O_NOK, l0_pop_nok, l0, noAssign));
        ra.addTransition(l1_pop_nok, O_NOK, new OutputTransition(gt, out, O_NOK, l1_pop_nok, l1, r1Copy));
        ra.addTransition(l2_pop_nok, O_NOK, new OutputTransition(gt, out, O_NOK, l2_pop_nok, l2, r1r2Copy));
        ra.addTransition(l1_pop_ok, O_OK, new OutputTransition(gt, out, O_OK, l1_pop_ok, l0, noAssign));
        ra.addTransition(l2_pop_ok, O_OK, new OutputTransition(gt, out, O_OK, l2_pop_ok, l1, r1Copy));

        return ra;
    }
}
