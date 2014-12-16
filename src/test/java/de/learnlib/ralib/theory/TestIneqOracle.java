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
import de.learnlib.ralib.theory.inequality.InequalityTheory;
import de.learnlib.ralib.theory.inequality.Compatibility;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
@Test
public class TestIneqOracle {

    public void testIneqOracle() {
        
        // define types
        
        final IntType intType = new IntType();
        final DoubType doubType = new DoubType();
        //final CompIntType intTypeComparator = new CompIntType();
    
                // define parameterized symbols
        final ParameterizedSymbol ini = new ParameterizedSymbol(
                "initialize", new DataType[] {intType});
        
        final ParameterizedSymbol lower = new ParameterizedSymbol(
                "lower", new DataType[] {intType});
        
        final ParameterizedSymbol higher = new ParameterizedSymbol(
                "higer", new DataType[] {intType});
        
        // create prefix: register(falk[userType], secret[passType])
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(ini, 
                    new DataValue(intType, 4)),
                new PSymbolInstance(lower, 
                    new DataValue(intType, 3))
            );
        
        // create suffix: login(falk[userType], secret[passType])

        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(lower, 
                    new DataValue(intType, 2)),
                new PSymbolInstance(higher, 
                    new DataValue(intType, 5)),
                new PSymbolInstance(lower, 
                    new DataValue(intType, 2))
                    );
        
        
        
        
   
        // hacked oracle
        
                DataWordOracle dwOracle = new DataWordOracle() {
            @Override
            public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
                
                // given a collection of queries, process each one (with Bool replies)
                
                //CompIntType fakeIntComp = new CompIntType();
                
                for (Query q : clctn) {
                    Word<PSymbolInstance> oTrace = q.getInput();     
                    
                    System.out.println("Original trace = " + oTrace.toString() + " >>> ");
                    
                    Word<PSymbolInstance> trace = Compatibility.intify(oTrace);     
                    System.out.print("Trace = " + trace.toString() + " >>> ");
                    //(qOut ? "ACCEPT (+)" : "REJECT (-)"));
            
                    // the trace now contains only integer values
                    
                    // if the trace is not 5, answer false (since then automatically incorrect)
                    
                    if (trace.length() != 5) {
                        q.answer(false);
                        continue;
                    }
                    
                    // get the first two symbols in the trace
                    
                    PSymbolInstance a1 = trace.getSymbol(0);
                    PSymbolInstance a2 = trace.getSymbol(1);
                    PSymbolInstance a3 = trace.getSymbol(2);
                    PSymbolInstance a4 = trace.getSymbol(3);
                    PSymbolInstance a5 = trace.getSymbol(4);
                    
                    DataValue<Integer>[] a1Params = a1.getParameterValues();
                    DataValue<Integer>[] a2Params = a2.getParameterValues();
                    DataValue<Integer>[] a3Params = a3.getParameterValues();
                    DataValue<Integer>[] a4Params = a4.getParameterValues();
                    DataValue<Integer>[] a5Params = a5.getParameterValues();
                    
                    // query reply is ACCEPT only if ...
                    
                    q.answer( a1.getBaseSymbol().equals(ini) &&
                            a2.getBaseSymbol().equals(lower) && 
                            a3.getBaseSymbol().equals(lower) && 
                            a4.getBaseSymbol().equals(higher)&& 
                            a5.getBaseSymbol().equals(lower)&& 
                            (intType.compare(a1Params[0], a4Params[0]) < 0) && 
                            (intType.compare(a5Params[0], a3Params[0]) < 0) && 
                            (intType.compare(a3Params[0], a2Params[0]) < 0) &&
                            (intType.compare(a2Params[0], a1Params[0]) < 0));
                   
                }
            }
        };

        
        Theory<Double> doubTheory = new InequalityTheory<Double>() {

            @Override
            public Branching getInitialBranching(SymbolicDecisionTree merged, VarsToInternalRegs vtir, ParValuation... parval) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            
            //no fresh value in this theory
            @Override
            public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
                return null;
            }
            
            public List<DataValue<Double>> getPotential(List<DataValue<Double>> dvs) {
                //assume we can just sort the list and get the values
                List<DataValue<Double>> sortedList = dvs;
                Collections.sort(sortedList,doubType);
                List<DataValue<Double>> retList = new ArrayList<DataValue<Double>>();
                int listSize = sortedList.size();
                
                if (dvs.size() < 2) {
                    retList.add(new DataValue(doubType, dvs.get(0).getId()-1));
                    retList.add(new DataValue(doubType, dvs.get(0).getId()+1));
                }
                
                else {
                // create smallest
                DataValue<Double> first = sortedList.get(0);
                double firstInterval = (sortedList.get(1).getId() - first.getId())/2;
                retList.add(new DataValue(doubType, first.getId()-firstInterval));
                
                // create middle
                for (int j = 0; j < listSize-1; j++) {
                    DataValue<Double> curr = sortedList.get(j);
                    double interval = (sortedList.get(j+1).getId() - curr.getId())/2;
                    retList.add(new DataValue(doubType, curr.getId()+interval));
                }
                
                // create biggest
                DataValue<Double> last = sortedList.get(listSize-1);
                double lastInterval = (last.getId() - sortedList.get(listSize-2).getId());
                retList.add(new DataValue(doubType, last.getId()+lastInterval));
                }
                return retList;
                
            }
            
            
            
        };

        Map<DataType, Theory> theories = new HashMap();
        theories.put(doubType,doubTheory);
        
        TreeOracle treeOracle = new TreeOracle(dwOracle, theories);
        
        //Word<PSymbolInstance>newPrefix = Compatibility.reinstantiatePrefix(prefix,DataWords.paramLength(symSuffix.getActions()));
        //System.out.println("reinstantiated prefix: " + newPrefix.toString());        
        //TreeQueryResult res = treeOracle.treeQuery(prefix, symSuffix);
//        System.out.println(res.getSdt().isAccepting());
        
//        List<DataValue<Integer>> datavalues = new ArrayList();
//        //System.out.println("array defined " + datavalues.toString());
//        //DVs must be ordered
//        DataValue<Integer> dv2 = new DataValue(intType, 2);
//        DataValue<Integer> dv7 = new DataValue(intType, 7);
//        DataValue<Integer> dv4 = new DataValue(intType, 4);
//        datavalues.add(dv2);
//        datavalues.add(dv7);
//        datavalues.add(dv4);
//        System.out.println("datavalues" + datavalues.toString());
//        
//        System.out.println(intTheory.getPotential(datavalues).toString());
        
        final Word<PSymbolInstance> dPrefix = Compatibility.doublify(prefix);
        final SymbolicSuffix symSuffix = new SymbolicSuffix(dPrefix, Compatibility.doublify(suffix));
        System.out.println("Prefix: " + dPrefix);
        System.out.println("Suffix: " + symSuffix);

        
        TreeQueryResult res = treeOracle.treeQuery(dPrefix, symSuffix);
//        System.out.println(res.getSdt().isAccepting());
        System.out.println("final SDT: \n" + res.getSdt().toString());
    } 
            
            
    
}
