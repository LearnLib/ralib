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

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.theory.SDTCompoundGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheoryBranching implements Branching {

    public static class Node {

        private final boolean isLeaf;

        private final Parameter parameter;
        private final Map<DataValue, Node> next = new LinkedHashMap<>();
        private final Map<DataValue, SDTGuard> guards = new LinkedHashMap<>();

        public Node(Parameter parameter) {
            this.parameter = parameter;
            this.isLeaf = true;
        }

        public Node(Parameter parameter,
                Map<DataValue, Node> next,
                Map<DataValue, SDTGuard> guards) {
            this.parameter = parameter;
            this.next.putAll(next);
            this.guards.putAll(guards);
            this.isLeaf = false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb, "");
            return sb.toString();
        }

        void toString(StringBuilder sb, String indentation) {
            //if (this.para)
            sb.append(indentation).append("[").append(parameter).append("]").append("\n");
            indentation += "  ";
            for (Map.Entry<DataValue, Node> e : next.entrySet()) {
                DataValue d = e.getKey();
                SDTGuard g = guards.get(d);

                sb.append(indentation).append("+ ").append(g.toString()).append(" with ").append(d.toString()).append("\n");

                String nextIndent = indentation + "    ";
                e.getValue().toString(sb, nextIndent);
            }
        }

//        private Node merge(Node other) {
//            //check that their nodes are for the same parameter
//            assert this.parameter.getId() == other.parameter.getId();
//
//            if (this.isLeaf) {
//                return other;
//            } else if (other.isLeaf) {
//                return this;
//            } else {
//                System.out.println("!!!! comparing..." + this.toString() + other.toString());
//                // both are not leaves
//                Map<DataValue, Node> nextMap = new LinkedHashMap<>();
//                Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();
//
//                Map<DataValue, Node> finerNext;
//                Map<DataValue, Node> coarserNext;
//
//                Map<DataValue, SDTGuard> finerGuards;
//
//                if (this.next.size() >= other.next.size()) {
//                    finerNext = this.next;
//                    finerGuards = this.guards;
//                    coarserNext = other.next;
//
//                } else {
//                    finerNext = other.next;
//                    finerGuards = other.guards;
//                    coarserNext = this.next;
//                }
//                System.out.println("!!!! " + finerNext.toString() + " is the finest");
//                    
//                for (Map.Entry<DataValue, Node> e : finerNext.entrySet()) {
//                    SDTGuard finerGuard = 
//                    //DataValue dvi = e.getKey();
//                    Node fineNode = e.getValue();
//                    for (Map.Entry<DataValue, Node> f : coarserNext.entrySet()) {
//                        nextMap.put(dvi, fineNode.merge(f.getValue()));
//                        guardMap.put(dvi, finerGuards.get(dvi));
//                    }
//                }
//                return new Node(this.parameter, nextMap, guardMap);
//            }
//        }
//        
//         public static Node merge(List<Node> nodes) {
//             return merge(0, nodes, nodes.get(0));
//         }
//         
//         public static Node merge (int i, List<Node> nodes, Node curr) {
//            i++;
//            if (nodes.size() == i) {
//                return curr;
//            } 
//            else {
//                Node nxt = nodes.get(i);
//                Node currNxt = curr.merge(nxt);
//                return merge(i, nodes, currNxt);}
//        }
    };

    private final Word<PSymbolInstance> prefix;

    private final ParameterizedSymbol action;

    private final Node node;

    private PIV piv;

    private ParValuation pval;

    private static final LearnLogger log
            = LearnLogger.getLogger(MultiTheoryBranching.class);

    public MultiTheoryBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol action, Node node, PIV piv, ParValuation pval, SDT... sdts) {
        log.log(Level.FINEST, "ps = " + action.toString());
        this.prefix = prefix;
        this.action = action;
        this.node = node;
        this.piv = new PIV();
        if (piv != null) {
            this.piv.putAll(piv);
        }
        this.pval = pval;
    }

    public Word<PSymbolInstance> getPrefix() {
        return prefix;
    }

    public ParValuation getPval() {
        return pval;
    }

    public PIV getPiv() {
        return piv;
    }

    public Set<SDTGuard> getGuards() {
        return collectGuards(this.node, new HashSet<SDTGuard>());
    }

    // collects guards
    private Set<SDTGuard> collectGuards(Node node, Set<SDTGuard> guards) {
        if (!node.isLeaf) {
            for (Map.Entry<DataValue, SDTGuard> e : node.guards.entrySet()) {
                guards.add(e.getValue());
                collectGuards(node.next.get(e.getKey()), guards);
            }
        }
        return guards;
    }

    private Map<DataValue[], List<SDTGuard>> collectDataValuesAndGuards(
            Node n, Map<DataValue[], List<SDTGuard>> dvgMap,
            DataValue[] dvs, List<SDTGuard> guards, List<Node> visited) {
        System.out.println("!!!node: " + n.toString());
        // if we are not at a leaf
        visited.add(n);
        if (!n.next.isEmpty()) {
                // get all next nodes
                     System.out.println("!!!next dvs: " + n.next.keySet().toString());
            // go through each of the 'next' nodes
            for (DataValue d : n.next.keySet()) {
                Node nextNode = n.next.get(d);
                // if the node hasn't been visited previously 
                if (!visited.contains(nextNode)) {
                    SDTGuard nextGuard = n.guards.get(d);
                    System.out.println("!!!here is the next guard: " + nextGuard.toString());
                    // add the node's data value to the array
                    int dvLength = dvs.length;
                    DataValue[] newDvs = new DataValue[dvLength + 1];
                    System.arraycopy(dvs, 0, newDvs, 0, dvLength);
                    newDvs[dvLength] = d;
                    // add the guard to the guardlist
                    List newGuards = new ArrayList<>();
                    newGuards.addAll(guards);
                    newGuards.add(nextGuard);
                        //                  log.log(Level.FINEST,"dvs are currently " + Arrays.toString(newDvs));
                    //                log.log(Level.FINEST,"guards are currently " + newGuards.toString());
                    //              log.log(Level.FINEST,"marked: " + visited.size());
                    // proceed down in the tree to the next node
                    collectDataValuesAndGuards(nextNode, dvgMap, newDvs, newGuards, visited);

                }
            }

        }
        if (this.action.getArity() > 0 && dvs.length == this.action.getArity()) {
                //           log.log(Level.FINEST,"Just adding: " + Arrays.toString(dvs));
            //dvgMap.put(dvs, toTGList(guards,0));
            dvgMap.put(dvs, guards);
        }
        return dvgMap;
    }

    private Expression<Boolean> toPC(List<Expression<Boolean>> gList, int i) {
        if (gList.size() == i + 1) {
            return gList.get(i);
        } else {
            return new PropositionalCompound(gList.get(i), LogicalOperator.AND, toPC(gList, i + 1));
        }
    }

    private TransitionGuard toTG(Expression<Boolean> guard, Map<SymbolicDataValue, Variable> variables) {
        assert guard != null;
        DataExpression<Boolean> cond = new DataExpression<>(guard, variables);
            return new IfGuard(cond);
        
    }
        //        // 1. Create a list of expressions.
        //        List<
        //        SDTGuard sdtGuard = guardList.get(i);
        //        if (sdtGuard instanceof SDTElseGuard) {
        //            
        //        }
        //            return thisExpr;
        //        } else {
        //            return new PropositionalCompound(thisExpr, LogicalOperator.AND, makeExpr(guardList, i + 1));
        //        }
        //    }        

        //    
        //      Map<DataValue[], List<SDTGuard>> psMap = collectDataValuesAndGuards(this.node, new LinkedHashMap<DataValue[],List<SDTGuard>>(), new DataValue[0], new ArrayList<SDTGuard>(), new ArrayList<Node>());
        ////        List<DataValue[]> psList = collectDataValues(this.node, new ArrayList<DataValue[]>(), new DataValue[0], new ArrayList<Node>());
        ////        for (DataValue[] d : psList) {
        ////            branches.put(Word.fromLetter(new PSymbolInstance(action,d)),null);
        ////        }
        //        for (DataValue[] d : psMap.keySet()) {
        //            branches.put(Word.fromLetter(new PSymbolInstance(action,d)),null);
        //        }
        // for each next-node
        //        Set<Word<PSymbolInstance>> words = new HashSet<>();
        //        Map<Word<PSymbolInstance>, TransitionGuard> returnMap = new LinkedHashMap<>();
        //        for (DataValue d : this.node.guards.keySet()) {
        //            
        //        }
        //  
        //    return branches;  
        //}
        
    private Set<SymbolicDataValue> makeVarSet(SDTGuard guard) {
        Set<SymbolicDataValue> currRegsAndParams = new HashSet<>();
        currRegsAndParams.add(guard.getParameter());
        if (guard instanceof SDTCompoundGuard) {
            currRegsAndParams.addAll(((SDTCompoundGuard) guard).getAllRegs());
        } else {
            currRegsAndParams.add(((SDTIfGuard) guard).getRegister());
        }
        return currRegsAndParams;
    }

    private Set<SymbolicDataValue> collectRegsAndParams(Map<DataValue[], List<SDTGuard>> guardMap) {
        Set<SymbolicDataValue> regsAndParams = new HashSet<>();
        for (DataValue[] dvs : guardMap.keySet()) {
            for (SDTGuard guard : guardMap.get(dvs)) {
                regsAndParams.add(guard.getParameter());
                if (guard instanceof SDTIfGuard) {
                    regsAndParams.add(((SDTIfGuard) guard).getRegister());
                } //else if (guard instanceof SDTElseGuard) {
                //    regsAndParams.addAll(((SDTElseGuard) guard).getRegisters());
                //} 
                else if (guard instanceof SDTCompoundGuard) {
                    for (SDTIfGuard ifGuard : ((SDTCompoundGuard) guard).getGuards()) {
                        regsAndParams.add(ifGuard.getRegister());
                    }
                }
            }

        }
        return regsAndParams;

    }

    public Map<SymbolicDataValue, Variable> makeVarMapping(Set<SymbolicDataValue> regsAndParams) {
        Map<SymbolicDataValue, Variable> vars = new LinkedHashMap<SymbolicDataValue, Variable>();
        for (SymbolicDataValue s : regsAndParams) {
            SymbolicDataValue z = s;
            String xpre = "";
            if (s instanceof SymbolicDataValue.SuffixValue) {
                xpre = "y" + s.getId();
                z = new Parameter(s.getType(), s.getId());
            }
            if (s instanceof SymbolicDataValue.Register) {
                xpre = "x" + s.getId();
            }
//            String xname = xpre + s.getId() + "_" + s.getType().getName();
            Variable x = new Variable(BuiltinTypes.SINT32, xpre);
            vars.put(z, x);
        }
        return vars;
    }

