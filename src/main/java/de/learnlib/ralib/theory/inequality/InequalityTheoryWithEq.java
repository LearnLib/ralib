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
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;
import de.learnlib.ralib.theory.DataRelation;
import static de.learnlib.ralib.theory.DataRelation.DEFAULT;
import static de.learnlib.ralib.theory.DataRelation.EQ;
import static de.learnlib.ralib.theory.DataRelation.GT;
import static de.learnlib.ralib.theory.DataRelation.LT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    protected boolean useNonFreeOptimization;
    
    private static final LearnLogger log
            = LearnLogger.getLogger(InequalityTheoryWithEq.class);

    @Override
    public EnumSet<DataRelation> recognizedRelations() {
        return EnumSet.of(DEFAULT, EQ, LT, GT);
    }  
    

    private void addAllSafely(Collection<SDTGuard> guards, Collection<SDTGuard> toAdd, List<SymbolicDataValue> regPotential) {
//        System.out.println("adding " + toAdd + " to " + guards);
        for (SDTGuard t : toAdd) {
            if (t instanceof SDTOrGuard) {
//                System.out.println("or guard");
                addAllSafely(guards, ((SDTOrGuard) t).getGuards(), regPotential);
//                System.out.println(guards);
            } else {
                addSafely(guards, t, regPotential);
            }
        }
        return;
    }

    private void addSafely(Collection<SDTGuard> guards, SDTGuard guard, List<SymbolicDataValue> regPotential) {
        boolean processed = false;
        if (guard instanceof SDTIfGuard) {
            SDTIfGuard oGuard = ((SDTIfGuard) guard).toDeqGuard();
//            System.out.println(guard + ": checking if guards contains " + oGuard);
            if (guards.contains(oGuard)) {
//                System.out.println("yes, removing " + oGuard);
                guards.remove(oGuard);
                processed = true;
            }
        } else if (guard instanceof IntervalGuard) {
//            System.out.println("guard is intervalguard");
            IntervalGuard iGuard = (IntervalGuard) guard;
            if (!iGuard.isIntervalGuard()) {
                IntervalGuard flipped = iGuard.flip();
//                System.out.println("flipped: " + flipped);
                if (guards.contains(flipped)) {
                    guards.remove(flipped);
                    addAllSafely(guards, iGuard.mergeWith(flipped, regPotential), regPotential);
                    processed = true;
                }
            }
        } else if (guard instanceof SDTOrGuard) {
//            System.out.println("found or guard");
            SDTOrGuard oGuard = (SDTOrGuard) guard;
            addAllSafely(guards, oGuard.getGuards(), regPotential);
        }

        if (processed == false) {
            guards.add(guard);
        }
//        System.out.println("added safely: " + guard + " to " + guards);
        return;
    }

    private void removeProhibited(Collection<SDTGuard> guards, Collection<SDTGuard> prohibited) {
        for (SDTGuard p : prohibited) {
            if (guards.contains(p)) {
                guards.remove(p);
            }
        }
    }
    
    private Set<SDTGuard> mergeOneWithSet(Set<SDTGuard> guardSet, SDTGuard target, List<SymbolicDataValue> regPotential) {
        List<Set<SDTGuard>> inAndOut = new ArrayList<Set<SDTGuard>>();
        inAndOut.add(guardSet);
        inAndOut.add(new LinkedHashSet<SDTGuard>());
        return mergeOneWithSet(inAndOut, target, regPotential).get(0);
    }

    private List<Set<SDTGuard>> mergeOneWithSet(List<Set<SDTGuard>> inAndOut, SDTGuard target, List<SymbolicDataValue> regPotential) {
        // merge target with the set guardSet
        Set<SDTGuard> guardSet = inAndOut.get(0);
        Set<SDTGuard> prohibited = inAndOut.get(1);

        Set<SDTGuard> newGuardSet = new LinkedHashSet<SDTGuard>();

        List<Set<SDTGuard>> newInAndOut = new ArrayList<Set<SDTGuard>>();
        newInAndOut.addAll(inAndOut);
        Boolean[] addedSomething = new Boolean[guardSet.size()];
        boolean processed = false;
        newGuardSet.addAll(guardSet);
//        System.out.println("target: " + target);
        int i = -1;
        if (!guardSet.isEmpty()) {
            for (SDTGuard m : guardSet) {
//                System.out.println("guards: " + newGuardSet + " target " + target);
                i++;
                addedSomething[i] = false;
                // if target isn't in guardSet and isn't prohibited
                if (!(guardSet.contains(target)) && !(prohibited.contains(target))) {

                    assert !(target instanceof SDTOrGuard && m instanceof SDTOrGuard);

                    if (target instanceof SDTIfGuard) {
                        // if it is an equality or disequality, remove the opposite by adding both to prohibited
                        SDTIfGuard oGuard = ((SDTIfGuard) target).toDeqGuard();
//                        System.out.println(target + ": checking if guards contains " + oGuard);
                        if (guardSet.contains(oGuard)) {
//                            System.out.println("yes, removing " + oGuard);
                            prohibited.add(oGuard);
                            prohibited.add(target);
                            processed = true;
                        }
                    } else if (target instanceof IntervalGuard) {
//                        System.out.println(target + " is iGuard");
                        // if it is an interval guard, check if the set contains flipped
                        IntervalGuard iGuard = (IntervalGuard) target;
                        if (!iGuard.isIntervalGuard()) {
                            IntervalGuard flipped = iGuard.flip();
//                            System.out.println("flipped: " + flipped);
//                            System.out.println("guardSet " + guardSet + " " + guardSet.contains(flipped));
                            if (guardSet.contains(flipped)) {
                                prohibited.add(flipped);
                                prohibited.add(target);
                                newInAndOut.add(0, newGuardSet);
                                newInAndOut.add(1, prohibited);
                                List<Set<SDTGuard>> nextInAndOut = mergeOneWithSet(newInAndOut, iGuard.toDeqGuard(), regPotential);
                                newGuardSet = nextInAndOut.get(0);
                                prohibited = nextInAndOut.get(1);//  
                                processed = true;
                            }
                        }
                    }
                    if (!processed) {
                        Set<SDTGuard> refSet = new LinkedHashSet<>();
                        refSet.add(target);
                        refSet.add(m);
                        Set<SDTGuard> mWithTarget = target.mergeWith(m, regPotential);
//                        System.out.println("merging: " + target + " + " + m + " --> " + mWithTarget + ", " + refSet.containsAll(mWithTarget));
                        if (!mWithTarget.contains(target)) {
                            prohibited.add(target);
                        }
                        if (!mWithTarget.contains(m)) {
                            prohibited.add(m);
                        }
                        addedSomething[i] = !(refSet.containsAll(mWithTarget));
//                    System.out.println("g: " + guardSet);
//                    System.out.println("n: " + newGuardSet);

                        if (addedSomething[i]) { // && !(newGuardSet.containsAll(mWithTarget))) {
                            for (SDTGuard x : mWithTarget) {
//                            List<Set<SDTGuard>> newInAndOut = new ArrayList<Set<SDTGuard>>();
//                                System.out.println("XXX: for " + x + ": adding in: " + newGuardSet + " and out: " + prohibited);
                                newInAndOut.add(0, newGuardSet);
                                newInAndOut.add(1, prohibited);
                                List<Set<SDTGuard>> nextInAndOut = mergeOneWithSet(newInAndOut, x, regPotential);
                                newGuardSet = nextInAndOut.get(0);
                                prohibited = nextInAndOut.get(1);
//                                System.out.println("XXX: after " + x + ": in: " + newGuardSet + " and out: " + prohibited);
                            }
                        } else {
                            newGuardSet.addAll(mWithTarget);
                        }

//                    newGuardSet.addAll(mWithTarget);
//                        System.out.println("into: " + newGuardSet);
                    }
                }

            }
            
        }
        else {
            newGuardSet.add(target);
            
        }
        newInAndOut.add(0, newGuardSet);
            newInAndOut.add(1, prohibited);

            
        removeProhibited(newGuardSet, prohibited);
//        System.out.println("... and after removing :: " + newGuardSet);
//        System.out.println("modified is " + Arrays.toString(addedSomething));
        if (hasTrue(addedSomething) && !(guardSet.equals(newGuardSet))) {
//            System.out.println("try again");
            return mergeOneWithSet(newInAndOut, target, regPotential);
        } else {
            return newInAndOut;
        }
    }


    // Returns true if all elements of a boolean array are true.
    private boolean hasTrue(Boolean[] maybeArr) {
        boolean maybe = false;
        for (int c = 0; c < (maybeArr.length); c++) {
            //log.log(Level.FINEST,maybeArr[c]);
            if (maybeArr[c]) {
                maybe = true;
                break;
            }
        }
        return maybe;
    }

    private SDTGuard GuardSetFromList(Set<SDTGuard> merged, List<SDTGuard> remaining, 
            SuffixValue currentParam, List<SymbolicDataValue> regPotential, boolean ineqs, boolean eqs) {
//        merge list of guards with a set of guards.  Initially, the set is empty and the list is full
//        System.out.println("rem = " + remaining + ", merged = " + merged);

        //if the set is empty
        // and the set is also empty: return true guard
        // and the set is NOT empty: return either 
        //if the set is empty
        if (remaining.isEmpty()) {
//            if (merged.isEmpty()) {
//                return new SDTTrueGuard(currentParam);
//            } else {
            SDTGuard[] guardArray = merged.toArray(new SDTGuard[]{});
            if ((merged.size() == 1)) { // || hasOnlyEqs(merged)) {
                return guardArray[0];
            } else {
                return new SDTOrGuard(currentParam, guardArray);
            }

//            }
        }
        if (merged.isEmpty()) {
            merged.add(remaining.remove(0));
            return GuardSetFromList(merged, remaining, currentParam, regPotential, ineqs, eqs);
        }
        SDTGuard target = remaining.remove(0);
        Set<SDTGuard> temp = mergeOneWithSet(merged, target, regPotential);

        return GuardSetFromList(temp, remaining, currentParam, regPotential, ineqs, eqs);
//         ensure that a new OrGuard is returned if there is nothing remaining
    }

