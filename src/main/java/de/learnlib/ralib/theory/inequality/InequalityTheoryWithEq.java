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
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author Sofia Cassel
 * @param<T>
 */
public abstract class InequalityTheoryWithEq<T> implements Theory<T> {

    private static final LearnLogger log
            = LearnLogger.getLogger(InequalityTheoryWithEq.class);

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
    private Set<SDTGuard> mergeWithOthers(SDTGuard target, SDT targetSDT,
            Set<SDTGuard> guards, Map<SDTGuard, SDT> reference,
            List<SDTGuard> tried) {
        // check if the target guard is equivalent to the guard in the list.
        for (SDTGuard guard : guards) {
            if (!(tried.contains(guard))) {
                // if they are:
                SDT otherSDT = reference.get(guard);
                if (targetSDT.canUse(otherSDT) && otherSDT.canUse(targetSDT)) {
                    tried.add(guard);
                    // let newTarget be the new target guard
                    Set<SDTGuard> newTargetSet = target.mergeWith(guard);
                    SDTGuard newTarget = new SDTOrGuard(target.getParameter(), newTargetSet.toArray(new SDTGuard[]{}));
                    SDTGuard nextTarget = newTarget;
//                    System.out.println("merging " + target + " + " + guard + " into " + newTargetSet);
                    if (newTarget.isSingle()) {
                        nextTarget = newTarget.getSingle();
                    }
                    else if (newTargetSet.isEmpty()) {
                        nextTarget = new SDTTrueGuard(target.getParameter());
                    }
                    return mergeWithOthers(nextTarget, targetSDT, guards,
                            reference, tried);
                    
                }
            }
        }
        Set<SDTGuard> targetList = new LinkedHashSet<>();
        targetList.add(target);
        targetList.addAll(tried);
        return targetList;
    
    }

            //    private List<SDTGuard> mergeWithMany(SDTGuard target, SDT targetSDT,
    //            List<SDTGuard> guards, Map<SDTGuard, SDT> reference,
    //            List<SDTGuard> tried) {
    //        // check if the target guard is equivalent to the guard in the list.
    //        for (SDTGuard guard : guards) {
    //            if (!(tried.contains(guard))) {
    //                // if they are:
    //                SDT otherSDT = reference.get(guard);
    //                if (targetSDT.canUse(otherSDT) && otherSDT.canUse(targetSDT)) {
    //                    tried.add(guard);
    //                    // let newTarget be the new target guard
    //                    SDTGuard newTarget = mergeGuardLists(target, guard);
    //                    return mergeWithMany(newTarget, targetSDT, guards,
    //                            reference, tried);
    //                }
    //            }
    //        }
    //        List<SDTGuard> targetList = new ArrayList<>();
    //        targetList.add(target);
    //        targetList.addAll(tried);
    //        return targetList;
    //    }
            // given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT.  
    private Map<SDTGuard, SDT>
            mergeGuards(Map<SDTGuard, SDT> unmerged, SuffixValue currentParam) {
        if (unmerged.isEmpty()) {
            return unmerged;
        }
        Map<SDTGuard, SDT> tempMap = new LinkedHashMap();
        Map<SDTGuard, SDT> finalMap = new LinkedHashMap();
        // list of unmerged guards
        Set<SDTGuard> guardList = unmerged.keySet();
        List<SDTGuard> consumedGuards = new ArrayList<>();

        // for each guard in the list
        for (SDTGuard guard : guardList) {
            if (!consumedGuards.contains(guard)) {
                consumedGuards.add(guard);
                SDT guardSDT = unmerged.get(guard);
                Set<SDTGuard> mergedGuardSet = mergeWithOthers(guard, guardSDT,
                        guardList, unmerged, consumedGuards);
                List<SDTGuard> mergedGuardList = new ArrayList<>(mergedGuardSet);
                tempMap.put(mergedGuardList.get(0), guardSDT);
            }
        }
//        System.out.println("tempMap: " + tempMap);
        // expand or guards before adding to the map
        for (Entry<SDTGuard, SDT> e : tempMap.entrySet()) {
            SDTGuard guard = e.getKey();
            SDT sdt = e.getValue();
            if (guard instanceof SDTOrGuard) {
                for (SDTGuard subguard : ((SDTOrGuard) guard).getGuards()) {
                    finalMap.put(subguard, sdt);
                }
            } else {
                finalMap.put(guard, sdt);
            }
        }
        assert !finalMap.isEmpty();
        return finalMap;
    }

