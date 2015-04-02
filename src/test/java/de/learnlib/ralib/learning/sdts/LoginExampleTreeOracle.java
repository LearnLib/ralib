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
package de.learnlib.ralib.learning.sdts;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.automata.guards.ElseGuard;
import de.learnlib.ralib.automata.guards.IfGuard;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
import de.learnlib.ralib.learning.sdts.LoginExampleSDT.SDTClass;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class LoginExampleTreeOracle implements TreeOracle {

    public static enum State {

        INIT, REGISTER, LOGIN, ERROR
    };
    
    private final Register rUid;
    private final Register rPwd;
    
    public LoginExampleTreeOracle() {
        RegisterGenerator gen = new RegisterGenerator();
        rUid = gen.next(T_UID);
        rPwd = gen.next(T_PWD);       
    }

    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {

        if (prefix.length() < 1) {
            return new TreeQueryResult(new PIV(),
                    new LoginExampleSDT(SDTClass.REJECT, suffix, new HashSet<Register>()));
        }

        DataValue uid = null;
        DataValue pwd = null;

        int idx = 0;
        State state = State.INIT;
        while (idx < prefix.length()) {
            PSymbolInstance psi = prefix.getSymbol(idx);

            switch (state) {
                case INIT:
                    if (I_REGISTER.equals(psi.getBaseSymbol())) {
                        uid = psi.getParameterValues()[0];
                        pwd = psi.getParameterValues()[1];
                        state = State.REGISTER;
                    }
                    break;
                case REGISTER:
                    if (I_LOGIN.equals(psi.getBaseSymbol())) {
                        if (uid.equals(psi.getParameterValues()[0])
                                && pwd.equals(psi.getParameterValues()[1])) {
                            state = State.LOGIN;
                        }
                    } else {
                        state = State.ERROR;
                    }
                    break;
                case LOGIN:
                    if (I_LOGOUT.equals(psi.getBaseSymbol())) {
                        state = State.REGISTER;
                    } else {
                        state = State.ERROR;
                    }
                    break;
            }

            if (state == State.ERROR) {
                return new TreeQueryResult(new PIV(),
                        new LoginExampleSDT(SDTClass.REJECT, suffix, new HashSet<Register>()));
            }

            idx++;
        }

        SDTClass clazz = SDTClass.REJECT;
        switch (state) {
            case REGISTER:
                if (suffix.getActions().length() == 1) {
                    clazz = SDTClass.LOGIN;
                }
                break;
            case LOGIN:
                switch (suffix.getActions().length()) {
                    case 0:
                        clazz = SDTClass.ACCEPT;
                        break;
                    case 2:
                        clazz = SDTClass.LOGIN;
                        break;
                }
                break;
        }

        PIV piv = new PIV();
        SymbolicDataValueGenerator.ParameterGenerator pgen = 
                new SymbolicDataValueGenerator.ParameterGenerator();
        if (uid != null && clazz == SDTClass.LOGIN) {
            piv.put(pgen.next(T_UID), rUid);
            piv.put(pgen.next(T_PWD), rPwd);
        }

        return new TreeQueryResult(piv,
                new LoginExampleSDT(clazz, suffix, new HashSet<Register>()));
    }

    
    private Word<PSymbolInstance> getDefaultExtension(
            Word<PSymbolInstance> prefix, ParameterizedSymbol ps) {

        DataValue[] params = new DataValue[ps.getArity()];
        int base = DataWords.paramLength(DataWords.actsOf(prefix)) + 1;
        for (int i = 0; i < ps.getArity(); i++) {
            params[i] = new DataValue(ps.getPtypes()[i], base + i);
        }
        return prefix.append(new PSymbolInstance(ps, params));
    }

    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, SymbolicDecisionTree... sdts) {
        
        Map<Word<PSymbolInstance>, TransitionGuard> branches = new LinkedHashMap<Word<PSymbolInstance>, TransitionGuard>();
        Word<ParameterizedSymbol> acts = DataWords.actsOf(prefix);
        DataValue[] vals = DataWords.valsOf(prefix);
        
        if (sdts.length > 0 && ps.equals(I_LOGIN) && acts.length() == 1 && 
                acts.firstSymbol().equals(I_REGISTER)) {
            
            System.out.println("+++++ special case");
            
            SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
            SymbolicDataValue.Parameter pUid = pgen.next(T_UID);
            SymbolicDataValue.Parameter pPwd = pgen.next(T_PWD);

            // guards
            Variable x1 = new Variable(BuiltinTypes.DOUBLE, "x1");
            Variable x2 = new Variable(BuiltinTypes.DOUBLE, "x2");
            Variable p1 = new Variable(BuiltinTypes.DOUBLE, "p1");
            Variable p2 = new Variable(BuiltinTypes.DOUBLE, "p2");
            Expression<Boolean> expression = new PropositionalCompound(
                    new NumericBooleanExpression(x1, NumericComparator.EQ, p1), 
                    LogicalOperator.AND, 
                    new NumericBooleanExpression(x2, NumericComparator.EQ, p2)); 

            Map<SymbolicDataValue, Variable> mapping = new HashMap<SymbolicDataValue, Variable>();
            mapping.put(rUid, x1);
            mapping.put(rPwd, x2);
            mapping.put(pUid, p1);
            mapping.put(pPwd, p2);

            IfGuard ifGuard = new IfGuard(
                    new DataExpression<Boolean>(expression, mapping));

            TransitionGuard elseGuard = new ElseGuard(
                    Collections.singletonList(ifGuard));

            branches.put(getDefaultExtension(prefix, ps), elseGuard);
            branches.put(prefix.append(new PSymbolInstance(ps, vals)), ifGuard);
            
        } else {
            
            TransitionGuard guard = new ElseGuard();
            branches.put(getDefaultExtension(prefix, ps), guard);
            
        }
        
        return new LoginExampleBranching(branches);
    }

    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, Branching current, 
            PIV piv, SymbolicDecisionTree... sdts) {

        return getInitialBranching(prefix, ps, piv, sdts);
    }

}
