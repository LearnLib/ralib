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

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.EquivalenceClassFilter;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.word.Word;

/**
 *
 * @author Sofia Cassel
 * @param<T>
 */
public abstract class InequalityTheoryWithEq<T> implements Theory<T> {

//    private static final LearnLogger LOGGER
//            = LearnLogger.getLogger(InequalityTheoryWithEq.class);

//    private Set<SDTGuard> setify(SDTGuard... gs) {
//        Set<SDTGuard> guardSet = new LinkedHashSet<>();
//        for (SDTGuard g : gs) {
//            // if g is an or guard
//            if (g instanceof SDTOrGuard) {
//                SDTOrGuard cg = (SDTOrGuard) g;
//                for (SDTGuard x : cg.getGuards()) {
//                    if (x instanceof SDTIfGuard) {
//                        SDTIfGuard ifX = (SDTIfGuard) x;
//                    // remove contradicting guards
//                    if (guardSet.contains(ifX.toDeqGuard())) {
//                        guardSet.remove(ifX.toDeqGuard());
//                    } else {
//                        guardSet.add(x);
//                    }
//                    }
//                    else {
//                        guardSet.add(x);
//                    }
//                }
//            } else if (g instanceof SDTIfGuard) {
//                SDTIfGuard x = (SDTIfGuard) g;
//                // if guardset already contains the contradicting guard, remove it
//                if (guardSet.contains(x.toDeqGuard())) {
//                    guardSet.remove(x.toDeqGuard());
//                    // if g is a Bigger or Smaller guard, merge into disequality guard
//                    if (!(g instanceof EqualityGuard)) {
//                        guardSet.add(new DisequalityGuard(
//                                x.getParameter(), x.getRegister()));
//                    }
//                } else {
//                    guardSet.add(x);
//                }
//            } else if (g instanceof SDTAndGuard) {
//                SDTAndGuard ag = (SDTAndGuard) g;
//                List<SDTIfGuard> ifs = new ArrayList();
//                for (SDTIfGuard x : ag.getGuards()) {
//                    if (guardSet.contains(x.toDeqGuard())) {
//                        guardSet.remove(x.toDeqGuard());
//                        if (!(g instanceof EqualityGuard)) {
//                            guardSet.add(new DisequalityGuard(
//                                    x.getParameter(), x.getRegister()));
//                        }
//                    } else {
//                        ifs.add(x);
//                    }
//                }
//                if (ifs.size() == 1) {
//                    guardSet.add(ifs.get(0));
//                } else if (ifs.size() > 1) {
//                    guardSet.add(new SDTAndGuard(g.getParameter(),
//                            ifs.toArray(new SDTIfGuard[]{})));
//                }
//
//            }
//        }
//        return guardSet;
//    }
//    private SDTGuard mergeGuardLists(SDTGuard g1, SDTGuard g2) {
//        Set<SDTGuard> guardSet = (setify(g1, g2));
//        if (guardSet.isEmpty()) {
//            return new SDTTrueGuard(g1.getParameter());
//        } else {
//            SDTIfGuard[] guardArray = guardSet.toArray(new SDTIfGuard[]{});
//            if (guardArray.length == 1) {
//                return guardArray[0];
//            } else {
//                return new SDTOrGuard(g1.getParameter(),
//                        guardSet.toArray(new SDTIfGuard[]{}));
//        }
//    }
//    }
//    private List<SDTGuard> preprocess(List<SDTGuard> guardList) {
//        for (SDTGuard g : guardList) {
//
//        }
//    }
//    private void addAllSafely(Collection<SDTGuard> guards, Collection<SDTGuard> toAdd, List<SymbolicDataValue> regPotential) {
////        System.out.println("adding " + toAdd + " to " + guards);
//        for (SDTGuard t : toAdd) {
//            if (t instanceof SDTOrGuard) {
////                System.out.println("or guard");
//                addAllSafely(guards, ((SDTOrGuard) t).getGuards(), regPotential);
////                System.out.println(guards);
//            } else {
//                addSafely(guards, t, regPotential);
//            }
//        }
//    }
//
//    private void addSafely(Collection<SDTGuard> guards, SDTGuard guard, List<SymbolicDataValue> regPotential) {
//        boolean processed = false;
//        if (guard instanceof SDTIfGuard) {
//            SDTIfGuard oGuard = ((SDTIfGuard) guard).toDeqGuard();
////            System.out.println(guard + ": checking if guards contains " + oGuard);
//            if (guards.contains(oGuard)) {
////                System.out.println("yes, removing " + oGuard);
//                guards.remove(oGuard);
//                processed = true;
//            }
//        } else if (guard instanceof IntervalGuard) {
////            System.out.println("guard is intervalguard");
//            IntervalGuard iGuard = (IntervalGuard) guard;
//            if (!iGuard.isIntervalGuard()) {
//                IntervalGuard flipped = iGuard.flip();
////                System.out.println("flipped: " + flipped);
//                if (guards.contains(flipped)) {
//                    guards.remove(flipped);
//                    addAllSafely(guards, iGuard.mergeWith(flipped, regPotential), regPotential);
//                    processed = true;
//                }
//            }
//        } else if (guard instanceof SDTOrGuard) {
////            System.out.println("found or guard");
//            SDTOrGuard oGuard = (SDTOrGuard) guard;
//            addAllSafely(guards, oGuard.getGuards(), regPotential);
//        }
//
//        if (processed == false) {
//            guards.add(guard);
//        }
////        System.out.println("added safely: " + guard + " to " + guards);
//    }
//
//    private void removeProhibited(Collection<SDTGuard> guards, Collection<SDTGuard> prohibited) {
//        for (SDTGuard p : prohibited) {
//            if (guards.contains(p)) {
//                guards.remove(p);
//            }
//        }
//        //System.out.println("guards after removing " + prohibited + " :: " + guards);
//    }
//
////    private Set<SDTGuard> mergeSetWithSet(Set<SDTGuard> guardSet, Set<SDTGuard> targets, Set<SDTGuard> prohibited) {
////        System.out.println("set-merging " + targets + " with " + guardSet);
////        Set<SDTGuard> newGuardSet = new LinkedHashSet<>();
////        for (SDTGuard s : targets) {
////            newGuardSet = mergeOneWithSet(guardSet, s, new ArrayList<SymbolicDataValue>(), prohibited);
////        }
////        return newGuardSet;
////    }
//    private Set<SDTGuard> mergeOneWithSet(Set<SDTGuard> guardSet, SDTGuard target, List<SymbolicDataValue> regPotential) {
//        List<Set<SDTGuard>> inAndOut = new ArrayList<Set<SDTGuard>>();
//        inAndOut.add(guardSet);
//        inAndOut.add(new LinkedHashSet<SDTGuard>());
//        return mergeOneWithSet(inAndOut, target, regPotential).get(0);
//    }
//
//    private List<Set<SDTGuard>> mergeOneWithSet(List<Set<SDTGuard>> inAndOut, SDTGuard target, List<SymbolicDataValue> regPotential) {
//        // merge target with the set guardSet
//        Set<SDTGuard> guardSet = inAndOut.get(0);
//        Set<SDTGuard> prohibited = inAndOut.get(1);
//
//        Set<SDTGuard> newGuardSet = new LinkedHashSet<SDTGuard>();
//
//        List<Set<SDTGuard>> newInAndOut = new ArrayList<Set<SDTGuard>>();
//        newInAndOut.addAll(inAndOut);
//        Boolean[] addedSomething = new Boolean[guardSet.size()];
//        boolean processed = false;
//        newGuardSet.addAll(guardSet);
////        System.out.println("target: " + target);
//        int i = -1;
//        if (!guardSet.isEmpty()) {
//            for (SDTGuard m : guardSet) {
////                System.out.println("guards: " + newGuardSet + " target " + target);
//                i++;
//                addedSomething[i] = false;
//                // if target isn't in guardSet and isn't prohibited
//                if (!(guardSet.contains(target)) && !(prohibited.contains(target))) {
//
//                    assert !(target instanceof SDTOrGuard && m instanceof SDTOrGuard);
//
//                    if (target instanceof SDTIfGuard) {
//                        // if it is an equality or disequality, remove the opposite by adding both to prohibited
//                        SDTIfGuard oGuard = ((SDTIfGuard) target).toDeqGuard();
////                        System.out.println(target + ": checking if guards contains " + oGuard);
//                        if (guardSet.contains(oGuard)) {
////                            System.out.println("yes, removing " + oGuard);
//                            prohibited.add(oGuard);
//                            prohibited.add(target);
//                            processed = true;
//                        }
//                    } else if (target instanceof IntervalGuard) {
////                        System.out.println(target + " is iGuard");
//                        // if it is an interval guard, check if the set contains flipped
//                        IntervalGuard iGuard = (IntervalGuard) target;
//                        if (!iGuard.isIntervalGuard()) {
//                            IntervalGuard flipped = iGuard.flip();
////                            System.out.println("flipped: " + flipped);
////                            System.out.println("guardSet " + guardSet + " " + guardSet.contains(flipped));
//                            if (guardSet.contains(flipped)) {
//                                prohibited.add(flipped);
//                                prohibited.add(target);
//                                newInAndOut.add(0, newGuardSet);
//                                newInAndOut.add(1, prohibited);
//                                List<Set<SDTGuard>> nextInAndOut = mergeOneWithSet(newInAndOut, iGuard.toDeqGuard(), regPotential);
//                                newGuardSet = nextInAndOut.get(0);
//                                prohibited = nextInAndOut.get(1);//
//                                processed = true;
//                            }
//                        }
//                    }
//                    if (!processed) {
//                        Set<SDTGuard> refSet = new LinkedHashSet<>();
//                        refSet.add(target);
//                        refSet.add(m);
//                        Set<SDTGuard> mWithTarget = target.mergeWith(m, regPotential);
////                        System.out.println("merging: " + target + " + " + m + " --> " + mWithTarget + ", " + refSet.containsAll(mWithTarget));
//                        if (!mWithTarget.contains(target)) {
//                            prohibited.add(target);
//                        }
//                        if (!mWithTarget.contains(m)) {
//                            prohibited.add(m);
//                        }
//                        addedSomething[i] = !(refSet.containsAll(mWithTarget));
////                    System.out.println("g: " + guardSet);
////                    System.out.println("n: " + newGuardSet);
//
//                        if (addedSomething[i]) { // && !(newGuardSet.containsAll(mWithTarget))) {
//                            for (SDTGuard x : mWithTarget) {
////                            List<Set<SDTGuard>> newInAndOut = new ArrayList<Set<SDTGuard>>();
////                                System.out.println("XXX: for " + x + ": adding in: " + newGuardSet + " and out: " + prohibited);
//                                newInAndOut.add(0, newGuardSet);
//                                newInAndOut.add(1, prohibited);
//                                List<Set<SDTGuard>> nextInAndOut = mergeOneWithSet(newInAndOut, x, regPotential);
//                                newGuardSet = nextInAndOut.get(0);
//                                prohibited = nextInAndOut.get(1);
////                                System.out.println("XXX: after " + x + ": in: " + newGuardSet + " and out: " + prohibited);
//                            }
//                        } else {
//                            newGuardSet.addAll(mWithTarget);
//                        }
//
////                    newGuardSet.addAll(mWithTarget);
////                        System.out.println("into: " + newGuardSet);
//                    }
//                }
//
//            }
//
//        }
//        else {
//            newGuardSet.add(target);
//
//        }
//        newInAndOut.add(0, newGuardSet);
//            newInAndOut.add(1, prohibited);
////        Set<SDTGuard> temp = new LinkedHashSet<>();
////        Set<SDTGuard> prohibited = new LinkedHashSet<>();
////            System.out.println("temp is: " + temp + " and prohibited: " + prohibited);
////        for (SDTGuard m : merged) {
////            assert !(target instanceof SDTOrGuard && m instanceof SDTOrGuard);
////            Set<SDTGuard> mWithTarget = target.mergeWith(m);
////            System.out.println("merging: " + target + " + " + m + " --> " + mWithTarget);
////            if (!mWithTarget.contains(target)) {
////                prohibited.add(target);
////                temp.remove(target);
////            }
////            if (!mWithTarget.contains(m)) {
////                prohibited.add(m);
////                temp.remove(target);
////            }
////            addAllSafely(temp, mWithTarget, prohibited);
////            System.out.println("... but after adding " + mWithTarget + " safely: " + temp + " with " + prohibited);
////        }
////        System.out.println("return: " + temp);
////        return temp;
////        System.out.println("removing: " + prohibited);
//        removeProhibited(newGuardSet, prohibited);
////        System.out.println("... and after removing :: " + newGuardSet);
////        System.out.println("modified is " + Arrays.toString(addedSomething));
//        if (hasTrue(addedSomething) && !(guardSet.equals(newGuardSet))) {
////            System.out.println("try again");
//            return mergeOneWithSet(newInAndOut, target, regPotential);
//        } else {
//            return newInAndOut;
//        }
//    }

//    private Set<SDTGuard> mergeOneWithSet(Set<SDTGuard> guardSet, SDTGuard target, List<SymbolicDataValue> regPotential, Set<SDTGuard> prohibited) {
//
//        //Set<SDTGuard> prohibited = new LinkedHashSet<>();
//        Set<SDTGuard> newGuardSet = new LinkedHashSet<>();
//        Boolean[] modified = new Boolean[guardSet.size()];
//        boolean processed = false;
//        newGuardSet.addAll(guardSet);
//
//        int i = -1;
//        for (SDTGuard m : guardSet) {
//            System.out.println("guards: " + newGuardSet);
//            i++;
//            modified[i] = false;
//            if (!(guardSet.contains(target)) && !(prohibited.contains(target))) {
//
//                assert !(target instanceof SDTOrGuard && m instanceof SDTOrGuard);
//
//                if (target instanceof SDTIfGuard) {
//                    SDTIfGuard oGuard = ((SDTIfGuard) target).toDeqGuard();
////            System.out.println(guard + ": checking if guards contains " + oGuard);
//                    if (guardSet.contains(oGuard)) {
////                System.out.println("yes, removing " + oGuard);
//                        prohibited.add(oGuard);
//                        prohibited.add(target);
//                        processed = true;
//                    }
//                } else if (target instanceof IntervalGuard) {
//                    IntervalGuard iGuard = (IntervalGuard) target;
//                    if (!iGuard.isIntervalGuard()) {
//                        IntervalGuard flipped = iGuard.flip();
//                System.out.println("flipped: " + flipped);
//                        if (guardSet.contains(flipped)) {
//                            prohibited.add(flipped);
//                            prohibited.add(target);
//                            processed = true;
//                        }
//                    }
//                }
//                if (!processed) {
//                    Set<SDTGuard> refSet = new LinkedHashSet<>();
//                    refSet.add(target);
//                    refSet.add(m);
//                    Set<SDTGuard> mWithTarget = target.mergeWith(m, regPotential);
//                    System.out.println("merging: " + target + " + " + m + " --> " + mWithTarget + ", " + refSet.equals(mWithTarget));
//                    if (!mWithTarget.contains(target)) {
//                        prohibited.add(target);
//                        //modified[i] = true;
////                guardSet.remove(target);
//                    }
//                    if (!mWithTarget.contains(m)) {
//                        prohibited.add(m);
//                        //modified[i] = true;
////                guardSet.remove(target);
//                    }
//                    modified[i] = !(refSet.equals(mWithTarget));
//                    System.out.println("g: " + guardSet);
//                    System.out.println("n: " + newGuardSet);
//
//
//                    if (modified[i] && !(newGuardSet.containsAll(mWithTarget))) {
//                        for (SDTGuard x : mWithTarget) {
//                            newGuardSet = mergeOneWithSet(newGuardSet, x, regPotential, prohibited);
//                        }
//                    }
//                    else {
//                        newGuardSet.addAll(mWithTarget);
//                    }
//
////                    newGuardSet.addAll(mWithTarget);
//                    System.out.println("into: " + newGuardSet);
//                }
//            }
//        }
////        Set<SDTGuard> temp = new LinkedHashSet<>();
////        Set<SDTGuard> prohibited = new LinkedHashSet<>();
////            System.out.println("temp is: " + temp + " and prohibited: " + prohibited);
////        for (SDTGuard m : merged) {
////            assert !(target instanceof SDTOrGuard && m instanceof SDTOrGuard);
////            Set<SDTGuard> mWithTarget = target.mergeWith(m);
////            System.out.println("merging: " + target + " + " + m + " --> " + mWithTarget);
////            if (!mWithTarget.contains(target)) {
////                prohibited.add(target);
////                temp.remove(target);
////            }
////            if (!mWithTarget.contains(m)) {
////                prohibited.add(m);
////                temp.remove(target);
////            }
////            addAllSafely(temp, mWithTarget, prohibited);
////            System.out.println("... but after adding " + mWithTarget + " safely: " + temp + " with " + prohibited);
////        }
////        System.out.println("return: " + temp);
////        return temp;
//        System.out.println("removing: " + prohibited);
//        removeProhibited(newGuardSet, prohibited);
//        System.out.println("... and after removing :: " + newGuardSet);
//        System.out.println("modified is " + Arrays.toString(modified));
//        if (hasTrue(modified) && !(guardSet.equals(newGuardSet))) {
//            System.out.println("try again");
//            return mergeOneWithSet(newGuardSet, target, regPotential, prohibited);
//        } else {
//            return newGuardSet;
//        }
//    }
    // Returns true if all elements of a boolean array are true.
//    private boolean hasTrue(Boolean[] maybeArr) {
//        boolean maybe = false;
//        for (int c = 0; c < (maybeArr.length); c++) {
//            //LOGGER.trace(maybeArr[c]);
//            if (maybeArr[c]) {
//                maybe = true;
//                break;
//            }
//        }
//        return maybe;
//    }
//
//    private SDTGuard GuardSetFromList(Set<SDTGuard> merged, List<SDTGuard> remaining, SuffixValue currentParam, List<SymbolicDataValue> regPotential) {
////        merge list of guards with a set of guards.  Initially, the set is empty and the list is full
////        System.out.println("rem = " + remaining + ", merged = " + merged);
//
//        //if the set is empty
//        // and the set is also empty: return true guard
//        // and the set is NOT empty: return either
//        //if the set is empty
//        if (remaining.isEmpty()) {
////            if (merged.isEmpty()) {
////                return new SDTTrueGuard(currentParam);
////            } else {
//            SDTGuard[] guardArray = merged.toArray(new SDTGuard[]{});
//            if ((merged.size() == 1)) { // || hasOnlyEqs(merged)) {
//                return guardArray[0];
//            } else {
//                return new SDTOrGuard(currentParam, guardArray);
//            }
//
////            }
//        }
//        if (merged.isEmpty()) {
//            merged.add(remaining.remove(0));
//            return GuardSetFromList(merged, remaining, currentParam, regPotential);
//        }
//        SDTGuard target = remaining.remove(0);
//        Set<SDTGuard> temp = mergeOneWithSet(merged, target, regPotential);
//
//        return GuardSetFromList(temp, remaining, currentParam, regPotential);
////         ensure that a new OrGuard is returned if there is nothing remaining
//    }
//
////    private Set<SDTGuard> getHeads(Set<List<SDTGuard>> listSets) {
////        Set<SDTGuard> retSet = new LinkedHashSet<>();
////        for (List<SDTGuard> l : listSets) {
////            retSet.add(l.get(0));
////        }
////        return retSet;
////    }
////    private List<SDTGuard> getListFromHead(SDTGuard guard, Set<List<SDTGuard>> listSets) {
////        Set<SDTGuard> heads = getHeads(listSets);
////        if (heads.contains(guard)) {
////            for (List<SDTGuard> l : listSets) {
////                if (l.get(0).equals(guard)) {
////                    return l;
////                }
////            }
////        }
////        throw new IllegalStateException("not found");
////    }
//    private Map<List<SDTGuard>, SDT> modGuardLists(SDTGuard refGuard, SDT refSDT, Map<List<SDTGuard>, SDT> partitionedMap) {
//        boolean merged = false;
//        Map<List<SDTGuard>, SDT> newParMap = new LinkedHashMap<>();
////        System.out.println("modGuardLists for refGuard " + refGuard + ", refSDT " + refSDT + ", map: " + partitionedMap);
//        for (Map.Entry<List<SDTGuard>, SDT> par : partitionedMap.entrySet()) {
////            merged = false;
////            System.out.println("par: " + par);
//            List<SDTGuard> headList = par.getKey();
//            List<SDTGuard> newHeadList = new ArrayList<>();
//            newHeadList.addAll(headList);
//            //addAllSafely(newHeadList, headList, new LinkedHashSet<SDTGuard>());
//            SDT headSDT = par.getValue();
//            assert headSDT.getClass().equals(refSDT.getClass());
//            //assert !(headSDT.isEmpty());
//            //assert !(refSDT.isEmpty());
//            //SDTGuard headGuard = headList.get(0);
//            SDT newSDT = headSDT;
////            System.out.println("head: " + newHeadList + " against " + refGuard);
//            if (headSDT instanceof SDTLeaf) {
////                System.out.println("sdt leaves");
//                assert refSDT instanceof SDTLeaf;
//                if (headSDT.isAccepting() == refSDT.isAccepting() && !merged) {
//                    merged = true;
//                    newHeadList.add(refGuard);
////                    System.out.println("added ref " + refSDT + " eq to " + headSDT);
//                }
//                //newParMap.put(newHeadList, newSDT);
//
//            }
//            if (refGuard instanceof IntervalGuard && !merged) {
//                IntervalGuard iRefGuard = (IntervalGuard) refGuard;
//                if (!iRefGuard.isIntervalGuard()) {
//                    EqualityGuard eqGuard = iRefGuard.toEqGuard();
//                    if (newHeadList.contains(eqGuard)) {
////                        System.out.println("trying Deq " + refGuard + " against " + newHeadList + " with " + eqGuard);
//                        SDT joinedSDT = getJoinedSDT(((IntervalGuard) refGuard).toEqGuard(), refSDT, headSDT);
//                        if (joinedSDT != null) {
////                            System.out.println("can merge: " + refGuard + " with EQ" + headList);
//                            newHeadList.add(refGuard);
//                            newSDT = joinedSDT;
//                            //newSDT = refSDT.relabelUnderEq((EqualityGuard) headGuard);
//                            merged = true;
//                            //newParMap.put(newHeadList, newSDT);
//                        }
//                    }
//                } else {
//                    assert iRefGuard.isIntervalGuard();
//                    EqualityGuard eqG = null;
//                    if (newHeadList.contains(new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getLeftReg()))) {
//                        eqG = new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getLeftReg());
//                    } else if (newHeadList.contains(new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getRightReg()))) {
//                        eqG = new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getLeftReg());
//                    }
//                    if (eqG != null) {
////                        System.out.println("trying Eq " + refGuard + " against " + newHeadList + " with " + eqG);
//                        SDT joinedSDT = getJoinedSDT(eqG, refSDT, headSDT);
//                        if (joinedSDT != null) {
////                            System.out.println("can merge: EQ" + headList + " with " + refGuard);
//                            newHeadList.add(refGuard);
//                            newSDT = joinedSDT;
//                            merged = true;
//                            //newParMap.put(newHeadList, newSDT);
//                        }
//                    }
//                }
//
//            }
//            if (refGuard instanceof EqualityGuard && !merged) {
////                System.out.println("trying Eq " + refGuard + " against " + newHeadList);
//                SDT joinedSDT = getJoinedSDT((EqualityGuard) refGuard, headSDT, refSDT);
//                if (joinedSDT != null) {
////                    System.out.println("can merge: EQ" + refGuard + " with " + headList);
//                    newHeadList.add(0, refGuard);
//                    newSDT = joinedSDT;
//                    merged = true;
//                    //newParMap.put(newHeadList, newSDT);
//                }
//            }
//            if (refSDT.canUse(headSDT) && headSDT.canUse(refSDT) && !merged) {
////                System.out.println("LAST RESORT: can use");
//                newHeadList.add(refGuard);
//                merged = true;
//                //newParMap.put(newHeadList, newSDT);
//            }
//
//            newParMap.put(newHeadList, newSDT);
//
//        }
//
//        if (!merged) {
////            System.out.println(refGuard + " cannot merge with anything in " + partitionedMap);
//            List<SDTGuard> newUnmergedList = new ArrayList<>();
//            newUnmergedList.add(refGuard);
////            newParMap.putAll(partitionedMap);
////            System.out.println("adding " + newUnmergedList + " ---> " + refSDT);
//
//            newParMap.put(newUnmergedList, refSDT);
////            System.out.println("newParMap is now " + newParMap);
//        }
//
//        //newParMap.putAll(partitionedMap);
//        assert !newParMap.isEmpty();
//
////        System.out.println("RETURNED: newParMap " + newParMap);
//        return newParMap;
//    }
//
//    private Map<List<SDTGuard>, SDT> partitionGuards(Map<SDTGuard, SDT> refMap) {
//
////        System.out.println("partitioning " + refMap);
//        // start with an empty map
//        Map<List<SDTGuard>, SDT> parMap = new LinkedHashMap<>();
//        for (Map.Entry<SDTGuard, SDT> ref : refMap.entrySet()) {
//            SDTGuard refGuard = ref.getKey();
//            SDT refSDT = ref.getValue();
//
////            if (refGuard instanceof SDTTrueGuard) {
////                assert refMap.size() == 1;
////                List<SDTGuard> trueList = new ArrayList<SDTGuard>();
////                trueList.add(refGuard);
////                parMap = new LinkedHashMap<List<SDTGuard>, SDT>();
////                parMap.put(trueList, refSDT);
////                return parMap;
////            }
////            System.out.println("pre par map = " + parMap);
//            parMap = modGuardLists(refGuard, refSDT, parMap);
//            // get the heads of all lists in the partitioned map
//            //System.out.println("partitioned map parMap: " + parMap);
//
//        }
////        System.out.println("partitioned map parMap: " + parMap);
//
//        return parMap;
//    }
//
//    // for each element in the map
//    // check if it can merge with the head of any of the lists in the set
//    // if yes, then add to that list
//    // if no, then start a new list
////}
//    private SDT getJoinedSDT(EqualityGuard guard, SDT deqSDT, SDT eqSDT) {
//        //boolean canJoin = false;
//
//        EqualityGuard eqGuard = (EqualityGuard) guard;
//        List<SDTIfGuard> ds = new ArrayList();
//        ds.add(eqGuard);
////        System.out.println("checking if T" + deqSDT + " is eq to O" + eqSDT + " under " + eqGuard);
//
//        SDT newTargetSDT = deqSDT.relabelUnderEq(ds);
//        assert newTargetSDT != null;
//
////        System.out.println("checking if T" + deqSDT + " is eq to O" + eqSDT + " under " + eqGuard);
//        if (eqSDT.isEquivalentUnder(deqSDT, ds)) {
////            System.out.println("yes");
////            System.out.println("return target: " + deqSDT + " eq under " + eqGuard);
//            return deqSDT;
//
////        } else {
////            System.out.println("checking if O" + otherSDT + " is eq to NT" + newTargetSDT + " under " + ds);
////
////            if (newTargetSDT.canUse(otherSDT)) {
////                System.out.println("return newTarget: " + newTargetSDT + " canUse ");
//////                System.out.println("yes");
////                return otherSDT;
////            }
////
//        }
////        } //else if (guard instanceof IntervalGuard) {
////            IntervalGuard iGuard = (IntervalGuard) guard;
////            List<SDTIfGuard> ds = new ArrayList();
////            if (!iGuard.isSmallerGuard()) {
////                ds.add(new EqualityGuard(iGuard.getParameter(), iGuard.getLeftReg()));
////            }
////            if (!iGuard.isBiggerGuard()) {
////                ds.add(new EqualityGuard(iGuard.getParameter(), iGuard.getRightReg()));
////            }
////            if (otherSDT.isEquivalentUnder(targetSDT, ds)) {
////                return true;
////            }
////        }
//        return null;
//    }

//    private Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> unmerged, SuffixValue currentParam, List<SymbolicDataValue> regPotential) {
//        if (unmerged.keySet().size() == 1) {
//            assert unmerged.containsKey(new SDTTrueGuard(currentParam));
//            return unmerged;
//        }
//        Map<SDTGuard, SDT> merged = mergeGuards(new LinkedHashMap<SDTGuard, SDT>(), currentParam, unmerged, regPotential);
//
//        if (merged.keySet().size() == 1) {
//
////            System.out.println("unmerged: " + merged);
//            // hack??
////            SDTGuard onlyGuard = merged.keySet().iterator().next();
////            SDT onlySDT = merged.get(onlyGuard);
////            if (!(onlyGuard instanceof SDTTrueGuard)) {
////                assert onlyGuard instanceof SDTIfGuard;
////                merged.remove(onlyGuard);
////                merged.put(new SDTTrueGuard(currentParam), onlySDT);
////            }
//            assert merged.containsKey(new SDTTrueGuard(currentParam));
//
//        }
////        System.out.println("merged: " + merged);
//        return merged;
//    }
//
//    private Map<SDTGuard, SDT>
//            mergeGuards(Map<SDTGuard, SDT> first, SuffixValue currentParam, Map<SDTGuard, SDT> second, List<SymbolicDataValue> regPotential) {
////        System.out.println("comparing " + first + " with " + second);
////        if (!first.keySet().isEmpty() && first.keySet().size() <= second.keySet().size()) {
////            System.out.println("return " + first);
////            return first;
////        } else {
//        // if they are not the same size, we want to keep merging them
//        Map<SDTGuard, SDT> newSecond = mgGuards(second, currentParam, regPotential);
////            return mergeGuards(second, currentParam, newSecond);
//        //      }
//        return newSecond;
//    }
    // given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT.
//    private Map<SDTGuard, SDT>
//            mgGuards(Map<SDTGuard, SDT> unmerged, SuffixValue currentParam, List<SymbolicDataValue> regPotential) {
//        assert !unmerged.isEmpty();
//
//        Map<SDTGuard, SDT> retMap = new LinkedHashMap<>();
////        System.out.println("unmerged: " + unmerged);
//        Map<List<SDTGuard>, SDT> partitionedMap = partitionGuards(unmerged);
////        System.out.println("partitionedMap: " + partitionedMap);
//        for (Map.Entry<List<SDTGuard>, SDT> par : partitionedMap.entrySet()) {
//
////            List<SDTGuard> preprocessed = new ArrayList<SDTGuard>();
////            preprocessed.addAll(par.getKey());
////            System.out.println("preprocessed: " + preprocessed);
////            addAllSafely(new ArrayList<SDTGuard>(), preprocessed, new LinkedHashSet<SDTGuard>());
////            System.out.println("postprocessed: " + preprocessed);
////            System.out.println("partitioned map entry: " + par.getKey());
//            SDTGuard newSDTGuard = GuardSetFromList(new LinkedHashSet<SDTGuard>(), par.getKey(), currentParam, regPotential);
////            System.out.println("--> Guard: " + newSDTGuard);
//            if (newSDTGuard instanceof SDTOrGuard) {
//                List<SDTGuard> subguards = ((SDTOrGuard) newSDTGuard).getGuards();
//                if (subguards.isEmpty()) {
//                    retMap.put(new SDTTrueGuard(currentParam), par.getValue());
//                } else {
//                    for (SDTGuard subguard : subguards) {
//                        retMap.put(subguard, par.getValue());
//                    }
//                }
//            } else {
//                retMap.put(newSDTGuard, par.getValue());
//            }
//        }
////        System.out.println("retMap: " + retMap);
//        assert !retMap.isEmpty();
//        //System.out.println("-----------------------------------\n" + partitionedMap + "\n-----------------------PARTITIONING-------------------\n" + retMap + "\n---------------------------------");
//        return retMap;
//    }

