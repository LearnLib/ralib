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
package de.learnlib.ralib.theory.equality;

import de.learnlib.logging.LearnLogger;
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
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author falk and sofia
 * @param <T>
 */
public abstract class EqualityTheoryMS<T> implements Theory<T> {

    protected boolean useNonFreeOptimization;

    private static final LearnLogger log = LearnLogger.getLogger(EqualityTheoryMS.class);

    public EqualityTheoryMS(boolean useNonFreeOptimization) {
        this.useNonFreeOptimization = useNonFreeOptimization;
    }

    public EqualityTheoryMS() {
        this(false);
    }

    public List<DataValue<T>> getPotential(List<DataValue<T>> vals) {
        return vals;
    }
    
    private Set<SDTGuard> openGuardLists(Set<SDTGuard> gs) {
        Set<SDTGuard> retSet = new LinkedHashSet();
        for (SDTGuard g : gs) {
            if (g instanceof SDTMultiGuard) {
                retSet.addAll(((SDTMultiGuard)g).getGuards());
            }
            else {
                retSet.add(g);
            }
        }
        return retSet;
    }

    private VarMapping makeVarMapping(Set<SDTGuard> _gSet1, Set<SDTGuard> _gSet2) {
        VarMapping vars = new VarMapping();
        SDTIfGuard ifg1;
        SymbolicDataValue r1;
        Set<SDTGuard> gSet1 = openGuardLists(_gSet1);
        Set<SDTGuard> gSet2 = openGuardLists(_gSet2);
        
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

    private Set<SDTGuard> setify(SDTGuard... gs) {
        Set<SDTGuard> guardSet = new LinkedHashSet<>();
        for (SDTGuard g : gs) {
//            System.out.println(g);
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
//                System.out.println("if");
                SDTIfGuard x = (SDTIfGuard) g;
                if (guardSet.contains(x.toDeqGuard())) {
                    guardSet.remove(x.toDeqGuard());
//                    if (!((g instanceof EqualityGuard) || (g instanceof DisequalityGuard))) {
//                        guardSet.add(new DisequalityGuard(x.getParameter(), x.getRegister()));
//                    }
                } else {
                    guardSet.add(x);
                }
            } else if (g instanceof SDTAndGuard) {
//                System.out.println("and");
                SDTAndGuard ag = (SDTAndGuard) g;
                List<SDTIfGuard> ifs = new ArrayList();
                for (SDTIfGuard x : ag.getGuards()) {
                    if (guardSet.contains(x.toDeqGuard())) {
                        guardSet.remove(x.toDeqGuard());
//                        if (!((g instanceof EqualityGuard) || (g instanceof DisequalityGuard))) {
//                            guardSet.add(new DisequalityGuard(x.getParameter(), x.getRegister()));
//                        }
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
//            System.out.println("curr guard set: " + guardSet);
        }
        return guardSet;
    }

    private SDTGuard[] toGuardArray(Set<SDTGuard> guards) {
        Set<SDTGuard> newGuards = new LinkedHashSet<>();
        for (SDTGuard g : guards) {
            if (g instanceof SDTMultiGuard) {
                SDTMultiGuard ag = (SDTMultiGuard) g;
                List<SDTIfGuard> agList = ag.getGuards();
                if (agList.isEmpty()) {
                    newGuards.add(new SDTTrueGuard(ag.getParameter()));
                } else if (agList.size() == 1) {
                    newGuards.add(agList.get(0));
                } else {
                    newGuards.add(g);
                }
            } else {
                newGuards.add(g);
            }
        }
        return newGuards.toArray(new SDTGuard[]{});

    }

    private SDTGuard mergeGuardLists(SDTGuard g1, SDTGuard g2) {
//        System.out.println("g1 = " + g1 + ", g2 = " + g2);
        Set<SDTGuard> guardSet = (setify(g1, g2));
        if (guardSet.isEmpty()) {
            return new SDTTrueGuard(g1.getParameter());
        } else {
            SDTGuard[] guardArray = toGuardArray(guardSet);
//            System.out.println("guard array: " + Arrays.toString(guardArray));
            if (guardArray.length == 1) {
                return guardArray[0];
            } else {
                return new SDTOrGuard(g1.getParameter(), guardSet.toArray(new SDTIfGuard[]{}));
            }
        }
    }
    
    private Set<SDTIfGuard> makeGuardSet(SDTGuard target) {
        Set<SDTIfGuard> ds = new LinkedHashSet();
                if (target instanceof SDTMultiGuard) {
                    List<SDTIfGuard> tSubs = ((SDTMultiGuard)target).getGuards();
                    if (target instanceof SDTOrGuard) {
                        ds.add(tSubs.get(0));
                    }
                    else {
                        ds.addAll(tSubs);
                    }
                }
                else if (target instanceof SDTIfGuard) {
                    
                    ds.add((SDTIfGuard)target);
                }
                return ds;
    }

    private List<SDTGuard> mergeWithMany(SDTGuard target, SDT targetSDT, List<SDTGuard> guards, Map<SDTGuard, SDT> reference, List<SDTGuard> tried) {
        // check if the target guard is equivalent to the guard in the list.
        for (SDTGuard guard : guards) {
            if (!(tried.contains(guard))) {
                // if they are:
                System.out.println("merging " + target.toString() + " with " + guard.toString());
                SDT otherSDT = reference.get(guard);
//                System.out.println("merging: can " + targetSDT.toString() + " use " + otherSDT.toString() + " ??");
                
               // if (target instanceof SDTMultiGuard && ((SDTMultiGuard) target).isSingle()) {
               //     target = ((SDTMultiGuard) target).getSingle();
               // }
                // if we are dealing with an equality guard somewhere
                
                
                VarMapping vars2 = makeVarMapping(targetSDT.getGuards(), otherSDT.getGuards());
                VarMapping vars1 = makeVarMapping(otherSDT.getGuards(), targetSDT.getGuards());
                
                Set<SDTIfGuard> preEq = new LinkedHashSet();
                if (target instanceof EqualityGuard && guard instanceof DisequalityGuard) {
                    preEq = makeGuardSet(target);
                }
                else if (guard instanceof EqualityGuard && target instanceof DisequalityGuard) {
                    preEq = makeGuardSet(guard); 
                }
                
//                System.out.println("under " + vars1 + " or " + vars2);
                if (targetSDT.isLooselyEquivalent(otherSDT, vars1, makeGuardSet(target)) ||
                        otherSDT.isLooselyEquivalent(targetSDT, vars2, makeGuardSet(guard))) {
                    
                        tried.add(guard);
                    System.out.println("yes!!");
                        // let newTarget be the new target guard
                        SDTGuard newTarget = mergeGuardLists(target, guard);
//                        System.out.println("target guard: " + newTarget);
                        return mergeWithMany(newTarget, targetSDT, guards, reference, tried);
                    }
                
                }
            }
            List<SDTGuard> targetList = new ArrayList<>();
            targetList.add(target);
            targetList.addAll(tried);
            return targetList;
        }
    
    

    private Map<SDTGuard, SDT> expandGuards(Map<SDTGuard, SDT> tempMap) {
        Map<SDTGuard, SDT> finalMap = new LinkedHashMap();
        for (Map.Entry<SDTGuard, SDT> e : tempMap.entrySet()) {
            SDTGuard guard = e.getKey();
            SDT sdt = e.getValue();
            if (guard instanceof SDTOrGuard) {
                for (SDTIfGuard subguard : ((SDTOrGuard) guard).getGuards()) {
                    finalMap.put(subguard, sdt);
                }
            } else if (guard instanceof SDTMultiGuard) {
                SDTMultiGuard ag = (SDTMultiGuard) guard;
                // ugly hack!
                if (ag.isEmpty()) {
                    finalMap.put(ag.asTrueGuard(),sdt);
                }
                else if (ag.isSingle()) {
                    finalMap.put(ag.getSingle(), sdt);
                } else {
                    finalMap.put(guard, sdt);
                }
            } else {
                finalMap.put(guard, sdt);
            }
        }
        return finalMap;
    }

    private Map<SDTGuard, SDT>
            mergeGuards(Map<SDTGuard, SDT> unmerged) {
        if (unmerged.isEmpty()) {
            return unmerged;
        }
//        log.log(Level.FINEST, "master merge...");
        Map<SDTGuard, SDT> tempMap = new LinkedHashMap();
        //Map<SDTGuard, SDT> finalMap = new LinkedHashMap();
        // list of unmerged guards
        List<SDTGuard> guardList = new ArrayList(unmerged.keySet());
        List<SDTGuard> consumedGuards = new ArrayList<>();

        // for each guard in the list
        for (SDTGuard guard : guardList) {
            if (!consumedGuards.contains(guard)) {
                consumedGuards.add(guard);
                SDT guardSDT = unmerged.get(guard);
//                System.out.println("merging " + guard.toString() + " with " + guardList.toString());
                List<SDTGuard> mergedGuardList = mergeWithMany(guard, guardSDT, guardList, unmerged, consumedGuards);
                //consumedGuards.addAll(mergedGuardList);
//                System.out.println("returned " + mergedGuardList.get(0).toString());
                tempMap.put(mergedGuardList.get(0), guardSDT);
            }
        }
        //        log.log(Level.FINEST, "unmerged: " + unmerged.toString());
        //int i = 0;

        // expand or guards before adding to the map
        //tempMap.putAll(gMerged);
        return expandGuards(tempMap);
    }

    // given a set of registers and a set of guards, keep only the registers
    // that are mentioned in any guard
    private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
        PIV ret = new PIV();
        for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
            SDTGuard mg = e.getKey();
            if (mg instanceof EqualityGuard) {
                log.log(Level.FINEST, mg.toString());
                //for (Register k : mg.getRegisters()) {
                SymbolicDataValue r = ((EqualityGuard) mg).getRegister();
                Parameter p = new Parameter(r.getType(), r.getId());
                if (r instanceof Register) {
                    ret.put(p, (Register) r);
                }
                //else {
                //    assert r instanceof SuffixValue;
                //    Register new_r = new Register(r.getType(),r.getId()+prefixValues.size());
                //    ret.put(p, new_r);
                //}
            }
        }
        return ret;
    }

    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix,
            WordValuation values,
            PIV pir,
            Constants constants,
            SuffixValuation suffixValues,
            SDTConstructor oracle) {

        int pId = values.size() + 1;

        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();

        SuffixValue currentParam = new SuffixValue(type, pId);

        Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

        // store temporary values here
//        PIV paramsToRegs = new PIV();
//        for (Map.Entry<Parameter, Register> e : pir) {
//            paramsToRegs.add(e.getKey(), e.getValue());
//        }
        //ParsInVars ifPiv = new ParsInVars();
        //ifPiv.putAll(piv);
        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                constants.<T>values(type),
                DataWords.<T>valSet(prefix, type),
                suffixValues.<T>values(type));

