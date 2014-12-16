/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.theory.Guard;
import de.learnlib.ralib.theory.IntType;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.TreeOracle;
import de.learnlib.ralib.theory.TreeQueryResult;
import de.learnlib.ralib.trees.SDT;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author Sofia Cassel
 * @param<T>
 */
public abstract class InequalityTheory<T> implements Theory<T> {

    IntType intType = new IntType();

    private Set<Guard> mergeGuardArrays(Set<Guard> one, Set<Guard> two) {
        return removeCo(one,two);
    }
    
    private Set<Guard> removeCo(Set<Guard> one, Set<Guard> two) {
        Set<Guard> onetwoSet = new HashSet();

        // remove contradicting guards
        Set<Guard> cSet = new HashSet();
        for (Guard o : one) {
            for (Guard t : two) {
                if (o.contradicts(t)) {
                    //System.out.println("CONTRADICTION");
                    cSet.add(o);
                    cSet.add(t);
                }
            }
        }

        //System.out.println("Contradictions: " + cSet.toString());
        for (Guard o : one) {
            for (Guard t : two) {
                if (!cSet.contains(o)) {
                    onetwoSet.add(o);
                }
                if (!cSet.contains(t)) {
                    onetwoSet.add(t);
                }
            }

        }

        return onetwoSet;
    }
    
    
    private Map<Set<Guard>, SymbolicDecisionTree> tryToMerge(Set<Guard> guard, 
            List<Set<Guard>> targetList, Map<Set<Guard>, SymbolicDecisionTree> refSDTMap, 
            Map<Set<Guard>, SymbolicDecisionTree> finalMap, Map<Set<Guard>, SymbolicDecisionTree> contMap) {
        System.out.println("---- Merging ----\nguard: " + guard.toString() + "\ntargetList: " + targetList.toString() + "\nfinalMap " + finalMap.toString() + "\ncontMap " + contMap.toString());
    Map<Set<Guard>, SymbolicDecisionTree> cMap = new HashMap();
    cMap.putAll(contMap);
        if (targetList.isEmpty()) {
            //finalMap.put(guard, refSDTMap.get(guard));
            finalMap.put(guard, refSDTMap.get(guard));
            finalMap.putAll(cMap);
            System.out.println( " ---> " + finalMap.toString());
            return finalMap;
        }
        else {
                Map<Set<Guard>, SymbolicDecisionTree> newSDTMap = new HashMap();
        newSDTMap.putAll(refSDTMap);
        SymbolicDecisionTree guardSDT = newSDTMap.get(guard);
         List<Set<Guard>> newTargetList = new ArrayList();
        newTargetList.addAll(targetList);
        Set<Guard> other = newTargetList.remove(0);
        SymbolicDecisionTree otherSDT = newSDTMap.get(other);
        cMap.put(other, otherSDT);
        
            if (guardSDT.canUse(otherSDT)) {
                // if yes, then merge them
                Set<Guard> merged = mergeGuardArrays(guard, other);
                System.out.println(guard.toString() + " and " + other.toString() + " are compatible, become " + merged.toString());
                // add the merged guard and SDT to merged map
                newSDTMap.put(merged, guardSDT);
                cMap.remove(other);
                
                return tryToMerge(merged, newTargetList, newSDTMap, new HashMap(), cMap);
            }
            else {
                return tryToMerge(guard, newTargetList, newSDTMap, new HashMap(), cMap);
            }
        }
       
    }
    
    
// given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT.  
    private Map<Set<Guard>, SymbolicDecisionTree>
            mergeGuards(Map<Set<Guard>, SymbolicDecisionTree> unmerged) {
        System.out.println("master merge...");
        Map<Set<Guard>, SymbolicDecisionTree> tempMap = new HashMap();        
        
        List<Set<Guard>> guardList = new ArrayList(unmerged.keySet());
        //for (int i = 0; i < guardList.size(); i++) {
        int i = 0;
            List<Set<Guard>> jGuardList = new ArrayList();
            jGuardList.addAll(guardList);
            Map<Set<Guard>, SymbolicDecisionTree> gMerged = tryToMerge(guardList.get(i), jGuardList.subList(i+1, guardList.size()), unmerged, new HashMap(), new HashMap());
            tempMap.putAll(gMerged);            
            //}
        System.out.println(tempMap.toString());
            if (tempMap.equals(unmerged)) {
                System.out.println("unchanged");
                return tempMap;
            }
            else {
                System.out.println("another round");
                return mergeGuards(tempMap);
            }
        }
//                
//                
//                
//                Map<Set<Guard>, SymbolicDecisionTree> mergingMap = new HashMap<>();
//        System.out.println("---- Merging! ---- " + unmerged.toString());
//        // guards we've already tried
//        Set<Set<Guard>> tried = new HashSet();
//        List<Set<Guard>> guardList = new ArrayList(unmerged.keySet());
////        for (Set<Guard> g : guardList) {
////            untried.add(g);
////        }
////        //untried.removeAll(finalMap.keySet());
//        //Set<Guard> tried = new HashSet();
//
//        // for each guard: check against all other guards, merge each one
//        // if all guards merge into one: replace this by TRUE
//        //starting list
//        //Set<Guard> guardSet = new ArrayList
//        int gSize = guardList.size();
//        
//        for (int i = 0; i < gSize - 1; i++) {
//            boolean uniq = true;
//            Set<Guard> one = guardList.get(i);
//            SymbolicDecisionTree oneSdt = unmerged.get(one);
//            System.out.print("one : " + one.toString());
//                // for each next guard array
//            for (int j = i + 1; j < gSize; j++) {
//                Set<Guard> two = guardList.get(j);
//                System.out.println("two : " + two.toString());
//                SymbolicDecisionTree twoSdt = unmerged.get(two);
//                if (oneSdt.canUse(twoSdt)) {
//                     Set<Guard> onetwo = mergeGuardArrays(one, two);
//                     System.out.println(one.toString() + " and " + two.toString() + " are compatible, become " + onetwo.toString());
//                     mergingMap.put(onetwo, twoSdt);
//                     uniq = false;
//                    }
//                }
//            if (uniq == true) {
//                finalMap.put(one, oneSdt);
//            }
//            }
//        
//        
        
        
                            