//    private Set<Register> collectRegisters(Map<DataValue[], List<SDTGuard>> guardMap) {
//        Set<Register> regs = new HashSet<>();
//        for (DataValue[] dvs : guardMap.keySet()) {
//            for (SDTGuard guard : guardMap.get(dvs)) {
//                if (guard instanceof SDTIfGuard) {
//                    regs.add(((SDTIfGuard) guard).getRegister());
//                } else if (guard instanceof SDTElseGuard) {
//                    regs.addAll(((SDTElseGuard) guard).getRegisters());
//                } else if (guard instanceof SDTCompoundGuard) {
//                    for (SDTIfGuard ifGuard : ((SDTCompoundGuard) guard).getGuards()) {
//                        regs.add(ifGuard.getRegister());
//                    }
//                }
//            }
//
//        }
//        return regs;
//    }
//
//    private Set<SuffixValue> collectParameters(Map<DataValue[], List<SDTGuard>> guardMap) {
//        Set<SuffixValue> params = new HashSet<>();
//        for (DataValue[] dvs : guardMap.keySet()) {
//            for (SDTGuard guard : guardMap.get(dvs)) {
//                params.add(guard.getParameter());
//            }
//        }
//        return params;
//    }
    @Override
    public Map<Word<PSymbolInstance>, TransitionGuard> getBranches() {

        System.out.println("!!get branches for " + this.action.toString() + " with arity " + this.action.getArity());
        System.out.println(this.toString());

        Map<Word<PSymbolInstance>, TransitionGuard> branches = new LinkedHashMap<>();

        if (this.action.getArity() == 0) {
            System.out.println("arity 0");
            TransitionGuard tg = new IfGuard(
                    new DataExpression<Boolean>(ExpressionUtil.TRUE,
                            new LinkedHashMap<SymbolicDataValue, Variable>()));
            PSymbolInstance psi = new PSymbolInstance(action, new DataValue[0]);
            branches.put(prefix.append(psi), tg);
            return branches;
        }

        Map<DataValue[], List<SDTGuard>> tempMap
                = collectDataValuesAndGuards(
                        this.node, new LinkedHashMap<DataValue[], List<SDTGuard>>(),
                        new DataValue[0], new ArrayList<SDTGuard>(),
                        new ArrayList<Node>());
        
        System.out.println("tempMap: ------- ");
                for (DataValue[] d: tempMap.keySet()) {
                    System.out.println("dvs: " + Arrays.toString(d));
                }
        System.out.println("---- NODE ----\n" + this.node.toString() + "\n ------------");
//        List<DataValue[]> psList = collectDataValues(this.node, new ArrayList<DataValue[]>(), new DataValue[0], new ArrayList<Node>());
//        for (DataValue[] d : psList) {
//            branches.put(Word.fromLetter(new PSymbolInstance(action,d)),null);
//        
        Set<SymbolicDataValue> regsAndParams = collectRegsAndParams(tempMap);

        Map<SymbolicDataValue, Variable> vars = makeVarMapping(regsAndParams);
//            new LinkedHashMap<SymbolicDataValue, Variable>();
//            for (SymbolicDataValue s : regsAndParams) {
//                SymbolicDataValue z = s;
//                String xpre = "";
//                if (s instanceof SuffixValue) {
//                    xpre = "y" + s.getId();
//                    z = new Parameter(s.getType(), s.getId());
//                }
//                if (s instanceof Register) {
//                    xpre = "x" + s.getId();
//                }
////            String xname = xpre + s.getId() + "_" + s.getType().getName();
//                Variable x = new Variable(BuiltinTypes.SINT32, xpre);
//                vars.put(z, x);
//            }

            log.log(Level.FINEST, "Vars =     " + vars.toString());
            IfGuard check = null;
            
            for (DataValue[] dvs : tempMap.keySet()) {
                System.out.println("!!!! current data value array is: " + Arrays.toString(dvs));
                List<Expression<Boolean>> gExpr = new ArrayList<>();
                List<SDTGuard> gList = tempMap.get(dvs);
                for (SDTGuard g : gList) {
                gExpr.add(g.toExpr());
            }
                //Word<PSymbolInstance> psWord = Word.fromLetter(new PSymbolInstance(action, dvs));
                //log.log(Level.FINEST,"psWord = " + psWord.toString());
                TransitionGuard tg = toTG(toPC(gExpr, 0), vars);
                assert tg != null;
                check = (IfGuard) tg;
                
                Word<PSymbolInstance> branch = prefix.append(new PSymbolInstance(action, dvs));
                System.out.println("!!!the branch is " + branch.toString());
                branches.put(branch, tg);
                //log.log(Level.FINEST,"guard: " + ((IfGuard)tg).toString());
                System.out.println("G: " + tg);
            }
            
            assert !branches.isEmpty();

            assert branches.size() > 1 || check.toString().contains("[true]");
            
            System.out.println(branches.toString());
            return branches;
        }

    @Override
    public String toString() {
        return "---- Branching for " + action.toString() + " after " + prefix.toString() + " ----\n" + node.toString() + "\n-------------------------------------------------------------------------------------";
    }

}
