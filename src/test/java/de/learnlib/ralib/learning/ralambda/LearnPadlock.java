package de.learnlib.ralib.learning.ralambda;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
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
import de.learnlib.ralib.smt.ConstraintSolver;

import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class LearnPadlock extends RaLibTestSuite {
    static final DataType DIGIT = new DataType("id");

    static final InputSymbol IN = new InputSymbol("in", DIGIT);

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
        Expression<Boolean> eqGuard = new NumericBooleanExpression(rVal, NumericComparator.EQ, pVal);
        Expression<Boolean>  neqGuard = new NumericBooleanExpression(rVal, NumericComparator.NE, pVal);
        Expression<Boolean>  trueGuard = ExpressionUtil.TRUE;

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
        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                  dwOracle, teachers, new Constants(), solver);

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        RaLambda ralambda = new RaLambda(mto, hypFactory, slo, consts, false, false, true, IN);
        ralambda.setSolver(solver);

        ralambda.learn();
        RegisterAutomaton hyp = ralambda.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(IN, new DataValue(DIGIT, BigDecimal.ZERO)),
                new PSymbolInstance(IN, new DataValue(DIGIT, BigDecimal.ZERO)),
                new PSymbolInstance(IN, new DataValue(DIGIT, BigDecimal.ZERO)),
                new PSymbolInstance(IN, new DataValue(DIGIT, BigDecimal.ZERO)));

        ralambda.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));

        ralambda.learn();
        hyp = ralambda.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);

    }
}
