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
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.theory.inequality.InequalityTheory;
import de.learnlib.ralib.theory.inequality.Compatibility;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
public class TestIneqOracle {

    public void testIneqOracle() {

        // define types
        final IntType intType = new IntType();
        final DoubType doubType = new DoubType();
        final CompTypes cparator = new CompTypes();

        // define parameterized symbols
        final ParameterizedSymbol ini = new InputSymbol(
                "initialize", new DataType[]{intType});

        final ParameterizedSymbol lower = new InputSymbol(
                "lower", new DataType[]{intType});

        final ParameterizedSymbol higher = new InputSymbol(
                "higer", new DataType[]{intType});

        // create prefix: register(falk[userType], secret[passType])
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(ini,
                        new DataValue(intType, 4)),
                new PSymbolInstance(lower,
                        new DataValue(intType, 3))
        );

        // create suffix: login(falk[userType], secret[passType])
        // longer suffix
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(lower,
                        new DataValue(intType, 2)),
                new PSymbolInstance(higher,
                        new DataValue(intType, 5)));

        // shorter suffix
//        final Word<PSymbolInstance> suffix = Word.fromSymbols(
//                new PSymbolInstance(higher, 
//                    new DataValue(intType, 5))
//                    );
        // hacked oracle
        DataWordOracle dwOracle = new DataWordOracle() {
            @Override
            public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {

                // given a collection of queries, process each one (with Bool replies)
                //CompIntType fakeIntComp = new CompIntType();
                for (Query q : clctn) {
                    Word<PSymbolInstance> oTrace = q.getInput();

                    //System.out.println("Original trace = " + oTrace.toString() + " >>> ");
                    Word<PSymbolInstance> trace = Compatibility.intify(oTrace);
                    System.out.print("Trace = " + trace.toString() + " >>> ");
                    //(qOut ? "ACCEPT (+)" : "REJECT (-)"));

                    // the trace now contains only integer values
                    // if the trace is not 5, answer false (since then automatically incorrect)
                    if (trace.length() != 4) {
                        q.answer(false);
                        continue;
                    }

                    // get the first two symbols in the trace
                    PSymbolInstance a1 = trace.getSymbol(0);
                    PSymbolInstance a2 = trace.getSymbol(1);
                    PSymbolInstance a3 = trace.getSymbol(2);
                    PSymbolInstance a4 = trace.getSymbol(3);

                    DataValue<IntType>[] a1Params = a1.getParameterValues();
                    DataValue<IntType>[] a2Params = a2.getParameterValues();
                    DataValue<IntType>[] a3Params = a3.getParameterValues();
                    DataValue<IntType>[] a4Params = a4.getParameterValues();

//                    System.out.println("Constraint: " 
//                            + cparator.compare(a1Params[0], a4Params[0]) + " AND " 
//                            + cparator.compare(a5Params[0], a3Params[0]) + " AND " 
//                            + cparator.compare(a3Params[0], a2Params[0]) + " AND " 
//                            + cparator.compare(a2Params[0], a1Params[0]));
//        
                    // query reply is ACCEPT only if ...
                    // longer answer
                    q.answer(a1.getBaseSymbol().equals(ini)
                            && a2.getBaseSymbol().equals(lower)
                            && a3.getBaseSymbol().equals(lower)
                            && a4.getBaseSymbol().equals(higher)
                            && (cparator.compare(a1Params[0], a4Params[0]) < 0)
                            && (cparator.compare(a3Params[0], a2Params[0]) < 0)
                            && (cparator.compare(a2Params[0], a1Params[0]) < 0));
//                  
                    // shorter answer
//                    q.answer( a1.getBaseSymbol().equals(ini) &&
//                            a2.getBaseSymbol().equals(lower) && 
//                            a3.getBaseSymbol().equals(higher) && 
//                            (cparator.compare(a1Params[0], a2Params[0]) > 0) && 
//                            (cparator.compare(a3Params[0], a1Params[0])< 0));

                }
            }
        };

        Theory<DoubType> doubTheory = new InequalityTheory<DoubType>() {

            public List<DataValue<DoubType>> getPotential(List<DataValue<DoubType>> dvs) {
                //assume we can just sort the list and get the values
                List<DataValue<DoubType>> sortedList = dvs;
                Collections.sort(sortedList, cparator);
                List<DataValue<DoubType>> retList = new ArrayList<DataValue<DoubType>>();
                int listSize = sortedList.size();
                Double first = Double.class.cast(Collections.min(sortedList, cparator).getId());

                if (dvs.size() < 2) {
                    Double doub = Double.class.cast(dvs.get(0).getId());
                    retList.add(new DataValue(doubType, doub - (doub / 2)));
                    retList.add(new DataValue(doubType, doub + (doub / 2)));
                } else {
                    // create smallest
                    Double second = Double.class.cast(sortedList.get(1).getId());
                    //double firstInterval = (second-first)/2;
                    retList.add(new DataValue(doubType, first - ((second - first) / 2)));
                //retList.add(new DataValue(doubType, first-.001));

                    // create middle
                    for (int j = 0; j < listSize - 1; j++) {
                        Double curr = Double.class.cast(sortedList.get(j).getId());
                        Double nxt = Double.class.cast(sortedList.get(j + 1).getId());
                        //double interval = (nxt - curr)/2;
                        retList.add(new DataValue(doubType, curr + ((nxt - curr) / 2)));
                        //retList.add(new DataValue(doubType, curr+.001));
                    }

                    // create biggest
                    Double last = Double.class.cast(sortedList.get(listSize - 1).getId());
                    Double prv = Double.class.cast(sortedList.get(listSize - 2).getId());
                    //double lastInterval = (last-prv);
                    retList.add(new DataValue(doubType, last + ((last - prv) / 2)));
                    //retList.add(new DataValue(doubType, last+.001));
                }
                return retList;

            }

            @Override
            public DataValue<DoubType> getFreshValue(List<DataValue<DoubType>> vals) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            
            @Override
            public DataValue instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval, Constants constants, SDTGuard guard, SymbolicDataValue.Parameter param) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        };

        Map<DataType, Theory> theories = new HashMap();
        theories.put(doubType, doubTheory);

        MultiTheoryTreeOracle treeOracle = new MultiTheoryTreeOracle(dwOracle, theories, new Constants());

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