                           // if one can use two, then one is not unique, and both have been tried
//                    if (oneSdt.canUse(twoSdt) && (uniq == true)) {
//                        uniq = 0;
//                //        tried.add(one);
//                //        tried.add(two);
//                        // merge one and two, add with SDT to map
//                        Set<Guard> onetwo = mergeGuardArrays(one, two);
//                        System.out.println(one.toString() + " and " + two.toString() + " are compatible, become " + onetwo.toString());
//                        mergingMap.put(onetwo, twoSdt);
//                    }
//                    if (uniq == 0) {
//                        break;
//                    }
//               
                //if it is unique, then add to final map (because definitely unmergable)
                
           
        // if there is nothing in the merging map, then return the final map (because everything is in the final map)
//        if (mergingMap.isEmpty()) {
//            System.out.println("---- End merging ----");
//            return finalMap;
//        } // otherwise, try to merge the mergingmap
//        else {
//            return mergeGuards(mergingMap, finalMap);
//        }
//    }

    // given a set of registers and a set of guards, keep only the registers
    // that are mentioned in any guard
    private ParsInVars keepMem(ParsInVars pivs, Set<Set<Guard>> guardSet) {
        ParsInVars ret = new ParsInVars();
        for (SymbolicDataValue k : pivs.keySet()) {
            for (Set<Guard> mg : guardSet) {
                for (Guard g : mg) {
                    if (g.getRegister().equals(k)) {
                        ret.put(k, pivs.get(k));
                    }
                }
            }
        }
        return ret;
    }

    private WordValuation valuatePrefix(Word<PSymbolInstance> p, DataType dt) {
        WordValuation prefixVal = new WordValuation();
        DataValue[] untypedPV = DataWords.valsOf(p);
        for (int t = 0; t < untypedPV.length; t++) {
            DataValue curr = untypedPV[t];
            if (curr.getType().equals(dt)) {
                prefixVal.put(t+1, curr);
            }
        }
        return prefixVal;
    }
    
