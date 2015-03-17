/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.theory.inequality;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.IntType;
import de.learnlib.ralib.theory.SDTCompoundGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author Sofia Cassel
 * @param<T>
 */
public abstract class InequalityTheory<T> implements Theory<T> {

    private static final LearnLogger log = LearnLogger.getLogger(InequalityTheory.class); 
    
    IntType intType = new IntType();

    private SDTGuard mergeGuardLists(SDTGuard one, SDTGuard two) {
        return removeCo(one, two);
    }

    // TODO: REWRITE THIS
    private SDTGuard removeCo(SDTGuard one, SDTGuard two) {
//        SDTGuard onetwoList = new ArrayList();
//
//        // remove contradicting guards
//        List<SDTGuard> cList = new ArrayList();
//        for (SDTGuard o : one) {
//            for (SDTGuard t : two) {
//// TODO: add contradiction condition 
////               if (o.contradicts(t)) {
//                    //log.log(Level.FINEST,"CONTRADICTION");
//                    cList.add(o);
//                    cList.add(t);
// //               }
//            }
//        }
//
//        //log.log(Level.FINEST,"Contradictions: " + cSet.toString());
//        for (SDTGuard o : one) {
//            for (SDTGuard t : two) {
//                if (!cList.contains(o)) {
//                    onetwoList.add(o);
//                }
//                if (!cList.contains(t)) {
//                    onetwoList.add(t);
//                }
//            }
//
//        }

        return one;
    }

