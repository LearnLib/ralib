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
package de.learnlib.ralib.theory.succ;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class SuccessorTheoryInt implements TypedTheory<Integer>{

    private DataType type = null;
    
    private boolean useNonFreeOptimization = true;
    
    @Override
    public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
        int max = 0;        
        for (DataValue<Integer> i : vals) {
            max = Math.max(max, i.getId());
        }
        
        return new DataValue<>(type, max + 2);
    }

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, 
            WordValuation values, PIV pir, Constants constants, 
            SuffixValuation suffixValues, SDTConstructor oracle, IOOracle traceOracle) {

        System.out.println("SuccessorTheoryInt.treeQuery()");
        
        System.out.println("Prefix: " + prefix);
        System.out.println("Sym. Suffix: " + suffix);
        System.out.println("Word Val.: " + values);
        System.out.println("PiR: " + pir);
        System.out.println("Constants: " + constants);
        System.out.println("Suffix Vals.: " + suffixValues);
        
        // position determines current parameter
        int pId = values.size() + 1;
        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();
        SuffixValue currentParam = new SuffixValue(type, pId);

        // compute potential
        Collection<DataValue<Integer>> potSet = DataWords.joinValsToSet(
                constants.values(type),
                DataWords.valSet(prefix, type),
                suffixValues.values(type));

//        //List<DataValue<T>> potList = new ArrayList<>(potSet);
//        //List<DataValue<T>> potential = getPotential(potList);
//
//        boolean free = suffix.getFreeValues().contains(sv);
//        if (!free && useNonFreeOptimization) {
//            DataValue d = suffixValues.get(sv);
//            if (d == null) {
//                d = getFreshValue(potential);
//            }
//            values.put(pId, d);
//            WordValuation trueValues = new WordValuation();
//            trueValues.putAll(values);
//            SuffixValuation trueSuffixValues = new SuffixValuation();
//            trueSuffixValues.putAll(suffixValues);
//            trueSuffixValues.put(sv, d);
//            SDT sdt = oracle.treeQuery(
//                    prefix, suffix, trueValues,
//                    pir, constants, trueSuffixValues);
//
//            log.log(Level.FINEST, " single deq SDT : " + sdt.toString());
//
//            Map<SDTGuard, SDT> merged = mergeGuards(tempKids,
//                    new SDTAndGuard(currentParam), sdt);
//
//            log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
//            //log.log(Level.FINEST,"temporary pivs = " + tempPiv.keySet());
//            log.log(Level.FINEST, "merged guards = " + merged.keySet());
//            log.log(Level.FINEST, "merged pivs = " + pir.toString());
//
//            return new SDT(merged);
//        }
//
//        // special case: fresh values in outputs
//        if (freshValues) {
//
//            ParameterizedSymbol ps = computeSymbol(suffix, pId);
//
//            if (ps instanceof OutputSymbol && ps.getArity() > 0) {
//
//                int idx = computeLocalIndex(suffix, pId);
//                Word<PSymbolInstance> query = buildQuery(
//                        prefix, suffix, values);
//                Word<PSymbolInstance> trace = ioOracle.trace(query);
//                PSymbolInstance out = trace.lastSymbol();
//
//                if (out.getBaseSymbol().equals(ps)) {
//
//                    DataValue d = out.getParameterValues()[idx];
//
//                    if (d instanceof FreshValue) {
//                        d = getFreshValue(potential);
//                        values.put(pId, d);
//                        WordValuation trueValues = new WordValuation();
//                        trueValues.putAll(values);
//                        SuffixValuation trueSuffixValues
//                                = new SuffixValuation();
//                        trueSuffixValues.putAll(suffixValues);
//                        trueSuffixValues.put(sv, d);
//                        SDT sdt = oracle.treeQuery(
//                                prefix, suffix, trueValues,
//                                pir, constants, trueSuffixValues);
//
//                        log.log(Level.FINEST,
//                                " single deq SDT : " + sdt.toString());
//
//                        Map<SDTGuard, SDT> merged = mergeGuards(tempKids,
//                                new SDTAndGuard(currentParam), sdt);
//
//                        log.log(Level.FINEST,
//                                "temporary guards = " + tempKids.keySet());
//                        log.log(Level.FINEST,
//                                "merged guards = " + merged.keySet());
//                        log.log(Level.FINEST,
//                                "merged pivs = " + pir.toString());
//
//                        return new SDT(merged);
//                    }
//                }
//            }
//        }
//
//        log.log(Level.FINEST, "potential " + potential.toString());
//
//        // process each 'if' case
//        // prepare by picking up the prefix values
//        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
//
//        log.log(Level.FINEST, "prefix list    " + prefixValues.toString());
//
//        DataValue fresh = getFreshValue(potential);
//
//        List<DisequalityGuard> diseqList = new ArrayList<DisequalityGuard>();
//        for (DataValue<T> newDv : potential) {
//            log.log(Level.FINEST, newDv.toString());
//
//            // this is the valuation of the suffixvalues in the suffix
//            SuffixValuation ifSuffixValues = new SuffixValuation();
//            ifSuffixValues.putAll(suffixValues);  // copy the suffix valuation
//
//            EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues,
//                    currentParam, values, constants);
//            log.log(Level.FINEST, "eqGuard is: " + eqGuard.toString());
//            diseqList.add(new DisequalityGuard(
//                    currentParam, eqGuard.getRegister()));
//            //construct the equality guard
//            // find the data value in the prefix
//            // this is the valuation of the positions in the suffix
//            WordValuation ifValues = new WordValuation();
//            ifValues.putAll(values);
//            ifValues.put(pId, newDv);
//            SDT eqOracleSdt = oracle.treeQuery(
//                    prefix, suffix, ifValues, pir, constants, ifSuffixValues);
//
//            tempKids.put(eqGuard, eqOracleSdt);
//        }
//
//        // process the 'else' case
//        // this is the valuation of the positions in the suffix
//        WordValuation elseValues = new WordValuation();
//        elseValues.putAll(values);
//        elseValues.put(pId, fresh);
//
//        // this is the valuation of the suffixvalues in the suffix
//        SuffixValuation elseSuffixValues = new SuffixValuation();
//        elseSuffixValues.putAll(suffixValues);
//        elseSuffixValues.put(sv, fresh);
//
//        SDT elseOracleSdt = oracle.treeQuery(
//                prefix, suffix, elseValues, pir, constants, elseSuffixValues);
//
//        SDTAndGuard deqGuard = new SDTAndGuard(currentParam,
//                (diseqList.toArray(new DisequalityGuard[]{})));
//        log.log(Level.FINEST, "diseq guard = " + deqGuard.toString());
//
//        // merge the guards
//        Map<SDTGuard, SDT> merged = mergeGuards(
//                tempKids, deqGuard, elseOracleSdt);
//
//        // only keep registers that are referenced by the merged guards
//        pir.putAll(keepMem(merged));
//
//        log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
//        log.log(Level.FINEST, "merged guards = " + merged.keySet());
//        log.log(Level.FINEST, "merged pivs = " + pir.toString());
//
//        // clear the temporary map of children
//        tempKids.clear();
//
//        for (SDTGuard g : merged.keySet()) {
//            assert !(g == null);
//        }
//
//        SDT returnSDT = new SDT(merged);
//        return returnSDT;

        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public DataValue instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, 
            PIV piv, ParValuation pval, Constants constants, SDTGuard guard, 
            SymbolicDataValue.Parameter param, Set<DataValue<Integer>> oldDvs, boolean useSolver) {
        
        System.out.println("SuccessorTheoryInt.instantiate()");
        
        System.out.println("Prefix: " + prefix);
        System.out.println("Symbol: " + ps);
        System.out.println("PiR: " + piv);
        System.out.println("Par Val.: " + pval);
        System.out.println("Constants: " + constants);
        System.out.println("SDT Guard: " + guard);
        System.out.println("Parameter: " + param);        
        System.out.println("Known Vals.: " + oldDvs);
        
        if (guard instanceof SDTTrueGuard) {
            Collection<DataValue<Integer>> potSet = DataWords.<Integer>joinValsToSet(
                    constants.values(type),
                    DataWords.valSet(prefix, type),
                    pval.values(type));            
            
            return getFreshValue(new ArrayList<>(potSet));
        }
        
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public void setUseSuffixOpt(boolean useit) {
        System.err.println("Suffix Optimization currently not implemented for theory " + 
                this.getClass().getName());
    }

    @Override
    public void setCheckForFreshOutputs(boolean doit) {
        System.err.println("Check for fresh outputs currently not implemented for theory " + 
                this.getClass().getName());
    }

    @Override
    public Collection<DataValue<Integer>> getAllNextValues(List<DataValue<Integer>> vals) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    
}
