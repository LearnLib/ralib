/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.mto;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.theory.SDTCompoundGuard;
import de.learnlib.ralib.theory.SDTElseGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

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

    @Override
    public Set<Register> getRegisters() {
        Set<Register> registers = new HashSet<>();
        for (Entry<SDTGuard, SDT> e : children.entrySet()) {
//            log.log(Level.FINEST,e.getKey().toString() + " " + e.getValue().toString());

            SDTGuard g = e.getKey();
            if (g instanceof SDTIfGuard) {
                SymbolicDataValue r = ((SDTIfGuard) g).getRegister();
                if (r instanceof Register) {
                    registers.add((Register) r);
                }
            } else if (g instanceof SDTCompoundGuard) {
                for (SDTIfGuard ifG : ((SDTCompoundGuard) g).getGuards()) {
                    SymbolicDataValue ifr = ((SDTIfGuard) ifG).getRegister();
                    if (ifr instanceof Register) {
                        registers.add((Register) ifr);
                    }
                }
            } else if (g instanceof SDTElseGuard) {
                registers.addAll(((SDTElseGuard) g).getRegisters());
            } else {
                throw new RuntimeException("unexpected case");
            }
            SDT child = e.getValue();
            // FIXME: this is bad style: it will break if the tree is only a leaf! I added code to SDTLeaf to prevent this 
            if (child.getChildren() != null) {
                //    Set<Register> chiRegs = child.getRegisters
                registers.addAll(child.getRegisters());
            }
        }
        
        return registers;
    }
    