        List<DataValue<T>> potList = new ArrayList<>(potSet);
        List<DataValue<T>> potential = getPotential(potList);

//        List<DataValue<T>> potential = getPotential(
//                DataWords.<T>joinValsToSet(
//                    DataWords.<T>valSet(prefix, type),
//                    suffixValues.<T>values(type)));
//      
        boolean free = suffix.getFreeValues().contains(sv);
        if (!free && useNonFreeOptimization) {
            DataValue d = suffixValues.get(sv);
            if (d == null) {
                d = getFreshValue(potential);
            }
            values.put(pId, d);
            WordValuation trueValues = new WordValuation();
            trueValues.putAll(values);
            SuffixValuation trueSuffixValues = new SuffixValuation();
            trueSuffixValues.putAll(suffixValues);
            trueSuffixValues.put(sv, d);
            SDT sdt = oracle.treeQuery(
                    prefix, suffix, trueValues, pir, constants, trueSuffixValues);
            tempKids.put(new SDTTrueGuard(currentParam), sdt);
            log.log(Level.FINEST, " single deq SDT : " + sdt.toString());

            Map<SDTGuard, SDT> merged = mergeGuards(tempKids);

            log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
            //log.log(Level.FINEST,"temporary pivs = " + tempPiv.keySet());
            log.log(Level.FINEST, "merged guards = " + merged.keySet());
            log.log(Level.FINEST, "merged pivs = " + pir.toString());

            return new SDT(merged);
        }

