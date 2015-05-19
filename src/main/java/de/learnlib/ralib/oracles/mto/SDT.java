/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.mto;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Sofia Cassel
 */
public class SDT implements SymbolicDecisionTree {

    private final Map<SDTGuard, SDT> children;

    private static final LearnLogger log = LearnLogger.getLogger(SDT.class);

    public SDT(Map<SDTGuard, SDT> children) {
        this.children = children;
    }

    public Set<SDTGuard> getGuards() {
        if (this instanceof SDTLeaf) {
            return new LinkedHashSet<>();
        }
        Set<SDTGuard> guards = new LinkedHashSet<>();
        for (Map.Entry<SDTGuard, SDT> e : this.children.entrySet()) {
            guards.add(e.getKey());
            if (!(e.getValue() instanceof SDTLeaf)) {
                guards.addAll(e.getValue().getGuards());
            }
        }
        return guards;
    }

    @Override
    public Set<Register> getRegisters() {
        Set<Register> registers = new LinkedHashSet<>();
        for (SymbolicDataValue x : this.getVariables()) {
            if (x.isRegister()) {
                registers.add((Register) x);
            }
        }
        return registers;
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

    protected Map<SDTGuard, SDT> getChildren() {
        return this.children;
    }

    @Override
    public boolean isEquivalent(
            SymbolicDecisionTree other, VarMapping renaming) {
        if (!(other instanceof SDT)) {
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
        boolean x = this.canUse((SDT) deqSDT.relabel(eqRenaming));
        return x;
    }

    @Override
    public SymbolicDecisionTree relabel(VarMapping relabelling) {

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

    DataExpression<Boolean> getAcceptingPaths(Constants consts) {

        List<List<SDTGuard>> paths = getPaths(new ArrayList<SDTGuard>());
        if (paths.isEmpty()) {
            return DataExpression.FALSE;
        }
        Set<SuffixValue> svals = new LinkedHashSet<>();
        Expression<Boolean> dis = null;
        for (List<SDTGuard> list : paths) {
            List<Expression<Boolean>> expr = new ArrayList<>();
            for (SDTGuard g : list) {
                expr.add(g.toExpr().toDataExpression().getExpression());
                svals.add(g.getParameter());
            }
            Expression<Boolean> con = ExpressionUtil.and(expr);
            dis = (dis == null) ? con : ExpressionUtil.or(dis, con);
        }

        Map<SymbolicDataValue, Variable> map = new LinkedHashMap<>();
        for (Register r : getRegisters()) {
            Variable x = new Variable(BuiltinTypes.DOUBLE, r.toString());
            map.put(r, x);
        }
        for (SuffixValue s : svals) {
            Variable p = new Variable(BuiltinTypes.DOUBLE, s.toString());
            map.put(s, p);
        }
        for (Constant c : consts.keySet()) {
            Variable _c = new Variable(BuiltinTypes.DOUBLE, c.toString());
            map.put(c, _c);
        }

        return new DataExpression<>(dis, map);
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

}
