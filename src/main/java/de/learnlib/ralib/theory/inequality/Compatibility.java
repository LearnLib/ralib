/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.CompTypes;
import de.learnlib.ralib.theory.DoubType;
//import de.learnlib.ralib.theory.CompIntType;
import de.learnlib.ralib.theory.IntType;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author Sofia Cassel
 */
public class Compatibility {

    final static IntType intType = new IntType();
    final static DoubType doubType = new DoubType();
    final static CompTypes cparator = new CompTypes();
//
//    private static Map<DataValue<Integer>, Integer> makeInitialSpace(Map<DataValue<Integer>, Integer> valMap, int space) {
//        
//          Map intMap = new LinkedHashMap<>();
////        for (DataValue<Integer> dV : valMap.keySet()) {
////            intMap.put((new DataValue(intType, dV.getId() - initialDiff)), valMap.get(dV));
////        }
////        
//        
//        List sortedKeys = new ArrayList<>(valMap.keySet());
//        Collections.sort(sortedKeys, intType);
//        int initialDiff = ((DataValue<Integer>)sortedKeys.get(0)).getId()-space;
//        
//        //List<DataValue<Integer>> sortedList = new ArrayList<>(dataValues);
//        //Collections.sort(sortedList, intType);
//        //System.out.println("list before initial space made: " + sortedKeys.toString());
//        for (DataValue<Integer> dV : valMap.keySet()) {
//            intMap.put((new DataValue(intType, dV.getId() - initialDiff)), valMap.get(dV));
//        }
//
//
////        for (int j = 0; j < sortedList.size(); j++) {
////            DataValue<Integer> curr = sortedList.get(j);
////            Integer curr_index = valMap.get(curr);
////            if (initialDiff != 0) {
////                intMap.put((new DataValue(intType, curr.getId() - initialDiff)), curr_index);
////                //System.out.println("adding chgd value to: " + intMap.toString());
////            } else {
////                intMap.put(curr, curr_index);
////            }
////        }
//        System.out.println("final initially modified map: " + intMap.toString());
//        return intMap;
//    }
//
//    private static Map<DataValue<Integer>, Integer> enlargeGaps(Map<DataValue<Integer>, Integer> sortedMap, int space) {
//        Set dataValues = sortedMap.keySet();
//        Map intMap = new LinkedHashMap<>();
//        List<DataValue<Integer>> sortedList = new ArrayList<>(dataValues);
//        Collections.sort(sortedList, intType);
//        //System.out.println("sorted list with original gaps: " + sortedList.toString());
//
//        int prevDiff;
//        DataValue<Integer> first = sortedList.get(0);
//        // add first element unchanged
//        intMap.put(first, sortedMap.get(first));
//        for (int j = 1; j < sortedList.size(); j++) {
//            DataValue<Integer> curr = sortedList.get(j);
//            DataValue<Integer> prev = sortedList.get(j - 1);
//            Integer curr_index = sortedMap.get(curr);
//            prevDiff = (curr.getId() - prev.getId());
//            if (Math.abs(prevDiff) > 1) {
//                intMap.put(curr, curr_index);
//            } else {
//                intMap.put((new DataValue(intType, curr.getId() + space - prevDiff)), curr_index);
//            }
//        }
//        System.out.println("map with gaps: " + intMap.toString());
//        return intMap;
//    }

//    public static Word<PSymbolInstance> reinstantiatePrefix(Word<PSymbolInstance> prefix, int suffixLength) {
//        assert suffixLength < 29;
//        int space = (int)Math.pow(2, suffixLength);
//        // sort values from prefix in a Map
//        DataValue[] vals = DataWords.valsOf(prefix);
//        Map<DataValue<Integer>, Integer> valMap = new LinkedHashMap<>();
//        for (int i = 0; i < vals.length; i++) {
//            if (vals[i].getType().equals(intType)) {
//                valMap.put(vals[i], i);
//            }
//        }
//        //System.out.println("prefix valMap: " + valMap.toString());
//        // run valMap through transform procedures
//        Map<DataValue<Integer>, Integer> transformedMap = enlargeGaps(makeInitialSpace(valMap, space),space);
//        // create complete map to reinstantiate prefix with
//        // first switch map
//        Map<Integer, DataValue<Integer>> switchedMap = new LinkedHashMap();
//        for (Map.Entry<DataValue<Integer>, Integer> entry : transformedMap.entrySet()) {
//            switchedMap.put(entry.getValue()+1, entry.getKey());
//        }
//
//        // update map according to vals
//        for (int j = 1; j < vals.length+1; j++) {
//            if (!switchedMap.containsKey(j)) {
//                switchedMap.put(j, vals[j]);
//
//            }
//        }
//        System.out.println("final switched map: " + switchedMap.toString());
//        return DataWords.instantiate(DataWords.actsOf(prefix), switchedMap);

//    }

    public static Word<PSymbolInstance> intify(Word<PSymbolInstance> w) {
                         Map<Integer,DataValue<?>> retMap = new LinkedHashMap();
                //used for positioning
                         DataValue<DoubType>[] valArray = DataWords.valsOf(w);
                DataValue<DoubType>[] typedArray = DataWords.valsOf(w, doubType);
                List<DataValue<DoubType>> sortList = new ArrayList();
                Collections.addAll(sortList, typedArray);
                Collections.sort(sortList,cparator);
                for (int i = 0; i < valArray.length; i++) {
                if (valArray[i].getType().equals(doubType)) {
                    int newValue = sortList.indexOf(valArray[i])+1;
                    retMap.put(i+1, new DataValue(intType,newValue));
                    
                }
                else {
                    retMap.put(i+1, valArray[i]);
                }
            }
                return DataWords.instantiate(DataWords.actsOf(w), retMap);
                     }
    
    
    public static Word<PSymbolInstance> doublify(Word<PSymbolInstance> w) {
                 Map<Integer,DataValue<? extends DataType>> retMap = new LinkedHashMap();
                //used for positioning
                DataValue[] valArray = DataWords.valsOf(w);
                //DataValue<Integer>[] typedArray = DataWords.valsOf(w, intType);
//                List<DataValue<DoubType>> sortList = new ArrayList();
//                Collections.addAll(sortList, typedArray);
//                Collections.sort(sortList,doubType);
                for (int i = 0; i < valArray.length; i++) {
                if (valArray[i].getType().equals(intType)) {
                    DataValue<Integer> oldDataValue = valArray[i];
                    System.out.println(oldDataValue.toString());
                    int oldValue = oldDataValue.getId();
                    retMap.put(i+1, new DataValue(doubType,(double)oldValue));
                    
                }
                else {
                    retMap.put(i+1, valArray[i]);
                }
            }
                return DataWords.instantiate(DataWords.actsOf(w), retMap);
                     }
    
    }

