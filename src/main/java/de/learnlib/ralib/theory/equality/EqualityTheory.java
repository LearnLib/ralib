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

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author falk and sofia
 * @param <T>
 */
public abstract class EqualityTheory<T> implements Theory<T> {

    //@Override
//    public List<DataValue<T>> getPotential(
//            Collection<DataValue<T>> vals) {        
//        Set<DataValue<T>> set = new LinkedHashSet<>(vals);
//        return new ArrayList<>(set);
//    }
    
        
    public List<DataValue<T>> getPotential(List<DataValue<T>> vals) {
        return vals;
    }

    @Deprecated
    private Map<List<SDTGuard>, SDT> 
            mergeGuards(Map<List<SDTGuard>,SDT> unmerged) {
        Map<List<SDTGuard>, SDT> merged = new HashMap<>();
        Map<List<SDTGuard>, SDT> ifs = new HashMap<>();
        for (List<SDTGuard> tempG : unmerged.keySet()) {
            SDT tempSdt = unmerged.get(tempG);
            if (tempG.isEmpty()) {
                //System.out.println("Adding else guard: " + tempG.toString());
                merged.put(tempG, tempSdt);
            }
            else {
                ifs.put(tempG, tempSdt);
            }
        }
        for (List<SDTGuard> elseG: merged.keySet()) {
            SDT elseSdt = merged.get(elseG);
            for (List<SDTGuard> ifG : ifs.keySet()) {
                SDT ifSdt = ifs.get(ifG);
                //System.out.println("comparing guards: " + ifG.toString() 
                // + " to " + elseG.toString() + "\nSDT    : " + 
                // ifSdt.toString() + "\nto SDT : " + elseSdt.toString());
                if (!(ifSdt.canUse(elseSdt))) {
                    //System.out.println("Adding if guard: " + ifG.toString());
                    //System.out.println(ifSdt.toString() + " not eq to " + elseSdt.toString());
                    merged.put(ifG, ifSdt);
                }
            }
        }
        return merged;
    }

    
// given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT.  Base case: always add the 'else' guard first.
    private Map<SDTGuard, SDT> 
            fluffGuards(Map<SDTGuard,SDT> unmerged) {
        Map<SDTGuard, SDT> merged = new HashMap<>();
        Map<SDTGuard, SDT> ifs = new HashMap<>();
        for (SDTGuard tempG : unmerged.keySet()) {
            SDT tempSdt = unmerged.get(tempG);
            if (tempG instanceof DisequalityGuard) {
                //System.out.println("Adding else guard: " + tempG.toString());
                merged.put(tempG, tempSdt);
            }
            else {
                ifs.put(tempG, tempSdt);
            }
        }
        for (SDTGuard elseG: merged.keySet()) {
            SDT elseSdt = merged.get(elseG);
            for (SDTGuard ifG : ifs.keySet()) {
                SDT ifSdt = ifs.get(ifG);
                System.out.println("comparing guards: " + ifG.toString() 
                 + " to " + elseG.toString() + "\nSDT    : " + 
                 ifSdt.toString() + "\nto SDT : " + elseSdt.toString());
                if (!(ifSdt.canUse(elseSdt))) {
                    //System.out.println("Adding if guard: " + ifG.toString());
                    //System.out.println(ifSdt.toString() + " not eq to " + elseSdt.toString());
                    merged.put(ifG, ifSdt);
                }
            }
        }
        return merged;
    }

