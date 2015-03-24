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
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.IntType;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
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
public abstract class InequalityTheoryWithEq<T> implements Theory<T> {

    private static final LearnLogger log = LearnLogger.getLogger(InequalityTheoryWithEq.class);

    IntType intType = new IntType();

    private VarMapping makeVarMapping(Set<SDTGuard> gSet1, Set<SDTGuard> gSet2) {
        VarMapping vars = new VarMapping();
        SDTIfGuard ifg1;
        SymbolicDataValue r1;
        for (SDTGuard g1 : gSet1) {
            if (g1 instanceof SDTIfGuard) {
                ifg1 = (SDTIfGuard) g1;
                r1 = ifg1.getRegister();
                SuffixValue p1 = g1.getParameter();
                for (SDTGuard g2 : gSet2) {
                    if (g2 instanceof SDTIfGuard) {
                        SDTIfGuard ifg2 = (SDTIfGuard) g2;
                        if ((p1.getId() == g2.getParameter().getId()) && (ifg1.getRelation().equals(ifg2.getRelation()))) {
                            vars.put(r1, ifg2.getRegister());
                        }
                    }
                }
            }
        }
        return vars;
    }

    private Set<SDTGuard> removeContradictions(SDTGuard... gs) {
        Set<SDTGuard> guardSet = new HashSet<>();
        for (SDTGuard g : gs) {
            if (g instanceof SDTIfGuard) {
                SDTIfGuard x = (SDTIfGuard) g;
                if (guardSet.contains(x.toDeqGuard())) {
                    guardSet.remove(x.toDeqGuard());
                    if (!(g instanceof EqualityGuard)) {
                        guardSet.add(new DisequalityGuard(x.getParameter(), x.getRegister()));
                    }
                } else {
                    guardSet.add(x);
                }
            } else if (g instanceof SDTAndGuard) {
                SDTAndGuard ag = (SDTAndGuard) g;
                List<SDTIfGuard> ifs = new ArrayList();
                for (SDTIfGuard x : ag.getGuards()) {
                    if (guardSet.contains(x.toDeqGuard())) {
                        guardSet.remove(x.toDeqGuard());
                        if (!(g instanceof EqualityGuard)) {
                            guardSet.add(new DisequalityGuard(x.getParameter(), x.getRegister()));
                        }
                    } else {
                        ifs.add(x);
                    }
                }
                if (ifs.size() == 1) {
                    guardSet.add(ifs.get(0));
                } else if (ifs.size() > 1) {
                    guardSet.add(new SDTAndGuard(g.getParameter(), ifs.toArray(new SDTIfGuard[]{})));
                }

            }
        }
        return guardSet;
    }

 
    private Set<SDTGuard> setify(SDTGuard... gs) {
        Set<SDTGuard> guardSet = new HashSet<>();
        for (SDTGuard g : gs) {
            if (g instanceof SDTOrGuard) {
                SDTOrGuard cg = (SDTOrGuard) g;
                for (SDTIfGuard x : cg.getGuards()) {
                    if (guardSet.contains(x.toDeqGuard())) {
                        guardSet.remove(x.toDeqGuard());
                    } else {
                        guardSet.add(x);
                    }
                }
            } else if (g instanceof SDTIfGuard) {
                SDTIfGuard x = (SDTIfGuard) g;
                if (guardSet.contains(x.toDeqGuard())) {
                    guardSet.remove(x.toDeqGuard());
                    if (!(g instanceof EqualityGuard)) {
                        guardSet.add(new DisequalityGuard(x.getParameter(), x.getRegister()));
                    }
                } else {
                    guardSet.add(x);
                }
            } else if (g instanceof SDTAndGuard) {
                SDTAndGuard ag = (SDTAndGuard) g;
                List<SDTIfGuard> ifs = new ArrayList();
                for (SDTIfGuard x : ag.getGuards()) {
                    if (guardSet.contains(x.toDeqGuard())) {
                        guardSet.remove(x.toDeqGuard());
                        if (!(g instanceof EqualityGuard)) {
                            guardSet.add(new DisequalityGuard(x.getParameter(), x.getRegister()));
                        }
                    } else {
                        ifs.add(x);
                    }
                }
                if (ifs.size() == 1) {
                    guardSet.add(ifs.get(0));
                } else if (ifs.size() > 1) {
                    guardSet.add(new SDTAndGuard(g.getParameter(), ifs.toArray(new SDTIfGuard[]{})));
                }

            }
        }
        return guardSet;
    }

