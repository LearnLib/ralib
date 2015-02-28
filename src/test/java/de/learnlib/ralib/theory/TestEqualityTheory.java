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
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class TestEqualityTheory {

    
    @Test
    public void testLoginExample1() {
    
        DataWordOracle oracle = new SimulatorOracle(AUTOMATON);
            
        Theory<Integer> uidTheory = new EqualityTheory<Integer>() {
            @Override
            public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
                DataValue v = vals.get(0);
                return new DataValue(v.getType(), vals.size() + 1);
            }

        };    
    
        Theory<Integer> pwdTheory = new EqualityTheory<Integer>() {
            @Override
            public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
                DataValue v = vals.get(0);
                return new DataValue(v.getType(), vals.size() + 1);
            }


        };  
    
        Map<DataType, Theory> theories = new HashMap();
        theories.put(T_UID, uidTheory);
        theories.put(T_PWD, pwdTheory);
        
        MultiTheoryTreeOracle treeOracle = new MultiTheoryTreeOracle(oracle, theories);
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 2),
                    new DataValue(T_PWD, 2)));           
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGOUT),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));
        
        //final Word<PSymbolInstance> testprefix = Word.fromSymbols(
        //        new PSymbolInstance(I_REGISTER, 
        //            new DataValue(T_UID, 1),
        //            new DataValue(T_PWD, 1)));
        
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, longsuffix);
        System.out.println("Prefix: " + prefix);
        System.out.println("Suffix: " + symSuffix);        
        
        TreeQueryResult res = treeOracle.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();
//        System.out.println(res.getSdt().isAccepting());
        System.out.println("final SDT: \n" + sdt.toString());
        
//        Parameter p1 = new Parameter(T_UID, 1);
//        Parameter p2 = new Parameter(T_PWD, 2);
//        DataValue d1 = new DataValue(T_UID, 1);
//        DataValue d2 = new DataValue(T_PWD, 1);
//        
//        PIV testPiv =  new PIV();
//        testPiv.put(p1, new Register(T_UID, 1));
//        testPiv.put(p2, new Register(T_PWD, 2));
//        
//        ParValuation testPval = new ParValuation();
//        testPval.put(p1, d1);
//        testPval.put(p2,d2);
//    
//        System.out.println("branching");
//        System.out.println("initial branching: \n" + treeOracle.getInitialBranching(testprefix, I_LOGIN, testPiv, testPval, sdt));
    }
    
    
    
}
