/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.ceanalysis;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.ceanalysis.PrefixFinder.Result;
import de.learnlib.ralib.ct.CTAutomatonBuilder;
import de.learnlib.ralib.ct.CTHypothesis;
import de.learnlib.ralib.ct.ClassificationTree;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SLLambdaRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class PrefixFinderTest extends RaLibTestSuite {

    @Test
    public void testPrefixFinder() {
        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);

        SLLambdaRestrictionBuilder rb = new SLLambdaRestrictionBuilder(consts, teachers, solver);
        OptimizedSymbolicSuffixBuilder sb = new OptimizedSymbolicSuffixBuilder(consts, rb);
        ClassificationTree ct = new ClassificationTree(mto, solver, rb, sb, consts, false,
        		I_LOGIN, I_LOGOUT, I_REGISTER);

        ct.initialize();
        boolean closed = false;
        while(!closed) {
        	closed = ct.checkLocationClosedness() &&
        			ct.checkTransitionClosedness() &&
        			ct.checkRegisterClosedness();
        }
        CTAutomatonBuilder ab = new CTAutomatonBuilder(ct, consts, false, solver);
        final CTHypothesis hyp = ab.buildHypothesis();

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                        new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)),
                new PSymbolInstance(I_LOGIN,
                        new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)));

        PrefixFinder pf = new PrefixFinder(mto, hyp, ct, teachers, rb, solver, consts);

        Result res = pf.analyzeCounterExample(ce);
        Word<PSymbolInstance> prefix = res.prefix();
        Assert.assertEquals(res.result(), PrefixFinder.ResultType.LOCATION);
        Assert.assertEquals(prefix.toString(), "register[0[T_uid], 0[T_pwd]]");
    }

    @Test
    public void testPrefixFinderMultipleAccessSequences() {
    	Constants consts = new Constants();
    	RegisterAutomaton sul = de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
    	DataWordOracle dwOracle = new SimulatorOracle(sul);

    	final Map<DataType, Theory> teachers = new LinkedHashMap<>();
    	teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);

        SLLambdaRestrictionBuilder rb = new SLLambdaRestrictionBuilder(consts, teachers, solver);
        OptimizedSymbolicSuffixBuilder sb = new OptimizedSymbolicSuffixBuilder(consts, rb);
        ClassificationTree ct = new ClassificationTree(mto, solver, rb, sb, consts, false,
        		I_PUSH, I_POP);

        ct.initialize();
        boolean closed = false;
        while(!closed) {
        	closed = ct.checkLocationClosedness() &&
        			ct.checkTransitionClosedness() &&
        			ct.checkRegisterClosedness();
        }
        CTAutomatonBuilder ab = new CTAutomatonBuilder(ct, consts, false, solver);
        final CTHypothesis hyp = ab.buildHypothesis();

        Word<PSymbolInstance> shortPrefix = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)));
        ct.expand(shortPrefix);

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, BigDecimal.ZERO)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, BigDecimal.ONE)));

        PrefixFinder pf = new PrefixFinder(mto, hyp, ct, teachers, rb, solver, consts);

        Result res = pf.analyzeCounterExample(ce);
        Assert.assertEquals(res.result(), PrefixFinder.ResultType.TRANSITION);
        Assert.assertEquals(res.prefix().toString(), "push[0[T_int]] pop[0[T_int]]");
    }

    private final DataType DT = new DataType("double");
    private final InputSymbol A = new InputSymbol("α", DT);
    private final InputSymbol B = new InputSymbol("β");

    private RegisterAutomaton buildTestAutomaton() {
    	MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

    	Register x1 = new Register(DT, 1);
    	Parameter p1 = new Parameter(DT, 1);

    	RALocation l0 = ra.addInitialState(true);
    	RALocation l1 = ra.addState(false);
    	RALocation l2 = ra.addState(false);
    	RALocation l3 = ra.addState(false);

    	Expression<Boolean> gTrue = ExpressionUtil.TRUE;
    	Expression<Boolean> gGT = new NumericBooleanExpression(x1, NumericComparator.GE, p1);
    	Expression<Boolean> gLT = new NumericBooleanExpression(x1, NumericComparator.LT, p1);

    	VarMapping<Register, Parameter> mapX1P1 = new VarMapping<>();
    	mapX1P1.put(x1, p1);
    	VarMapping<Register, SymbolicDataValue> mapNo = new VarMapping<>();

    	Assignment assX1P1 = new Assignment(mapX1P1);
    	Assignment assNo = new Assignment(mapNo);

    	ra.addTransition(l0, A, new InputTransition(gTrue, A, l0, l1, assX1P1));
    	ra.addTransition(l0, B, new InputTransition(gTrue, B, l0, l0, assNo));
    	ra.addTransition(l1, A, new InputTransition(gGT, A, l1, l2, assNo));
    	ra.addTransition(l1, A, new InputTransition(gLT, A, l1, l3, assNo));
    	ra.addTransition(l1, B, new InputTransition(gTrue, B, l1, l0, assNo));
    	ra.addTransition(l2, A, new InputTransition(gTrue, A, l2, l0, assNo));
    	ra.addTransition(l2, B, new InputTransition(gTrue, B, l2, l2, assNo));
    	ra.addTransition(l3, A, new InputTransition(gTrue, A, l3, l0, assNo));
    	ra.addTransition(l3, B, new InputTransition(gTrue, B, l3, l0, assNo));

    	return ra;
    }

    @Test
    public void testAnalyzeCELocation() {
    	RegisterAutomaton ra = buildTestAutomaton();
    	DataWordOracle dwOracle = new SimulatorOracle(ra);

    	final Map<DataType, Theory> teachers = new LinkedHashMap<>();
    	DoubleInequalityTheory dit = new DoubleInequalityTheory(DT);
    	dit.useSuffixOptimization(false);
    	teachers.put(DT, dit);

    	ConstraintSolver solver = new ConstraintSolver();
    	Constants consts = new Constants();

    	MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, consts, solver);

    	SLLambdaRestrictionBuilder restrBuilder = new SLLambdaRestrictionBuilder(consts, teachers, solver);
    	OptimizedSymbolicSuffixBuilder suffixBuilder = new OptimizedSymbolicSuffixBuilder(consts, restrBuilder);

        DataValue dv0 = new DataValue(DT, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(DT, BigDecimal.ONE);
        DataValue dv2 = new DataValue(DT, BigDecimal.valueOf(2));
        DataValue dv3 = new DataValue(DT, BigDecimal.valueOf(3));

    	ClassificationTree ct = new ClassificationTree(mto, solver, restrBuilder, suffixBuilder, consts, false, A, B);

    	ct.initialize();
    	ct.checkLocationClosedness();
    	ct.checkLocationClosedness();

        CTAutomatonBuilder ab = new CTAutomatonBuilder(ct, new Constants(), false, solver);
        CTHypothesis hyp = ab.buildHypothesis();

    	Word<PSymbolInstance> ce1 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv2),
    			new PSymbolInstance(A, dv3));

    	PrefixFinder pf = new PrefixFinder(mto, hyp, ct, teachers, restrBuilder, solver, consts);

    	Result res = pf.analyzeCounterExample(ce1);
    	Assert.assertEquals(res.result(), PrefixFinder.ResultType.LOCATION);
    	Assert.assertEquals(res.prefix().toString(), "α[1[double]] α[2[double]]");

    	ct.expand(res.prefix());
    	boolean consistent = ct.checkLocationConsistency();
    	Assert.assertFalse(consistent);

    	ab = new CTAutomatonBuilder(ct, consts, false, solver);
    	hyp = ab.buildHypothesis();
    	pf = new PrefixFinder(mto, hyp, ct, teachers, restrBuilder, solver, consts);

    	Word<PSymbolInstance> ce2 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv0),
    			new PSymbolInstance(B));

    	Assert.assertFalse(hyp.accepts(ce2) == ra.accepts(ce2));

    	res = pf.analyzeCounterExample(ce2);
    	Assert.assertEquals(res.result(), PrefixFinder.ResultType.TRANSITION);
    	Assert.assertEquals(res.prefix().toString(), "α[1[double]] α[0[double]]");
    }

}
