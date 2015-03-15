/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.example.keygen;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.automata.guards.ElseGuard;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author falk
 */
public final class MapAutomatonExample {
    
    public static final DataType T_KEY = new DataType("T_key", Integer.class) {};
    public static final DataType T_VAL = new DataType("T_val", Integer.class) {};

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
        SymbolicDataValue.Register rKey1 = rgen.next(T_KEY);
        SymbolicDataValue.Register rKey2 = rgen.next(T_KEY);        
        SymbolicDataValue.Register rVal1 = rgen.next(T_VAL);
        SymbolicDataValue.Register rVal2 = rgen.next(T_VAL);        
        SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
        SymbolicDataValue.Parameter pKey = pgen.next(T_KEY);
        pgen = new SymbolicDataValueGenerator.ParameterGenerator();
        SymbolicDataValue.Parameter pVal = pgen.next(T_VAL);
        
        // guards
        Variable x1 = new Variable(BuiltinTypes.SINT32, "x1");
        Variable p1 = new Variable(BuiltinTypes.SINT32, "p1");
        Expression<Boolean> expression = 
                new NumericBooleanExpression(x1, NumericComparator.EQ, p1);
        
        Map<SymbolicDataValue, Variable> mapping1 = new HashMap<>();
        mapping1.put(rKey1, x1);
        mapping1.put(pKey, p1);

        Map<SymbolicDataValue, Variable> mapping2 = new HashMap<>();
        mapping2.put(rKey2, x1);
        mapping2.put(pKey, p1);
        
        DataExpression<Boolean> condition1 = new DataExpression<>(expression, mapping1);
        DataExpression<Boolean> condition2 = new DataExpression<>(expression, mapping2);
        
        IfGuard   get1Guard    = new IfGuard(condition1);
        IfGuard   get2Guard    = new IfGuard(condition2);
        ElseGuard error1Guard = new ElseGuard(Collections.singleton(get1Guard));        
        
        ElseGuard trueGuard  = new ElseGuard(Collections.EMPTY_SET);        
        
        // assignments
        VarMapping<SymbolicDataValue.Register, SymbolicDataValue> store1IMapping = new VarMapping<>();
        store1IMapping.put(rVal1, pVal);

        VarMapping<SymbolicDataValue.Register, SymbolicDataValue> store1OMapping = new VarMapping<>();
        store1OMapping.put(rVal1, rVal1);
        store1OMapping.put(rKey1, pKey);

        VarMapping<SymbolicDataValue.Register, SymbolicDataValue> store2IMapping = new VarMapping<>();
        store2IMapping.put(rKey1, rKey1);
        store2IMapping.put(rVal1, rVal1);
        store2IMapping.put(rVal2, pVal);

        VarMapping<SymbolicDataValue.Register, SymbolicDataValue> store2OMapping = new VarMapping<>();
        store2OMapping.put(rKey1, rKey1);
        store2OMapping.put(rVal1, rVal1);
        store2OMapping.put(rVal2, rVal2);
        store2OMapping.put(rKey2, pKey);
        
        VarMapping<Register, SymbolicDataValue> copy2Mapping = new VarMapping<>();
        copy2Mapping.put(rKey1, rKey1);
        copy2Mapping.put(rVal1, rVal1);
        copy2Mapping.put(rKey2, rKey2);
        copy2Mapping.put(rVal2, rVal2);
        
        VarMapping<Register, SymbolicDataValue> copy1Mapping = new VarMapping<>();
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