//    private Set<SDTGuard> getHeads(Set<List<SDTGuard>> listSets) {
//        Set<SDTGuard> retSet = new LinkedHashSet<>();
//        for (List<SDTGuard> l : listSets) {
//            retSet.add(l.get(0));
//        }
//        return retSet;
//    }
//    private List<SDTGuard> getListFromHead(SDTGuard guard, Set<List<SDTGuard>> listSets) {
//        Set<SDTGuard> heads = getHeads(listSets);
//        if (heads.contains(guard)) {
//            for (List<SDTGuard> l : listSets) {
//                if (l.get(0).equals(guard)) {
//                    return l;
//                }
//            }
//        }
//        throw new IllegalStateException("not found");
//    }
    private Map<List<SDTGuard>, SDT> modGuardLists(SDTGuard refGuard, SDT refSDT, Map<List<SDTGuard>, SDT> partitionedMap) {
        boolean merged = false;
        Map<List<SDTGuard>, SDT> newParMap = new LinkedHashMap<>();
//        System.out.println("modGuardLists for refGuard " + refGuard + ", refSDT " + refSDT + ", map: " + partitionedMap);
        for (Map.Entry<List<SDTGuard>, SDT> par : partitionedMap.entrySet()) {
//            merged = false;
//            System.out.println("par: " + par);
            List<SDTGuard> headList = par.getKey();
            List<SDTGuard> newHeadList = new ArrayList<>();
            newHeadList.addAll(headList);
            //addAllSafely(newHeadList, headList, new LinkedHashSet<SDTGuard>());
            SDT headSDT = par.getValue();
            assert headSDT.getClass().equals(refSDT.getClass());
            //assert !(headSDT.isEmpty());
            //assert !(refSDT.isEmpty());
            SDTGuard headGuard = headList.get(0);
            SDT newSDT = headSDT;
//            System.out.println("head: " + newHeadList + " against " + refGuard);
            if (headSDT instanceof SDTLeaf) {
//                System.out.println("sdt leaves");
                assert refSDT instanceof SDTLeaf;
                if (headSDT.isAccepting() == refSDT.isAccepting() && !merged) {
                    merged = true;
                    newHeadList.add(refGuard);
//                    System.out.println("added ref " + refSDT + " eq to " + headSDT);
                }
                //newParMap.put(newHeadList, newSDT);

            }
            if (refGuard instanceof IntervalGuard && !merged) {
                IntervalGuard iRefGuard = (IntervalGuard) refGuard;
                if (!iRefGuard.isIntervalGuard()) {
                    EqualityGuard eqGuard = iRefGuard.toEqGuard();
                    if (newHeadList.contains(eqGuard)) {
//                        System.out.println("trying Deq " + refGuard + " against " + newHeadList + " with " + eqGuard);
                        SDT joinedSDT = getJoinedSDT(((IntervalGuard) refGuard).toEqGuard(), refSDT, headSDT);
                        if (joinedSDT != null) {
//                            System.out.println("can merge: " + refGuard + " with EQ" + headList);
                            newHeadList.add(refGuard);
                            newSDT = joinedSDT;
                            //newSDT = refSDT.relabelUnderEq((EqualityGuard) headGuard);
                            merged = true;
                            //newParMap.put(newHeadList, newSDT);
                        }
                    }
                } else {
                    assert iRefGuard.isIntervalGuard();
                    EqualityGuard eqG = null;
                    if (newHeadList.contains(new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getLeftReg()))) {
                        eqG = new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getLeftReg());
                    } else if (newHeadList.contains(new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getRightReg()))) {
                        eqG = new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getLeftReg());
                    }
                    if (eqG != null) {
//                        System.out.println("trying Eq " + refGuard + " against " + newHeadList + " with " + eqG);
                        SDT joinedSDT = getJoinedSDT(eqG, refSDT, headSDT);
                        if (joinedSDT != null) {
//                            System.out.println("can merge: EQ" + headList + " with " + refGuard);
                            newHeadList.add(refGuard);
                            newSDT = joinedSDT;
                            merged = true;
                            //newParMap.put(newHeadList, newSDT);
                        }
                    }
                }

            }
            if (refGuard instanceof EqualityGuard && !merged) {
//                System.out.println("trying Eq " + refGuard + " against " + newHeadList);
                SDT joinedSDT = getJoinedSDT((EqualityGuard) refGuard, headSDT, refSDT);
                if (joinedSDT != null) {
//                    System.out.println("can merge: EQ" + refGuard + " with " + headList);
                    newHeadList.add(0, refGuard);
                    newSDT = joinedSDT;
                    merged = true;
                    //newParMap.put(newHeadList, newSDT);
                }
            }
            if (refSDT.canUse(headSDT) && headSDT.canUse(refSDT) && !merged) {
//                System.out.println("LAST RESORT: can use");
                newHeadList.add(refGuard);
                merged = true;
                //newParMap.put(newHeadList, newSDT);
            }

            newParMap.put(newHeadList, newSDT);

        }

        if (!merged) {
//            System.out.println(refGuard + " cannot merge with anything in " + partitionedMap);
            List<SDTGuard> newUnmergedList = new ArrayList<>();
            newUnmergedList.add(refGuard);
//            newParMap.putAll(partitionedMap);
//            System.out.println("adding " + newUnmergedList + " ---> " + refSDT);

            newParMap.put(newUnmergedList, refSDT);
//            System.out.println("newParMap is now " + newParMap);
        }

        //newParMap.putAll(partitionedMap);
        assert !newParMap.isEmpty();

