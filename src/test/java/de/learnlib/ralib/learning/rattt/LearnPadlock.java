package de.learnlib.ralib.learning.rattt;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class LearnPadlock extends RaLibTestSuite {
    static final DataType DIGIT = new DataType("id", Integer.class);

    static final InputSymbol IN = new InputSymbol("in", new DataType[] { DIGIT });

    private static RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        // locations
        RALocation l0 = ra.addInitialState(true);
        RALocation l1 = ra.addState(true);
        RALocation l2 = ra.addState(true);
        RALocation l3 = ra.addState(true);
        RALocation l4 = ra.addState(false);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register rVal = rgen.next(DIGIT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter pVal = pgen.next(DIGIT);

        // guards
        TransitionGuard eqGuard = new TransitionGuard(
                new AtomicGuardExpression<Register, Parameter>(rVal, Relation.EQUALS, pVal));
        TransitionGuard neqGuard = new TransitionGuard(
                new AtomicGuardExpression<Register, Parameter>(rVal, Relation.NOT_EQUALS, pVal));
        TransitionGuard trueGuard = new TransitionGuard();

        // assignments
        VarMapping<Register, SymbolicDataValue> copyMapping = new VarMapping<Register, SymbolicDataValue>();
        copyMapping.put(rVal, rVal);

        VarMapping<Register, SymbolicDataValue> storeMapping = new VarMapping<Register, SymbolicDataValue>();
        storeMapping.put(rVal, pVal);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);
        Assignment noAssign = new Assignment(new VarMapping<>());

        // initial location
        ra.addTransition(l0, IN, new InputTransition(trueGuard, IN, l0, l1, storeAssign));

        // IN 0 location
        ra.addTransition(l1, IN, new InputTransition(eqGuard, IN, l1, l2, copyAssign));
        ra.addTransition(l1, IN, new InputTransition(neqGuard, IN, l1, l0, noAssign));

        // IN 0 OK location
        ra.addTransition(l2, IN, new InputTransition(eqGuard, IN, l2, l3, copyAssign));
        ra.addTransition(l2, IN, new InputTransition(neqGuard, IN, l2, l0, noAssign));

        // IN 0 OK in 1 location
        ra.addTransition(l3, IN, new InputTransition(eqGuard, IN, l3, l4, noAssign));
        ra.addTransition(l3, IN, new InputTransition(neqGuard, IN, l3, l0, noAssign));

        ra.addTransition(l4, IN, new InputTransition(trueGuard, IN, l4, l4, noAssign));

        return ra;
    }

    @Test
    public void learnPadlock() {

        Constants consts = new Constants();
        RegisterAutomaton sul = buildAutomaton();
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(DIGIT, new IntegerEqualityTheory(DIGIT));
        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                  dwOracle, teachers, new Constants(), solver);

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        RaLambda rattt = new RaLambda(mto, hypFactory, slo, consts, false, false, true, IN);
		rattt.setSolver(solver);

        rattt.learn();
        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(IN, new DataValue<>(DIGIT, 0)),
                new PSymbolInstance(IN, new DataValue<>(DIGIT, 0)),
                new PSymbolInstance(IN, new DataValue<>(DIGIT, 0)),
                new PSymbolInstance(IN, new DataValue<>(DIGIT, 0)));

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

    }
}
