/*
 * Copyright (C) 2015 falk.
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

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.theory.SDTElseGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheoryBranching implements Branching {

    @Override
    public Map<Word<PSymbolInstance>, TransitionGuard> getBranches() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static class Node {
        private final Parameter parameter;
        private final Map<DataValue, Node> next = new HashMap<>();
        private final Map<DataValue, SDTGuard> guards = new HashMap<>();
        
        public Node(Parameter parameter) {
            this.parameter = parameter;
        }
        
        public Node(Parameter parameter, 
                Map<DataValue, Node> next, 
                Map<DataValue, SDTGuard> guards) {
            this.parameter = parameter;
            this.next.putAll(next);
            this.guards.putAll(guards);
        }
        
//        @Override
//        public String toString() {
//            return ":Node: \n " + parameter.toString() + "\n" 
//                    + "--next on: " + next.keySet().toString() + " -->\n" + next.toString() + "\n"
//                    + "--guards-->\n" + guards.toString() + "\n:End node:";
//        }
//              
            @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String start = parameter.toString();        
        sb.append(start).append("::\n");
        toString(sb, spaces(1));
        return sb.toString();
    }
    
    void toString(StringBuilder sb, String indentation) {
        sb.append(indentation);
        final int childCount = next.keySet().size();
        int count = 1;
        for (Map.Entry<DataValue, Node> e : next.entrySet()) {
            DataValue d = e.getKey();
            SDTGuard g = guards.get(d);
            //TODO: replace lists of guards by guards
            String nextIndent;
            if (count == childCount) {
                nextIndent = indentation + "      ";
            } else {
                nextIndent = indentation + " |    ";
            } 
            if (count > 1) {            
                sb.append(indentation);
            }
            sb.append("-- ").append(g.toString()).append(" (").append(d.toString()).append(") -->\n");
            e.getValue().toString(sb, nextIndent);
            
            count++;
        }
    }

    private String spaces(int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

 
    }
    
    private final Word<PSymbolInstance> prefix;
    
    private final ParameterizedSymbol action;
    
    private final Node node;

    public MultiTheoryBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol action, Node node) {
        this.prefix = prefix;
        this.action = action;
        this.node = node;
    }
    
    // collects DVs along one branch
        private List<DataValue[]> collectDataValues(Node n, List<DataValue[]> dvList, DataValue[] dvs, List<Node> visited) {
            // if we are not at a leaf
            visited.add(n);
            if (!n.next.isEmpty()) {
                // get all next nodes
                System.out.println("next dvs: " + n.next.keySet().toString());
                // go through each of the 'next' nodes
                for (DataValue d : n.next.keySet()) {
                    Node nextNode = n.next.get(d);
                    // if the node hasn't been visited previously 
                    if (!visited.contains(nextNode)) {
                        // add the node's data value to the array
                        int dvLength = dvs.length;
                        DataValue[] newDvs = new DataValue[dvLength+1];
                        System.arraycopy(dvs, 0, newDvs, 0, dvLength);
                        newDvs[dvLength] = d;
                        System.out.println("dvs are currently " + Arrays.toString(newDvs));
                        System.out.println("marked: " + visited.size());
                        // proceed down in the tree to the next node
                        collectDataValues(nextNode,dvList, newDvs, visited);
                        
                    }
                }
                
            }
            if (dvs.length == this.action.getArity()) {
                System.out.println("Just adding: " + Arrays.toString(dvs));    
                dvList.add(dvs);
            }
            return dvList;
        }
        
            private Map<DataValue[],TransitionGuard> collectDataValuesAndGuards(
                    Node n, Map<DataValue[],TransitionGuard> dvgMap, 
                    DataValue[] dvs, List<SDTGuard> guards, List<Node> visited) 
            {
            // if we are not at a leaf
            visited.add(n);
            if (!n.next.isEmpty()) {
                // get all next nodes
                System.out.println("next dvs: " + n.next.keySet().toString());
                // go through each of the 'next' nodes
                for (DataValue d : n.next.keySet()) {
                    Node nextNode = n.next.get(d);
                    // if the node hasn't been visited previously 
                    if (!visited.contains(nextNode)) {
                        SDTGuard nextGuard = n.guards.get(d);
                        // add the node's data value to the array
                        int dvLength = dvs.length;
                        DataValue[] newDvs = new DataValue[dvLength+1];
                        System.arraycopy(dvs, 0, newDvs, 0, dvLength);
                        newDvs[dvLength] = d;
                        // add the guard to the guardlist
                        List newGuards = new ArrayList<>();
                        newGuards.addAll(guards);
                        newGuards.add(nextGuard);
                        System.out.println("dvs are currently " + Arrays.toString(newDvs));
                        System.out.println("guards are currently " + newGuards.toString());
                        System.out.println("marked: " + visited.size());
                        // proceed down in the tree to the next node
                        collectDataValuesAndGuards(nextNode,dvgMap, newDvs, newGuards, visited);
                        
                    }
                }
                
            }
            if (dvs.length == this.action.getArity()) {
                System.out.println("Just adding: " + Arrays.toString(dvs));    
                //dvgMap.put(dvs, toTGList(guards,0));
                dvgMap.put(dvs,null);
            }
            return dvgMap;
        }
           
//    public TransitionGuard toTGList(List<SDTGuard> guardList, int i) {
//        // 1. Create a list of expressions.
//        List<
//        SDTGuard sdtGuard = guardList.get(i);
//        if (sdtGuard instanceof SDTElseGuard) {
//            
//        }
//        if (guardList.size() == i + 1) {
//            return thisExpr;
//        } else {
//            return new PropositionalCompound(thisExpr, LogicalOperator.AND, makeExpr(guardList, i + 1));
//        }
//    }        
    
//    
   //      Map<DataValue[], List<SDTGuard>> psMap = collectDataValuesAndGuards(this.node, new HashMap<DataValue[],List<SDTGuard>>(), new DataValue[0], new ArrayList<SDTGuard>(), new ArrayList<Node>());
////        List<DataValue[]> psList = collectDataValues(this.node, new ArrayList<DataValue[]>(), new DataValue[0], new ArrayList<Node>());
////        for (DataValue[] d : psList) {
////            branches.put(Word.fromLetter(new PSymbolInstance(action,d)),null);
////        }
//        for (DataValue[] d : psMap.keySet()) {
//            branches.put(Word.fromLetter(new PSymbolInstance(action,d)),null);
//        }
 

// for each next-node
        
        
        
//        Set<Word<PSymbolInstance>> words = new HashSet<>();
//        Map<Word<PSymbolInstance>, TransitionGuard> returnMap = new HashMap<>();
//        for (DataValue d : this.node.guards.keySet()) {
//            
//        }
//  
    //    return branches;  
    //}

            
            
//    @Override
//    public Map<Word<PSymbolInstance>, TransitionGuard> getBranches() {
//        Map<Word<PSymbolInstance>, TransitionGuard> branches = new HashMap<>();
//        Map<DataValue[], Expression<Boolean>> psMap = collectDataValuesAndGuards(this.node, new HashMap<DataValue[],Expression<Boolean>>(), new DataValue[0], new ArrayList<SDTGuard>(), new ArrayList<Node>());
////        List<DataValue[]> psList = collectDataValues(this.node, new ArrayList<DataValue[]>(), new DataValue[0], new ArrayList<Node>());
////        for (DataValue[] d : psList) {
////            branches.put(Word.fromLetter(new PSymbolInstance(action,d)),null);
//        
//        Map<SymbolicDataValue, Variable> mapping = new HashMap<SymbolicDataValue, Variable>();
//        mapping.put(rUid, x1);
//        mapping.put(rPwd, x2);
//        mapping.put(pUid, p1);
//        mapping.put(pPwd, p2);
//                
////        }
//        for (DataValue[] d : psMap.keySet()) {
//            Word<PSymbolInstance> psWord = Word.fromLetter(new PSymbolInstance(action,d));
//            TransitionGuard t = new IfGuard(new DataExpression<Boolean>(psMap.get(d), mapping);
//                    ,psMap.get(d));
//        }
// 
//
//// for each next-node
//        
//        
//        
////        Set<Word<PSymbolInstance>> words = new HashSet<>();
////        Map<Word<PSymbolInstance>, TransitionGuard> returnMap = new HashMap<>();
////        for (DataValue d : this.node.guards.keySet()) {
////            
////        }
////  
//        return branches;  
//    }
    
    
    
    @Override
    public String toString() {
        return "---- Branching for " + action.toString() + " after " + prefix.toString() + " ----\n" + node.toString() + "\n-------------------------------------------------------------------------------------";
    }
    
}
