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
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.theory.Guard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.TreeOracle;
import de.learnlib.ralib.theory.TreeQueryResult;
import de.learnlib.ralib.trees.SDT;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

    @Override
    public List<DataValue<T>> getPotential(
            List<DataValue<T>> vals) {        
        Set<DataValue<T>> set = new LinkedHashSet<>(vals);
        return new ArrayList<>(set);
    }
    
    // given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT.  Base case: always add the 'else' guard first.
    private Map<Guard, SymbolicDecisionTree> 
            mergeGuards(Map<Guard,SymbolicDecisionTree> unmerged) {
        Map<Guard, SymbolicDecisionTree> merged = new HashMap<>();
        Map<EqualityGuard, SymbolicDecisionTree> ifs = new HashMap<>();
        for (Guard tempG : unmerged.keySet()) {
            SymbolicDecisionTree tempSdt = unmerged.get(tempG);
            if (tempG instanceof ElseGuard) {
                //System.out.println("Adding else guard: " + tempG.toString());
                merged.put((ElseGuard) tempG, tempSdt);
            }
            else {
                ifs.put((EqualityGuard) tempG, tempSdt);
            }
        }
        for (Guard elseG: merged.keySet()) {
            SymbolicDecisionTree elseSdt = merged.get(elseG);
            for (EqualityGuard ifG : ifs.keySet()) {
                SymbolicDecisionTree ifSdt = ifs.get(ifG);
                //System.out.println("comparing guards: " + ifG.toString() 
                // + " to " + elseG.toString() + "\nSDT    : " + 
                // ifSdt.toString() + "\nto SDT : " + elseSdt.toString());
                if (!(ifSdt.canUse(elseSdt))) {
                    //System.out.println("Adding if guard: " + ifG.toString());
                    //System.out.println(ifSdt.toString() + " not eq to " + elseSdt.toString());
                    merged.put((EqualityGuard) ifG, ifSdt);
                }
            }
        }
        return merged;
    }
            
            private Map<Set<Guard>, SymbolicDecisionTree> convToArrGuards(Map<Guard,SymbolicDecisionTree> someMap) {
                Map<Set<Guard>, SymbolicDecisionTree> convMap = new HashMap<>();
                for (Map.Entry<Guard, SymbolicDecisionTree> entry : someMap.entrySet()) {
                    convMap.put(Collections.singleton(entry.getKey()), entry.getValue());
                }
                return convMap;
            }

    // given a set of registers and a set of guards, keep only the registers
    // that are mentioned in any guard
    private ParsInVars keepMem(ParsInVars pivs, Set<Guard> guardSet) {
        ParsInVars ret = new ParsInVars();
        for (Register k : pivs.keySet()) {
            for (Guard mg : guardSet) {
                if (!(mg instanceof ElseGuard) && mg.getRegister().equals(k)) {
                    ret.put(k, pivs.get(k));
                }
            }
        }
        return ret;
    }
    
    
    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, 
            SymbolicSuffix suffix,
            WordValuation values, 
            ParsInVars piv,
            SuffixValuation suffixValues,
            TreeOracle oracle) {
        
        int pId = suffixValues.size() + 1;
        
//        System.out.println(values.toString());
        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();
        
        Map<Guard, SymbolicDecisionTree> tempKids = new HashMap<>();
        
        ParsInVars ifPiv  = new ParsInVars();
        ifPiv.putAll(piv);
        
        //boolean free = suffix.getFreeValues().contains(sv);
        
                Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                    DataWords.<T>valSet(prefix, type),
                    suffixValues.<T>values(type));
                        
//        if (!free) {  // for now, we assume that all values are free.
//            DataValue d = suffixValues.get(sv);
//            if (d == null) {
//                d = getFreshValue( potential );
//                //suffixValues.put(sv, d);
//            }
//            values.put(pId, d);
//        } 
        
         List<DataValue<T>> potential = getPotential(new ArrayList<>(potSet));
               
                
//        if (!free) {  // for now, we assume that all values are free.
//            DataValue d = suffixValues.get(sv);
//            if (d == null) {
//                d = getFreshValue( potential );
//                suffixValues.put(sv, d);
//            }
//            values.put(pId, d);
//        } 
//        
        // process the 'else' case
        DataValue fresh = getFreshValue(potential);
        
        WordValuation elseValues = new WordValuation();
        elseValues.putAll(values);
        elseValues.put(pId, fresh);
        
        SuffixValuation elseSuffixValues = new SuffixValuation();
        elseSuffixValues.putAll(suffixValues);
        elseSuffixValues.put(sv, fresh);
        
        System.out.println("query: " + elseValues.toString()+ "|||, " + piv.toString() + ", " + elseSuffixValues.toString());
        TreeQueryResult elseOracleReply = oracle.treeQuery(
                prefix, suffix, elseValues, piv, elseSuffixValues);
        SymbolicDecisionTree elseOracleSdt = elseOracleReply.getSdt();
        tempKids.put(new ElseGuard(sv), elseOracleSdt);
        
        // process each 'if' case
        for (DataValue<T> newDv : potential) {
        
            WordValuation ifValues = new WordValuation();
            ifValues.putAll(values);
            ifValues.put(pId, newDv);
            
            SuffixValuation ifSuffixValues = new SuffixValuation();
            ifSuffixValues.putAll(suffixValues);  // copy the suffix valuation
            ifSuffixValues.put(sv, newDv);
            
            Register rv = ifPiv.getKey(newDv);
            Integer rvPos = pId;
        
            if (rv == null) {
                rv = new Register(type, rvPos);
                ifPiv.put(rv, newDv);
            }
            
            TreeQueryResult eqOracleReply = oracle.treeQuery(
                    prefix, suffix, ifValues, ifPiv, ifSuffixValues);
            SymbolicDecisionTree eqOracleSdt = eqOracleReply.getSdt();
                
            Guard newGuard = new EqualityGuard(sv,rv);
            tempKids.put(newGuard, eqOracleSdt);
        }
        
        // merge the guards
        Map<Guard, SymbolicDecisionTree> merged = mergeGuards(tempKids);
        
        // only keep registers that are referenced by the merged guards
        ParsInVars addPiv = keepMem(ifPiv, merged.keySet());
        
        //System.out.println("temporary guards = " + tempKids.keySet());
        //System.out.println("temporary pivs = " + tempPiv.keySet());
        //System.out.println("merged guards = " + merged.keySet());
        //System.out.println("merged pivs = " + addPiv.toString());
        
        // clear the temporary map of children
        tempKids.clear();
        
        SDT returnSDT = new SDT(true, addPiv.keySet(), convToArrGuards(merged));
        return new TreeQueryResult(addPiv, null, returnSDT);         
    
        
               
                    
    }
    
    
}

