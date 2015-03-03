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

package de.learnlib.ralib.oracles.mto;

import de.learnlib.ralib.oracles.mto.MultiTheoryBranching.Node;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheoryTreeOracle implements TreeOracle, SDTConstructor {
        
    private final DataWordOracle oracle;
    
    private final Map<DataType, Theory> teachers;

    public MultiTheoryTreeOracle(DataWordOracle oracle, Map<DataType, Theory> teachers) {
        this.oracle = oracle;
        this.teachers = teachers;
    }
    
    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        //WordValuation prefixValuation = new WordValuation();
        //DataValue[] prefixValues = DataWords.valsOf(prefix);
        //for (int k = 0; k<prefixValues.length; k++) {
        //    prefixValuation.put(k, prefixValues[k]);
        //}
        ParsInVars piv = new ParsInVars();
        SDT sdt = treeQuery(prefix, suffix, 
                new WordValuation(), piv, new SuffixValuation());
        
        return new TreeQueryResult(piv, sdt);
    }
    
    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            WordValuation values, ParsInVars piv, 
            SuffixValuation suffixValues) {
        
        //System.out.println("suffix length: " + DataWords.paramLength(suffix.getActions()) + ", values size: " + values.size() + ", suffixValues size " + suffixValues.size());
        
        // IF at the end of the word!
        if (values.size() == DataWords.paramLength(suffix.getActions())) { 
            //System.out.println("we're at the end of the wor(l)d!");
            // if we are at the end of the word, i.e., nothing more to 
            // instantiate, the number of values in the whole word is equal to 
            // the number of actions in the suffix
            
            //System.out.println("attempting to concatenate suffix: " + suffix.getActions().toString() + " AND " + values.toString());
//            int startingPoint = DataWords.paramLength(DataWords.actsOf(prefix));
//            Map<
//          
            Word<PSymbolInstance> concSuffix = DataWords.instantiate(
                    suffix.getActions(), values);
            
            //Word<PSymbolInstance> trace = prefix.concat(concSuffix);
            DefaultQuery<PSymbolInstance, Boolean> query = 
                    new DefaultQuery<>(prefix, concSuffix);
            oracle.processQueries(Collections.singletonList(query));
            boolean qOut = query.getOutput();
            
            //System.out.println("Trace = " + trace.toString() + " >>> " + 
            //        (qOut ? "ACCEPT (+)" : "REJECT (-)"));
            return qOut ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;
            
            // return accept / reject as a leaf
        }
        
//        System.out.println("values passed to theory: " + values.toString());
        
        // OTHERWISE get the first noninstantiated data value in the suffix and its type
        SymbolicDataValue sd = suffix.getDataValue(values.size() + 1);
        //if (sd == null) {System.out.println("breaking");}
        //System.out.println("first uninstantiated value in suffix... " + sd.toString() + " of type " + sd.getType().getName());
        
        Theory teach = teachers.get(sd.getType());
        //System.out.println("Teacher theory: " + sd.getType().toString());
        // make a new tree query for prefix, suffix, prefix valuation, ...
        // to the correct teacher (given by type of first DV in suffix)
        return teach.treeQuery(prefix, suffix, values, piv, suffixValues, this);
    }    
    

    /**
     * This method computes the initial branching for
     * an SDT. It re-uses existing valuations where 
     * possible.
     * 
     */   
    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, SymbolicDecisionTree... sdts) {
        
        //TODO: check if this casting can be avoided by proper use of generics
        //TODO: the problem seems to be 
        SDT[] casted = new SDT[sdts.length];
        for (int i = 0; i < casted.length; i++) {
                casted[i] = (SDT) sdts[i];                
            }
        
        MultiTheoryBranching mtb = getInitialBranching(
                prefix, ps, piv, new ParValuation(), 
                new ArrayList<SDTGuard>(), casted);
        
        return mtb;
    }

    public Branching getInitialBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, ParValuation pval, SymbolicDecisionTree... sdts) {
        SDT[] casted = new SDT[sdts.length];
        for (int i = 0; i < casted.length; i++) {
                casted[i] = (SDT) sdts[i];                
            }
        
        MultiTheoryBranching mtb = getInitialBranching(
                prefix, ps, piv, pval, 
                new ArrayList<SDTGuard>(), casted);
        
        return mtb;
    }
    
    
    @Override
    // get the initial branching for the symbol ps after prefix given a certain tree
    public MultiTheoryBranching getInitialBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, ParValuation pval, 
//ParVal maps from parameters to data values; PIV maps from parameters to registers
            List<SDTGuard> guards, SDT... sdts) {
        
        Node n = createNode(0, prefix, ps, piv, pval, sdts);
        
        return new MultiTheoryBranching(prefix, ps, n);
    
    }
    
    
    private Node createNode(int i, Word<PSymbolInstance> prefix, ParameterizedSymbol ps, 
            PIV piv, ParValuation pval, SDT... sdts) {
        
        if (i < ps.getArity()) {
            DataType type = ps.getPtypes()[i];
            System.out.println("current type: " + type.getName());
            int j = i+1;
            Parameter p = new Parameter(type,j);
            Map<DataValue, Node> nextMap = new HashMap<>();
            Map<DataValue, SDTGuard> guardMap = new HashMap<>();
            
            //for each guard in each sdt
            for (SDT sdt : sdts) {
                Map<SDTGuard, SDT> children = sdt.getChildren();
                System.out.println("guards are: " + children.keySet().toString());
                for (SDTGuard guard : children.keySet()) {
                    //if (!visited.contains(nextNode)) {
                    System.out.println("processing guard: " + guard.toString());
                    Theory teach = teachers.get(type);
                    DataValue dvi = teach.instantiate(prefix, ps, piv, pval, guard, p);
                    System.out.println(dvi.toString() + " maps to " + guard.toString() );
                    //System.out.println("dvi = " + dvi.toString());
                    nextMap.put(dvi, createNode(j, prefix, ps, piv, pval, children.get(guard)));
                    // another ugly hack because yuck
                    //SDTGuard newGuard = new ElseGuard(s);
                    //if (!guardList.isEmpty()) {
                    //    newGuard = guardList.get(0);
                    //}
                    
                    guardMap.put(dvi, guard);
                }
            }
            System.out.println("guardMap: " + guardMap.toString());
            System.out.println("nextMap: " + nextMap.toString());
            return new Node(p,nextMap,guardMap);
        }
        
        else {
            return new Node(new Parameter(null,ps.getArity()));
        }
        
    }
    
    /**
     * This method computes the initial branching for
     * an SDT. It re-uses existing valuations where 
     * possible.
     * 
     */    
    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, Branching current, 
            PIV piv, SymbolicDecisionTree... sdts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

   


    
}