//        System.out.println("RETURNED: newParMap " + newParMap);
        return newParMap;
    }

    private Map<List<SDTGuard>, SDT> partitionGuards(Map<SDTGuard, SDT> refMap) {

//        System.out.println("partitioning " + refMap);
        // start with an empty map
        Map<List<SDTGuard>, SDT> parMap = new LinkedHashMap<>();
        for (Map.Entry<SDTGuard, SDT> ref : refMap.entrySet()) {
            SDTGuard refGuard = ref.getKey();
            SDT refSDT = ref.getValue();

            parMap = modGuardLists(refGuard, refSDT, parMap);
        }
        return parMap;
    }


    private SDT getJoinedSDT(EqualityGuard guard, SDT deqSDT, SDT eqSDT) {
        //boolean canJoin = false;

        EqualityGuard eqGuard = (EqualityGuard) guard;
        List<SDTIfGuard> ds = new ArrayList();
        ds.add(eqGuard);
//        System.out.println("checking if T" + deqSDT + " is eq to O" + eqSDT + " under " + eqGuard);

        SDT newTargetSDT = deqSDT.relabelUnderEq(ds);
        assert newTargetSDT != null;

//        System.out.println("checking if T" + deqSDT + " is eq to O" + eqSDT + " under " + eqGuard);
        if (eqSDT.isEquivalentUnder(deqSDT, ds)) {
//            System.out.println("yes");
//            System.out.println("return target: " + deqSDT + " eq under " + eqGuard);
            return deqSDT;
        }

        return null;
    }

    // given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT.  
    private Map<SDTGuard, SDT>
            mgGuards(Map<SDTGuard, SDT> unmerged, SuffixValue currentParam, List<SymbolicDataValue> regPotential,
                    boolean ineqs, boolean eqs) {
        assert !unmerged.isEmpty();

        Map<SDTGuard, SDT> retMap = new LinkedHashMap<>();
//        System.out.println("unmerged: " + unmerged);
        Map<List<SDTGuard>, SDT> partitionedMap = partitionGuards(unmerged);
//        System.out.println("partitionedMap: " + partitionedMap);
        for (Map.Entry<List<SDTGuard>, SDT> par : partitionedMap.entrySet()) {
            System.out.println("--> Guards: " + Arrays.toString(par.getKey().toArray()));
            SDTGuard newSDTGuard = GuardSetFromList(new LinkedHashSet<SDTGuard>(), par.getKey(), 
                    currentParam, regPotential, ineqs, eqs);
            System.out.println("--> Guard: " + newSDTGuard);
            if (newSDTGuard instanceof SDTOrGuard) {
                List<SDTGuard> subguards = ((SDTOrGuard) newSDTGuard).getGuards();
                if (subguards.isEmpty()) {
                    retMap.put(new SDTTrueGuard(currentParam), par.getValue());
                } else {
                    for (SDTGuard subguard : subguards) {
                        retMap.put(subguard, par.getValue());
                    }
                }
            } else {
                retMap.put(newSDTGuard, par.getValue());
            }
        }
//        System.out.println("retMap: " + retMap);
        assert !retMap.isEmpty();
        //System.out.println("-----------------------------------\n" + partitionedMap + "\n-----------------------PARTITIONING-------------------\n" + retMap + "\n---------------------------------");
        return retMap;
    }

    private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
        PIV ret = new PIV();
        for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
            SDTGuard mg = e.getKey();
            if (mg instanceof SDTIfGuard) {
                log.log(Level.FINEST, mg.toString());
                SymbolicDataValue r = ((SDTIfGuard) mg).getRegister();
                Parameter p = new Parameter(r.getType(), r.getId());
                if (r instanceof Register) {
                    ret.put(p, (Register) r);
                }
            } else if (mg instanceof IntervalGuard) {
                IntervalGuard iGuard = (IntervalGuard) mg;
                if (!iGuard.isBiggerGuard()) {
                    SymbolicDataValue r = iGuard.getRightReg();
                    Parameter p = new Parameter(r.getType(), r.getId());
                    if (r instanceof Register) {
                        ret.put(p, (Register) r);
                    }

                }
                if (!iGuard.isSmallerGuard()) {
                    SymbolicDataValue r = iGuard.getLeftReg();
                    Parameter p = new Parameter(r.getType(), r.getId());
                    if (r instanceof Register) {
                        ret.put(p, (Register) r);
                    }
                }
            } else if (mg instanceof SDTOrGuard) {
                Set<SymbolicDataValue> rSet = ((SDTOrGuard) mg).getAllRegs();
                for (SymbolicDataValue r : rSet) {
                    Parameter p = new Parameter(r.getType(), r.getId());
                    if (r instanceof Register) {
                        ret.put(p, (Register) r);
                    }
                }
            } else if (!(mg instanceof SDTTrueGuard)) {
                throw new IllegalStateException("wrong kind of guard");
            }
        }
        return ret;
    }

    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix,
            GeneralizedSymbolicSuffix suffix,
            WordValuation values,
            PIV piv,
            Constants constants,
            SuffixValuation suffixValues,
            SDTConstructor oracle) {

        int pId = values.size() + 1;
        List<SymbolicDataValue> regPotential = new ArrayList<>();
        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        DataValue<T>[] typedPrefixValues = DataWords.valsOf(prefix, type);
        WordValuation typedPrefixValuation = new WordValuation();
        for (int i = 0; i < typedPrefixValues.length; i++) {
            typedPrefixValuation.put(i + 1, typedPrefixValues[i]);
        }

        SuffixValue currentParam = new SuffixValue(type, pId);

        Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                constants.<T>values(type),
                DataWords.<T>valSet(prefix, type),
                suffixValues.<T>values(type));

        List<DataValue<T>> potList = new ArrayList<>(potSet);
        List<DataValue<T>> potential = getPotential(potList);
        // WE ASSUME THE POTENTIAL IS SORTED

        int potSize = potential.size();

       //if (potential.isEmpty()) { 
        if (potential.isEmpty() || 
                (suffix.getPrefixRelations(pId).size() < 2 && 
                !hasSuffixRelations(suffix, pId) && 
                useNonFreeOptimization)) {
            
            WordValuation elseValues = new WordValuation();
            DataValue<T> fresh = getFreshValue(potential);
            elseValues.putAll(values);
            elseValues.put(pId, fresh);

            // this is the valuation of the suffixvalues in the suffix
            SuffixValuation elseSuffixValues = new SuffixValuation();
            elseSuffixValues.putAll(suffixValues);
            elseSuffixValues.put(sv, fresh);

            SDT elseOracleSdt = oracle.treeQuery(
                    prefix, suffix, elseValues, piv,
                    constants, elseSuffixValues);
            tempKids.put(new SDTTrueGuard(currentParam), elseOracleSdt);
        } // process each '<' case
        else {
            //Parameter p = new Parameter(
            //      currentParam.getType(), currentParam.getId());

            if ((suffix.getPrefixRelations(pId).contains(DataRelation.GT) || 
                 suffix.getPrefixRelations(pId).contains(DataRelation.LT) ||
//                 suffix.getPrefixRelations(pId).contains(DataRelation.EQ) ||   
                hasSuffixRelations(suffix, pId)) ||                
                !useNonFreeOptimization|| true) {
                
                // smallest case
                WordValuation smValues = new WordValuation();
                smValues.putAll(values);
                SuffixValuation smSuffixValues = new SuffixValuation();
                smSuffixValues.putAll(suffixValues);

                Valuation smVal = new Valuation();
                DataValue<T> dvRight = potential.get(0);
                IntervalGuard sguard = makeSmallerGuard(
                        dvRight, prefixValues, currentParam, smValues, piv);
                SymbolicDataValue rsm = sguard.getRightReg();
    //            System.out.println("setting valuation, symDV: " + rsm.toVariable() + " dvright: " + dvRight);
                smVal.setValue(toVariable(rsm), dvRight.getId());
                DataValue<T> smcv = instantiate(
                        sguard, smVal, constants, potential);
                smValues.put(pId, smcv);
                smSuffixValues.put(sv, smcv);

                SDT smoracleSdt = oracle.treeQuery(
                        prefix, suffix, smValues, piv, constants, smSuffixValues);

                tempKids.put(sguard, smoracleSdt);
            }
            
            // biggest case
            WordValuation bgValues = new WordValuation();
            bgValues.putAll(values);
            SuffixValuation bgSuffixValues = new SuffixValuation();
            bgSuffixValues.putAll(suffixValues);

            Valuation bgVal = new Valuation();

            DataValue<T> dvLeft = potential.get(potSize - 1);
            IntervalGuard bguard = makeBiggerGuard(
                    dvLeft, prefixValues, currentParam, bgValues, piv);
            SymbolicDataValue rbg = bguard.getLeftReg();

            bgVal.setValue(toVariable(rbg), dvLeft.getId());
            DataValue<T> bgcv = instantiate(
                    bguard, bgVal, constants, potential);
            bgValues.put(pId, bgcv);
            bgSuffixValues.put(sv, bgcv);

            SDT bgoracleSdt = oracle.treeQuery(
                    prefix, suffix, bgValues, piv, constants, bgSuffixValues);

            tempKids.put(bguard, bgoracleSdt);

            if (potSize > 1 && (true || !useNonFreeOptimization ||
                suffix.getPrefixRelations(pId).contains(DataRelation.GT) || 
                suffix.getPrefixRelations(pId).contains(DataRelation.LT) ||  
//                suffix.getPrefixRelations(pId).contains(DataRelation.EQ) ||    
                hasSuffixRelations(suffix, pId))) {
                
                //middle cases
                for (int i = 1; i < potSize; i++) {

                    WordValuation currentValues = new WordValuation();
                    currentValues.putAll(values);
                    SuffixValuation currentSuffixValues = new SuffixValuation();
                    currentSuffixValues.putAll(suffixValues);
                    //SDTGuard guard;
                    Valuation val = new Valuation();
                    DataValue<T> dvMRight = potential.get(i);
                    DataValue<T> dvMLeft = potential.get(i - 1);

//                    IntervalGuard smallerGuard = makeSmallerGuard(
//                            dvMRight, prefixValues,
//                            currentParam, currentValues, piv);
//                    IntervalGuard biggerGuard = makeBiggerGuard(
//                            dvMLeft, prefixValues, currentParam,
//                            currentValues, piv);
                    IntervalGuard intervalGuard = makeIntervalGuard(
                            dvMLeft, dvMRight, prefixValues, currentParam, currentValues, piv);

//                    IntervalGuard guard = new IntervalGuard(
//                            currentParam, biggerGuard.getLeftReg(), smallerGuard.getRightReg());
                    SymbolicDataValue rs = intervalGuard.getRightReg();
                    SymbolicDataValue rb = intervalGuard.getLeftReg();

                    val.setValue(toVariable(rs), dvMRight.getId());
                    val.setValue(toVariable(rb), dvMLeft.getId());

                    DataValue<T> cv = instantiate(
                            intervalGuard, val, constants, potential);
                    currentValues.put(pId, cv);
                    currentSuffixValues.put(sv, cv);

                    SDT oracleSdt = oracle.treeQuery(
                            prefix, suffix, currentValues, piv,
                            constants, currentSuffixValues);

                    tempKids.put(intervalGuard, oracleSdt);
                    regPotential.add(i - 1, rb);
                    regPotential.add(i, rs);
                }
            }
            
            if (true || !useNonFreeOptimization || 
                suffix.getPrefixRelations(pId).contains(DataRelation.EQ) ||
                suffix.getPrefixRelations(pId).contains(DataRelation.GT) ||
                suffix.getPrefixRelations(pId).contains(DataRelation.LT) ||    
                hasSuffixRelations(suffix, pId)) {
                
//            System.out.println("eq potential is: " + potential);
                for (DataValue<T> newDv : potential) {
    //                log.log(Level.FINEST, newDv.toString());

                    // this is the valuation of the suffixvalues in the suffix
                    SuffixValuation ifSuffixValues = new SuffixValuation();
                    ifSuffixValues.putAll(suffixValues);  // copy the suffix valuation

                    EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues,
                            currentParam, values, constants);
    //                log.log(Level.FINEST, "eqGuard is: " + eqGuard.toString());
                    //construct the equality guard
                    // find the data value in the prefix
                    // this is the valuation of the positions in the suffix
                    WordValuation ifValues = new WordValuation();
                    ifValues.putAll(values);
                    ifValues.put(pId, newDv);
                    SDT eqOracleSdt = oracle.treeQuery(
                            prefix, suffix, ifValues, piv, constants, ifSuffixValues);

                    tempKids.put(eqGuard, eqOracleSdt);
                }
            }
        }

