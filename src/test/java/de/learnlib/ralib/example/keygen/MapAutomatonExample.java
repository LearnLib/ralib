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
package de.learnlib.ralib.example.keygen;

import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.impl.InputTransition;
import net.automatalib.automaton.ra.impl.MutableRegisterAutomaton;
import net.automatalib.automaton.ra.impl.RALocation;
import net.automatalib.automaton.ra.impl.RegisterAutomaton;
import net.automatalib.automaton.ra.impl.TransitionGuard;
import net.automatalib.automaton.ra.guard.impl.AtomicGuardExpression;
import net.automatalib.automaton.ra.GuardExpression;
import net.automatalib.automaton.ra.guard.impl.Negation;
import net.automatalib.automaton.ra.guard.impl.Relation;
import net.automatalib.automaton.ra.impl.OutputMapping;
import net.automatalib.automaton.ra.impl.OutputTransition;
import net.automatalib.data.DataType;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.VarMapping;
import net.automatalib.data.SymbolicDataValueGenerator;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;

/**
 *
 * @author falk
 */
public final class MapAutomatonExample {

    public static final DataType<Integer> T_KEY = new DataType<>("T_key", Integer.class);
    public static final DataType<Integer> T_VAL = new DataType<>("T_val", Integer.class);

    public static final InputSymbol I_PUT =
            new InputSymbol("put", new DataType[] {T_VAL});

    public static final InputSymbol I_GET =
            new InputSymbol("get", new DataType[] {T_KEY});

    public static final OutputSymbol O_PUT =
            new OutputSymbol("o_p", new DataType[] {T_KEY});

    public static final OutputSymbol O_GET =
            new OutputSymbol("o_g", new DataType[] {T_VAL});

    public static final OutputSymbol O_NULL =
            new OutputSymbol("null", new DataType[] {});

    public static final RegisterAutomaton AUTOMATON = buildAutomaton();

    private MapAutomatonExample() {
    }

    private static RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

        // locations
        RALocation l0 = ra.addInitialState();
        RALocation l1 = ra.addState();
        RALocation l2 = ra.addState();
        RALocation l0_put = ra.addState();
        RALocation l1_put = ra.addState();
        RALocation l1_get_k1   = ra.addState();
        RALocation l1_get_null = ra.addState();
        RALocation l2_get_k1   = ra.addState();
        RALocation l2_get_k2   = ra.addState();

        // registers and parameters
        SymbolicDataValueGenerator.RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
        SymbolicDataValue.Register<Integer> rKey1 = rgen.next(T_KEY);
        SymbolicDataValue.Register<Integer> rKey2 = rgen.next(T_KEY);
        SymbolicDataValue.Register<Integer> rVal1 = rgen.next(T_VAL);
        SymbolicDataValue.Register<Integer> rVal2 = rgen.next(T_VAL);
        SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
        SymbolicDataValue.Parameter<Integer> pKey = pgen.next(T_KEY);
        pgen = new SymbolicDataValueGenerator.ParameterGenerator();
        SymbolicDataValue.Parameter<Integer> pVal = pgen.next(T_VAL);

        // guards

        GuardExpression condition1 = new AtomicGuardExpression<>(rKey1, Relation.EQUALS, pKey);
        GuardExpression condition2 = new AtomicGuardExpression<>(rKey2, Relation.EQUALS, pKey);

        TransitionGuard get1Guard   = new TransitionGuard(condition1);
        TransitionGuard get2Guard   = new TransitionGuard(condition2);
        TransitionGuard error1Guard = new TransitionGuard(new Negation(condition1));
        TransitionGuard trueGuard   = new TransitionGuard();

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> store1IMapping = new VarMapping<>();
        store1IMapping.put(rVal1, pVal);

        VarMapping<Register<?>, SymbolicDataValue<?>> store1OMapping = new VarMapping<>();
        store1OMapping.put(rVal1, rVal1);
        store1OMapping.put(rKey1, pKey);

        VarMapping<Register<?>, SymbolicDataValue<?>> store2IMapping = new VarMapping<>();
        store2IMapping.put(rKey1, rKey1);
        store2IMapping.put(rVal1, rVal1);
        store2IMapping.put(rVal2, pVal);

        VarMapping<Register<?>, SymbolicDataValue<?>> store2OMapping = new VarMapping<>();
        store2OMapping.put(rKey1, rKey1);
        store2OMapping.put(rVal1, rVal1);
        store2OMapping.put(rVal2, rVal2);
        store2OMapping.put(rKey2, pKey);

        VarMapping<Register<?>, SymbolicDataValue<?>> copy2Mapping = new VarMapping<>();
        copy2Mapping.put(rKey1, rKey1);
        copy2Mapping.put(rVal1, rVal1);
        copy2Mapping.put(rKey2, rKey2);
        copy2Mapping.put(rVal2, rVal2);

        VarMapping<Register<?>, SymbolicDataValue<?>> copy1Mapping = new VarMapping<>();
        copy1Mapping.put(rKey1, rKey1);
        copy1Mapping.put(rVal1, rVal1);

        Assignment copy1Assign  = new Assignment(copy1Mapping);
        Assignment copy2Assign  = new Assignment(copy2Mapping);
        Assignment store1IAssign = new Assignment(store1IMapping);
        Assignment store1OAssign = new Assignment(store1OMapping);
        Assignment store2IAssign = new Assignment(store2IMapping);
        Assignment store2OAssign = new Assignment(store2OMapping);

        // output mappings
        OutputMapping om1 = new OutputMapping(pVal, rVal1);
        OutputMapping om2  = new OutputMapping(pVal, rVal2);
        OutputMapping omNull = new OutputMapping();
        OutputMapping omFresh = new OutputMapping(pKey);

        // initial location
        ra.addTransition(l0, I_PUT, new InputTransition(
                trueGuard, I_PUT, l0, l0_put, store1IAssign));

        ra.addTransition(l0_put, O_PUT, new OutputTransition(
                omFresh, O_PUT, l0_put, l1, store1OAssign));

        // 1 stored
        ra.addTransition(l1, I_PUT, new InputTransition(
                trueGuard, I_PUT, l1, l1_put, store2IAssign));

        ra.addTransition(l1_put, O_PUT, new OutputTransition(
                omFresh, O_PUT, l1_put, l2, store2OAssign));

        ra.addTransition(l1, I_GET, new InputTransition(
                get1Guard, I_GET, l1, l1_get_k1, copy1Assign));

        ra.addTransition(l1_get_k1, O_GET, new OutputTransition(
                om1, O_GET, l1_get_k1, l1, copy1Assign));

        ra.addTransition(l1, I_GET, new InputTransition(
                error1Guard, I_GET, l1, l1_get_null, copy1Assign));

        ra.addTransition(l1_get_null, O_NULL, new OutputTransition(
                omNull, O_NULL, l1_get_null, l1, copy1Assign));

        // 2 stored
        ra.addTransition(l2, I_GET, new InputTransition(
                get1Guard, I_GET, l2, l2_get_k1, copy2Assign));

        ra.addTransition(l2_get_k1, O_GET, new OutputTransition(
                om1, O_GET, l2_get_k1, l2, copy2Assign));

        ra.addTransition(l2, I_GET, new InputTransition(
                get2Guard, I_GET, l2, l2_get_k2, copy2Assign));

        ra.addTransition(l2_get_k2, O_GET, new OutputTransition(
                om2, O_GET, l2_get_k2, l2, copy2Assign));

        return ra;
    }

}