    private SymbolicDataValue initReg(DataValue<T> dv, WordValuation preVal, WordValuation vals, int pId, DataType type) {
        SymbolicDataValue ret;
        //ParsInVars newPiv = new ParsInVars();
        //newPiv.putAll(ifPiv);
        // if value already in prefix, get appropriate index
        Integer pvPos = preVal.getKey(dv);
        if (pvPos == null) {
            Integer vPos  = vals.getKey(dv);
            if (vPos == null) {
                ret = SymbolicDataValue.temp(type, pId);
            }
            else {
                ret = SymbolicDataValue.temp(type, vPos+preVal.size()+1);
            }
        }
        else {
        ret = SymbolicDataValue.register(type,pvPos);
        }
        return ret;
    }
    
//        private SymbolicDataValue getRightRegister(int i, List<DataValue<T>> pList, WordValuation preVal, int curr_id, DataType type) {
//        DataValue<T> right = pList.get(i);
//        Integer rightPos = preVal.getKey(right);
//        if (rightPos == null) {
//        //nextPos = (suffixValues.getKey(next).getId()); // + prefixValuation.size()+1);
//        rightPos = (preVal.size() + curr_id);
//                }
//        return SymbolicDataValue.register(type, rightPos);
//    }
//    
//    prev = potList.get(i - 1);
//                prevPos = prefixValuation.getKey(prev);  // this is either 1 or 2
//                if (prevPos == null) {
//                    //prevPos = (suffixValues.getKey(prev).getId()); // + prefixValuation.size()+1);
//                    prevPos = (prefixValuation.size() + pId);
//                }

    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix,
            WordValuation values,
            ParsInVars piv,
            VarValuation suffixValues,
            TreeOracle oracle) {

        System.out.println("Prefix: " + prefix);
        System.out.println("Suffix: " + suffix);
        System.out.println("values " + values.toString());
        System.out.println("suffix values: " + suffixValues.toString());

        int pId = values.size() + 1;
        // System.out.println("pId " + pId);

        SymbolicDataValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();

        WordValuation prefixValuation = valuatePrefix(prefix, type);
        // System.out.println("prefix valuation: " + prefixValuation.toString());

        Map<Set<Guard>, SymbolicDecisionTree> tempKids = new HashMap<>();
        ParsInVars ifPiv = new ParsInVars();
        ifPiv.putAll(piv);
        // System.out.println("ifpiv is " + ifPiv.toString());

        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                DataWords.<T>valSet(prefix, type),
                suffixValues.<T>values(type));

        List<DataValue<T>> potList = new ArrayList<>(potSet);
        List<DataValue<T>> potential = getPotential(potList);
        int potSize = potential.size();
        // System.out.println("potential is: " + potential.toString());

        // process each '<' case
        for (int i = 0; i < potSize; i++) {
            DataValue<T> currentDv = potential.get(i);

            WordValuation currentValues = new WordValuation();
            currentValues.putAll(values);
            currentValues.put(pId, currentDv);

          //  System.out.println("new values " + currentValues.toString());

            VarValuation currentSuffixValues = new VarValuation();
            currentSuffixValues.putAll(suffixValues);
            currentSuffixValues.put(sv, currentDv);
//
//            Integer prevPos;
//            Integer nextPos;
//
//            DataValue<T> prev = null;
//            DataValue<T> next = null;
//
//            SymbolicDataValue rvPrev = null;
//            SymbolicDataValue rvNext = null;

            Set<Guard> guardSet = new HashSet();
            
            // SMALLEST case
            if (i == 0) {
                DataValue<T> dvRight = potList.get(i);
                //int pos = suffixValues.getKey(dvRight).getId();
                SymbolicDataValue rvRight = initReg(dvRight, prefixValuation, values, pId, type);
                ifPiv.put(rvRight, dvRight);
                guardSet.add(new SmallerGuard(sv, rvRight));
                
            }
            
            // BIGGEST case
            else if (i == potSize-1) {
                DataValue<T> dvLeft = potList.get(i-1);
                //int pos = suffixValues.getKey(dvLeft).getId();
                SymbolicDataValue rvLeft = initReg(dvLeft, prefixValuation, values, pId, type);
                ifPiv.put(rvLeft, dvLeft);
                guardSet.add(new BiggerGuard(sv, rvLeft));
            }
            
            // MIDDLE cases
            else {
                DataValue<T> dvLeft = potList.get(i-1);
                DataValue<T> dvRight = potList.get(i);
                
//                int lPos = suffixValues.getKey(dvLeft).getId();
//                int rPos = suffixValues.getKey(dvRight).getId();
//                                
                SymbolicDataValue rvLeft = initReg(dvLeft, prefixValuation, values, pId, type);
                ifPiv.put(rvLeft, dvLeft);
                guardSet.add(new BiggerGuard(sv, rvLeft));
                
                SymbolicDataValue rvRight = initReg(dvRight, prefixValuation, values, pId, type);
                ifPiv.put(rvRight, dvRight);
                guardSet.add(new SmallerGuard(sv, rvRight));
            }
            
            System.out.println("Guard set: " + guardSet.toString());
           
// for the 1 - last value in the potential
//            if (i > 0) {
//                // prev is the 0 - second last of potential (i.e. something that is < currentDv)
//                prev = potList.get(i - 1);
//                prevPos = prefixValuation.getKey(prev);  // this is either 1 or 2
//                if (prevPos == null) {
//                    //prevPos = (suffixValues.getKey(prev).getId()); // + prefixValuation.size()+1);
//                    prevPos = (prefixValuation.size() + pId);
//                }
//                rvPrev = SymbolicDataValue.register(type, prevPos);
//                ifPiv.put(rvPrev, prev);
//            }
//
//            if (next != null) {
//                guardSet.add(new SmallerGuard(sv, rvNext));
//            }
//            if (prev != null) {
//                guardSet.add(new BiggerGuard(sv, rvPrev));
//            }
            TreeQueryResult oracleReply = oracle.treeQuery(
                    prefix, suffix, currentValues, ifPiv, currentSuffixValues);
            SymbolicDecisionTree oracleSdt = oracleReply.getSdt();
            tempKids.put(guardSet, oracleSdt);
            

        }

//        System.out.println("-------> Level finished!\nTemporary guards = " + tempKids.keySet().toString());

        // merge the guards
        Map<Set<Guard>, SymbolicDecisionTree> merged = mergeGuards(tempKids);

        // only keep registers that are referenced by the merged guards
        ParsInVars addPiv = keepMem(ifPiv, merged.keySet());

        //System.out.println("temporary guards = " + tempKids.keySet());
        //System.out.println("temporary pivs = " + tempPiv.keySet());
        System.out.println("merged guards = " + merged.keySet().toString());
        //System.out.println("merged pivs = " + addPiv.toString());

        // clear the temporary map of children
        tempKids.clear();

        SDT returnSDT = new SDT(true, addPiv.keySet(), merged);

        return new TreeQueryResult(addPiv, null, returnSDT);

    }
}