//        System.out.println("TEMPKIDS for " + prefix + " + " + suffix + " = " + tempKids);
        Map<SDTGuard, SDT> merged = mgGuards(tempKids, currentParam, regPotential,
                (suffix.getPrefixRelations(pId).contains(DataRelation.GT) ||
                 suffix.getPrefixRelations(pId).contains(DataRelation.LT)),
                suffix.getPrefixRelations(pId).contains(DataRelation.EQ));
        // only keep registers that are referenced by the merged guards
//        System.out.println("MERGED = " + merged);
        assert !merged.keySet().isEmpty();

//        System.out.println("MERGED = " + merged);
        piv.putAll(keepMem(merged));

        log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
        log.log(Level.FINEST, "merged guards = " + merged.keySet());
        log.log(Level.FINEST, "merged pivs = " + piv.toString());

        // clear the temporary map of children
        tempKids = new LinkedHashMap<SDTGuard, SDT>();

        for (SDTGuard g : merged.keySet()) {
            assert !(g == null);
            System.out.println("merged: " + g);
            if (g instanceof SDTTrueGuard) {
                if (merged.keySet().size() != 1) {
                    throw new IllegalStateException("only one true guard allowed: \n" + prefix + " + " + suffix);
                }
                //assert merged.keySet().size() == 1;
            }
        }
