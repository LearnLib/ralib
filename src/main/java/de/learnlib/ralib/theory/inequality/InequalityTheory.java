/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.IntType;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
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
    
    private List<SDTGuard> mergeGuardLists(List<SDTGuard> one, List<SDTGuard> two) {
        return removeCo(one,two);
    }
    
    private List<SDTGuard> removeCo(List<SDTGuard> one, List<SDTGuard> two) {
        List<SDTGuard> onetwoList = new ArrayList();

        // remove contradicting guards
        List<SDTGuard> cList = new ArrayList();
        for (SDTGuard o : one) {
            for (SDTGuard t : two) {
                if (o.contradicts(t)) {
                    //System.out.println("CONTRADICTION");
                    cList.add(o);
                    cList.add(t);
                }
            }
        }

        //System.out.println("Contradictions: " + cSet.toString());
        for (SDTGuard o : one) {
            for (SDTGuard t : two) {
                if (!cList.contains(o)) {
                    onetwoList.add(o);
                }
                if (!cList.contains(t)) {
                    onetwoList.add(t);
                }
            }

        }

        return onetwoList;
    }
    
      
    
    private Map<List<SDTGuard>, SDT> tryToMerge(List<SDTGuard> guard, 
            List<List<SDTGuard>> targetList, Map<List<SDTGuard>, SDT> refSDTMap, 
            Map<List<SDTGuard>, SDT> finalMap, Map<List<SDTGuard>, SDT> contMap) {
        System.out.println("---- Merging ----\nguard: " + guard.toString() + "\ntargetList: " + targetList.toString() + "\nfinalMap " + finalMap.toString() + "\ncontMap " + contMap.toString());
    Map<List<SDTGuard>, SDT> cMap = new HashMap();
    cMap.putAll(contMap);
        if (targetList.isEmpty()) {
            //finalMap.put(guard, refSDTMap.get(guard));
            finalMap.put(guard, refSDTMap.get(guard));
            finalMap.putAll(cMap);
            System.out.println( " ---> " + finalMap.toString());
            return finalMap;
        }
        else {
                Map<List<SDTGuard>, SDT> newSDTMap = new HashMap();
        newSDTMap.putAll(refSDTMap);
        SDT guardSDT = newSDTMap.get(guard);
         List<List<SDTGuard>> newTargetList = new ArrayList();
        newTargetList.addAll(targetList);
        List<SDTGuard> other = newTargetList.remove(0);
        SDT otherSDT = newSDTMap.get(other);
        cMap.put(other, otherSDT);
        
            if (guardSDT.canUse(otherSDT) && otherSDT.canUse(guardSDT)) {
                // if yes, then merge them
                List<SDTGuard> merged = mergeGuardLists(guard, other);
                System.out.println(guard.toString() + " and " + other.toString() + " are compatible, become " + merged.toString() + " using SDT " + otherSDT.toString());
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
    private Map<List<SDTGuard>, SDT>
            mergeGuards(Map<List<SDTGuard>, SDT> unmerged) {
        System.out.println("master merge...");
        Map<List<SDTGuard>, SDT> tempMap = new HashMap();        
        
        List<List<SDTGuard>> guardList = new ArrayList(unmerged.keySet());
        System.out.println("unmerged: " + unmerged.toString());
        //int i = 0;
            List<List<SDTGuard>> jGuardList = new ArrayList();
            jGuardList.addAll(guardList);
            Map<List<SDTGuard>, SDT> gMerged = tryToMerge(guardList.get(0), jGuardList.subList(1, guardList.size()), unmerged, new HashMap(), new HashMap());
            tempMap.putAll(gMerged);   
            
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
//                Map<Set<Guard>, SDT> mergingMap = new HashMap<>();
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
//            SDT oneSdt = unmerged.get(one);
//            System.out.print("one : " + one.toString());
//                // for each next guard array
//            for (int j = i + 1; j < gSize; j++) {
//                Set<Guard> two = guardList.get(j);
//                System.out.println("two : " + two.toString());
//                SDT twoSdt = unmerged.get(two);
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
    private ParsInVars keepMem(ParsInVars pivs, Set<List<SDTGuard>> guardSet) {
        ParsInVars ret = new ParsInVars();
        for (Register k : pivs.keySet()) {
            for (List<SDTGuard> mg : guardSet) {
                for (SDTGuard g : mg) {
                    if (g.getRegister().equals(k)) {
                        ret.put(k, pivs.get(k));
                    }
                }
            }
        }
        return ret;
    }
    
//    private int getFirstOcc(WordValuation preVal, WordValuation curVal, DataValue<T> dv) {
//        System.out.println(dv.toString() + " should be in " + preVal.toString() + " or in "+ curVal.toString());
//        List<Integer> rvPoss1 = new ArrayList(preVal.getAllKeys(dv));
//        List<Integer> rvPoss2 = new ArrayList(curVal.getAllKeys(dv));
//        if (rvPoss1.isEmpty()) {
//            return Collections.min(rvPoss2);
//        }
//        else if (rvPoss2.isEmpty()) {
//            return Collections.min(rvPoss1);
//        }
//        else {
//            System.out.println("possible indices: " + rvPoss1.toString() + " or " + rvPoss2.toString());
//            Integer rvPos1 = Collections.min(rvPoss1);
//            Integer rvPos2 = Collections.min(rvPoss2);
//            return (rvPos1 < rvPos2) ? rvPos1 : rvPos2;
//        }
//    }

        private int getFirstOcc(WordValuation preVal, WordValuation curVal, DataValue<T> dv) {
        System.out.println(dv.toString() + " should be in preVal = " + preVal.toString() + " or in curVal = "+ curVal.toString());
            Integer rvPos = preVal.getOneKey(dv);
            if (rvPos !=null) {
                return rvPos;
            }
            else {
                List<Integer> rvPositions = new ArrayList(curVal.getAllKeys(dv));
                    return Collections.min(rvPositions) + preVal.size();
            }
        }
    
    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix,
            WordValuation values,
            ParsInVars piv,
            SuffixValuation suffixValues,
            SDTConstructor oracle) {

           
//        System.out.println("Prefix: " + prefix);
//        System.out.println("Suffix: " + suffix);
//        System.out.println("values " + values.toString());
//        System.out.println("suffix values: " + suffixValues.toString());

        //int offset = DataWords.valsOf(prefix).length;
        int pId = values.size() + 1;
        // System.out.println("pId " + pId);

        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();

        DataValue<T>[] prefixValues = DataWords.valsOf(prefix, type);
        WordValuation prefixValuation = new WordValuation();
        for (int i = 0; i < prefixValues.length; i++)
            prefixValuation.put(i+1, prefixValues[i]);
        
        //int pId = sId + prefixValuation.size();
        
        Parameter currentParam = new Parameter(type, pId+prefixValuation.size());    
        

    //    WordValuation prefixValuation = valuatePrefix(prefix, type);
        // System.out.println("prefix valuation: " + prefixValuation.toString());

        Map<List<SDTGuard>, SDT> tempKids = new HashMap<>();
        ParsInVars ifPiv = new ParsInVars();
        ifPiv.putAll(piv);
        // System.out.println("ifpiv is " + ifPiv.toString());

        
        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                DataWords.<T>valSet(prefix, type),
                suffixValues.<T>values(type));

        List<DataValue<T>> potList = new ArrayList<>(potSet);
        List<DataValue<T>> potential = getPotential(potList);
        int potSize = potential.size();
         System.out.println("potential is: " + potential.toString());

        // process each '<' case
        for (int i = 0; i < potSize; i++) {
            DataValue<T> currentDv = potential.get(i);

            WordValuation currentValues = new WordValuation();
            currentValues.putAll(values);
            currentValues.put(pId, currentDv);
            
          //  System.out.println("new values " + currentValues.toString());

            SuffixValuation currentSuffixValues = new SuffixValuation();
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

            List<SDTGuard> guards = new ArrayList();
            
            // SMALLEST case
            if (i == 0) {
                DataValue<T> dvRight = potList.get(i);
                Integer rvPos = getFirstOcc(prefixValuation, currentValues, dvRight);
                
                Register rvRight = ifPiv.getOneKey(currentDv);
                if (rvRight == null) {
                    rvRight = new Register(type, rvPos);
                    ifPiv.put(rvRight, dvRight);
                }
                
                //int pos = suffixValues.getKey(dvRight).getId();
                //Register rvRight = regGenerator.next(type);
//                SymbolicDataValue rvRight = initReg(dvRight, prefixValuation, values, pId, type);
                guards.add(new SmallerGuard(currentParam, rvRight));
                
            }
            
            // MIDDLE cases
            else if (i > 0 && i < potSize-1) {
                DataValue<T> dvLeft = potList.get(i-1);
                Integer lvPos = getFirstOcc(prefixValuation, currentValues, dvLeft);
                
                DataValue<T> dvRight = potList.get(i);
                Integer rvPos = getFirstOcc(prefixValuation, currentValues, dvRight);
//                                
                Register rvLeft = ifPiv.getOneKey(currentDv);
                if (rvLeft == null) {
                    rvLeft = new Register(type, lvPos);
                    ifPiv.put(rvLeft, dvLeft);
                }
                guards.add(new BiggerGuard(currentParam, rvLeft));
                //SymbolicDataValue rvRight = initReg(dvRight, prefixValuation, values, pId, type);
                
                Register rvRight = ifPiv.getOneKey(currentDv);
                if (rvRight == null) {
                    rvRight = new Register(type, rvPos);
                    ifPiv.put(rvRight, dvRight);
                }
                guards.add(new SmallerGuard(currentParam, rvRight));
            }

            // BIGGEST case
            else {
                DataValue<T> dvLeft = potList.get(i-1);
                Integer lvPos = getFirstOcc(prefixValuation, currentValues, dvLeft);
                
                Register rvLeft = ifPiv.getOneKey(currentDv);
                if (rvLeft == null) {
                    rvLeft = new Register(type, lvPos);
                    ifPiv.put(rvLeft, dvLeft);
                }
                guards.add(new BiggerGuard(currentParam, rvLeft));
            }

            
            System.out.println("Guard list: " + guards.toString());
           
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
            SDT oracleSdt = oracle.treeQuery(
                    prefix, suffix, currentValues, ifPiv, currentSuffixValues);
            
            tempKids.put(guards, oracleSdt);
            

        }

//        System.out.println("-------> Level finished!\nTemporary guards = " + tempKids.keySet().toString());

        // merge the guards
        Map<List<SDTGuard>, SDT> merged = mergeGuards(tempKids);

        // only keep registers that are referenced by the merged guards
        ParsInVars addPiv = keepMem(ifPiv, merged.keySet());

        //System.out.println("temporary guards = " + tempKids.keySet());
        //System.out.println("temporary pivs = " + tempPiv.keySet());
        System.out.println("merged guards = " + merged.keySet().toString());
        //System.out.println("merged pivs = " + addPiv.toString());

        // clear the temporary map of children
        tempKids.clear();

        SDT returnSDT = new SDT(merged);

        return returnSDT;

    }

    protected abstract List<DataValue<T>> getPotential(List<DataValue<T>> potList);

}