//    public SDT truify() {
//        for (Entry<SDTGuard,SDT> e : children.entrySet()) {
//            if (g.isEmpty)
//        }
//}

    @Override
    public boolean isAccepting() {
        if (this instanceof SDTLeaf) {
        return ((SDTLeaf)this).isAccepting();
        }
        else {
            System.out.println("HEY KIDS!!" + this.children.keySet());
            assert !this.children.isEmpty();
        for (SDT child : children.values()) {
            if (!child.isAccepting()) {
                return false;
            }
        }
        }

        return true;
    }

    protected Map<SDTGuard, SDT> getChildren() {
        return this.children;
    }

    @Override
    public boolean isEquivalent(SymbolicDecisionTree other, VarMapping renaming) {
        if (!(other instanceof SDT)) {
            return false;
        }
        SDT otherSDT = (SDT) other;
        return this.canUse(otherSDT) && otherSDT.canUse(this);
    }

    @Override
    public SymbolicDecisionTree relabel(VarMapping relabelling) {
        if (relabelling.isEmpty()) {
            return this;
        }

        //log.log(Level.FINEST,"RELABEL: " + relabelling);        
        Map<SDTGuard, SDT> reChildren = new HashMap<>();
        for (Entry<SDTGuard, SDT> e : children.entrySet()) {
            reChildren.put(e.getKey().relabel(relabelling),
                    (SDT) e.getValue().relabel(relabelling));
        }
        SDT relabelled = new SDT(reChildren);
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
        sb.append(indentation).append("[").append(isAccepting() ? "+" : "-").append("]");
        final int childCount = children.size();
        int count = 1;
        for (Entry<SDTGuard, SDT> e : children.entrySet()) {
            SDTGuard g = e.getKey();
            String gString = g.toString();
            //TODO: replace lists of guards by guards
//            if (gString.length() < 3) {
//                if (g instanceof SDTCompoundGuard) {
//                gString = "[else]";
//                }
//                else {
//                    gString = "[uncaptured]";
//                }
//            }
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

    // Method in progress.    
//    @Override
//    public SymbolicDecisionTree createCopy(VarMapping<SymbolicDataValue, SymbolicDataValue> renaming) {
//        boolean acc = this.isAccepting();
//        // registers: add the ones that are mapped
//        Set<SymbolicDataValue> newRegs = new HashSet<>();
//        for (SymbolicDataValue reg : (Set<SymbolicDataValue>) this.getRegisters()) {
//            newRegs.add(renaming.get(reg));
//        }
//        // children (map from guards to trees): change the guards
//        Map<Guard, SymbolicDecisionTree> newChildren = new HashMap<>();
//        Map<Guard, SymbolicDecisionTree> currChildren = this.getChildren();
//        for (Guard guard : (Set<Guard>) currChildren.keySet()) {
//            newChildren.put(guard.createCopy(renaming), currChildren.get(guard).createCopy(renaming));
//        }
//        return new SDT(acc, newRegs, newChildren);
//        
//        }
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

        log.log(Level.FINEST,thisRegisters.toString() + " vs " + otherRegisters.toString());

        if (otherRegisters.isEmpty() && thisRegisters.isEmpty()) {
            log.log(Level.FINEST,"no regs anywhere");
            return true;
        } else {
            log.log(Level.FINEST,"regs");
            Boolean[] regEqArr = new Boolean[thisRegisters.size()];
            Integer i = 0;
            for (SymbolicDataValue thisReg : thisRegisters) { // if the trees have the same type and size
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

    // Returns true if this SDT can use the children of another SDT.
    private boolean chiCanUse(SDT other) {
        Map<SDTGuard, SDT> thisChildren = this.getChildren();
        Map<SDTGuard, SDT> otherChildren = other.getChildren();
        Boolean[] chiEqArr = new Boolean[thisChildren.keySet().size()];
        Integer i = 0;
        for (SDTGuard thisGuard : thisChildren.keySet()) {
            SDT thisBranch = thisChildren.get(thisGuard);
            chiEqArr[i] = false;
            for (SDTGuard otherGuard : otherChildren.keySet()) {
//                log.log(Level.FINEST,"comparing " + thisGuard.toString() + " to " + otherGuard.toString() + "...");    
                if (thisBranch.canUse(otherChildren.get(otherGuard))) {
                    chiEqArr[i] = true;
//                    log.log(Level.FINEST,"... OK");
                    break;
                }
            }
            i++;
        }
        return isArrayTrue(chiEqArr);
    }

    // Returns true if this SDT can use another SDT's registers and children, 
    // and if additionally they are either both rejecting and accepting.
    public boolean canUse(SDT other) {
        if (other instanceof SDTLeaf) { // trees with incompatible sizes can't use each other
            return false;
        } else {
            log.log(Level.FINEST,"no sdt leaf");
            boolean regEq = this.regCanUse((SDT) other);
            log.log(Level.FINEST,"regs " + this.getRegisters().toString() + ", " + other.getRegisters() + (regEq ? " eq." : " not eq."));
            boolean accEq = (this.isAccepting() == other.isAccepting());
//            log.log(Level.FINEST,accEq ? "acc eq." : "acc not eq.");
            log.log(Level.FINEST,"comparing children : \n" + this.getChildren().toString() + "\n and " + other.getChildren().toString());
            // both must use each other
            boolean chiEq = this.chiCanUse((SDT) other);
            //return regEq && accEq && chiEq;
            return accEq && chiEq;
        }
    }

//    public boolean isEquivalentUnder(SymbolicDecisionTree other) {
//        Map<Guard, SymbolicDecisionTree> otherChildren = other.getChildren();
//        
//        SymbolicDecisionTree otherMod = new SDT(otherSdt.isAccepting(), )
//        
//        return this.isEquivalent(otherMod);
//    }
    public boolean isEmpty() {
        return this.getChildren().isEmpty();
    }

    DataExpression<Boolean> getAcceptingPaths() {

        List<List<SDTGuard>> paths = getPaths(new ArrayList<SDTGuard>());
        if (paths.isEmpty()) {
            return DataExpression.FALSE;
        }
        Set<SuffixValue> svals = new HashSet<>();
        Expression<Boolean> dis = null;
        for (List<SDTGuard> list : paths) {
            List<Expression<Boolean>> expr = new ArrayList<>();
            for (SDTGuard g : list) {
                expr.add(g.toExpr());
                svals.add(g.getParameter());
            } 
            Expression<Boolean> con = ExpressionUtil.and(expr);
            dis = (dis == null) ? con : ExpressionUtil.or(dis, con);
        }
        
        Map<SymbolicDataValue, Variable> map = new HashMap<>();
        for (Register r : getRegisters()) {
            Variable x = new Variable(BuiltinTypes.SINT32, "x" + r.getId());
            map.put(r, x);
        }
        for (SuffixValue s : svals) {
            Variable p = new Variable(BuiltinTypes.SINT32, "y" + s.getId());
            map.put(s, p);
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
            } 
        else {
                SDT nxt = sdts.get(i);
                if (curr.getChildren().size() >= nxt.getChildren().size()) {
                    return findFinest(i, sdts, curr);
                }
                else {
                    return findFinest(i, sdts, nxt);
                }
        }
    }
    

}