//        System.out.println("MERGED = " + merged);
        SDT returnSDT = new SDT(merged);
        return returnSDT;

    }

    private EqualityGuard pickupDataValue(DataValue<T> newDv,
            List<DataValue> prefixValues, SuffixValue currentParam,
            WordValuation ifValues, Constants constants) {
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
            Register newDv_r = new Register(type, newDv_i);
            return new EqualityGuard(currentParam, newDv_r);

        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
        else {
            int smallest = Collections.min(ifValues.getAllKeys(newDv));
            return new EqualityGuard(currentParam, new SuffixValue(type, smallest));
        }
    }

    private IntervalGuard makeSmallerGuard(DataValue<T> smallerDv,
            List<DataValue> prefixValues, SuffixValue currentParam,
            WordValuation ifValues, PIV pir) {
        DataType type = currentParam.getType();
        int newDv_i;
        if (prefixValues.contains(smallerDv)) {
            newDv_i = prefixValues.indexOf(smallerDv) + 1;
            SymbolicDataValue.Parameter newDv_p
                    = new SymbolicDataValue.Parameter(type, newDv_i);
            Register newDv_r;
            if (pir.containsKey(newDv_p)) {
                newDv_r = pir.get(newDv_p);
            } else {
                newDv_r = new Register(type, newDv_i);
            }
            return new IntervalGuard(currentParam, null, newDv_r);

        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
        else {
            int smallest = Collections.min(ifValues.getAllKeys(smallerDv));
            IntervalGuard sg = new IntervalGuard(
                    currentParam, null, new SuffixValue(type, smallest));
            return sg;
        }
    }

    private IntervalGuard makeIntervalGuard(DataValue<T> biggerDv,
            DataValue<T> smallerDv,
            List<DataValue> prefixValues, SuffixValue currentParam,
            WordValuation ifValues, PIV pir) {
        IntervalGuard smallerGuard = makeSmallerGuard(smallerDv, prefixValues, currentParam, ifValues, pir);
        IntervalGuard biggerGuard = makeBiggerGuard(biggerDv, prefixValues, currentParam, ifValues, pir);
        return new IntervalGuard(currentParam, biggerGuard.getLeftReg(), smallerGuard.getRightReg());
    }

    private IntervalGuard makeBiggerGuard(DataValue<T> biggerDv,
            List<DataValue> prefixValues, SuffixValue currentParam,
            WordValuation ifValues, PIV pir) {
        DataType type = currentParam.getType();
        int newDv_i;
        if (prefixValues.contains(biggerDv)) {
            newDv_i = prefixValues.indexOf(biggerDv) + 1;
            SymbolicDataValue.Parameter newDv_p
                    = new SymbolicDataValue.Parameter(type, newDv_i);
            Register newDv_r;
            if (pir.containsKey(newDv_p)) {
                newDv_r = pir.get(newDv_p);
            } else {
                newDv_r = new Register(type, newDv_i);
            }
            return new IntervalGuard(currentParam, newDv_r, null);

        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
        else {
            int smallest = Collections.min(ifValues.getAllKeys(biggerDv));
            IntervalGuard bg = new IntervalGuard(
                    currentParam, new SuffixValue(type, smallest), null);
            return bg;
        }
    }

    public abstract List<DataValue<T>> getPotential(List<DataValue<T>> vals);

    private DataValue getRegisterValue(SymbolicDataValue r, PIV piv,
            List<DataValue> prefixValues, Constants constants,
            ParValuation pval) {
        if (r.isRegister()) {
            log.log(Level.FINEST,
                    "piv: " + piv + " " + r.toString() + " " + prefixValues);
            Parameter p = piv.getOneKey((Register) r);
            log.log(Level.FINEST, "p: " + p.toString());
            int idx = p.getId();
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

    public abstract DataValue<T> instantiate(SDTGuard guard, Valuation val,
            Constants constants, Collection<DataValue<T>> alreadyUsedValues);

    @Override
    public DataValue instantiate(
            Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv,
            ParValuation pval,
            Constants constants,
            SDTGuard guard,
            Parameter param,
            Set<DataValue<T>> oldDvs) {

        DataType type = param.getType();
        DataValue<T> returnThis = null;
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        log.log(Level.FINEST, "prefix values : " + prefixValues.toString());

        if (guard instanceof EqualityGuard) {
            EqualityGuard eqGuard = (EqualityGuard) guard;
            SymbolicDataValue ereg = eqGuard.getRegister();
            if (ereg.isRegister()) {
                log.log(Level.FINEST, "piv: " + piv.toString()
                        + " " + ereg.toString() + " " + param.toString());
                Parameter p = piv.getOneKey((Register) ereg);
                log.log(Level.FINEST, "p: " + p.toString());
                int idx = p.getId();
                returnThis = prefixValues.get(idx - 1);
            } else if (ereg.isSuffixValue()) {
                Parameter p = new Parameter(type, ereg.getId());
                returnThis = (DataValue<T>) pval.get(p);
            } else if (ereg.isConstant()) {
                returnThis = (DataValue<T>) constants.get((SymbolicDataValue.Constant) ereg);
            }
        } else if (guard instanceof SDTTrueGuard) {

            Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                    constants.<T>values(type),
                    DataWords.<T>valSet(prefix, type),
                    pval.<T>values(type));

            returnThis = this.getFreshValue(new ArrayList<>(potSet));
        } else {
            Collection<DataValue<T>> alreadyUsedValues
                    = DataWords.<T>joinValsToSet(
                            constants.<T>values(type),
                            DataWords.<T>valSet(prefix, type),
                            pval.<T>values(type));
            Valuation val = new Valuation();
            //System.out.println("already used = " + alreadyUsedValues);
            if (guard instanceof IntervalGuard) {
                IntervalGuard iGuard = (IntervalGuard) guard;
                if (!iGuard.isBiggerGuard()) {
                    SymbolicDataValue r = iGuard.getRightReg();
                    DataValue<T> regVal = getRegisterValue(r, piv,
                            prefixValues, constants, pval);

                    val.setValue(toVariable(r), regVal.getId());
                }
                if (!iGuard.isSmallerGuard()) {
                    SymbolicDataValue l = iGuard.getLeftReg();
                    DataValue regVal = getRegisterValue(l, piv,
                            prefixValues, constants, pval);

                    val.setValue(toVariable(l), regVal.getId());
                }
                //instantiate(guard, val, param, constants);
            } else if (guard instanceof SDTIfGuard) {
                SymbolicDataValue r = ((SDTIfGuard) guard).getRegister();
                DataValue<T> regVal = getRegisterValue(r, piv,
                        prefixValues, constants, pval);
                val.setValue(toVariable(r), regVal.getId());
            } else if (guard instanceof SDTOrGuard) {
                SDTGuard iGuard = ((SDTOrGuard) guard).getGuards().get(0);

                returnThis = instantiate(iGuard, val, constants, alreadyUsedValues);
            } else {
                throw new IllegalStateException("only =, != or interval allowed");
            }

            if (!(oldDvs.isEmpty())) {
//                System.out.println("old dvs: " + oldDvs);
                for (DataValue<T> oldDv : oldDvs) {
                    Valuation newVal = new Valuation();
                    newVal.putAll(val);
                    newVal.setValue(toVariable( new SuffixValue(param.getType(), param.getId()) ), oldDv.getId());
//            System.out.println("instantiating " + guard + " with " + newVal);
                    DataValue inst = instantiate(guard, newVal, constants, alreadyUsedValues);
                    if (inst != null) {
//                        System.out.println("returning (reused): " + inst);
                        return inst;
                    }
                }
            }
            returnThis = instantiate(guard, val, constants, alreadyUsedValues);
//            System.out.println("returning (no reuse): " + ret);

        }
        return returnThis;
    }

    
    private boolean hasSuffixRelations(GeneralizedSymbolicSuffix suffix, int idx) {
        // FIXME: support muliple types
        
        if (idx == 1) {
            return false;
        }
        
        EnumSet<DataRelation> dset = EnumSet.noneOf(DataRelation.class);
        for (int i=1; i<idx; i++) {
            dset.addAll(suffix.getSuffixRelations(i, idx));
        }
        return !dset.contains(DataRelation.DEFAULT) || dset.size() > 1;
    }
}