    // given a set of registers and a set of guards, keep only the registers
// that are mentioned in any guard
//    private PIV keepMem(Set<SDTGuard> guardSet) {
//        PIV ret = new PIV();
//
//        // 1. create guard list
//        Set<SDTGuard> ifGuards = new LinkedHashSet<>();
//        for (SDTGuard g : guardSet) {
//            if (g instanceof SDTIfGuard) {
//                ifGuards.add((SDTIfGuard) g);
//            } else if (g instanceof SDTOrGuard) {
//                ifGuards.addAll(((SDTOrGuard) g).getGuards());
//            }
//        }
//
//        // 2. determine which registers to keep
//        Set<SymbolicDataValue> tempRegs = new LinkedHashSet<>();
//        for (SDTGuard g : ifGuards) {
//            if (g instanceof SDTAndGuard) {
//                tempRegs.addAll(((SDTAndGuard) g).getAllRegs());
//            } else if (g instanceof SDTIfGuard) {
//                tempRegs.add(((SDTIfGuard) g).getRegister());
//            }
//        }
//        for (SymbolicDataValue r : tempRegs) {
//            Parameter p = new Parameter(r.getType(), r.getId());
//            if (r instanceof Register) {
//                ret.put(p, (Register) r);
//            }
//        }
//        return ret;
//    }
//

