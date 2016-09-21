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
package de.learnlib.ralib.example.succ;

import org.testng.Assert;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public final class SimpleSuccAutomatonExample {
    
    public static final DataType T_SEQ = new DataType("int", Integer.class);

    public static final InputSymbol I_A = 
            new InputSymbol("register", new DataType[] {T_SEQ}); 
    
    
    public static final RegisterAutomaton AUTOMATON = buildAutomaton();
    
    private SimpleSuccAutomatonExample() {      
    }
    
    private static RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();        
        
        // locations
        RALocation l0 = ra.addInitialState(true);
        RALocation l1 = ra.addState(true);
        RALocation l2 = ra.addState(false);
        RALocation lsink = ra.addState(false);
        
        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register r = rgen.next(T_SEQ);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter p = pgen.next(T_SEQ);;
        
        // guards
               
        GuardExpression invalidSeq = new Conjunction(
                new AtomicGuardExpression(r, Relation.NOT_IN_WIN, p),
                new AtomicGuardExpression(r, Relation.NOT_SUCC, p));
        
        
        GuardExpression inWindowSeq = new Conjunction(
        		new AtomicGuardExpression(r, Relation.IN_WIN, p));
        

        GuardExpression succSeq = new Conjunction(
        		new AtomicGuardExpression(r, Relation.SUCC, p));
        
        
        TransitionGuard invalidSeqGuard = new TransitionGuard(invalidSeq);
        TransitionGuard inWindowSeqGuard = new TransitionGuard(inWindowSeq);
        TransitionGuard succSeqGuard = new TransitionGuard(succSeq);
        TransitionGuard trueGuard  = new TransitionGuard();
        
        // assignments
        VarMapping<Register, SymbolicDataValue> copyMapping = new VarMapping<Register, SymbolicDataValue>();
        copyMapping.put(r, r);
        
        VarMapping<Register, SymbolicDataValue> storeMapping = new VarMapping<Register, SymbolicDataValue>();
        storeMapping.put(r, p);
        
        Assignment copyAssign  = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);
                
        // initial location
        ra.addTransition(l0, I_A, new InputTransition(trueGuard, I_A, l0, l1, storeAssign));        
        
        //location accepting as long as succ is supplied
        ra.addTransition(l1, I_A, new InputTransition(inWindowSeqGuard, I_A, l1, l2, copyAssign));
        ra.addTransition(l1, I_A, new InputTransition(succSeqGuard, I_A, l1, l1, copyAssign));
        ra.addTransition(l1, I_A, new InputTransition(invalidSeqGuard, I_A, l1, lsink, copyAssign));
        
        // rejecting (but in window)
        ra.addTransition(l2, I_A, new InputTransition(inWindowSeqGuard, I_A, l2, l2, copyAssign));
        ra.addTransition(l2, I_A, new InputTransition(succSeqGuard, I_A, l2, l1, copyAssign));
        ra.addTransition(l2, I_A, new InputTransition(invalidSeqGuard, I_A, l2, lsink, copyAssign));
        
        // sink
        ra.addTransition(lsink, I_A, new InputTransition(trueGuard, I_A, lsink, lsink, copyAssign));
        
        return ra;
    }
    
    public static void testAutomaton() {
        Word<PSymbolInstance> test1 = Word.epsilon();        
        System.out.format("test1: {0}\n", test1);     
        Assert.assertTrue(AUTOMATON.accepts(test1));

        Word<PSymbolInstance> test2 = Word.epsilon();
        test2 = test2.append(new PSymbolInstance(I_A, new DataValue[] {
                new DataValue(T_SEQ, 1)})).append(new PSymbolInstance(I_A, new DataValue[] {
                new DataValue(T_SEQ, 2)}));
                
        Assert.assertTrue(AUTOMATON.accepts(test2));
        
                
        Word<PSymbolInstance> test3 = Word.epsilon();        
        test3 = test3.append(new PSymbolInstance(I_A, new DataValue[] {
                new DataValue(T_SEQ, 1)})).append(new PSymbolInstance(I_A, new DataValue[] {
                new DataValue(T_SEQ, 2)})).append(new PSymbolInstance(I_A, new DataValue[] {
                new DataValue(T_SEQ, 50)}));
        
        Assert.assertFalse(AUTOMATON.accepts(test3));
        
        Word<PSymbolInstance> test4 = test3.append(new PSymbolInstance(I_A, new DataValue[] {
                new DataValue(T_SEQ, 2)}));
        
        Assert.assertTrue(AUTOMATON.accepts(test4));
    }
    
}