    // given a set of registers and a set of guards, keep only the registers
    // that are mentioned in any guard
    private ParsInVars keepMem(ParsInVars pivs, Set<SDTGuard> guardSet) {
        System.out.println("available regs: " + pivs.toString());
        ParsInVars ret = new ParsInVars();
        for (SDTGuard mg : guardSet) {
            if (mg instanceof EqualityGuard) {
                System.out.println(mg.toString());
                //for (Register k : mg.getRegisters()) {
                    Register k = ((EqualityGuard)mg).getRegister();
                    DataValue dv = pivs.get(k);
                    ret.put(k, dv);
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
            ParsInVars piv,
            SuffixValuation suffixValues,
            SDTConstructor oracle) {
        
        int pId = values.size() + 1;
        
        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();
        
        
        SuffixValue currentParam = new SuffixValue(type, pId);    
        
        Map<SDTGuard, SDT> tempKids = new HashMap<>();
        
        ParsInVars ifPiv  = new ParsInVars();
        ifPiv.putAll(piv);
        
        boolean free = suffix.getFreeValues().contains(sv);
        
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
        if (!free) {  // for now, we assume that all values are free.
            DataValue d = suffixValues.get(sv);
            if (d == null) {
                d = getFreshValue( potential );
                //suffixValues.put(sv, d);
            }
            values.put(pId, d);
        } 
        
        System.out.println("potential " + potential.toString());
        // process the 'else' case
        DataValue fresh = getFreshValue(potential);
        
        WordValuation elseValues = new WordValuation();
        elseValues.putAll(values);
        elseValues.put(pId, fresh);
        
        SuffixValuation elseSuffixValues = new SuffixValuation();
        elseSuffixValues.putAll(suffixValues);
        elseSuffixValues.put(sv, fresh);
                        
        SDT elseOracleSdt = oracle.treeQuery(
                prefix, suffix, elseValues, piv, elseSuffixValues);

        //tempKids.put(new ArrayList<SDTGuard>(), elseOracleSdt);
        tempKids.put(new DisequalityGuard(currentParam,ifPiv.keySet()), elseOracleSdt);
        
        
        
        // process each 'if' case
        for (DataValue<T> newDv : potential) {
        
            WordValuation ifValues = new WordValuation();
            ifValues.putAll(values);
            ifValues.put(pId, newDv);
            
            SuffixValuation ifSuffixValues = new SuffixValuation();
            ifSuffixValues.putAll(suffixValues);  // copy the suffix valuation
            ifSuffixValues.put(sv, newDv);
            
            //System.out.println("this is the piv: " + ifPiv.toString() + " and newDv " + newDv.toString());
            
            List<Integer> rvPositions = new ArrayList(ifValues.getAllKeys(newDv));
            Collections.sort(rvPositions);
            Register rv = ifPiv.getOneKey(newDv);
            
            Integer rvPos = rvPositions.get(0);
            //Integer rvPos = ifValues.getKey(newDv);
            
            if (rv == null) {
                //rv = regGenerator.next(type);
                rv = new Register(type, rvPos);
                ifPiv.put(rv, newDv);
            }
            
            SDT eqOracleSdt = oracle.treeQuery(
                    prefix, suffix, ifValues, ifPiv, ifSuffixValues);

                
            //List newGuardList = new ArrayList<>();
            //newGuardList.add(new EqualityGuard(currentParam,rv));
            
            EqualityGuard eqGuard = new EqualityGuard(currentParam,rv);
            
            tempKids.put(eqGuard, eqOracleSdt);
        }
        
        // merge the guards
        Map<SDTGuard, SDT> merged = fluffGuards(tempKids);
        
        // only keep registers that are referenced by the merged guards
        ParsInVars addPiv = keepMem(ifPiv, merged.keySet());
        
        System.out.println("temporary guards = " + tempKids.keySet());
        //System.out.println("temporary pivs = " + tempPiv.keySet());
        System.out.println("merged guards = " + merged.keySet());
        System.out.println("merged pivs = " + addPiv.toString());
        
        // clear the temporary map of children
        tempKids.clear();
        
        SDT returnSDT = new SDT(merged);
        return returnSDT;
    
        
               
                    
    }

    @Override
    public DataValue instantiate(
            Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, 
            ParValuation pval, 
            SDTGuard guard, 
            Parameter param) {
        
    //    System.out.println("size of guards: " + guard.getRegisters().size() + "; guards are: " + guard.toString());
        
        if (guard instanceof EqualityGuard) {
                System.out.println("equality guard");
//                if (!pval.isEmpty()) {
//                    System.out.println("pval = " + pval.toString());
//                }
//                else {
//                    System.out.println("pval is empty");
//                }
                System.out.println("pval says " + pval.get(param).toString());
                return pval.get(param);
            }
        
//        System.out.println("base case");
        DataType type = param.getType();
        Collection potSet = DataWords.<T>joinValsToSet(
                            DataWords.<T>valSet(prefix, type),
                            pval.<T>values(type));
        DataValue fresh = this.getFreshValue(new ArrayList<>(potSet));
        System.out.println("fresh = " + fresh.toString());
        return fresh;
        
        }
    
    
}