        log.log(Level.FINEST, "potential " + potential.toString());

        // process each 'if' case
        // prepare by picking up the prefix values
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        log.log(Level.FINEST, "prefix list    " + prefixValues.toString());

        DataValue fresh = getFreshValue(potential);

        List<DisequalityGuard> diseqList = new ArrayList<DisequalityGuard>();
        for (DataValue<T> newDv : potential) {
            log.log(Level.FINEST, newDv.toString());

            // this is the valuation of the suffixvalues in the suffix
            SuffixValuation ifSuffixValues = new SuffixValuation();
            ifSuffixValues.putAll(suffixValues);  // copy the suffix valuation
            //ifSuffixValues.put(sv, newDv);

            EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues, currentParam, values, pir, constants);
            log.log(Level.FINEST, "eqGuard is: " + eqGuard.toString());
            diseqList.add(new DisequalityGuard(currentParam, eqGuard.getRegister()));
            //log.log(Level.FINEST,"this is the piv: " + ifPiv.toString() + " and newDv " + newDv.toString());
            //construct the equality guard
            // find the data value in the prefix
            // this is the valuation of the positions in the suffix
            WordValuation ifValues = new WordValuation();
            ifValues.putAll(values);
            ifValues.put(pId, newDv);
            SDT eqOracleSdt = oracle.treeQuery(
                    prefix, suffix, ifValues, pir, constants, ifSuffixValues);

