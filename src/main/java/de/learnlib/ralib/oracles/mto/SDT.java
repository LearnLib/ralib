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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.FalseGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.inequality.IntervalGuard;

/**
 * Implementation of Symbolic Decision Trees.
 *
 * @author Sofia Cassel
 */
public class SDT implements SymbolicDecisionTree {

    private final Map<SDTGuard, SDT> children;

    public SDT(Map<SDTGuard, SDT> children) {
        this.children = children;
    }

//    public Set<SDTGuard> getGuards() {
//        if (this instanceof SDTLeaf) {
//            return new LinkedHashSet<>();
//        }
//        Set<SDTGuard> guards = new LinkedHashSet<>();
//        for (Map.Entry<SDTGuard, SDT> e : this.children.entrySet()) {
//            guards.add(e.getKey());
//            if (!(e.getValue() instanceof SDTLeaf)) {
//                guards.addAll(e.getValue().getGuards());
//            }
//        }
//        return guards;
//    }

    /**
     * Returns the registers of this SDT.
     *
     * @return
     */
    Set<Register> getRegisters() {
        Set<Register> registers = new LinkedHashSet<>();
        this.getVariables().stream().filter((x) -> (x.isRegister())).forEach((x) -> {
            registers.add((Register) x);
        });
        return registers;
    }

    public int getHeight() {
        if (this instanceof SDTLeaf || children.size() == 0) {
            return 0;
        } else {
            return children.values().stream().map(c -> c.getHeight()).max( (i1, i2) -> i1.compareTo(i2)).get() + 1;
        }
    }

    public Set<SymbolicDataValue> getVariables() {
        Set<SymbolicDataValue> variables = new LinkedHashSet<>();
        for (Entry<SDTGuard, SDT> e : children.entrySet()) {
            SDTGuard g = e.getKey();
            if (g instanceof SDTIfGuard) {
                SymbolicDataValue r = ((SDTIfGuard) g).getRegister();
                variables.add(r);
            } else if (g instanceof SDTMultiGuard) {
                for (SDTGuard ifG : ((SDTMultiGuard) g).getGuards()) {
                    if (ifG instanceof SDTIfGuard) {
                        SymbolicDataValue ifr = ((SDTIfGuard) ifG).getRegister();
                        variables.add(ifr);
                    } else if (ifG instanceof SDTMultiGuard) {
                        Set<SymbolicDataValue> rSet = ((SDTMultiGuard) ifG).getAllRegs();
                        variables.addAll(rSet);
                    }
                }
            } else if (g instanceof IntervalGuard) {
                IntervalGuard iGuard = (IntervalGuard) g;
                if (!iGuard.isBiggerGuard()) {
                    variables.add(iGuard.getRightReg());
                }
                if (!iGuard.isSmallerGuard()) {
                    variables.add(iGuard.getLeftReg());
                }
            } else if (!(g instanceof SDTTrueGuard)) {
                throw new RuntimeException("unexpected case");
            }
            SDT child = e.getValue();
            // FIXME: this is bad style: it will break if the tree is only a leaf! I added code to SDTLeaf to prevent this
            if (child.getChildren() != null) {
                variables.addAll(child.getVariables());
            }
        }
        return variables;
    }

    @Override
    public boolean isAccepting() {
        if (this instanceof SDTLeaf) {
            return ((SDTLeaf) this).isAccepting();
        } else {
            for (Map.Entry<SDTGuard, SDT> e : children.entrySet()) {
                if (!e.getValue().isAccepting()) {
                    return false;
                }
            }
        }

        return true;
        //return false;
    }

    public boolean isAccepting(Mapping<SymbolicDataValue, DataValue<?>> vals, Constants consts) {
    	Mapping<SymbolicDataValue, DataValue<?>> mapping = new Mapping<SymbolicDataValue, DataValue<?>>();
    	mapping.putAll(vals);
    	mapping.putAll(consts);
    	GuardExpression expr = getAcceptingPaths(consts);
    	return expr.isSatisfied(mapping);
    }

    protected Map<SDTGuard, SDT> getChildren() {
        return this.children;
    }

    @Override
    public boolean isEquivalent(
            SymbolicDecisionTree other, VarMapping renaming) {
        if (other instanceof SDTLeaf) {
            return false;
        }
        SDT otherSDT = (SDT) other;
        SDT otherRelabeled = (SDT) otherSDT.relabel(renaming);
        boolean regEq = this.regCanUse(otherSDT) && otherSDT.regCanUse(this);
        return regEq && this.canUse(otherRelabeled)
                && otherRelabeled.canUse(this);
    }

    public boolean isEquivalentUnder(
            SymbolicDecisionTree deqSDT, List<SDTIfGuard> ds) {
        if (deqSDT instanceof SDTLeaf) {
            if (this instanceof SDTLeaf) {
                return (this.isAccepting() == deqSDT.isAccepting());
            }
            return false;
        }
        VarMapping eqRenaming = new VarMapping<>();
        for (SDTIfGuard d : ds) {
            eqRenaming.put(d.getParameter(), d.getRegister());
        }
//        System.out.println(eqRenaming);
//        System.out.println(this + " vs " + deqSDT);
        boolean x = this.canUse((SDT) deqSDT.relabel(eqRenaming));
        return x;
    }

