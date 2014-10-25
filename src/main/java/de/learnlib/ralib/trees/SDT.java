/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.trees;

import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.Guard;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Sofia Cassel
 */
public class SDT extends SymbolicDecisionTree {
    
    public SDT(boolean accepting, Set<SymbolicDataValue> registers, Map<Guard, SymbolicDecisionTree> sdt) {
        super(accepting, registers, sdt);
    }
    
    // Method in progress.    
    @Override
    public SymbolicDecisionTree createCopy(VarMapping renaming) {
        boolean acc = this.isAccepting();
        // registers: add the ones that are mapped
        Set<SymbolicDataValue> newRegs = new HashSet<SymbolicDataValue>();
        for (SymbolicDataValue reg : (Set<SymbolicDataValue>) this.getRegisters()) {
            newRegs.add(renaming.get(reg));
        }
        // children (map from guards to trees): change the guards
        Map<Guard, SymbolicDecisionTree> newChildren = new HashMap<>();
        Map<Guard, SymbolicDecisionTree> currChildren = this.getChildren();
        for (Guard guard : (Set<Guard>) currChildren.keySet()) {
            newChildren.put(guard.createCopy(renaming), currChildren.get(guard).createCopy(renaming));
        }
        return new SDT(acc, newRegs, newChildren);
        
        }
    
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
        Set<SymbolicDataValue> otherRegisters = other.getRegisters();
        Set<SymbolicDataValue> thisRegisters = this.getRegisters();
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
        Map<Guard, SymbolicDecisionTree> thisChildren  = this.getChildren();
        Map<Guard, SymbolicDecisionTree> otherChildren  = other.getChildren();
        Boolean[] chiEqArr = new Boolean[thisChildren.keySet().size()];
        Integer i = 0;
        for (Guard thisGuard : thisChildren.keySet()) {
            SymbolicDecisionTree thisBranch = thisChildren.get(thisGuard);
            chiEqArr[i] = false;
            for (Guard otherGuard : otherChildren.keySet()) {
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
    @Override
    public boolean canUse(SymbolicDecisionTree other) {
        if (other instanceof SDTLeaf) { // trees with incompatible sizes can't use each other
            return false;
        }
        else {
            boolean regEq = this.regCanUse((SDT) other);
//            System.out.println("regs " + this.getRegisters().toString() + ", " + other.getRegisters() + (regEq ? " eq." : " not eq."));
            boolean accEq = (this.isAccepting() == other.isAccepting());
//            System.out.println(accEq ? "acc eq." : "acc not eq.");
//            System.out.println("comparing children : " + this.getChildren().keySet().toString() + "\n and "+ other.getChildren().keySet().toString());
            boolean chiEq = this.chiCanUse((SDT) other);
            return regEq && accEq && chiEq;
        }
    }
    
//    public boolean isEquivalentUnder(SymbolicDecisionTree other) {
//        Map<Guard, SymbolicDecisionTree> otherChildren = other.getChildren();
//        
//        SymbolicDecisionTree otherMod = new SDT(otherSdt.isAccepting(), )
//        
//        return this.isEquivalent(otherMod);
//    }
    
    
    private String spaces(int n) {
        String spaceStr = "";
        for (int j = 1; j < n; j++) {
            spaceStr = spaceStr + "    "; 
        }
        return spaceStr;
    }
    
    @Override
    public String toString() {
        return makeString(0);
    }
    
    public String makeString(int level) {
        Map<Guard,SymbolicDecisionTree> kids = this.getChildren();
        Set<SymbolicDataValue> thisRegisters = this.getRegisters();
        int numRegs = thisRegisters.size();
        String rootString = (this.isAccepting() ? "+" : "-") + 
                ", " + ((numRegs != 0) ? thisRegisters.toString() : "") 
                + " ...\n";
        String kidString = "";
        level++;
        for (Guard g : kids.keySet()) {
            int gl = g.getParameter().getId();
            SymbolicDecisionTree kidSdt = kids.get(g);
            kidString = kidString + spaces(gl) + g.toString() + " ==> ";
            if (kidSdt instanceof SDTLeaf) {
                kidString = kidString + (((SDTLeaf) kidSdt).toString()) + "\n";
            }
            else {
                if (!(kidSdt instanceof SDTLeaf)) {
                    kidString = kidString + ((SDT) kids.get(g)).makeString(level);
                }
            }
        }
        return (rootString + kidString);
    }
    
}
    
