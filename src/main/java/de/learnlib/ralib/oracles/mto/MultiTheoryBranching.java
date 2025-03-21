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
package de.learnlib.ralib.oracles.mto;

import java.util.*;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.smt.SMTUtil;
import de.learnlib.ralib.theory.Memorables;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTLeaf;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class MultiTheoryBranching implements Branching {

    public static class Node {

        private final boolean isLeaf;

        private final SuffixValue parameter;
        private final Map<DataValue, Node> next = new LinkedHashMap<>();
        private final Map<DataValue, SDTGuard> guards = new LinkedHashMap<>();

        public Node(SuffixValue parameter) {
            this.parameter = parameter;
            this.isLeaf = true;
        }

        public Node(SuffixValue parameter, Map<DataValue, Node> next, Map<DataValue, SDTGuard> guards) {
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
            sb.append(indentation).append("[").append(parameter).append("]").append("\n");
            indentation += "  ";
            for (Map.Entry<DataValue, Node> e : next.entrySet()) {
                DataValue d = e.getKey();
                SDTGuard g = guards.get(d);

                sb.append(indentation).append("+ ").append(g.toString()).append(" with ").append(d.toString())
                        .append("\n");

                String nextIndent = indentation + "    ";
                e.getValue().toString(sb, nextIndent);
            }
        }

        protected Map<SuffixValue, Set<DataValue>> collectDVs() {
            Map<SuffixValue, Set<DataValue>> dvs = new LinkedHashMap();
            if (!(this.next.keySet()).isEmpty()) {
                dvs.put(this.parameter, this.next.keySet());
                for (Map.Entry<DataValue, Node> e : this.next.entrySet()) {
                    dvs.putAll(e.getValue().collectDVs());
                }
            }
            return dvs;
        }

        protected Map<SuffixValue, Set<SDTGuard>> collectGuards() {
            Map<SuffixValue, Set<SDTGuard>> guards = new LinkedHashMap();
            if (!(this.next.keySet()).isEmpty()) {
                guards.put(this.parameter, new LinkedHashSet<SDTGuard>(this.guards.values()));
                for (Map.Entry<DataValue, Node> e : this.next.entrySet()) {
                    guards.putAll(e.getValue().collectGuards());
                }
            }
            return guards;
        }

        protected SDT buildFakeSDT() {
            if (!(this.next.keySet()).isEmpty()) {
                Map<SDTGuard, SDT> map = new LinkedHashMap();
                for (Map.Entry<DataValue, Node> e : this.next.entrySet()) {
                    SDTGuard guard = guards.get(e.getKey());
                    map.put(guard, e.getValue().buildFakeSDT());
                }
                return new SDT(map);
            }
            return SDTLeaf.REJECTING;
        }
    }

    private final Word<PSymbolInstance> prefix;

    private final ParameterizedSymbol action;

    private final Node node;

    private final Constants constants;

    private final SDT[] sdts;

    public MultiTheoryBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol action,
                                Node node, Constants constants, SDT... sdts) {
        this.prefix = prefix;
        this.action = action;
        this.node = node;
        this.constants = constants;
        this.sdts = sdts;
    }

    public Word<PSymbolInstance> getPrefix() {
        return prefix;
    }

    public Map<SuffixValue, Set<DataValue>> getDVs() {
        return this.node.collectDVs();
    }

    public Map<SuffixValue, Set<SDTGuard>> getParamGuards() {
        return node.collectGuards();
    }

    public SDT buildFakeSDT() {
        return node.buildFakeSDT();
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

    private Map<DataValue[], List<SDTGuard>> collectDataValuesAndGuards(Node n, Map<DataValue[], List<SDTGuard>> dvgMap,
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
                    collectDataValuesAndGuards(nextNode, dvgMap, newDvs, newGuards, visited);

                }
            }

        }
        if (this.action.getArity() > 0 && dvs.length == this.action.getArity()) {
            dvgMap.put(dvs, guards);
        }
        return dvgMap;
    }

    @Override
    public Map<Word<PSymbolInstance>, Expression<Boolean>> getBranches() {

        Map<Word<PSymbolInstance>, Expression<Boolean>> branches = new LinkedHashMap<>();

        if (this.action.getArity() == 0) {
            // System.out.println("arity 0");
            PSymbolInstance psi = new PSymbolInstance(action);
            branches.put(prefix.append(psi), ExpressionUtil.TRUE);
            return branches;
        }

        Map<DataValue[], List<SDTGuard>> tempMap = collectDataValuesAndGuards(this.node,
                new LinkedHashMap<DataValue[], List<SDTGuard>>(), new DataValue[0], new ArrayList<SDTGuard>(),
                new ArrayList<Node>());

        for (Map.Entry <DataValue[], List<SDTGuard>> entry : tempMap.entrySet()) {
            List<Expression<Boolean> > gExpr = new ArrayList<>();
            List<SDTGuard> gList = entry.getValue();
            for (SDTGuard g : gList) {
                gExpr.add(renameSuffixValues(SDTGuard.toExpr(g)));
            }
            Expression<Boolean> tg = ExpressionUtil.and(
                    gExpr.stream().toList().toArray(new Expression[]{})
            );
            assert tg != null;

            Word<PSymbolInstance> branch = prefix.append(new PSymbolInstance(action, entry.getKey()));
            branches.put(branch, tg);
        }

        assert !branches.isEmpty();

        return branches;
    }

    @Override
    public Word<PSymbolInstance> transformPrefix(Word<PSymbolInstance> dw) {

    	Set<SDTGuard> guardSet = getGuards();
    	Set<SuffixValue> paramSet = new LinkedHashSet<SuffixValue>();
    	for (SDTGuard g : guardSet) {
    		paramSet.add(g.getParameter());
    	}
    	DataValue[] dwParamValues = dw.lastSymbol().getParameterValues();
    	SuffixValue[] params = new SuffixValue[paramSet.size()];
    	paramSet.toArray(params);
    	ParameterValuation vals = new ParameterValuation();
    	for (int i=0; i<paramSet.size(); i++) {
    		DataValue dv = dwParamValues[params[i].getId()-1];
    		SuffixValue s = params[i];
    		Parameter p = new Parameter(s.getDataType(), s.getId());
    		vals.put(p, dv);
    	}
    	RegisterValuation vars = Memorables.getAssignment(this.sdts).registerValuation();
    	Map<Word<PSymbolInstance>, Expression<Boolean>> branches = getBranches();

    	Word<PSymbolInstance> prefix = null;
    	for (Map.Entry<Word<PSymbolInstance>,  Expression<Boolean>> e : branches.entrySet()) {
            Expression<Boolean> g = e.getValue();
    		if (g.evaluateSMT(SMTUtil.compose(vars, vals, constants))) {
    			prefix = e.getKey();
    			break;
    		}
    	}

    	return prefix;
    }

    private Expression<Boolean>  renameSuffixValues(Expression<Boolean>  expr) {
        Collection<SymbolicDataValue> svals = SMTUtil.getSymbolicDataValues(expr);
        VarMapping vmap = new VarMapping();
        for (SymbolicDataValue sv : svals) {
            if (sv instanceof SuffixValue) {
                vmap.put(sv, new Parameter(sv.getDataType(), sv.getId()));
            }
        }
        return SMTUtil.renameVars(expr, vmap);
    }

    @Override
    public String toString() {
        return "---- Branching for " + action.toString() + " after " + prefix.toString() + " ----\n" + node.toString()
                + "\n-------------------------------------------------------------------------------------";
    }

}
