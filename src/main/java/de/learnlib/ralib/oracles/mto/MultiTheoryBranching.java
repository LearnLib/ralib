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
import de.learnlib.ralib.automata.guards.Conjuction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
            sb.append(indentation).append("[").
                    append(parameter).append("]").append("\n");
            indentation += "  ";
            for (Map.Entry<DataValue, Node> e : next.entrySet()) {
                DataValue d = e.getKey();
                SDTGuard g = guards.get(d);

                sb.append(indentation).append("+ ").append(g.toString()).
                        append(" with ").append(d.toString()).append("\n");

                String nextIndent = indentation + "    ";
                e.getValue().toString(sb, nextIndent);
            }
        }
        
        
        protected Map<Parameter, Set<DataValue>> collectDVs() {
        Map<Parameter, Set<DataValue>> dvs = new LinkedHashMap();
        if (!(this.next.keySet()).isEmpty()) {
            dvs.put(this.parameter, this.next.keySet());
            for (Map.Entry<DataValue, Node> e : this.next.entrySet()) {
                dvs.putAll(e.getValue().collectDVs());
            }
        }
        return dvs;
    }

    };

    private final Word<PSymbolInstance> prefix;

    private final ParameterizedSymbol action;

    private final Node node;

    private PIV piv;

    private Constants constants;

    private ParValuation pval;

    private static final LearnLogger log
            = LearnLogger.getLogger(MultiTheoryBranching.class);

    public MultiTheoryBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol action, Node node, PIV piv,
            ParValuation pval, Constants constants, SDT... sdts) {
        log.log(Level.FINEST, "ps = " + action.toString());
        this.prefix = prefix;
        this.action = action;
        this.node = node;
        this.piv = new PIV();
        this.constants = constants;
        if (piv != null) {
            this.piv.putAll(piv);
        }
        this.pval = pval;
    }

    public Word<PSymbolInstance> getPrefix() {
        return prefix;
    }
    
    public Map<Parameter, Set<DataValue>> getDVs() {
    return this.node.collectDVs();
}

    public ParValuation getPval() {
        return pval;
    }

    public PIV getPiv() {
        return piv;
    }

    public Set<SDTGuard> getGuards() {
        return collectGuards(this.node, new LinkedHashSet<SDTGuard>());
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
        // if we are not at a leaf
        visited.add(n);
        if (!n.next.isEmpty()) {
            // go through each of the 'next' nodes
            for (DataValue d : n.next.keySet()) {
                Node nextNode = n.next.get(d);
                // if the node hasn't been visited previously 
                if (!visited.contains(nextNode)) {
                    SDTGuard nextGuard = n.guards.get(d);
                    // add the node's data value to the array
                    int dvLength = dvs.length;
                    DataValue[] newDvs = new DataValue[dvLength + 1];
                    System.arraycopy(dvs, 0, newDvs, 0, dvLength);
                    newDvs[dvLength] = d;
                    // add the guard to the guardlist
                    List newGuards = new ArrayList<>();
                    newGuards.addAll(guards);
                    newGuards.add(nextGuard);
                    // proceed down in the tree to the next node
                    collectDataValuesAndGuards(nextNode, dvgMap, newDvs,
                            newGuards, visited);

                }
            }

        }
        if (this.action.getArity() > 0
                && dvs.length == this.action.getArity()) {
            dvgMap.put(dvs, guards);
        }
        return dvgMap;
    }

    public Map<SymbolicDataValue, Variable> makeVarMapping(
            Map<DataValue[], List<SDTGuard>> guardMap) {
        Map<SymbolicDataValue, Variable> vars
                = new LinkedHashMap<SymbolicDataValue, Variable>();
        for (Map.Entry<DataValue[], List<SDTGuard>> e : guardMap.entrySet()) {
            for (SDTGuard guard : e.getValue()) {
                SuffixValue ps = guard.getParameter();
                Parameter p = new Parameter(ps.getType(), ps.getId());
                Variable px = ps.toVariable();
                vars.put(p, px);
                if (guard instanceof SDTIfGuard) {
                    SymbolicDataValue s = ((SDTIfGuard) guard).getRegister();
                    Variable sx = s.toVariable();
                    vars.put(s, sx);
                } else if (guard instanceof SDTMultiGuard) {
                    for (SymbolicDataValue z
                            : ((SDTMultiGuard) guard).getAllRegs()) {
                        Variable zx = z.toVariable();
                        vars.put(z, zx);
                    }
                }
            }
        }
        return vars;
    }

    @Override
    public Map<Word<PSymbolInstance>, TransitionGuard> getBranches() {

        Map<Word<PSymbolInstance>, TransitionGuard> branches
                = new LinkedHashMap<>();

        if (this.action.getArity() == 0) {
            //System.out.println("arity 0");
            TransitionGuard tg = new TransitionGuard();
            PSymbolInstance psi = new PSymbolInstance(action, new DataValue[0]);
            branches.put(prefix.append(psi), tg);
            return branches;
        }

        Map<DataValue[], List<SDTGuard>> tempMap
                = collectDataValuesAndGuards(
                        this.node,
                        new LinkedHashMap<DataValue[], List<SDTGuard>>(),
                        new DataValue[0],
                        new ArrayList<SDTGuard>(),
                        new ArrayList<Node>());

        Map<SymbolicDataValue, Variable> vars = makeVarMapping(tempMap);

        log.log(Level.FINEST, "Vars =     " + vars.toString());

        for (DataValue[] dvs : tempMap.keySet()) {
            List<GuardExpression> gExpr = new ArrayList<>();
            List<SDTGuard> gList = tempMap.get(dvs);
            for (SDTGuard g : gList) {
                gExpr.add(renameSuffixValues(g.toExpr()));
            }
            TransitionGuard tg = new TransitionGuard(
                    new Conjuction(gExpr.toArray(
                                    new GuardExpression[]{})));
            assert tg != null;

            Word<PSymbolInstance> branch = prefix.append(
                    new PSymbolInstance(action, dvs));
            branches.put(branch, tg);
        }

        assert !branches.isEmpty();

        return branches;
    }

    private GuardExpression renameSuffixValues(GuardExpression expr) {
        Set<SymbolicDataValue> svals = expr.getSymbolicDataValues();
        VarMapping vmap = new VarMapping();
        for (SymbolicDataValue sv : svals) {
            if (sv instanceof SuffixValue) {
                vmap.put(sv, new Parameter(sv.getType(), sv.getId()));
            }
        }
        return expr.relabel(vmap);
    }

    @Override
    public String toString() {
        return "---- Branching for " + action.toString()
                + " after " + prefix.toString() + " ----\n"
                + node.toString()
                + "\n-------------------------------------------------------------------------------------";
    }

}
