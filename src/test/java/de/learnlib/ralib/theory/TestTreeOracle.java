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

package de.learnlib.ralib.theory;

import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.api.Query;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
@Test
public class TestTreeOracle {
   
    private static final class UserType extends DataType {
        UserType() {
            super("userType", String.class);
        }
    }
   
    private static final class PassType extends DataType {
        PassType() {
            super("passType", String.class);
        }
    }

    public void testTreeOracle() {
        
        // define types
        
        final UserType userType = new UserType();
        final PassType passType = new PassType();
        
        // define parameterized symbols
        
        final ParameterizedSymbol register = new InputSymbol(
                "register", new DataType[] {userType, passType});
        
        final ParameterizedSymbol login = new InputSymbol(
                "login", new DataType[] {userType, passType});
        
        final ParameterizedSymbol change = new InputSymbol(
                "change", new DataType[] {passType});
        
        final ParameterizedSymbol logout = new InputSymbol(
                "logout", new DataType[] {userType});
        
        // create prefix: register(falk[userType], secret[passType])
        
        final Word<PSymbolInstance> prefix = Word.fromLetter(
                new PSymbolInstance(register, 
                    new DataValue(userType, "falk"),
                    new DataValue(passType, "secret")));
        
        // create suffix: login(falk[userType], secret[passType])

        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(login, 
                    new DataValue(userType, "falk"),
                    new DataValue(passType, "secret")),
                new PSymbolInstance(change, 
                    new DataValue(passType, "secret")),
                new PSymbolInstance(logout,
                    new DataValue(userType, "falk")),
                new PSymbolInstance(login,
                    new DataValue(userType, "falk"),
                    new DataValue(passType, "secret"))
                    );
        
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(login, 
                    new DataValue(userType, "falk"),
                    new DataValue(passType, "secret"))
                    );
        
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);
        System.out.println("Prefix: " + prefix);
        System.out.println("Suffix: " + symSuffix);
        
        // hacked oracle
        
        DataWordOracle dwOracle = new DataWordOracle() {
            @Override
            public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
                
                // given a collection of queries, process each one (with Bool replies)
                
                for (Query q : clctn) {
                    Word<PSymbolInstance> trace = q.getInput();
                    
                    // if the trace is not 5, answer false (since then automatically incorrect)
                    
                    if (trace.length() != 2) {
                        q.answer(false);
                        continue;
                    }
                    
                    // get the first two symbols in the trace
                    
                    PSymbolInstance a1 = trace.getSymbol(0);
                    PSymbolInstance a2 = trace.getSymbol(1);
//                    PSymbolInstance a3 = trace.getSymbol(2);
//                    PSymbolInstance a4 = trace.getSymbol(3);
//                    PSymbolInstance a5 = trace.getSymbol(4);
                    
                    DataValue[] a1Params = a1.getParameterValues();
                    DataValue[] a2Params = a2.getParameterValues();
//                    DataValue[] a3Params = a3.getParameterValues();
//                    DataValue[] a4Params = a4.getParameterValues();
//                    DataValue[] a5Params = a5.getParameterValues();
                    
                    // query reply is ACCEPT only if length 2 and symbols equal each other
                    
                    q.answer( a1.getBaseSymbol().equals(register) &&
                            a2.getBaseSymbol().equals(login) &&
                            Arrays.equals(a1Params, a2Params)); //&& 
//                            a3.getBaseSymbol().equals(change) && 
//                            a4.getBaseSymbol().equals(logout) && 
//                            a5.getBaseSymbol().equals(login) && 
//                            a4Params[0].equals(a5Params[0]) && 
//                            a5Params[0].equals(a1Params[0]) && 
//                            a1Params[0].equals(a2Params[0]) && 
//                            a3Params[0].equals(a5Params[1]));
                   
                }
            }
        };
        
        Theory<UserType> userTheory = new EqualityTheory<UserType>() {

            @Override
            public DataValue<UserType> getFreshValue(List<DataValue<UserType>> vals) {
                DataValue v = vals.get(0);
                return new DataValue(v.getType(), 
                        v.getId().toString() + "_" + vals.size());
            }

            
        };

        Theory<PassType> passTheory = new EqualityTheory<PassType>() {

            @Override
            public DataValue<PassType> getFreshValue(List<DataValue<PassType>> vals) {
                DataValue v = vals.get(0);
                return new DataValue(v.getType(), 
                        v.getId().toString() + "_" + vals.size());
            }


        };
        
        Map<DataType, Theory> theories = new HashMap();
        theories.put(userType, userTheory);
        theories.put(passType, passTheory);
        
        MultiTheoryTreeOracle treeOracle = new MultiTheoryTreeOracle(dwOracle, theories, new Constants());
        
        TreeQueryResult res = treeOracle.treeQuery(prefix, symSuffix);
//        System.out.println(res.getSdt().isAccepting());
        System.out.println("final SDT: \n" + res.getSdt().toString());
        
    } 
            
            
    
}
