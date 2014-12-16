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

import de.learnlib.api.Query;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.VarsToInternalRegs;
import de.learnlib.ralib.sul.DataWordOracle;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;
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
        
        final UserType userType = new UserType();
        final PassType passType = new PassType();
        
        final ParameterizedSymbol register = new ParameterizedSymbol(
                "register", new DataType[] {userType, passType});
        
        final ParameterizedSymbol login = new ParameterizedSymbol(
                "login", new DataType[] {userType, passType});
        
        final Word<PSymbolInstance> prefix = Word.fromLetter(
                new PSymbolInstance(register, 
                    new DataValue(userType, "falk"),
                    new DataValue(passType, "secret")));

        final Word<PSymbolInstance> suffix = Word.fromLetter(
                new PSymbolInstance(login, 
                    new DataValue(userType, "falk"),
                    new DataValue(passType, "secret")));
        
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);
        System.out.println("Prefix: " + prefix);
        System.out.println("Suffix: " + symSuffix);
        
        DataWordOracle dwOracle = new DataWordOracle() {
            @Override
            public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
                for (Query q : clctn) {
                    Word<PSymbolInstance> trace = q.getInput();
                    if (trace.length() != 2) {
                        q.answer(false);
                        continue;
                    }
                    PSymbolInstance a1 = trace.getSymbol(0);
                    PSymbolInstance a2 = trace.getSymbol(1);
                    
                    DataValue[] a1Params = a1.getParameterValues();
                    DataValue[] a2Params = a2.getParameterValues();
                    
                    q.answer( a1.getBaseSymbol().equals(register) &&
                            a2.getBaseSymbol().equals(login) &&
                            Arrays.equals(a1Params, a2Params));
                    
                    
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

            @Override
            public Branching getInitialBranching(SymbolicDecisionTree merged, VarsToInternalRegs vtir, ParValuation... parval) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };

        Theory<PassType> passTheory = new EqualityTheory<PassType>() {

            @Override
            public DataValue<PassType> getFreshValue(List<DataValue<PassType>> vals) {
                DataValue v = vals.get(0);
                return new DataValue(v.getType(), 
                        v.getId().toString() + "_" + vals.size());
            }

            @Override
            public Branching getInitialBranching(SymbolicDecisionTree merged, VarsToInternalRegs vtir, ParValuation... parval) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        };
        
        Map<DataType, Theory> theories = new HashMap<>();
        theories.put(userType, userTheory);
        theories.put(passType, passTheory);
        
        TreeOracle treeOracle = new TreeOracle(dwOracle, theories);
        
        TreeQueryResult res = treeOracle.treeQuery(prefix, symSuffix);
    } 
            
            
    
}