    private Map<SDTGuard, SDT> tryToMerge(SDTGuard guard,
            List<SDTGuard> targetList, Map<SDTGuard, SDT> refSDTMap,
            Map<SDTGuard, SDT> finalMap, Map<SDTGuard, SDT> contMap) {
        log.log(Level.FINEST,"---- Merging ----\nguard: " + guard.toString() + "\ntargetList: " + targetList.toString() + "\nfinalMap " + finalMap.toString() + "\ncontMap " + contMap.toString());
        Map<SDTGuard, SDT> cMap = new LinkedHashMap();
        cMap.putAll(contMap);
        if (targetList.isEmpty()) {
            //finalMap.put(guard, refSDTMap.get(guard));
            finalMap.put(guard, refSDTMap.get(guard));
            finalMap.putAll(cMap);
            log.log(Level.FINEST," ---> " + finalMap.toString());
            return finalMap;
        } else {
            Map<SDTGuard, SDT> newSDTMap = new LinkedHashMap();
            newSDTMap.putAll(refSDTMap);
            SDT guardSDT = newSDTMap.get(guard);
            List<SDTGuard> newTargetList = new ArrayList();
            newTargetList.addAll(targetList);
            SDTGuard other = newTargetList.remove(0);
            SDT otherSDT = newSDTMap.get(other);
            cMap.put(other, otherSDT);

            if (guardSDT.canUse(otherSDT) && otherSDT.canUse(guardSDT)) {
                // if yes, then merge them
                SDTGuard merged = mergeGuardLists(guard, other);
                log.log(Level.FINEST,guard.toString() + " and " + other.toString() + " are compatible, become " + merged.toString() + " using SDT " + otherSDT.toString());
                // add the merged guard and SDT to merged map
                newSDTMap.put(merged, guardSDT);
                cMap.remove(other);

                return tryToMerge(merged, newTargetList, newSDTMap, new LinkedHashMap(), cMap);
            } else {
                return tryToMerge(guard, newTargetList, newSDTMap, new LinkedHashMap(), cMap);
            }
        }

    }

// given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT.  
    private Map<SDTGuard, SDT>
            mergeGuards(Map<SDTGuard, SDT> unmerged) {
        log.log(Level.FINEST,"master merge...");
        Map<SDTGuard, SDT> tempMap = new LinkedHashMap();

        List<SDTGuard> guardList = new ArrayList(unmerged.keySet());
        log.log(Level.FINEST,"unmerged: " + unmerged.toString());
        //int i = 0;
        List<SDTGuard> jGuardList = new ArrayList();
        jGuardList.addAll(guardList);
        Map<SDTGuard, SDT> gMerged = tryToMerge(guardList.get(0), jGuardList.subList(1, guardList.size()), unmerged, new LinkedHashMap(), new LinkedHashMap());
        tempMap.putAll(gMerged);

        log.log(Level.FINEST,tempMap.toString());
        if (tempMap.equals(unmerged)) {
            log.log(Level.FINEST,"unchanged");
            return tempMap;
        } else {
            log.log(Level.FINEST,"another round");
            return mergeGuards(tempMap);
        }
    }
//                
//                
//                
//                Map<Set<Guard>, SDT> mergingMap = new LinkedHashMap<>();
//        log.log(Level.FINEST,"---- Merging! ---- " + unmerged.toString());
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
//                log.log(Level.FINEST,"two : " + two.toString());
//                SDT twoSdt = unmerged.get(two);
//                if (oneSdt.canUse(twoSdt)) {
//                     Set<Guard> onetwo = mergeGuardArrays(one, two);
//                     log.log(Level.FINEST,one.toString() + " and " + two.toString() + " are compatible, become " + onetwo.toString());
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
//                        log.log(Level.FINEST,one.toString() + " and " + two.toString() + " are compatible, become " + onetwo.toString());
//                        mergingMap.put(onetwo, twoSdt);
//                    }
//                    if (uniq == 0) {
//                        break;
//                    }
//               
    //if it is unique, then add to final map (because definitely unmergable)
        // if there is nothing in the merging map, then return the final map (because everything is in the final map)
//        if (mergingMap.isEmpty()) {
//            log.log(Level.FINEST,"---- End merging ----");
//            return finalMap;
//        } // otherwise, try to merge the mergingmap
//        else {
//            return mergeGuards(mergingMap, finalMap);
//        }
//    }
    // given a set of registers and a set of guards, keep only the registers
    // that are mentioned in any guard
    private PIV keepMem(Set<SDTGuard> guardSet) {
        PIV ret = new PIV();
        
        // 1. create guard list
        Set<SDTIfGuard> ifGuards = new HashSet<>();
        for (SDTGuard g : guardSet) {
            if (g instanceof SDTIfGuard) {
                ifGuards.add((SDTIfGuard)g);
            }
            else if (g instanceof SDTCompoundGuard) {
                ifGuards.addAll(((SDTCompoundGuard)g).getGuards());
            }
        }
        
        // 2. determine which registers to keep
        for (SDTIfGuard g : ifGuards) {
            SymbolicDataValue r = g.getRegister();
            Parameter p = new Parameter(r.getType(), r.getId());
            if (r instanceof Register) {
                ret.put(p, (Register) r);
            }
        }
        return ret;
    }

//    private int getFirstOcc(WordValuation preVal, WordValuation curVal, DataValue<T> dv) {
//        log.log(Level.FINEST,dv.toString() + " should be in " + preVal.toString() + " or in "+ curVal.toString());
//        List<Integer> rvPoss1 = new ArrayList(preVal.getAllKeys(dv));
//        List<Integer> rvPoss2 = new ArrayList(curVal.getAllKeys(dv));
//        if (rvPoss1.isEmpty()) {
//            return Collections.min(rvPoss2);
//        }
//        else if (rvPoss2.isEmpty()) {
//            return Collections.min(rvPoss1);
//        }
//        else {
//            log.log(Level.FINEST,"possible indices: " + rvPoss1.toString() + " or " + rvPoss2.toString());
//            Integer rvPos1 = Collections.min(rvPoss1);
//            Integer rvPos2 = Collections.min(rvPoss2);
//            return (rvPos1 < rvPos2) ? rvPos1 : rvPos2;
//        }
//    }
//    private int getFirstOcc(WordValuation preVal, WordValuation curVal, DataValue<T> dv) {
//        log.log(Level.FINEST,dv.toString() + " should be in preVal = " + preVal.toString() + " or in curVal = " + curVal.toString());
//        Integer rvPos = preVal.getOneKey(dv);
//        if (rvPos != null) {
//            return rvPos;
//        } else {
//            List<Integer> rvPositions = new ArrayList(curVal.getAllKeys(dv));
//            return Collections.min(rvPositions) + preVal.size();
//        }
//    }

    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix,
            WordValuation values,
            PIV piv,
            Constants constants,
            SuffixValuation suffixValues,
            SDTConstructor oracle) {

        int pId = values.size() + 1;

        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        
        DataValue<T>[] typedPrefixValues = DataWords.valsOf(prefix, type);
        WordValuation typedPrefixValuation = new WordValuation();
        for (int i = 0; i < typedPrefixValues.length; i++) {
            typedPrefixValuation.put(i + 1, typedPrefixValues[i]);
        }

        //int pId = sId + prefixValuation.size();
        SuffixValue currentParam = new SuffixValue(type, pId);

        Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();
        // log.log(Level.FINEST,"ifpiv is " + ifPiv.toString());

        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                DataWords.<T>valSet(prefix, type),
                suffixValues.<T>values(type));

        List<DataValue<T>> potList = new ArrayList<>(potSet);
        List<DataValue<T>> potential = getPotential(potList);
        int potSize = potential.size();
        
        // process each '<' case
        for (int i = 0; i < potSize; i++) {
            DataValue<T> currentDv = potential.get(i);
            
            WordValuation currentValues = new WordValuation();
            currentValues.putAll(values);
            currentValues.put(pId, currentDv);

          //  log.log(Level.FINEST,"new values " + currentValues.toString());
            SuffixValuation currentSuffixValues = new SuffixValuation();
            currentSuffixValues.putAll(suffixValues);
            //currentSuffixValues.put(sv, currentDv);

//
//            Integer prevPos;
//            Integer nextPos;
//
//            DataValue<T> prev = null;
//            DataValue<T> next = null;
//
//            SymbolicDataValue rvPrev = null;
//            SymbolicDataValue rvNext = null;
            //List<SDTGuard> guards = new ArrayList();
            SDTGuard guard;

            // SMALLEST case
            if (i == 0) {
                // find the data value we're comparing to
                DataValue<T> dvRight = potList.get(i);
                
                guard = makeSmallerGuard(dvRight, prefixValues, currentParam, currentValues, piv);
                // find the position of this data value
                
                //Integer rvPos = getFirstOcc(prefixValuation, currentValues, dvRight);

                //Register rvRight = ifPiv.getOneKey(currentDv);
                //if (rvRight == null) {
                //    rvRight = new Register(type, rvPos);
                //    ifPiv.put(rvRight, dvRight);
                //}

                //int pos = suffixValues.getKey(dvRight).getId();
                //Register rvRight = regGenerator.next(type);
//                SymbolicDataValue rvRight = initReg(dvRight, prefixValuation, values, pId, type);
                //guard = new SmallerGuard(currentParam, rvRight);

            } // MIDDLE cases
            else if (i > 0 && i < potSize - 1) {
                DataValue<T> dvRight = potList.get(i);
                DataValue<T> dvLeft = potList.get(i - 1);
                
                SmallerGuard smallerGuard = makeSmallerGuard(dvRight, prefixValues, currentParam, currentValues, piv);
                BiggerGuard biggerGuard = makeBiggerGuard(dvLeft, prefixValues, currentParam, currentValues, piv);
                guard = new SDTCompoundGuard(currentParam, smallerGuard, biggerGuard);
//                SDTIfGuard[] middleGuard = new SDTIfGuard[2];
//                Integer lvPos = getFirstOcc(prefixValuation, currentValues, dvLeft);
//
//                Integer rvPos = getFirstOcc(prefixValuation, currentValues, dvRight);
////                                
//                Register rvLeft = ifPiv.getOneKey(currentDv);
//                if (rvLeft == null) {
//                    rvLeft = new Register(type, lvPos);
//                    ifPiv.put(rvLeft, dvLeft);
//                }
//                middleGuard[0] = new BiggerGuard(currentParam, rvLeft);
//                //SymbolicDataValue rvRight = initReg(dvRight, prefixValuation, values, pId, type);
//
//                Register rvRight = ifPiv.getOneKey(currentDv);
//                if (rvRight == null) {
//                    rvRight = new Register(type, rvPos);
//                    ifPiv.put(rvRight, dvRight);
//                }
//                middleGuard[1] = new SmallerGuard(currentParam, rvRight);
//
//                guard = new SDTCompoundGuard(currentParam, middleGuard);
            } // BIGGEST case
            else {
                DataValue<T> dvLeft = potList.get(i - 1);
                
                guard = makeBiggerGuard(dvLeft, prefixValues, currentParam, currentValues, piv);
//                Integer lvPos = getFirstOcc(prefixValuation, currentValues, dvLeft);
//
//                Register rvLeft = ifPiv.getOneKey(currentDv);
//                if (rvLeft == null) {
//                    rvLeft = new Register(type, lvPos);
//                    ifPiv.put(rvLeft, dvLeft);
//                }
//                guard = new BiggerGuard(currentParam, rvLeft);
            }

            log.log(Level.FINEST,"Guard: " + guard.toString());

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
                    prefix, suffix, currentValues, piv, constants, currentSuffixValues);

            tempKids.put(guard, oracleSdt);

        }