    boolean useSuffixOpt = false;

    public Map<DataValue<T>, SDTGuard> equivalenceClasses(Word<PSymbolInstance> prefix,
    		SymbolicSuffix suffix,
    		SuffixValue suffixValue,
    		Map<DataValue<T>, SymbolicDataValue> potValuation,
    		SuffixValuation suffixValues,
    		Constants consts,
    		WordValuation values) {
    	Map<DataValue<T>, SDTGuard> valueGuards = generateEquivClasses(prefix, suffixValue, potValuation, consts);
    	// apply suffix restrictions
    	return filterEquivClasses(valueGuards, prefix, suffix, suffixValue, potValuation, suffixValues, consts, values);
    }

    private Map<DataValue<T>, SDTGuard> generateEquivClasses(Word<PSymbolInstance> prefix,
    		SuffixValue suffixValue,
    		Map<DataValue<T>, SymbolicDataValue> potValuation,
    		Constants consts) {

    	Map<DataValue<T>, SDTGuard> valueGuards = new LinkedHashMap<>();

    	if (potValuation.isEmpty()) {
    		DataValue<T> fresh = getFreshValue(new ArrayList<>());
    		valueGuards.put(fresh, new SDTTrueGuard(suffixValue));
    		return valueGuards;
    	}
		int usedVals = potValuation.size();
		List<DataValue<T>> potential = new ArrayList<>();
		potential.addAll(potValuation.keySet());
		List<DataValue<T>> sortedPot = sort(potential);

		Valuation vals = new Valuation();
		for (Map.Entry<DataValue<T>, SymbolicDataValue> pot : potValuation.entrySet()) {
			SymbolicDataValue r = pot.getValue();
			DataValue<T> dv = pot.getKey();
			// TODO: fix unchecked invocation
			vals.setValue(toVariable(r), dv.getId());
		}

		// smallest
		DataValue<T> dl = sortedPot.get(0);
		SymbolicDataValue rl = potValuation.get(dl);
		IntervalGuard sg = new IntervalGuard(suffixValue, null, rl);
		DataValue<T> smallest = instantiate(sg, vals, consts, sortedPot);
		valueGuards.put(smallest, sg);

		for (int i = 1; i < usedVals; i++) {
			// equality
			EqualityGuard eg = new EqualityGuard(suffixValue, rl);
			valueGuards.put(dl, eg);

			// interval
			DataValue<T> dr = sortedPot.get(i);
			SymbolicDataValue rr = potValuation.get(dr);
			IntervalGuard ig = new IntervalGuard(suffixValue, rl, rr);
			DataValue<T> di = instantiate(ig, vals, consts, sortedPot);
			valueGuards.put(di, ig);

			dl = dr;
			rl = rr;
		}
		EqualityGuard eg = new EqualityGuard(suffixValue, rl);
		valueGuards.put(dl, eg);

		// greatest
		IntervalGuard gg = new IntervalGuard(suffixValue, rl, null);
		DataValue<T> dg = instantiate(gg, vals, consts, sortedPot);
		valueGuards.put(dg, gg);

		return valueGuards;
	}

