/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.oracles.mto;

import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.SDTGuard;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Sofia Cassel
 */
public class SDT implements SymbolicDecisionTree {
    
    private final Map<List<SDTGuard>, SDT> children;

    public SDT(Map<List<SDTGuard>, SDT> children) {
        this.children = children;
    }
    
    public Set<Register> getRegisters() {
        Set<Register> registers = new HashSet<>();
        for (Entry<List<SDTGuard>, SDT> e : children.entrySet()) {
            //TODO: do something to collect registers
        }
        return registers;
    }
    
    @Override
    public boolean isAccepting() {
        assert !this.children.isEmpty();
        for (SDT child : children.values()) {
            if (!child.isAccepting()) {
                return false;
            }
        }
       
        return true;
    }
    
    protected Map<List<SDTGuard>, SDT> getChildren() {
        return this.children;
    }

    @Override
    public boolean isEquivalent(SymbolicDecisionTree other, VarMapping renaming) {
        if (!(other instanceof SDT)) {
            return false;
        }
        SDT otherSDT = (SDT)other;
        return this.canUse(otherSDT) && otherSDT.canUse(this);
    }
    
    @Override
    public SymbolicDecisionTree relabel(VarMapping relabelling) {
        if (relabelling.isEmpty()) {
            return this;
        }
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        for (Entry<List<SDTGuard>, SDT> e : children.entrySet()) {
            List<SDTGuard> g = e.getKey();
            String gString = Arrays.toString(g.toArray());
            //TODO: replace lists of guards by guards
            if (gString.length() < 3) {
                gString = "[else]";
            }
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
            //System.out.println(maybeArr[c]);
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
    
    // Returns true if this SDT can use the children of another SDT.
    private boolean chiCanUse (SDT other) {
        Map<List<SDTGuard>, SDT> thisChildren  = this.getChildren();
        Map<List<SDTGuard>, SDT> otherChildren  = other.getChildren();
        Boolean[] chiEqArr = new Boolean[thisChildren.keySet().size()];
        Integer i = 0;
        for (List<SDTGuard> thisGuard : thisChildren.keySet()) {
            SDT thisBranch = thisChildren.get(thisGuard);
            chiEqArr[i] = false;
            for (List<SDTGuard> otherGuard : otherChildren.keySet()) {
//                System.out.println("comparing " + thisGuard.toString() + " to " + otherGuard.toString() + "...");    
                if (thisBranch.canUse(otherChildren.get(otherGuard))) {
                    chiEqArr[i] = true;
//                    System.out.println("... OK");
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
        }
        else {
            boolean regEq = this.regCanUse((SDT) other);
//            System.out.println("regs " + this.getRegisters().toString() + ", " + other.getRegisters() + (regEq ? " eq." : " not eq."));
            boolean accEq = (this.isAccepting() == other.isAccepting());
//            System.out.println(accEq ? "acc eq." : "acc not eq.");
            System.out.println("comparing children : " + this.getChildren().toString() + "\n and "+ other.getChildren().toString());
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
    
}
    
