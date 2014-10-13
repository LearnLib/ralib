/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.trees;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.Guard;
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
    
    @Override
    public SymbolicDecisionTree createCopy(VarMapping renaming) {
        return this;
    }
    
    @Override
    public boolean isEquivalent(SymbolicDecisionTree other) {
        return false;
        // not yet implemented properly
    }
}
    