    public SDT relabelUnderEq(List<SDTIfGuard> ds) {
        VarMapping eqRenaming = new VarMapping<>();
        for (SDTIfGuard d : ds) {
            eqRenaming.put(d.getParameter(), d.getRegister());
        }
        return (SDT) this.relabel(eqRenaming);
    }

    @Override
    public SymbolicDecisionTree relabel(VarMapping relabelling) {
        //System.out.println("relabeling " + relabelling);
        SDT thisSdt = this;
        if (relabelling.isEmpty()) {
            return this;
        }

        Map<SDTGuard, SDT> reChildren = new LinkedHashMap<>();
        // for each of the kids
        for (Entry<SDTGuard, SDT> e : thisSdt.children.entrySet()) {
                reChildren.put(e.getKey().relabel(relabelling),
                    (SDT) e.getValue().relabel(relabelling));
            }
        SDT relabelled = new SDT(reChildren);
        assert !relabelled.isEmpty();
        return relabelled;
    }


    /* ***
     *
     * Logging helpers
     *
     */

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String regs = Arrays.toString(getRegisters().toArray());
        sb.append(regs).append("-+\n");
        toString(sb, spaces(regs.length()));
        return sb.toString();
    }

    void toString(StringBuilder sb, String indentation) {
        SDT idioticSdt = this;
        sb.append(indentation).append("[]");
        final int childCount = idioticSdt.children.size();
        int count = 1;
        for (Entry<SDTGuard, SDT> e : idioticSdt.children.entrySet()) {
            SDTGuard g = e.getKey();
            String gString = g.toString();
            String nextIndent;
            if (count == childCount) {
                nextIndent = indentation + "      ";
            } else {
                nextIndent = indentation + " |    ";
            }

            if (count > 1) {
                sb.append(indentation).append(" +");
            }
            sb.append("-").append(gString).append("\n");
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

    // Returns true if all elements of a boolean array are true.
    private boolean isArrayTrue(Boolean[] maybeArr) {
        boolean maybe = true;
        for (int c = 0; c < (maybeArr.length); c++) {
            //log.log(Level.FINEST,maybeArr[c]);
            if (!maybeArr[c]) {
                maybe = false;
                break;
            }
        }
        return maybe;
    }

    // Returns true if this SDT can use the registers of another SDT.  Registers
    // are matched by name (no remapping)
    private boolean regCanUse(SDT other) {

        Set<Register> otherRegisters = other.getRegisters();
        Set<Register> thisRegisters = this.getRegisters();

        if (otherRegisters.isEmpty() && thisRegisters.isEmpty()) {
            return true;
        } else {
            Boolean[] regEqArr = new Boolean[thisRegisters.size()];
            Integer i = 0;
            for (SymbolicDataValue thisReg : thisRegisters) {
                // if the trees have the same type and size
                regEqArr[i] = false;
                for (SymbolicDataValue otherReg : otherRegisters) {
                    if (thisReg.equals(otherReg)) {
                        regEqArr[i] = true;
                        break;
                    }
                }
                i++;
            }
            return isArrayTrue(regEqArr);
        }
    }

    private boolean hasPair(
            SDTGuard thisGuard, SDT thisSdt, Map<SDTGuard, SDT> otherBranches) {
        for (Map.Entry<SDTGuard, SDT> otherB : otherBranches.entrySet()) {
            if (thisGuard.equals(otherB.getKey())) {
                if (thisSdt.canUse(otherB.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canPairBranches(
            Map<SDTGuard, SDT> thisBranches, Map<SDTGuard, SDT> otherBranches) {
        if (thisBranches.size() != otherBranches.size()) {
            return false;
        }
        Boolean[] pairedArray = new Boolean[thisBranches.size()];
        Integer i = 0;
        for (Map.Entry<SDTGuard, SDT> thisB : thisBranches.entrySet()) {
            pairedArray[i] = hasPair(
                    thisB.getKey(), thisB.getValue(), otherBranches);
            i++;
        }
        return isArrayTrue(pairedArray);

    }

    public boolean canUse(SDT other) {
        SDT thisSdt = this;
        if (other instanceof SDTLeaf) { // trees with incompatible sizes can't use each other
            return false;
        } else {
            boolean accEq = (thisSdt.isAccepting() == other.isAccepting());
            boolean chiEq = canPairBranches(thisSdt.getChildren(),
                    ((SDT) other).getChildren());
            return accEq && chiEq;
        }
    }

    public boolean isEmpty() {
        return this.getChildren().isEmpty();
    }

    GuardExpression getAcceptingPaths(Constants consts) {

        List<List<SDTGuard>> paths = getPaths(new ArrayList<SDTGuard>());
        if (paths.isEmpty()) {
            return FalseGuardExpression.FALSE;
        }
        Set<SuffixValue> svals = new LinkedHashSet<>();
        GuardExpression dis = null;
        for (List<SDTGuard> list : paths) {
            List<GuardExpression> expr = new ArrayList<>();
            for (SDTGuard g : list) {
                expr.add(g.toExpr());
                svals.add(g.getParameter());
            }
            Conjunction con = new Conjunction(
                    expr.toArray(new GuardExpression[] {}));

            dis = (dis == null) ? con : new Disjunction(dis, con);
        }

        return dis;
    }

    GuardExpression getPaths(Constants consts) {

        List<List<SDTGuard>> paths = getPaths(new ArrayList<SDTGuard>());
        if (paths.isEmpty()) {
            return FalseGuardExpression.FALSE;
        }
        Set<SuffixValue> svals = new LinkedHashSet<>();
        GuardExpression dis = null;
        for (List<SDTGuard> list : paths) {
            List<GuardExpression> expr = new ArrayList<>();
            for (SDTGuard g : list) {
                expr.add(g.toExpr());
                svals.add(g.getParameter());
            }
            Conjunction con = new Conjunction(
                    expr.toArray(new GuardExpression[] {}));

            dis = (dis == null) ? con : new Disjunction(dis, con);
        }

        return dis;
    }

    Map<GuardExpression, Boolean> getGuardExpressions(Constants consts) {
    	Map<GuardExpression, Boolean> expressions = new LinkedHashMap<>();
    	Map<List<SDTGuard>, Boolean> paths = getAllPaths(new ArrayList<SDTGuard>());
    	if (paths.isEmpty()) {
    		expressions.put(FalseGuardExpression.FALSE, false);
    		return expressions;
    	}
    	Set<SuffixValue> svals = new LinkedHashSet<>();
    	for (Map.Entry<List<SDTGuard>, Boolean> e : paths.entrySet()) {
    		List<SDTGuard> list = e.getKey();
    		List<GuardExpression> expr = new ArrayList<>();
    		for (SDTGuard g : list) {
    			expr.add(g.toExpr());
    			svals.add(g.getParameter());
    		}
    		Conjunction con = new Conjunction(
    				expr.toArray(new GuardExpression[] {}));
    		expressions.put(con, e.getValue());
    	}

    	return expressions;
    }

    List<List<SDTGuard>> getPaths(List<SDTGuard> path) {
        List<List<SDTGuard>> ret = new ArrayList<>();
        for (Entry<SDTGuard, SDT> e : this.children.entrySet()) {
            List<SDTGuard> nextPath = new ArrayList<>(path);
            nextPath.add(e.getKey());
            List<List<SDTGuard>> nextRet = e.getValue().getPaths(nextPath);
            ret.addAll(nextRet);
        }

        return ret;
    }


    List<List<SDTGuard>> getPaths(boolean accepting) {
        List<List<SDTGuard>> collectedPaths = new ArrayList<List<SDTGuard>>();
        getPaths(accepting, new ArrayList<>(), this, collectedPaths);
        return collectedPaths;
    }

    private void getPaths(boolean accepting, List<SDTGuard> path, SDT sdt, List<List<SDTGuard>> collectedPaths) {
        if (sdt instanceof SDTLeaf) {
            if (sdt.isAccepting() == accepting) {
                collectedPaths.add(path);
            }
        } else {
            for (Entry<SDTGuard, SDT> e : sdt.children.entrySet()) {
                List<SDTGuard> nextPath = new ArrayList<>(path);
                nextPath.add(e.getKey());
                SDT nextSdt = e.getValue();
                getPaths(accepting, nextPath, nextSdt, collectedPaths);
            }
        }
    }

    Map<List<SDTGuard>, Boolean> getAllPaths(List<SDTGuard> path) {
        Map<List<SDTGuard>, Boolean> ret = new LinkedHashMap<>();
        for (Entry<SDTGuard, SDT> e : this.children.entrySet()) {
            List<SDTGuard> nextPath = new ArrayList<>(path);
            nextPath.add(e.getKey());
            Map<List<SDTGuard>, Boolean> nextRet = e.getValue().getAllPaths(nextPath);
            ret.putAll(nextRet);
        }

        return ret;
    }

    public static SDT getFinest(SDT... sdts) {
        return findFinest(0, Arrays.asList(sdts), sdts[0]);
    }

    private static SDT findFinest(int i, List<SDT> sdts, SDT curr) {
        i++;
        if (sdts.size() == i) {
            return curr;
        } else {
            SDT nxt = sdts.get(i);
            if (curr.getChildren().size() >= nxt.getChildren().size()) {
                return findFinest(i, sdts, curr);
            } else {
                return findFinest(i, sdts, nxt);
            }
        }
    }

	@Override
	public SDT copy() {
		Map<SDTGuard, SDT> cc = new LinkedHashMap<>();
		if (children != null) {
			for (Map.Entry<SDTGuard, SDT> e : children.entrySet()) {
				cc.put(e.getKey().copy(), e.getValue().copy());
			}
		}
		return new SDT(cc);
	}

}
