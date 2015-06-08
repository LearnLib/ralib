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
package de.learnlib.ralib.example.login;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.words.InputSymbol;

/**
 *
 * @author falk
 */
public final class LoginAutomatonExample {
    
    public static final DataType T_UID = new DataType("T_uid", Integer.class);
    public static final DataType T_PWD = new DataType("T_pwd", Integer.class);

    public static final InputSymbol I_REGISTER = 
            new InputSymbol("register", new DataType[] {T_UID, T_PWD}); 
    
    public static final InputSymbol I_LOGIN = 
            new InputSymbol("login", new DataType[] {T_UID, T_PWD});
    
    public static final InputSymbol I_LOGOUT = 
            new InputSymbol("logout", new DataType[] {});
    
    public static final RegisterAutomaton AUTOMATON = buildAutomaton();
    
    private LoginAutomatonExample() {      
    }
    
    private static RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();        
        
        // locations
        RALocation l0 = ra.addInitialState(false);
        RALocation l1 = ra.addState(false);
        RALocation l2 = ra.addState(true);
        
        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register rUid = rgen.next(T_UID);
        Register rPwd = rgen.next(T_PWD);        
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter pUid = pgen.next(T_UID);
        Parameter pPwd = pgen.next(T_PWD);
        
        // guards
               
        GuardExpression condition = new Conjunction(
                new AtomicGuardExpression(rUid, Relation.EQUALS, pUid),
                new AtomicGuardExpression(rPwd, Relation.EQUALS, pPwd));
        
        GuardExpression elseCond = new Disjunction(
                new AtomicGuardExpression(rUid, Relation.NOT_EQUALS, pUid),
                new AtomicGuardExpression(rPwd, Relation.NOT_EQUALS, pPwd));

        TransitionGuard okGuard    = new TransitionGuard(condition);
        TransitionGuard errorGuard = new TransitionGuard(elseCond);
        TransitionGuard trueGuard  = new TransitionGuard();
        
        // assignments
        VarMapping<Register, SymbolicDataValue> copyMapping = new VarMapping<Register, SymbolicDataValue>();
        copyMapping.put(rUid, rUid);
        copyMapping.put(rPwd, rPwd);
        
        VarMapping<Register, SymbolicDataValue> storeMapping = new VarMapping<Register, SymbolicDataValue>();
        storeMapping.put(rUid, pUid);
        storeMapping.put(rPwd, pPwd);
        
        Assignment copyAssign  = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);
                
        // initial location
        ra.addTransition(l0, I_REGISTER, new InputTransition(trueGuard, I_REGISTER, l0, l1, storeAssign));        
        
        // reg. location
        ra.addTransition(l1, I_LOGIN, new InputTransition(okGuard, I_LOGIN, l1, l2, copyAssign));        
        ra.addTransition(l1, I_LOGIN, new InputTransition(errorGuard, I_LOGIN, l1, l1, copyAssign));        
        
        // login location
        ra.addTransition(l2, I_LOGOUT, new InputTransition(trueGuard, I_LOGOUT, l2, l1, copyAssign));        
        
        return ra;
    }
    
}