            tempKids.put(eqGuard, eqOracleSdt);
        }

        // process the 'else' case
        // this is the valuation of the positions in the suffix
        WordValuation elseValues = new WordValuation();
        elseValues.putAll(values);
        elseValues.put(pId, fresh);

        // this is the valuation of the suffixvalues in the suffix
        SuffixValuation elseSuffixValues = new SuffixValuation();
        elseSuffixValues.putAll(suffixValues);
        elseSuffixValues.put(sv, fresh);

        SDT elseOracleSdt = oracle.treeQuery(
                prefix, suffix, elseValues, pir, constants, elseSuffixValues);

//        ParsInVars diseqPiv = new ParsInVars();
//        for (Register rg : ifPiv.keySet()) {
//            DataValue tdv = ifPiv.get(rg);
//            if (tdv.getType() == type) {
//                diseqPiv.put(rg, tdv);
//            }
//        }
//        
        SDTAndGuard deqGuard = new SDTAndGuard(currentParam, (diseqList.toArray(new DisequalityGuard[]{})));
        log.log(Level.FINEST, "diseq guard = " + deqGuard.toString());
        // tempKids is the temporary SDT (sort of)
        //tempKids.put(deqGuard, elseOracleSdt);

        tempKids.put(deqGuard, elseOracleSdt);

        // merge the guards
        Map<SDTGuard, SDT> merged = mergeGuards(tempKids);

        // only keep registers that are referenced by the merged guards
        pir.putAll(keepMem(merged));

        log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
        //log.log(Level.FINEST,"temporary pivs = " + tempPiv.keySet());
        log.log(Level.FINEST, "merged guards = " + merged.keySet());
        log.log(Level.FINEST, "merged pivs = " + pir.toString());

        // clear the temporary map of children
        tempKids.clear();

        for (SDTGuard g : merged.keySet()) {
//            assert !(g instanceof SDTAndGuard);
            assert !(g == null);
        }

        SDT returnSDT = new SDT(merged);
        // this keeps only the REGISTERS
//        Set<Register> regs = returnSDT.getRegisters();
//        for (Map.Entry<Parameter, Register> e : pir.entrySet()) {
//            if (regs.contains(e.getValue())) {
//                pir.put(e.getKey(), e.getValue());
//            }
//        }
        //System.out.println("P::  " + prefix.toString() + "    S::  " + suffix.toString() + "    RETURN SDT::::  " + returnSDT.toString());
        return returnSDT;

    }

    private EqualityGuard pickupDataValue(DataValue<T> newDv, List<DataValue> prefixValues, SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
        DataType type = currentParam.getType();
        int newDv_i;
        for (Constant c : constants.keySet()) {
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

    @Override
    public DataValue instantiate(
            Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv,
            ParValuation pval,
            Constants constants,
            SDTGuard guard,
            Parameter param
    ) {

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        log.log(Level.FINEST, "prefix values : " + prefixValues.toString());
        DataType type = param.getType();

        if (guard instanceof EqualityGuard) {
            log.log(Level.FINEST, "equality guard " + guard.toString());
            // 
            //if (pval.containsKey(param)) {
            //    log.log(Level.FINEST,"pval = " + pval.toString());
            //    log.log(Level.FINEST,"pval says " + pval.get(param).toString());
            //    return pval.get(param);
            //} else {
            //log.log(Level.FINEST,"piv = " + piv.toString());
            //log.log(Level.FINEST,"piv says " + piv.get((Parameter)param).toString());
            EqualityGuard eqGuard = (EqualityGuard) guard;
            SymbolicDataValue ereg = eqGuard.getRegister();
            if (ereg.isRegister()) {
                log.log(Level.FINEST, "piv: " + piv.toString() + " " + ereg.toString() + " " + param.toString());
                Parameter p = piv.getOneKey((Register) ereg);
                log.log(Level.FINEST, "p: " + p.toString());
                int idx = p.getId();
                //return piv.get(param);
                // trying to not pickup values from prefix
                return prefixValues.get(idx - 1);
            } else if (ereg.isSuffixValue()) {
                Parameter p = new Parameter(type, ereg.getId());
                return pval.get(p);
            } else if (ereg.isConstant()) {
                return constants.get((Constant) ereg);
            }
            //}
        }

//        log.log(Level.FINEST,"base case");
//        Collection potSet = DataWords.<T>joinValsToSet(
//                DataWords.<T>valSet(prefix, type),
//                pval.<T>values(type));
        Collection potSet = DataWords.<T>joinValsToSet(
                constants.<T>values(type),
                DataWords.<T>valSet(prefix, type),
                pval.<T>values(type));

        if (!potSet.isEmpty()) {
            log.log(Level.FINEST, "potSet = " + potSet.toString());
        } else {
            log.log(Level.FINEST, "potSet is empty");
        }
        DataValue fresh = this.getFreshValue(new ArrayList<DataValue<T>>(potSet));
        log.log(Level.FINEST, "fresh = " + fresh.toString());
        return fresh;

    }

}
