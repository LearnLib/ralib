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
package de.learnlib.ralib.theory.equality;

import static de.learnlib.ralib.theory.DataRelation.DEQ;
import static de.learnlib.ralib.theory.DataRelation.EQ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.api.Query;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
@Test
public class TestTreeOracle extends RaLibTestSuite {
   

    public void testTreeOracle() {
        
        // define types        
        final DataType userType = new DataType("userType", String.class);
        final DataType passType = new DataType("passType", String.class);
        
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
        
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(login, 
                    new DataValue(userType, "falk"),
                    new DataValue(passType, "secret"))
                    );
        
        DataWordOracle dwOracle = new DataWordOracle() {
            @Override
            public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
                
                // given a collection of queries, process each one (with Bool replies)
                
                for (Query q : clctn) {
                    Word<PSymbolInstance> trace = q.getInput();
                    
                    if (trace.length() != 2) {
                        q.answer(false);
                        continue;
                    }
                    
                    // get the first two symbols in the trace                   
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
        
        Theory<String> userTheory = new EqualityTheory<String>() {

            @Override
            public DataValue getFreshValue(List<DataValue<String>> vals) {
                DataValue v = vals.get(0);
                return new DataValue(v.getType(), 
                        v.getId().toString() + "_" + vals.size());
            }

            @Override
            public Collection<DataValue<String>> getAllNextValues(List<DataValue<String>> vals) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public List<EnumSet<DataRelation>> getRelations(List<DataValue<String>> left, DataValue<String> right) {
                List<EnumSet<DataRelation>> ret = new ArrayList<>();
                for (DataValue<String> v : left) {
                    ret.add( v.equals(right) ? EnumSet.of(DEQ, EQ) : EnumSet.noneOf(DataRelation.class));
                }
                return ret;                
            }

          
        };

        Theory<String> passTheory = new EqualityTheory<String>() {

            @Override
            public DataValue<String> getFreshValue(List<DataValue<String>> vals) {
                DataValue v = vals.get(0);
                return new DataValue(v.getType(), 
                        v.getId().toString() + "_" + vals.size());
            }

            @Override
            public Collection<DataValue<String>> getAllNextValues(List<DataValue<String>> vals) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public List<EnumSet<DataRelation>> getRelations(List<DataValue<String>> left, DataValue<String> right) {
                List<EnumSet<DataRelation>> ret = new ArrayList<>();
                for (DataValue<String> v : left) {
                    ret.add( v.equals(right) ? EnumSet.of(DEQ, EQ) : EnumSet.of(DEQ));
                }
                return ret;                
            }

        };
        
        Map<DataType, Theory> theories = new LinkedHashMap();
        theories.put(userType, userTheory);
        theories.put(passType, passTheory);
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)        
        final GeneralizedSymbolicSuffix symSuffix = 
                new GeneralizedSymbolicSuffix(prefix, suffix,
                        new Constants(), theories);
        //System.out.println("Prefix: " + prefix);
        //System.out.println("Suffix: " + symSuffix);
        
        
        MultiTheoryTreeOracle treeOracle = new MultiTheoryTreeOracle(
                dwOracle, null, theories, 
                new Constants(), new SimpleConstraintSolver());
        
        TreeQueryResult res = treeOracle.treeQuery(prefix, symSuffix);

        String expectedTree = "[r2, r1]-+\n" +
"        []-(s1=r2)\n" +
"         |    []-(s2=r1)\n" +
"         |     |    [Leaf+]\n" +
"         |     +-(s2!=r1)\n" +
"         |          [Leaf-]\n" +
"         +-(s1!=r2)\n" +
"              []-TRUE: s2\n" +
"                    [Leaf-]\n";
        
        String tree = res.getSdt().toString();
        Assert.assertEquals(tree, expectedTree);
        
        logger.log(Level.FINE, "final SDT: \n{0}", tree);
        
    } 
            
            
    
}