    private SDTGuard mergeGuardLists(SDTGuard g1, SDTGuard g2) {
        Set<SDTGuard> guardSet = (setify(g1, g2));
        return new SDTOrGuard(g1.getParameter(), guardSet.toArray(new SDTIfGuard[]{}));
    }

//    private boolean canMerge(SDTGuard th, SDTGuard other) {
//        Set<SymbolicDataValue> regs = new HashSet();
//        Set<SDTGuard> guardSet = setify(th, other);
//        for (SDTIfGuard g : guardSet) {
//            regs.add(g.getRegister());
//        }
//        return (regs.size() == 1);
//    }
    private Map<SDTGuard, SDT> tryToMerge(SDTGuard guard,
            List<SDTGuard> targetList, Map<SDTGuard, SDT> refSDTMap,
            Map<SDTGuard, SDT> finalMap, Map<SDTGuard, SDT> contMap) {
        log.log(Level.FINEST, "---- Merging ----\nguard: " + guard.toString() + "\ntargetList: " + targetList.toString() + "\nfinalMap " + finalMap.toString() + "\ncontMap " + contMap.toString());
        Map<SDTGuard, SDT> cMap = new LinkedHashMap();
        cMap.putAll(contMap);
        if (targetList.isEmpty()) {
            //finalMap.put(guard, refSDTMap.get(guard));
            finalMap.put(guard, refSDTMap.get(guard));
            finalMap.putAll(cMap);
            log.log(Level.FINEST, " ---> " + finalMap.toString());
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

            VarMapping vars = makeVarMapping(guardSDT.getGuards(), otherSDT.getGuards());
//            if (canMerge(guard, other) && 
            if (guardSDT.isLooselyEquivalent(otherSDT, vars)) {
                // if yes, then merge them
                SDTGuard merged = mergeGuardLists(guard, other);
//                System.out.println(guard.toString() + " and " + other.toString() + " are compatible, become " + merged.toString() + " using SDT " + otherSDT.toString());
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
        if (unmerged.isEmpty()) {
            return unmerged;
        }
        log.log(Level.FINEST, "master merge...");
        Map<SDTGuard, SDT> tempMap = new LinkedHashMap();

        List<SDTGuard> guardList = new ArrayList(unmerged.keySet());
        log.log(Level.FINEST, "unmerged: " + unmerged.toString());
        //int i = 0;
        List<SDTGuard> jGuardList = new ArrayList();
        jGuardList.addAll(guardList);
        Map<SDTGuard, SDT> gMerged = tryToMerge(guardList.get(0), jGuardList.subList(1, guardList.size()), unmerged, new LinkedHashMap(), new LinkedHashMap());
        tempMap.putAll(gMerged);

        log.log(Level.FINEST, tempMap.toString());
        if (tempMap.size() == unmerged.size()) {
            log.log(Level.FINEST, "unchanged");
            return tempMap;
        } else {
            log.log(Level.FINEST, "another round");
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
                ifGuards.add((SDTIfGuard) g);
            } else if (g instanceof SDTOrGuard) {
                ifGuards.addAll(((SDTOrGuard) g).getGuards());
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

//        System.out.println("prefix: " + prefix.toString());
//        System.out.println("sym suffix: " + suffix.toString());
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

        if (potential.isEmpty()) {
            WordValuation elseValues = new WordValuation();
            DataValue<T> fresh = getFreshValue(potential);
            elseValues.putAll(values);
            elseValues.put(pId, fresh);

            // this is the valuation of the suffixvalues in the suffix
            SuffixValuation elseSuffixValues = new SuffixValuation();
            elseSuffixValues.putAll(suffixValues);
            elseSuffixValues.put(sv, fresh);

            SDT elseOracleSdt = oracle.treeQuery(
                    prefix, suffix, elseValues, piv, constants, elseSuffixValues);
            tempKids.put(new SDTTrueGuard(currentParam), elseOracleSdt);
        } // process each '<' case
        else {
            for (int i = 0; i < potSize; i++) {
                //DataValue<T> currentDv = potential.get(i);

                WordValuation currentValues = new WordValuation();
                currentValues.putAll(values);
                Parameter p = new Parameter(currentParam.getType(), currentParam.getId());
                //  log.log(Level.FINEST,"new values " + currentValues.toString());
                SuffixValuation currentSuffixValues = new SuffixValuation();
                currentSuffixValues.putAll(suffixValues);
                SDTGuard guard;
                System.out.println(potential.toString());
                Valuation val = new Valuation();
                // SMALLEST case
                if (i == 0) {
                    // find the data value we're comparing to
                    DataValue<T> dvRight = potential.get(i);
//                    System.out.println("dvRight = " + dvRight.toString());
                    guard = makeSmallerGuard(dvRight, prefixValues, currentParam, currentValues, piv);
                    SymbolicDataValue r = (((SDTIfGuard) guard).getRegister());

                    val.setValue(r.toVariable(), dvRight);
                    //cv = makeSmallerValue(dvRight);
                    //                  System.out.println("smaller, " + dvRight.toString() + "  vs  " + cv.toString() + "  becomes  " + guard.toString());

                } // MIDDLE cases
                else if (i > 0 && i < potSize - 1) {
                    DataValue<T> dvRight = potential.get(i);
                    DataValue<T> dvLeft = potential.get(i - 1);

                    SmallerGuard smallerGuard = makeSmallerGuard(dvRight, prefixValues, currentParam, currentValues, piv);
                    BiggerGuard biggerGuard = makeBiggerGuard(dvLeft, prefixValues, currentParam, currentValues, piv);
                    guard = new SDTAndGuard(currentParam, smallerGuard, biggerGuard);
                    SymbolicDataValue rs = smallerGuard.getRegister();
                    SymbolicDataValue rb = biggerGuard.getRegister();

                    val.setValue(rs.toVariable(), dvRight);
                    val.setValue(rb.toVariable(), dvLeft);

//                    currentValues.put(pId, makeMiddleValue(dvLeft, dvRight));
                } // BIGGEST case
                else {
                    DataValue<T> dvLeft = potential.get(i);
                    guard = makeBiggerGuard(dvLeft, prefixValues, currentParam, currentValues, piv);
                    SymbolicDataValue r = (((SDTIfGuard) guard).getRegister());

                    val.setValue(r.toVariable(), dvLeft);
                }
               System.out.println("IneqValuation: " + val.toString());
                DataValue<T> cv = instantiate(guard, val, p, constants);
                currentValues.put(pId, cv);
                log.log(Level.FINEST, "Guard: " + guard.toString());

                SDT oracleSdt = oracle.treeQuery(
                        prefix, suffix, currentValues, piv, constants, currentSuffixValues);

                tempKids.put(guard, oracleSdt);

            }

            for (DataValue<T> newDv : potential) {
                log.log(Level.FINEST, newDv.toString());

                // this is the valuation of the suffixvalues in the suffix
                SuffixValuation ifSuffixValues = new SuffixValuation();
                ifSuffixValues.putAll(suffixValues);  // copy the suffix valuation
                //ifSuffixValues.put(sv, newDv);

                EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues, currentParam, values, piv, constants);
                log.log(Level.FINEST, "eqGuard is: " + eqGuard.toString());
                WordValuation ifValues = new WordValuation();
                ifValues.putAll(values);
                ifValues.put(pId, newDv);
                SDT eqOracleSdt = oracle.treeQuery(
                        prefix, suffix, ifValues, piv, constants, ifSuffixValues);

                tempKids.put(eqGuard, eqOracleSdt);
            }

        }

//        log.log(Level.FINEST,"-------> Level finished!\nTemporary guards = " + tempKids.keySet().toString());
        // merge the guards
        System.out.println("temporary guards = " + tempKids.keySet());

        Map<SDTGuard, SDT> merged = mergeGuards(tempKids);

        // only keep registers that are referenced by the merged guards
        piv.putAll(keepMem(merged.keySet()));

        //log.log(Level.FINEST,"temporary pivs = " + tempPiv.keySet());
        log.log(Level.FINEST, "merged guards = " + merged.keySet().toString());
        //log.log(Level.FINEST,"merged pivs = " + addPiv.toString());

        // clear the temporary map of children
        tempKids.clear();

        SDT returnSDT = new SDT(merged);

        System.out.println("returnsdt: " + returnSDT.toString());

        return returnSDT;

    }

    //public abstract DataValue<T> makeSmallerValue(DataValue<T> x, DataValue<T> y);
    //public abstract DataValue<T> makeSmallerValue(DataValue<T> x);
    //public abstract DataValue<T> makeMiddleValue(DataValue<T> x, DataValue<T> y);
    //public abstract DataValue<T> makeBiggerValue(DataValue<T> x, DataValue<T> y);
    //public abstract DataValue<T> makeBiggerValue(DataValue<T> x);
    private EqualityGuard pickupDataValue(DataValue<T> newDv, List<DataValue> prefixValues, SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
        DataType type = currentParam.getType();
        int newDv_i;
        for (SymbolicDataValue.Constant c : constants.keySet()) {
            if (constants.get(c).equals(newDv)) {
                return new EqualityGuard(currentParam, c);
            }
        }
        if (prefixValues.contains(newDv)) {
            // first index of the data value in the prefixvalues list
            newDv_i = prefixValues.indexOf(newDv) + 1;
            // create a new parameter
            //Parameter newDv_p = new Parameter(type, newDv_i);
            Register newDv_r;
            // always ensure we pickup the data value directly from the prefix
            //if (pir.containsKey(newDv_p)) {
            //    newDv_r = pir.get(newDv_p);
            //} else {
            newDv_r = new Register(type, newDv_i);
            //}
            log.log(Level.FINEST, "current param = " + currentParam.toString());
            log.log(Level.FINEST, "New register = " + newDv_r.toString());
            return new EqualityGuard(currentParam, newDv_r);

        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
        else {
            //log.log(Level.FINEST, "looking for smallest index of " + newDv.toString() + " in " + ifValues.toString());

            int smallest = Collections.min(ifValues.getAllKeys(newDv));
            //log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
            return new EqualityGuard(currentParam, new SuffixValue(type, smallest));
        }
    }

    private SmallerGuard makeSmallerGuard(DataValue<T> smallerDv, List<DataValue> prefixValues, SuffixValue currentParam, WordValuation ifValues, PIV pir) {
        DataType type = currentParam.getType();
        int newDv_i;
        System.out.println("prefixValues are: " + prefixValues.toString());
        if (prefixValues.contains(smallerDv)) {
            System.out.println("dv in prefixvalues");
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
            System.out.println("current wordvaluation: " + ifValues.toString() + " -- looking for: " + smallerDv.toString());
            int smallest = Collections.min(ifValues.getAllKeys(smallerDv));
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
            int smallest = Collections.min(ifValues.getAllKeys(biggerDv));
            return new BiggerGuard(currentParam, new SuffixValue(type, smallest));
        }
    }

    public abstract List<DataValue<T>> getPotential(List<DataValue<T>> vals);// {
//        return vals;
//    }

    private DataValue getRegisterValue(SymbolicDataValue r, PIV piv, List<DataValue> prefixValues, Constants constants, ParValuation pval) {
        if (r.isRegister()) {
            //log.log(Level.FINEST, "piv: " + piv.toString() + " " + ereg.toString() + " " + param.toString());
            Parameter p = piv.getOneKey((Register) r);
            //log.log(Level.FINEST, "p: " + p.toString());
            int idx = p.getId();
            //return piv.get(param);
            // trying to not pickup values from prefix
            return prefixValues.get(idx - 1);
        } else if (r.isSuffixValue()) {
            Parameter p = new Parameter(r.getType(), r.getId());
            return pval.get(p);
        } else if (r.isConstant()) {
            return constants.get((SymbolicDataValue.Constant) r);
        } else {
            throw new IllegalStateException("this can't possibly happen");
        }
    }

    public abstract DataValue instantiate(SDTGuard guard, Valuation val, Parameter param, Constants constants);

    @Override
    public DataValue instantiate(
            Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv,
            ParValuation pval,
            Constants constants,
            SDTGuard guard,
            Parameter param) {

        DataType type = param.getType();

        //Variable px = param.toVariable();
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        log.log(Level.FINEST, "prefix values : " + prefixValues.toString());

        if (guard instanceof EqualityGuard) {
            EqualityGuard eqGuard = (EqualityGuard) guard;
            SymbolicDataValue ereg = eqGuard.getRegister();
            return getRegisterValue(ereg, piv, prefixValues, constants, pval);
        } else if (guard instanceof SDTTrueGuard) {

            Collection potSet = DataWords.<T>joinValsToSet(
                    constants.<T>values(type),
                    DataWords.<T>valSet(prefix, type),
                    pval.<T>values(type));

            return this.getFreshValue(new ArrayList<DataValue<T>>(potSet));
        } else {
            Valuation val = new Valuation();
            if (guard instanceof SDTIfGuard) {
                SymbolicDataValue r = (((SDTIfGuard) guard).getRegister());
                DataValue regVal = getRegisterValue(r, piv, prefixValues, constants, pval);

                val.setValue(r.toVariable(), regVal);
                //instantiate(guard, val, param, constants);
            } else if (guard instanceof SDTMultiGuard) {
                for (SDTIfGuard ifGuard : ((SDTMultiGuard) guard).getGuards()) {
                    SymbolicDataValue r = ifGuard.getRegister();
                    DataValue regVal = getRegisterValue(r, piv, prefixValues, constants, pval);
                    val.setValue(r.toVariable(), regVal);
                }
            }
            instantiate(guard, val, param, constants);
            //solver.solve(guard.toExpr(constants), val);
            return new DataValue(type, val.getValue(param.toVariable()));
        }

    }
//
//        if (guard instanceof SDTAndGuard) {
//            log.log(Level.FINEST, "compound guard " + guard.toString());
//
//            SDTAndGuard aGuard = (SDTAndGuard) guard;
//            List<SDTIfGuard> andGuards = aGuard.getGuards();
//            assert andGuards.size() == 2;
//            DataValue ag1 = getConcrete(andGuards.get(0).getRegister(), piv, prefixValues, param, pval, constants);
//            DataValue ag2 = getConcrete(andGuards.get(1).getRegister(), piv, prefixValues, param, pval, constants);
//            return makeMiddleValue(ag1, ag2);
//
//        } else if (guard instanceof SDTOrGuard) {
//            return 
//        } else if (guard instanceof SmallerGuard) {
//            log.log(Level.FINEST, "smaller guard " + guard.toString());
//            DataValue s = getConcrete(((SmallerGuard) guard).getRegister(), piv, prefixValues, param, pval, constants);
//            return makeSmallerValue(s);
//        } else if (guard instanceof BiggerGuard) {
//            log.log(Level.FINEST, "smaller guard " + guard.toString());
//            DataValue s = getConcrete(((BiggerGuard) guard).getRegister(), piv, prefixValues, param, pval, constants);
//            return makeBiggerValue(s);
//
//            // ugly hack!!
//         else {
//            System.out.println("guard is " + guard.toString());
//            throw new IllegalStateException("not supposed to happen");
//        }
//    }
//
//    private DataValue getConcrete(SymbolicDataValue x, PIV piv, List<DataValue> prefixValues, Parameter param, ParValuation pval, Constants constants) {
//
//        DataType type = param.getType();
//        if (x.isRegister()) {
////            log.log(Level.FINEST, "piv: " + piv.toString() + " " + x.toString() + " " + param.toString());
//            Parameter p = piv.getOneKey((Register) x);
//            log.log(Level.FINEST, "p: " + p.toString());
//            int idx = p.getId();
//            //return piv.get(param);
//            // trying to not pickup values from prefix
//            return prefixValues.get(idx - 1);
//        } else if (x.isSuffixValue()) {
//            Parameter p = new Parameter(type, x.getId());
//            return pval.get(p);
//        } else if (x.isConstant()) {
//            return constants.get((SymbolicDataValue.Constant) x);
//        }
//        return null;
//    }

//    public abstract getMiddle(DataValue dv1, DataValue dv2);
//    public abstract getSmallerThan(DataValue dv);
//    public abstract getBiggerThan(DataValue dv);
//    
//
//    private DataValue getMiddle(DataValue dv1, DataValue dv2) {
//        Double v1 = (Double) dv1.getId();
//        Double v2 = (Double) dv2.getId();
//        if (v1 < v2) {
//            return new DataValue(dv1.getType(), ((v2 - v1) / 2));
//        } else {
//            return new DataValue(dv1.getType(), ((v1 - v2) / 2));
//        }
//
//    }
//
//    private DataValue getLeft(DataValue dv) {
//        Double v = 0.0;
//        if (dv.getId() instanceof Double) {
//            v = (Double) dv.getId();
//        } else if (dv.getId() instanceof Integer) {
//            v = ((Integer) dv.getId()).doubleValue();
//
//        } else {
//            throw new IllegalStateException("not supposed to happen");
//        }
//        return new DataValue(dv.getType(), (v - .1));
//
//    }
//
//    private DataValue getRight(DataValue dv) {
//        Double v = 0.0;
//        if (dv.getId() instanceof Double) {
//            v = (Double) dv.getId();
//        } else if (dv.getId() instanceof Integer) {
//            v = ((Integer) dv.getId()).doubleValue();
//
//        } else {
//            throw new IllegalStateException("not supposed to happen");
//        }
//        return new DataValue(dv.getType(), (v + .1));
//    }
}
