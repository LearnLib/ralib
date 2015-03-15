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
import de.learnlib.ralib.theory.SDTCompoundGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
public abstract class EqualityTheory<T> implements Theory<T> {

    private static final LearnLogger log = LearnLogger.getLogger(EqualityTheory.class);

    //@Override
//    public List<DataValue<T>> getPotential(
//            Collection<DataValue<T>> vals) {        
//        Set<DataValue<T>> set = new LinkedHashSet<>(vals);
//        return new ArrayList<>(set);
//    }
    public List<DataValue<T>> getPotential(List<DataValue<T>> vals) {
        return vals;
    }

// given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT.  Base case: always add the 'else' guard first.
    private Map<SDTGuard, SDT>
            mergeGuards(Map<EqualityGuard, SDT> eqs, SDTCompoundGuard deqGuard, SDT deqSdt) {

        Map<SDTGuard, SDT> retMap = new LinkedHashMap<>();
        List<DisequalityGuard> deqList = new ArrayList<>();

        // 2. see which of the equality guards can use the else guard
        for (Map.Entry<EqualityGuard, SDT> e : eqs.entrySet()) {
            SDT eqSdt = e.getValue();
            EqualityGuard eqGuard = e.getKey();
            log.log(Level.FINEST, "comparing guards: " + eqGuard.toString()
                    + " to " + deqGuard.toString() + "\nSDT    : "
                    + eqSdt.toString() + "\nto SDT : " + deqSdt.toString());
            if (!(eqSdt.canUse(deqSdt))) {
                log.log(Level.FINEST,"CANNOT USE: Adding if guard");
                retMap.put(eqGuard, eqSdt);
                deqList.add(eqGuard.toDeqGuard());
            }
        }
        SDTCompoundGuard retDeqGuard = new SDTCompoundGuard(
                deqGuard.getParameter(),
                deqList.toArray(new DisequalityGuard[]{}));
        retMap.put(retDeqGuard, deqSdt);
        return retMap;
    }
            
//    private Map<SDTGuard,SDT> truify(Map<SDTGuard,SDT> untrue) {
//        Map<SDTGuard,SDT> trueMap = new LinkedHashMap<SDTGuard, SDT>();
//        for (Map.Entry<SDTGuard, SDT> e: untrue.entrySet()) {
//            SDTGuard guard = e.getKey();
//            if (guard instanceof SDTCompoundGuard 
//                    && ((SDTCompoundGuard) guard).getGuards().isEmpty()) {
//                trueMap.add()
//                
//            }
//        }
//    }

    // given a set of registers and a set of guards, keep only the registers
    // that are mentioned in any guard
    private PIV keepMem(Set<SDTGuard> guardSet, List<DataValue> prefixValues) {
        PIV ret = new PIV();
        for (SDTGuard mg : guardSet) {
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
            SuffixValuation suffixValues,
            SDTConstructor oracle) {

        int pId = values.size() + 1;

        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();

        SuffixValue currentParam = new SuffixValue(type, pId);

        Map<EqualityGuard, SDT> tempKids = new LinkedHashMap<>();

        // store temporary values here
//        PIV paramsToRegs = new PIV();
//        for (Map.Entry<Parameter, Register> e : pir) {
//            paramsToRegs.add(e.getKey(), e.getValue());
//        }
        //ParsInVars ifPiv = new ParsInVars();
        //ifPiv.putAll(piv);
        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
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
        if (!free) {  // for now, we assume that all values are free.
            DataValue d = suffixValues.get(sv);
            if (d == null) {
                d = getFreshValue(potential);
                //suffixValues.put(sv, d);
            }
            values.put(pId, d);
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

            EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues, currentParam, values, pir);
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
                    prefix, suffix, ifValues, pir, ifSuffixValues);

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
                prefix, suffix, elseValues, pir, elseSuffixValues);

//        ParsInVars diseqPiv = new ParsInVars();
//        for (Register rg : ifPiv.keySet()) {
//            DataValue tdv = ifPiv.get(rg);
//            if (tdv.getType() == type) {
//                diseqPiv.put(rg, tdv);
//            }
//        }
//        
        SDTCompoundGuard deqGuard = new SDTCompoundGuard(currentParam, (diseqList.toArray(new DisequalityGuard[]{})));
        log.log(Level.FINEST, "diseq guard = " + deqGuard.toString());
        // tempKids is the temporary SDT (sort of)
        //tempKids.put(deqGuard, elseOracleSdt);

        // merge the guards
        Map<SDTGuard, SDT> merged = mergeGuards(tempKids, deqGuard, elseOracleSdt);

        // only keep registers that are referenced by the merged guards
        pir.putAll(keepMem(merged.keySet(),prefixValues));

        log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
        //log.log(Level.FINEST,"temporary pivs = " + tempPiv.keySet());
        log.log(Level.FINEST, "merged guards = " + merged.keySet());
        log.log(Level.FINEST, "merged pivs = " + pir.toString());

        // clear the temporary map of children
        tempKids.clear();

        SDT returnSDT = new SDT(merged);
        // this keeps only the REGISTERS
//        Set<Register> regs = returnSDT.getRegisters();
//        for (Map.Entry<Parameter, Register> e : pir.entrySet()) {
//            if (regs.contains(e.getValue())) {
//                pir.put(e.getKey(), e.getValue());
//            }
//        }
        return returnSDT;

    }

    private EqualityGuard pickupDataValue(DataValue<T> newDv, List<DataValue> prefixValues, SuffixValue currentParam, WordValuation ifValues, PIV pir) {
        DataType type = currentParam.getType();
        int newDv_i;
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
            log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
            return new EqualityGuard(currentParam, new SuffixValue(type, smallest));
        }

    }

    @Override
    public DataValue instantiate(
            Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv,
            ParValuation pval,
            SDTGuard guard,
            Parameter param) {

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
            if (ereg instanceof Register) {
                            log.log(Level.FINEST, "piv: " + piv.toString() + " " + ereg.toString() + " " + param.toString());
                Parameter p = piv.getOneKey((Register)ereg);
                            log.log(Level.FINEST, "p: " + p.toString());
                int idx = p.getId();
                //return piv.get(param);
                // trying to not pickup values from prefix
                return prefixValues.get(idx - 1);
            } else {
                assert ereg instanceof SuffixValue;
                Parameter p = new Parameter(type, ereg.getId());
                return pval.get(p);
            }
            //}
        }

//        log.log(Level.FINEST,"base case");
        Collection potSet = DataWords.<T>joinValsToSet(
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
