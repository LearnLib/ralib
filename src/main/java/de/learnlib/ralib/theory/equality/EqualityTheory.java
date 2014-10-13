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
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.theory.Guard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.TreeOracle;
import de.learnlib.ralib.theory.TreeQueryResult;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.trees.SDT;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 * @param <T>
 */
public abstract class EqualityTheory<T> implements Theory<T> {

    //
      
    // potential for equality theory: equivalence with all previously seen values.
    @Override
    public List<DataValue<T>> getPotential(
            Collection<DataValue<T>> vals) {        
        Set<DataValue<T>> set = new LinkedHashSet<>(vals);
        return new ArrayList<>(set);
    }
    
    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, 
            SymbolicSuffix suffix,
            WordValuation values, 
            ParsInVars piv,
            VarValuation suffixValues,
            TreeOracle oracle) {
        
        //System.out.println("values received by equality: " + values.toString());

        // 1. check degree of freedom for this parameter
        int prefixLength = DataWords.paramLength(DataWords.actsOf(prefix));
        int pId = values.size() + 1;
        //int suffixPId = pId - prefixLength;
        SymbolicDataValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();
        
        boolean free = suffix.getFreeValues().contains(sv);
        List<DataValue<T>> potential = getPotential(
                DataWords.<T>joinValsToSet(
                    DataWords.<T>valSet(prefix, type),
                    suffixValues.<T>values(type)));
                        
        if (!free) {  // for now, we assume that all values are free.
            //System.out.println("not free");
            DataValue d = suffixValues.get(sv);
            if (d == null) {
                d = getFreshValue( potential );
                suffixValues.put(sv, d);
            }
            values.put(pId, d);
            
            // call next ...
        } 
        // possible values are, for equality, potential + fresh
        
        
        List<DataValue<T>> freshAndPotential = new ArrayList<>();
        DataValue fresh = getFreshValue(potential);
        freshAndPotential.addAll(potential);
        freshAndPotential.add(fresh);
        //freshAndPotential.add(fresh);
        
        //System.out.println("here's the potential: " + potential.toString());
        
        WordValuation newValues = new WordValuation();
        newValues.putAll(values);
        
        VarValuation newSuffixValues = new VarValuation();
        newSuffixValues.putAll(suffixValues);  // copy the suffix valuation
                
        Map<Guard, SymbolicDecisionTree> kids = new HashMap<>();
                
        // Start with the 'else' case
        //System.out.println("-------- Begin else case --------");
        //newValues.put(pId, fresh);
        //newSuffixValues.put(sv, fresh);
        
        //System.out.println("Querying oracle for fresh value");
        //System.out.println("suffix actions: " + suffix.getActions().toString());
        //System.out.println("new prefix: " + newPrefix.toString());
        //System.out.println("old suffix: " + suffix.toString());
        //System.out.println("new suffix: " + newSuffix.toString());
        //System.out.println("new values: " + newValues.toString());
        //System.out.println("new suffix values: " + newSuffixValues.toString());
        
        //return treeQuery(prefix, suffix, newValues, piv, newSuffixValues, oracle);
        //Word<PSymbolInstance> elsePrefix = prefix;
        //SymbolicSuffix elseSuffix = suffix;
        
        //TreeQueryResult elseOracleReply = treeQuery(prefix, suffix, newValues, piv, newSuffixValues, oracle);
        //SymbolicDecisionTree elseOracleSdt = elseOracleReply.getSdt();
        
        //newValues.remove(pId);
        //newSuffixValues.remove(sv);
        
        //kids.put(new ElseGuard(sv), elseOracleSdt);
        
        // now time for the special cases
        
        for (DataValue<T> newDv : freshAndPotential) {
            System.out.println("-------- Begin iteration for sv: " + sv.toString() + " and dv: " + newDv.toString() + " --------");
            newValues.put(pId, newDv);
            newSuffixValues.put(sv, newDv);
            
            System.out.println("Querying oracle");
            //System.out.println("suffix actions: " + suffix.getActions().toString() + ", suffix length: " + DataWords.paramLength(suffix.getActions()));
            //System.out.println("new prefix: " + newPrefix.toString());
            //System.out.println("old suffix: " + suffix.toString());
            //System.out.println("new suffix: " + newSuffix.toString());
            System.out.println("new values: " + newValues.toString() + ", size: " + newValues.size());
            //System.out.println("new suffix values: " + newSuffixValues.toString());
            
            
            if (newDv != fresh) {
            SymbolicDataValue rv = new SymbolicDataValue(SymbolicDataValue.ValueClass.REGISTER, type, (piv.size() +1) );
            piv.put(rv, sv);     
            System.out.println("Add to piv: " + sv.toString() + " (store " + newDv.toString() + " in " + rv.toString() + ")");
            TreeQueryResult eqOracleReply = oracle.treeQuery(prefix, suffix, newValues, piv, newSuffixValues);
            SymbolicDecisionTree eqOracleSdt = eqOracleReply.getSdt();
            //System.out.println("SDT: " + eqOracleSdt.toString());
            
            kids.put(new Equality(sv, rv), eqOracleSdt);               
            }
            
            else{
                TreeQueryResult eqOracleReply = oracle.treeQuery(prefix, suffix, newValues, piv, newSuffixValues);
            SymbolicDecisionTree eqOracleSdt = eqOracleReply.getSdt();
            //System.out.println("SDT: " + eqOracleSdt.toString());
            
                kids.put(new ElseGuard(sv), eqOracleSdt);
            }
            
            
                        
            newValues.remove(pId);
            newSuffixValues.remove(sv);
            
            System.out.println("--------- End of iteration --------");
            
            // Don't forget to add the nonfresh values to memorable
        
            //    subtrees.add(oracle.treeQuery(prefix, suffix, values, piv, newSuffixValues));
        }
        
        //return treeQuery(prefix, suffix, newValues, piv, newSuffixValues, oracle);
        
        
        // System.out.println("reached end of for loop");
//       bad attempt at grouping branches
        
        // for (int i = 1; i < subtrees.size(); i++) {
//            TreeQueryResult current = subtrees.get(i);
//            for (int j = i+1; j < subtrees.size(); j++) {
//                TreeQueryResult next = subtrees.get(j);
//                if (current.getSdt().isEquivalent(next.getSdt())) {
//                    subtrees.remove(j);
//                }
//            }
//            // add current.getSdt()
//        }
//            // if unique flag still OK, then create a branch for current.
//        
        System.out.println("done!");
        
        
        return new TreeQueryResult(piv, null, new SDT(true, piv.keySet(), kids));         
         
    }
}

        
    