	private Map<DataValue<T>, SDTGuard> filterEquivClasses(Map<DataValue<T>, SDTGuard> valueGuards,
			Word<PSymbolInstance> prefix,
			SymbolicSuffix suffix,
			SuffixValue suffixValue,
			Map<DataValue<T>, SymbolicDataValue> potValuation,
			SuffixValuation suffixVals,
			Constants consts,
			WordValuation values) {
		List<DataValue<T>> equivClasses = new ArrayList<>();
		equivClasses.addAll(valueGuards.keySet());
		EquivalenceClassFilter<T> eqcFilter = new EquivalenceClassFilter<>(equivClasses, useSuffixOpt);
		List<DataValue<T>> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(suffixValue), prefix, suffix.getActions(), values);

		Map<DataValue<T>, SDTGuard> ret = new LinkedHashMap<>();
		for (Map.Entry<DataValue<T>, SDTGuard> e : valueGuards.entrySet()) {
			DataValue<T> ec = e.getKey();
			if (filteredEquivClasses.contains(ec)) {
				ret.put(ec, e.getValue());
			}
		}
		return ret;
	}

	private List<DataValue<T>> sort(Collection<DataValue<T>> list) {
		List<DataValue<T>> sorted = new ArrayList<>();
		sorted.addAll(list);
		sorted.sort(getComparator());
		return sorted;
	}

	protected abstract Comparator<DataValue<T>> getComparator();

	protected Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> sdts,
			Map<DataValue<T>, SDTGuard> equivClasses,
			Collection<DataValue<T>> filteredOut) {
		Map<SDTGuard, SDT> merged = new LinkedHashMap<>();

		List<DataValue<T>> ecValuesSorted = sort(equivClasses.keySet());
		Iterator<DataValue<T>> ecit = ecValuesSorted.iterator();
		DataValue<T> first = ecit.next();
		SDTGuard mergedGuards = equivClasses.get(first);
		SDT currSdt = sdts.get(mergedGuards);

		Comparator<DataValue<T>> comparator = getComparator();
		Iterator<DataValue<T>> filtit = sort(filteredOut).iterator();
		DataValue<T> smallerDataValue = getSmallerDataValue(first);
		DataValue<T> filtered = filtit.hasNext() ? filtit.next() : smallerDataValue;
		boolean lastFilteredOut = comparator.compare(first, filtered) == 0;
		if (lastFilteredOut) {
			assert ecit.hasNext();
			filtered = filtit.hasNext() ? filtit.next() : smallerDataValue;
		}

		while(ecit.hasNext()) {
			DataValue<T> next = ecit.next();
			SDTGuard nextGuard = equivClasses.get(next);
			SDT nextSdt = sdts.get(nextGuard);
			boolean thisFilteredOut = comparator.compare(next, filtered) == 0;
			boolean inequivalentSdts = thisFilteredOut || (currSdt == null ? false : !currSdt.isEquivalent(nextSdt, new VarMapping<>()));
			if (thisFilteredOut || inequivalentSdts || lastFilteredOut) {
				if (!lastFilteredOut) {
					merged.put(mergedGuards, currSdt);
				}
				mergedGuards = nextGuard;
				currSdt = nextSdt;
			} else {
				mergedGuards = mergeIntervals(mergedGuards, nextGuard);
			}

			lastFilteredOut = thisFilteredOut;
			if (comparator.compare(next, filtered) >= 0 && filtit.hasNext()) {
				filtered = filtit.next();
			}
		}
		if (!lastFilteredOut) {
			merged.put(mergedGuards, currSdt);
		}

		// check for disequality guard (i.e., both s < r and s > r guards are present for some r)
		merged = checkForDisequality(merged);

		// if only one guard, replace with true guard
		if (merged.size() == 1) {
			Map.Entry<SDTGuard, SDT> entry = merged.entrySet().iterator().next();
			SDTGuard g = entry.getKey();
			if (g instanceof DisequalityGuard || (g instanceof IntervalGuard && ((IntervalGuard) g).isBiggerGuard())) {
				merged = new LinkedHashMap<>();
				merged.put(new SDTTrueGuard(g.getParameter()), entry.getValue());
			}
		}

		assert !merged.isEmpty();

		return merged;
	}

	private SDTGuard mergeIntervals(SDTGuard leftGuard, SDTGuard rightGuard) {
		SuffixValue suffixValue = leftGuard.getParameter();
		if (leftGuard instanceof EqualityGuard) {
			EqualityGuard egLeft = (EqualityGuard) leftGuard;
			SymbolicDataValue rl = egLeft.getRegister();
			if (rightGuard instanceof IntervalGuard) {
				IntervalGuard igRight = (IntervalGuard) rightGuard;
				if (!igRight.isSmallerGuard() && igRight.getLeftReg().equals(rl)) {
					if (igRight.isBiggerGuard()) {
						return IntervalGuard.greaterOrEqualGuard(suffixValue, rl);
					} else {
						return new IntervalGuard(suffixValue, rl, igRight.getRightReg(), true, false);
					}
				}
			}
		} else if (leftGuard instanceof IntervalGuard && !((IntervalGuard) leftGuard).isBiggerGuard()) {
			IntervalGuard igLeft = (IntervalGuard) leftGuard;
			SymbolicDataValue rr = igLeft.getRightReg();
			if (igLeft.isSmallerGuard()) {
				if (rightGuard instanceof EqualityGuard && ((EqualityGuard) rightGuard).getRegister().equals(rr)) {
					return IntervalGuard.lessOrEqualGuard(suffixValue, rr);
				} else if (rightGuard instanceof IntervalGuard &&
						!((IntervalGuard) rightGuard).isSmallerGuard() &&
						((IntervalGuard) rightGuard).getLeftReg().equals(rr)) {
					IntervalGuard igRight = (IntervalGuard) rightGuard;
					if (igRight.isIntervalGuard()) {
						return IntervalGuard.lessGuard(suffixValue, igRight.getRightReg());
					} else {
						return new SDTTrueGuard(suffixValue);
					}
				}
			} else if (igLeft.isIntervalGuard()) {
				if (rightGuard instanceof EqualityGuard && ((EqualityGuard) rightGuard).getRegister().equals(rr)) {
					return new IntervalGuard(suffixValue, igLeft.getLeftReg(), rr, igLeft.isLeftClosed(), true);
				} else if (rightGuard instanceof IntervalGuard &&
						!((IntervalGuard) rightGuard).isSmallerGuard() &&
						((IntervalGuard) rightGuard).getLeftReg().equals(rr)) {
					IntervalGuard igRight = (IntervalGuard) rightGuard;
					if (igRight.isBiggerGuard()) {
						return new IntervalGuard(suffixValue, igLeft.getLeftReg(), null, igLeft.isLeftClosed(), false);
					} else {
						return new IntervalGuard(suffixValue, igLeft.getLeftReg(), igRight.getRightReg(), igLeft.isLeftClosed(), igRight.isRightClosed());
					}
				}
			}
		}
		throw new java.lang.IllegalArgumentException("Guards are not compatible for merging");
	}

	private Map<SDTGuard, SDT> checkForDisequality(Map<SDTGuard, SDT> guards) {
		int size = guards.size();
		if (size < 1 || size > 3)
			return guards;

		Optional<SDTGuard> less = guards.keySet().stream().filter(g -> g instanceof IntervalGuard && ((IntervalGuard) g).isSmallerGuard()).findAny();
		Optional<SDTGuard> greater = guards.keySet().stream().filter(g -> g instanceof IntervalGuard && ((IntervalGuard) g).isBiggerGuard()).findAny();
		if (less.isPresent() && greater.isPresent()) {
			IntervalGuard lg = (IntervalGuard) less.get();
			IntervalGuard gg = (IntervalGuard) greater.get();
			SDT ls = guards.get(lg);
			SDT gs = guards.get(gg);
			SymbolicDataValue rr = lg.getRightReg();
			SymbolicDataValue rl = gg.getLeftReg();
			if (rr.equals(rl) && ls.isEquivalent(gs, new VarMapping<>())) {
				Map<SDTGuard, SDT> diseq = new LinkedHashMap<>();
				diseq.put(new DisequalityGuard(lg.getParameter(), rr), guards.get(lg));
				Optional<SDTGuard> equal = guards.keySet().stream().filter(g -> g instanceof EqualityGuard).findAny();
				if (equal.isPresent()) {
					EqualityGuard eg = (EqualityGuard) equal.get();
					assert eg.getRegister().equals(rr);
					diseq.put(eg, guards.get(eg));
				}
				return diseq;
			}
		}
		return guards;
	}

	private DataValue<T> getSmallerDataValue(DataValue<T> dv) {
		SuffixValue s = new SuffixValue(dv.getType(), 1);
		Register r = new Register(dv.getType(), 1);
		IntervalGuard ig = new IntervalGuard(s, null, r);
		Valuation val = new Valuation();
		val.setValue(toVariable(r), dv.getId());
		return instantiate(ig, val, new Constants(), new ArrayList<>());
	}

    private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
        PIV ret = new PIV();
        for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
            SDTGuard mg = e.getKey();
            if (mg instanceof SDTIfGuard) {
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
            } else if (mg instanceof SDTAndGuard) {
            	Set<SymbolicDataValue> rSet = ((SDTAndGuard) mg).getAllRegs();
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
    public SDT treeQuery(Word<PSymbolInstance> prefix,
    		SymbolicSuffix suffix,
    		WordValuation values,
    		PIV piv,
    		Constants consts,
    		SuffixValuation suffixValues,
    		SDTConstructor oracle) {

    	int pId = values.size() + 1;
    	SuffixValue currentParam = suffix.getSuffixValue(pId);
    	Map<DataValue<T>, SymbolicDataValue> pot = getPotential(prefix, suffixValues, consts);

        Map<DataValue<T>, SDTGuard> equivClasses = generateEquivClasses(prefix, currentParam, pot, consts);
        Map<DataValue<T>, SDTGuard> filteredEquivClasses = filterEquivClasses(equivClasses, prefix, suffix, currentParam, pot, suffixValues, consts, values);

        Map<SDTGuard, SDT> children = new LinkedHashMap<>();
        for (Map.Entry<DataValue<T>, SDTGuard> ec : filteredEquivClasses.entrySet()) {
        	SuffixValuation nextSuffixVals = new SuffixValuation();
        	WordValuation nextVals = new WordValuation();
        	nextVals.putAll(values);
        	nextVals.put(pId, ec.getKey());
        	nextSuffixVals.putAll(suffixValues);
        	nextSuffixVals.put(currentParam, ec.getKey());
        	SDT sdt = oracle.treeQuery(prefix, suffix, nextVals, piv, consts, nextSuffixVals);
        	children.put(ec.getValue(), sdt);
        }

        Collection<DataValue<T>> filteredOut = new ArrayList<>();
        filteredOut.addAll(equivClasses.keySet());
        filteredOut.removeAll(filteredEquivClasses.keySet());
        Map<SDTGuard, SDT> merged = mergeGuards(children, equivClasses, filteredOut);
        piv.putAll(keepMem(merged));

        Map<SDTGuard, SDT> reversed = new LinkedHashMap<>();
        List<SDTGuard> keys = new ArrayList<>(merged.keySet());
        Collections.reverse(keys);
        for (SDTGuard g : keys) {
        	reversed.put(g, merged.get(g));
        }

        return new SDT(reversed);
    }

    private Map<DataValue<T>, SymbolicDataValue> getPotential(Word<PSymbolInstance> prefix,
    		SuffixValuation suffixValues,
    		Constants consts) {
    	Map<DataValue<T>, SymbolicDataValue> pot = new LinkedHashMap<>();
    	RegisterGenerator rgen = new RegisterGenerator();

    	List<DataValue<T>> seen = new ArrayList<>();
    	for (PSymbolInstance psi : prefix) {
    		DataValue<?> dvs[] = psi.getParameterValues();
    		DataType dts[] = psi.getBaseSymbol().getPtypes();
    		for (int i = 0; i < dvs.length; i++) {
    			Register r = rgen.next(dts[i]);
    			DataValue<T> dv = safeCast(dvs[i]);
    			if (dv != null && !seen.contains(dv)) {
    				pot.put(dv, r);
    				seen.add(dv);
    			}
    		}
    	}

    	for (Map.Entry<SuffixValue, DataValue<?>> e : suffixValues.entrySet()) {
    		SuffixValue sv = e.getKey();
    		DataValue<T> dv = safeCast(e.getValue());
    		if (dv != null) {
    			pot.put(dv, sv);
    		}
    	}

    	for (Map.Entry<Constant, DataValue<?>> e : consts.entrySet()) {
    		Constant c = e.getKey();
    		DataValue<T> dv = safeCast(e.getValue());
    		if (dv != null) {
    			pot.put(dv, c);
    		}
    	}

    	return pot;
    }

    protected abstract DataValue<T> safeCast(DataValue<?> val);

//    @Override
//    public SDT treeQuery(
//            Word<PSymbolInstance> prefix,
//            SymbolicSuffix suffix,
//            WordValuation values,
//            PIV piv,
//            Constants constants,
//            SuffixValuation suffixValues,
//            SDTConstructor oracle) {
//
//        int pId = values.size() + 1;
//        List<SymbolicDataValue> regPotential = new ArrayList<>();
//        SuffixValue sv = suffix.getDataValue(pId);
//        DataType type = sv.getType();
//
//        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
//
//        SuffixValue currentParam = new SuffixValue(type, pId);
//
//        Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();
//
//        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
//                constants.<T>values(type),
//                DataWords.<T>valSet(prefix, type),
//                suffixValues.<T>values(type));
//
//        List<DataValue<T>> potList = new ArrayList<>(potSet);
//        List<DataValue<T>> potential = getPotential(potList);
//        // WE ASSUME THE POTENTIAL IS SORTED
//
//        int potSize = potential.size();
//
////        System.out.println("potential " + potential);
//        if (potential.isEmpty()) {
////            System.out.println("empty potential");
//            WordValuation elseValues = new WordValuation();
//            DataValue<T> fresh = getFreshValue(potential);
//            elseValues.putAll(values);
//            elseValues.put(pId, fresh);
//
//            // this is the valuation of the suffixvalues in the suffix
//            SuffixValuation elseSuffixValues = new SuffixValuation();
//            elseSuffixValues.putAll(suffixValues);
//            elseSuffixValues.put(sv, fresh);
//
//            SDT elseOracleSdt = oracle.treeQuery(
//                    prefix, suffix, elseValues, piv,
//                    constants, elseSuffixValues);
//            tempKids.put(new SDTTrueGuard(currentParam), elseOracleSdt);
//        } // process each '<' case
//        else {
//            //Parameter p = new Parameter(
//            //      currentParam.getType(), currentParam.getId());
//
//            // smallest case
//            WordValuation smValues = new WordValuation();
//            smValues.putAll(values);
//            SuffixValuation smSuffixValues = new SuffixValuation();
//            smSuffixValues.putAll(suffixValues);
//
//            Valuation smVal = new Valuation();
//            DataValue<T> dvRight = potential.get(0);
//            IntervalGuard sguard = makeSmallerGuard(
//                    dvRight, prefixValues, currentParam, smValues, piv);
//            SymbolicDataValue rsm = sguard.getRightReg();
////            System.out.println("setting valuation, symDV: " + rsm.toVariable() + " dvright: " + dvRight);
//            smVal.setValue(toVariable(rsm), dvRight.getId());
//            DataValue<T> smcv = instantiate(
//                    sguard, smVal, constants, potential);
//            smValues.put(pId, smcv);
//            smSuffixValues.put(sv, smcv);
//
//            SDT smoracleSdt = oracle.treeQuery(
//                    prefix, suffix, smValues, piv, constants, smSuffixValues);
//
//            tempKids.put(sguard, smoracleSdt);
//
//            // biggest case
//            WordValuation bgValues = new WordValuation();
//            bgValues.putAll(values);
//            SuffixValuation bgSuffixValues = new SuffixValuation();
//            bgSuffixValues.putAll(suffixValues);
//
//            Valuation bgVal = new Valuation();
//
//            DataValue<T> dvLeft = potential.get(potSize - 1);
//            IntervalGuard bguard = makeBiggerGuard(
//                    dvLeft, prefixValues, currentParam, bgValues, piv);
//            SymbolicDataValue rbg = bguard.getLeftReg();
//
//            bgVal.setValue(toVariable(rbg), dvLeft.getId());
//            DataValue<T> bgcv = instantiate(
//                    bguard, bgVal, constants, potential);
//            bgValues.put(pId, bgcv);
//            bgSuffixValues.put(sv, bgcv);
//
//            SDT bgoracleSdt = oracle.treeQuery(
//                    prefix, suffix, bgValues, piv, constants, bgSuffixValues);
//
//            tempKids.put(bguard, bgoracleSdt);
//
//            if (potSize > 1) {        //middle cases
//                for (int i = 1; i < potSize; i++) {
//
//                    WordValuation currentValues = new WordValuation();
//                    currentValues.putAll(values);
//                    SuffixValuation currentSuffixValues = new SuffixValuation();
//                    currentSuffixValues.putAll(suffixValues);
//                    //SDTGuard guard;
//                    Valuation val = new Valuation();
//                    DataValue<T> dvMRight = potential.get(i);
//                    DataValue<T> dvMLeft = potential.get(i - 1);
//
////                    IntervalGuard smallerGuard = makeSmallerGuard(
////                            dvMRight, prefixValues,
////                            currentParam, currentValues, piv);
////                    IntervalGuard biggerGuard = makeBiggerGuard(
////                            dvMLeft, prefixValues, currentParam,
////                            currentValues, piv);
//                    IntervalGuard intervalGuard = makeIntervalGuard(
//                            dvMLeft, dvMRight, prefixValues, currentParam, currentValues, piv);
//
////                    IntervalGuard guard = new IntervalGuard(
////                            currentParam, biggerGuard.getLeftReg(), smallerGuard.getRightReg());
//                    SymbolicDataValue rs = intervalGuard.getRightReg();
//                    SymbolicDataValue rb = intervalGuard.getLeftReg();
//
//                    val.setValue(toVariable(rs), dvMRight.getId());
//                    val.setValue(toVariable(rb), dvMLeft.getId());
//
//                    DataValue<T> cv = instantiate(
//                            intervalGuard, val, constants, potential);
//                    currentValues.put(pId, cv);
//                    currentSuffixValues.put(sv, cv);
//
//                    SDT oracleSdt = oracle.treeQuery(
//                            prefix, suffix, currentValues, piv,
//                            constants, currentSuffixValues);
//
//                    tempKids.put(intervalGuard, oracleSdt);
//                    regPotential.add(i - 1, rb);
//                    regPotential.add(i, rs);
//                }
//            }
////            System.out.println("eq potential is: " + potential);
//            for (DataValue<T> newDv : potential) {
////                LOGGER.trace(newDv.toString());
//
//                // this is the valuation of the suffixvalues in the suffix
//                SuffixValuation ifSuffixValues = new SuffixValuation();
//                ifSuffixValues.putAll(suffixValues);  // copy the suffix valuation
//
//                EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues,
//                        currentParam, values, constants);
////                LOGGER.trace("eqGuard is: " + eqGuard.toString());
//                //construct the equality guard
//                // find the data value in the prefix
//                // this is the valuation of the positions in the suffix
//                WordValuation ifValues = new WordValuation();
//                ifValues.putAll(values);
//                ifValues.put(pId, newDv);
//                SDT eqOracleSdt = oracle.treeQuery(
//                        prefix, suffix, ifValues, piv, constants, ifSuffixValues);
//
//                tempKids.put(eqGuard, eqOracleSdt);
//            }
//
//        }
//
////        System.out.println("TEMPKIDS for " + prefix + " + " + suffix + " = " + tempKids);
//        Map<SDTGuard, SDT> merged = mgGuards(tempKids, currentParam, regPotential);
//        // only keep registers that are referenced by the merged guards
////        System.out.println("MERGED = " + merged);
//        assert !merged.keySet().isEmpty();
//
////        System.out.println("MERGED = " + merged);
//        piv.putAll(keepMem(merged));
//
////        LOGGER.trace("temporary guards = " + tempKids.keySet());
////        LOGGER.trace("merged guards = " + merged.keySet());
////        LOGGER.trace("merged pivs = " + piv.toString());
//
//        tempKids.clear();
//
//        for (SDTGuard g : merged.keySet()) {
//            assert !(g == null);
//            if (g instanceof SDTTrueGuard) {
//                if (merged.keySet().size() != 1) {
//                    throw new IllegalStateException("only one true guard allowed: \n" + prefix + " + " + suffix);
//                }
//                //assert merged.keySet().size() == 1;
//            }
//        }
////        System.out.println("MERGED = " + merged);
//        SDT returnSDT = new SDT(merged);
//        return returnSDT;
//    }
//
//    private EqualityGuard pickupDataValue(DataValue<T> newDv,
//            List<DataValue> prefixValues, SuffixValue currentParam,
//            WordValuation ifValues, Constants constants) {
//        DataType type = currentParam.getType();
//        int newDv_i;
//        for (Map.Entry<SymbolicDataValue.Constant, DataValue<?>> entry : constants.entrySet()) {
//            if (entry.getValue().equals(newDv)) {
//                return new EqualityGuard(currentParam, entry.getKey());
//            }
//        }
//        if (prefixValues.contains(newDv)) {
//            // first index of the data value in the prefixvalues list
//            newDv_i = prefixValues.indexOf(newDv) + 1;
//            Register newDv_r = new Register(type, newDv_i);
//            return new EqualityGuard(currentParam, newDv_r);
//
//        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
//        else {
//            int smallest = Collections.min(ifValues.getAllKeys(newDv));
//            return new EqualityGuard(currentParam, new SuffixValue(type, smallest));
//        }
//    }
//
//    private IntervalGuard makeSmallerGuard(DataValue<T> smallerDv,
//            List<DataValue> prefixValues, SuffixValue currentParam,
//            WordValuation ifValues, PIV pir) {
//        DataType type = currentParam.getType();
//        int newDv_i;
//        if (prefixValues.contains(smallerDv)) {
//            newDv_i = prefixValues.indexOf(smallerDv) + 1;
//            SymbolicDataValue.Parameter newDv_p
//                    = new SymbolicDataValue.Parameter(type, newDv_i);
//            Register newDv_r;
//            if (pir.containsKey(newDv_p)) {
//                newDv_r = pir.get(newDv_p);
//            } else {
//                newDv_r = new Register(type, newDv_i);
//            }
//            return new IntervalGuard(currentParam, null, newDv_r);
//
//        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
//        else {
//            int smallest = Collections.min(ifValues.getAllKeys(smallerDv));
//            IntervalGuard sg = new IntervalGuard(
//                    currentParam, null, new SuffixValue(type, smallest));
//            return sg;
//        }
//    }
//
//    private IntervalGuard makeIntervalGuard(DataValue<T> biggerDv,
//            DataValue<T> smallerDv,
//            List<DataValue> prefixValues, SuffixValue currentParam,
//            WordValuation ifValues, PIV pir) {
//        IntervalGuard smallerGuard = makeSmallerGuard(smallerDv, prefixValues, currentParam, ifValues, pir);
//        IntervalGuard biggerGuard = makeBiggerGuard(biggerDv, prefixValues, currentParam, ifValues, pir);
//        return new IntervalGuard(currentParam, biggerGuard.getLeftReg(), smallerGuard.getRightReg());
//    }
//
//    private IntervalGuard makeBiggerGuard(DataValue<T> biggerDv,
//            List<DataValue> prefixValues, SuffixValue currentParam,
//            WordValuation ifValues, PIV pir) {
//        DataType type = currentParam.getType();
//        int newDv_i;
//        if (prefixValues.contains(biggerDv)) {
//            newDv_i = prefixValues.indexOf(biggerDv) + 1;
//            SymbolicDataValue.Parameter newDv_p
//                    = new SymbolicDataValue.Parameter(type, newDv_i);
//            Register newDv_r;
//            if (pir.containsKey(newDv_p)) {
//                newDv_r = pir.get(newDv_p);
//            } else {
//                newDv_r = new Register(type, newDv_i);
//            }
//            return new IntervalGuard(currentParam, newDv_r, null);
//
//        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
//        else {
//            int smallest = Collections.min(ifValues.getAllKeys(biggerDv));
//            IntervalGuard bg = new IntervalGuard(
//                    currentParam, new SuffixValue(type, smallest), null);
//            return bg;
//        }
//    }

    public abstract List<DataValue<T>> getPotential(List<DataValue<T>> vals);

    private DataValue getRegisterValue(SymbolicDataValue r, PIV piv,
            List<DataValue> prefixValues, Constants constants,
            ParValuation pval) {
        if (r.isRegister()) {
//            LOGGER.trace("piv: " + piv + " " + r.toString() + " " + prefixValues);
            Parameter p = piv.getOneKey((Register) r);
//            LOGGER.trace("p: " + p.toString());
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

//        LOGGER.trace("prefix values : " + prefixValues.toString());

        if (guard instanceof EqualityGuard) {
            EqualityGuard eqGuard = (EqualityGuard) guard;
            SymbolicDataValue ereg = eqGuard.getRegister();
            if (ereg.isRegister()) {
//                LOGGER.trace("piv: " + piv.toString() + " " + ereg.toString() + " " + param.toString());
                Parameter p = piv.getOneKey((Register) ereg);
//                LOGGER.trace("p: " + p.toString());
                int idx = p.getId();
                returnThis = prefixValues.get(idx - 1);
            } else if (ereg.isSuffixValue()) {
                Parameter p = new Parameter(type, ereg.getId());
                returnThis = (DataValue<T>) pval.get(p);
            } else if (ereg.isConstant()) {
                returnThis = (DataValue<T>) constants.get((SymbolicDataValue.Constant) ereg);
            }
        } else if (guard instanceof SDTTrueGuard || guard instanceof DisequalityGuard) {

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
            } else if (guard instanceof SDTAndGuard) {
                assert ((SDTAndGuard) guard).getGuards().stream().allMatch(g -> g instanceof DisequalityGuard);
                SDTGuard aGuard = ((SDTAndGuard) guard).getGuards().get(0);



                returnThis = instantiate(aGuard, val, constants, alreadyUsedValues);
            } else {
                throw new IllegalStateException("only =, != or interval allowed. Got " + guard);
            }

//                        }
//                    } else if (iGuard instanceof SDTIfGuard) {
//                        SymbolicDataValue r = ((SDTIfGuard) iGuard).getRegister();
//                        DataValue<T> regVal = getRegisterValue(r, piv,
//                                prefixValues, constants, pval);
//                        val.setValue(r.toVariable(), regVal);
//                    }
//                }
//            }
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

//    @Override
//    public DataValue instantiate(
//            Word<PSymbolInstance> prefix,
//            ParameterizedSymbol ps, PIV piv,
//            ParValuation pval,
//            Constants constants,
//            SDTGuard guard,
//            Parameter param) {
//
//        DataType type = param.getType();
//
//        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
//
//        LOGGER.trace("prefix values : " + prefixValues.toString());
//
//        if (guard instanceof EqualityGuard) {
//            EqualityGuard eqGuard = (EqualityGuard) guard;
//            SymbolicDataValue ereg = eqGuard.getRegister();
//            DataValue x = getRegisterValue(
//                    ereg, piv, prefixValues, constants, pval);
//            return x;
//        } else if ((guard instanceof SDTTrueGuard)
//                || guard instanceof DisequalityGuard) {
//
//            Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
//                    constants.<T>values(type),
//                    DataWords.<T>valSet(prefix, type),
//                    pval.<T>values(type));
//
//            return this.getFreshValue(new ArrayList<DataValue<T>>(potSet));
//        } else {
//            Collection<DataValue<T>> alreadyUsedValues
//                    = DataWords.<T>joinValsToSet(
//                            constants.<T>values(type),
//                            DataWords.<T>valSet(prefix, type),
//                            pval.<T>values(type));
//            Valuation val = new Valuation();
//            if (guard instanceof SDTIfGuard) {
//                SymbolicDataValue r = (((SDTIfGuard) guard).getRegister());
//                DataValue<T> regVal = getRegisterValue(r, piv,
//                        prefixValues, constants, pval);
//
//                val.setValue(r.toVariable(), regVal);
//                //instantiate(guard, val, param, constants);
//            } else if (guard instanceof SDTMultiGuard) {
//                for (SDTIfGuard ifGuard : ((SDTMultiGuard) guard).getGuards()) {
//                    SymbolicDataValue r = ifGuard.getRegister();
//                    DataValue<T> regVal = getRegisterValue(r, piv,
//                            prefixValues, constants, pval);
//                    val.setValue(r.toVariable(), regVal);
//                }
//            }
//            return instantiate(guard, val, constants, alreadyUsedValues);
//        }
//
//    }

    public void useSuffixOptimization(boolean useSuffixOpt) {
    	this.useSuffixOpt = useSuffixOpt;
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts) {
    	int firstActionArity = suffix.size() > 0 ? suffix.getSymbol(0).getBaseSymbol().getArity() : 0;
    	if (suffixValue.getId() <= firstActionArity) {
    		return new UnrestrictedSuffixValue(suffixValue);
    	}

    	DataValue<?> prefixVals[] = DataWords.valsOf(prefix);
    	DataValue<?> suffixVals[] = DataWords.valsOf(suffix);
    	DataValue<?> constVals[] = new DataValue<?>[consts.size()];
    	constVals = consts.values().toArray(constVals);
    	DataValue<?> priorVals[] = new DataValue<?>[prefixVals.length + constVals.length + suffixValue.getId() - 1];
    	DataType svType = suffixValue.getType();
    	DataValue<T> svDataValue = safeCast(suffixVals[suffixValue.getId()-1]);
    	assert svDataValue != null;

    	System.arraycopy(prefixVals, 0, priorVals, 0, prefixVals.length);
    	System.arraycopy(constVals, 0, priorVals, prefixVals.length, constVals.length);
    	System.arraycopy(suffixVals, 0, priorVals, prefixVals.length + constVals.length, suffixValue.getId() - 1);

    	// is suffix value greater than all prior or smaller than all prior?
    	Comparator<DataValue<T>> comparator = getComparator();
    	boolean greater = false;
    	boolean lesser = false;
    	boolean foundFirst = false;
    	for (int i = 0; i < priorVals.length; i++) {
    		if (priorVals[i].getType().equals(svType)) {
    			DataValue<T> dv = safeCast(priorVals[i]);
    			assert dv != null;
    			int comparison = comparator.compare(svDataValue, dv);

    			if (foundFirst) {
    				if ((greater && comparison < 0) ||
    						(lesser && comparison > 0) ||
    						comparison == 0) {
    					return new UnrestrictedSuffixValue(suffixValue);
    				}
    			} else {
    				if (comparison > 0) {
    					greater = true;
    				} else if (comparison < 0) {
    					lesser = true;
    				} else {
    					return new UnrestrictedSuffixValue(suffixValue);
    				}
    				foundFirst = true;
    			}
    		}
    	}

    	if (!foundFirst) {
    		return new GreaterSuffixValue(suffixValue);
    	}

    	assert (greater && !lesser) || (!greater && lesser);
    	return greater ? new GreaterSuffixValue(suffixValue) : new LesserSuffixValue(suffixValue);
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
    	SuffixValue sv = guard.getParameter();

    	if (guard instanceof IntervalGuard) {
    		IntervalGuard ig = (IntervalGuard) guard;
    		if (ig.isBiggerGuard()) {
    			return new GreaterSuffixValue(sv);
    		} else if (ig.isSmallerGuard()) {
    			return new LesserSuffixValue(sv);
    		}
    	}
    	SuffixValueRestriction restr = SuffixValueRestriction.genericRestriction(guard, prior);
    	if (restr instanceof FreshSuffixValue) {
    		restr = new GreaterSuffixValue(sv);
    	}
    	return restr;
    }

    @Override
    public boolean guardRevealsRegister(SDTGuard guard, SymbolicDataValue register) {
    	if (guard instanceof EqualityGuard && ((EqualityGuard) guard).getRegister().equals(register)) {
    		return true;
    	} else if (guard instanceof DisequalityGuard && ((DisequalityGuard)guard).getRegister().equals(register)) {
    		return true;
    	} else if (guard instanceof IntervalGuard) {
    		IntervalGuard ig = (IntervalGuard) guard;
    		if (ig.getLeftReg().equals(register) || ig.getRightReg().equals(register)) {
    			return true;
    		}
    	} else if (guard instanceof SDTMultiGuard) {
    		boolean revealsGuard = false;
    		for (SDTGuard g : ((SDTMultiGuard)guard).getGuards()) {
    			revealsGuard = revealsGuard || this.guardRevealsRegister(g, register);
    		}
    		return revealsGuard;
    	}
    	return false;
    }
}