//        log.log(Level.FINEST,"-------> Level finished!\nTemporary guards = " + tempKids.keySet().toString());
        // merge the guards
        Map<SDTGuard, SDT> merged = mergeGuards(tempKids);

        // only keep registers that are referenced by the merged guards
        piv.putAll(keepMem(merged.keySet()));

        //log.log(Level.FINEST,"temporary guards = " + tempKids.keySet());
        //log.log(Level.FINEST,"temporary pivs = " + tempPiv.keySet());
        log.log(Level.FINEST,"merged guards = " + merged.keySet().toString());
        //log.log(Level.FINEST,"merged pivs = " + addPiv.toString());

        // clear the temporary map of children
        tempKids.clear();

        SDT returnSDT = new SDT(merged);

        return returnSDT;

    }
    
    private SmallerGuard makeSmallerGuard(DataValue<T> smallerDv, List<DataValue> prefixValues, SuffixValue currentParam, WordValuation ifValues, PIV pir) {
        DataType type = currentParam.getType();
        int newDv_i;
        if (prefixValues.contains(smallerDv)) {
            newDv_i = prefixValues.indexOf(smallerDv) + 1;
            SymbolicDataValue.Parameter newDv_p = new SymbolicDataValue.Parameter(type, newDv_i);
            Register newDv_r;
            if (pir.containsKey(newDv_p)) {
                newDv_r = pir.get(newDv_p);
            } else {
                newDv_r = new Register(type, newDv_i);
            }
            return new SmallerGuard(currentParam, newDv_r);

        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
        else {
            int smallest = Collections.min(ifValues.getAllKeys(smallerDv)) + 1;
            return new SmallerGuard(currentParam, new SuffixValue(type, smallest));
        }
    }
        
        private BiggerGuard makeBiggerGuard(DataValue<T> biggerDv, List<DataValue> prefixValues, SuffixValue currentParam, WordValuation ifValues, PIV pir) {
        DataType type = currentParam.getType();
        int newDv_i;
        if (prefixValues.contains(biggerDv)) {
            newDv_i = prefixValues.indexOf(biggerDv) + 1;
            SymbolicDataValue.Parameter newDv_p = new SymbolicDataValue.Parameter(type, newDv_i);
            Register newDv_r;
            if (pir.containsKey(newDv_p)) {
                newDv_r = pir.get(newDv_p);
            } else {
                newDv_r = new Register(type, newDv_i);
            }
            return new BiggerGuard(currentParam, newDv_r);

        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
        else {
            int smallest = Collections.min(ifValues.getAllKeys(biggerDv)) + 1;
            return new BiggerGuard(currentParam, new SuffixValue(type, smallest));
        }
        }

    protected abstract List<DataValue<T>> getPotential(List<DataValue<T>> potList);

}