    // given a set of registers and a set of guards, keep only the registers
    // that are mentioned in any guard
    private PIV keepMem(Set<SDTGuard> guardSet) {
        PIV ret = new PIV();

        // 1. create guard list
        Set<SDTGuard> ifGuards = new LinkedHashSet<>();
        for (SDTGuard g : guardSet) {
            if (g instanceof SDTIfGuard) {
                ifGuards.add((SDTIfGuard) g);
            } else if (g instanceof SDTOrGuard) {
                ifGuards.addAll(((SDTOrGuard) g).getGuards());
            }
        }

        // 2. determine which registers to keep
        Set<SymbolicDataValue> tempRegs = new LinkedHashSet<>();
        for (SDTGuard g : ifGuards) {
            if (g instanceof SDTAndGuard) {
                tempRegs.addAll(((SDTAndGuard) g).getAllRegs());
            } else if (g instanceof SDTIfGuard) {
                tempRegs.add(((SDTIfGuard) g).getRegister());
            }
        }
        for (SymbolicDataValue r : tempRegs) {
            Parameter p = new Parameter(r.getType(), r.getId());
            if (r instanceof Register) {
                ret.put(p, (Register) r);
            }
        }
        return ret;
    }

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

        SuffixValue currentParam = new SuffixValue(type, pId);

        Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

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
                    prefix, suffix, elseValues, piv,
                    constants, elseSuffixValues);
            tempKids.put(new SDTTrueGuard(currentParam), elseOracleSdt);
        } // process each '<' case
        else {
            Parameter p = new Parameter(
                    currentParam.getType(), currentParam.getId());

            // smallest case
            WordValuation smValues = new WordValuation();
            smValues.putAll(values);
            SuffixValuation smSuffixValues = new SuffixValuation();
            smSuffixValues.putAll(suffixValues);

            Valuation smVal = new Valuation();
            DataValue<T> dvRight = potential.get(0);
            SmallerGuard sguard = makeSmallerGuard(
                    dvRight, prefixValues, currentParam, smValues, piv);
            SymbolicDataValue rsm = sguard.getRegister();

            smVal.setValue(rsm.toVariable(), dvRight);
            DataValue<T> smcv = instantiate(
                    sguard, smVal, constants, potential);
            smValues.put(pId, smcv);
            smSuffixValues.put(sv, smcv);

            SDT smoracleSdt = oracle.treeQuery(
                    prefix, suffix, smValues, piv, constants, smSuffixValues);

            tempKids.put(sguard, smoracleSdt);

            // biggest case
            WordValuation bgValues = new WordValuation();
            bgValues.putAll(values);
            SuffixValuation bgSuffixValues = new SuffixValuation();
            bgSuffixValues.putAll(suffixValues);

            Valuation bgVal = new Valuation();

            DataValue<T> dvLeft = potential.get(potSize - 1);
            BiggerGuard bguard = makeBiggerGuard(
                    dvLeft, prefixValues, currentParam, bgValues, piv);
            SymbolicDataValue rbg = bguard.getRegister();

            bgVal.setValue(rbg.toVariable(), dvLeft);
            DataValue<T> bgcv = instantiate(
                    bguard, bgVal, constants, potential);
            bgValues.put(pId, bgcv);
            bgSuffixValues.put(sv, bgcv);

            SDT bgoracleSdt = oracle.treeQuery(
                    prefix, suffix, bgValues, piv, constants, bgSuffixValues);

            tempKids.put(bguard, bgoracleSdt);

            if (potSize > 1) {        //middle cases
                for (int i = 1; i < potSize; i++) {

                    WordValuation currentValues = new WordValuation();
                    currentValues.putAll(values);
                    SuffixValuation currentSuffixValues = new SuffixValuation();
                    currentSuffixValues.putAll(suffixValues);
                    SDTGuard guard;
                    Valuation val = new Valuation();
                    DataValue<T> dvMRight = potential.get(i);
                    DataValue<T> dvMLeft = potential.get(i - 1);

                    SmallerGuard smallerGuard = makeSmallerGuard(
                            dvMRight, prefixValues,
                            currentParam, currentValues, piv);
                    BiggerGuard biggerGuard = makeBiggerGuard(
                            dvMLeft, prefixValues, currentParam,
                            currentValues, piv);
                    guard = new SDTAndGuard(
                            currentParam, smallerGuard, biggerGuard);
                    SymbolicDataValue rs = smallerGuard.getRegister();
                    SymbolicDataValue rb = biggerGuard.getRegister();

                    val.setValue(rs.toVariable(), dvMRight);
                    val.setValue(rb.toVariable(), dvMLeft);

                    DataValue<T> cv = instantiate(
                            guard, val, constants, potential);
                    currentValues.put(pId, cv);
                    currentSuffixValues.put(sv, cv);

                    SDT oracleSdt = oracle.treeQuery(
                            prefix, suffix, currentValues, piv,
                            constants, currentSuffixValues);

                    tempKids.put(guard, oracleSdt);

                }
            }

            for (DataValue<T> newDv : potential) {

                // this is the valuation of the suffixvalues in the suffix
                SuffixValuation ifSuffixValues = new SuffixValuation();
                ifSuffixValues.putAll(suffixValues);  // copy the suffix valuation

                EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues,
                        currentParam, values, constants);
                WordValuation ifValues = new WordValuation();
                ifValues.putAll(values);
                ifValues.put(pId, newDv);
                SDT eqOracleSdt = oracle.treeQuery(
                        prefix, suffix, ifValues, piv,
                        constants, ifSuffixValues);

                tempKids.put(eqGuard, eqOracleSdt);
            }

        }

        Map<SDTGuard, SDT> merged = mergeGuards(tempKids, currentParam);
        // only keep registers that are referenced by the merged guards
        piv.putAll(keepMem(merged.keySet()));

        // clear the temporary map of children
        tempKids.clear();

        return new SDT(merged);

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

    private SmallerGuard makeSmallerGuard(DataValue<T> smallerDv,
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
            return new SmallerGuard(currentParam, newDv_r);

        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
        else {
            int smallest = Collections.min(ifValues.getAllKeys(smallerDv));
            SmallerGuard sg = new SmallerGuard(
                    currentParam, new SuffixValue(type, smallest));
            return sg;
        }
    }

    private BiggerGuard makeBiggerGuard(DataValue<T> biggerDv,
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
            return new BiggerGuard(currentParam, newDv_r);

        } // if the data value isn't in the prefix, it is somewhere earlier in the suffix
        else {
            int smallest = Collections.min(ifValues.getAllKeys(biggerDv));
            BiggerGuard bg = new BiggerGuard(
                    currentParam, new SuffixValue(type, smallest));
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

    public abstract DataValue instantiate(SDTGuard guard, Valuation val,
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

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        log.log(Level.FINEST, "prefix values : " + prefixValues.toString());

        if (guard instanceof EqualityGuard) {
            EqualityGuard eqGuard = (EqualityGuard) guard;
            SymbolicDataValue ereg = eqGuard.getRegister();
            DataValue x = getRegisterValue(
                    ereg, piv, prefixValues, constants, pval);
            return x;
        } else if (guard instanceof SDTTrueGuard) {

            Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                    constants.<T>values(type),
                    DataWords.<T>valSet(prefix, type),
                    pval.<T>values(type));

            return this.getFreshValue(new ArrayList<DataValue<T>>(potSet));
        } else {
            Collection<DataValue<T>> alreadyUsedValues
                    = DataWords.<T>joinValsToSet(
                            constants.<T>values(type),
                            DataWords.<T>valSet(prefix, type),
                            pval.<T>values(type));
            Valuation val = new Valuation();
            if (guard instanceof SDTIfGuard) {
                SymbolicDataValue r = (((SDTIfGuard) guard).getRegister());
                DataValue<T> regVal = getRegisterValue(r, piv,
                        prefixValues, constants, pval);

                val.setValue(r.toVariable(), regVal);
                //instantiate(guard, val, param, constants);
            } else if (guard instanceof SDTMultiGuard) {
                for (SDTGuard iGuard : ((SDTMultiGuard) guard).getGuards()) {
                    if (iGuard instanceof SDTMultiGuard) {
                        for (SymbolicDataValue r : ((SDTMultiGuard)iGuard).getAllRegs()) {
                            DataValue<T> rVal = getRegisterValue(r, piv, prefixValues, constants, pval);
                            val.setValue(r.toVariable(), rVal);
                        }
                    }
                    else if (iGuard instanceof SDTIfGuard) {
                        SymbolicDataValue r = ((SDTIfGuard)iGuard).getRegister();
                        DataValue<T> regVal = getRegisterValue(r, piv,
                            prefixValues, constants, pval);
                        val.setValue(r.toVariable(), regVal);
                }
            }
        }

        if (!(oldDvs.isEmpty())) {
            for (DataValue<T> oldDv : oldDvs) {
                Valuation newVal = new Valuation();
                newVal.putAll(val);
                newVal.setValue(new SuffixValue(param.getType(), param.getId()).toVariable(), oldDv);
//                    System.out.println("instantiating " + guard + " with " + newVal);
                DataValue inst = instantiate(guard, newVal, constants, alreadyUsedValues);
                if (inst != null) {
//                        System.out.println("returning (reused): " + inst);
                    return inst;
                }
            }
        }
        DataValue ret = instantiate(guard, val, constants, alreadyUsedValues);
//            System.out.println("returning (no reuse): " + ret);
        return ret;
    }

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
//        log.log(Level.FINEST, "prefix values : " + prefixValues.toString());
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
}
